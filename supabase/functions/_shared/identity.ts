import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

export interface ResolvedIdentity {
  userId: string;
  profileId: string;
  role: "CUSTOMER" | "PARTNER" | "ADMIN";
  isAdmin: boolean;
  isProd: boolean;
}

function normalizeRole(role?: string | null): "CUSTOMER" | "PARTNER" | "ADMIN" {
  if (!role) return "CUSTOMER";
  const upper = role.toUpperCase();
  if (upper === "PROFESSIONAL" || upper === "PROVIDER" || upper === "PARTNER") {
    return "PARTNER";
  }
  if (upper === "ADMIN") {
    return "ADMIN";
  }
  return "CUSTOMER";
}

/**
 * Resolves the authenticated user's verified identity and profile ID.
 * Never trusts client-supplied user_metadata for role or profile_id.
 */
export async function resolveIdentity(
  req: Request,
  supabaseClient: SupabaseClient
): Promise<ResolvedIdentity> {
  const authHeader = req.headers.get("Authorization") ?? "";
  const jwt = authHeader.replace("Bearer ", "").trim();

  // 1. Staging / Debug fallback (Allowed unless explicitly set to production)
  const isProd = Deno.env.get("ENVIRONMENT") === "production";
  const devProfileId = req.headers.get("X-Dev-Profile-Id");
  const devRole = req.headers.get("X-Dev-User-Role");

  if (!isProd && devProfileId) {
    const roleUpper = normalizeRole(devRole);
    return {
      userId: devProfileId,
      profileId: devProfileId,
      role: roleUpper,
      isAdmin: roleUpper === "ADMIN",
      isProd: false,
    };
  }

  // 2. Production Auth Verification
  if (!jwt) {
    throw new Error("Missing authorization token");
  }

  const { data: { user }, error: authError } = await supabaseClient.auth.getUser(jwt);
  if (authError || !user) {
    throw new Error("Unauthorized");
  }

  // Role comes ONLY from app_metadata.role
  const normRole = normalizeRole(user.app_metadata?.role);
  const isAdmin = normRole === "ADMIN";

  // Lookup profile from user_profiles by auth_user_id
  const { data: profile } = await supabaseClient
    .from("user_profiles")
    .select("id, role")
    .eq("auth_user_id", user.id)
    .single();

  const profileId = profile?.id || user.id;

  return {
    userId: user.id,
    profileId,
    role: normRole,
    isAdmin,
    isProd: true,
  };
}
