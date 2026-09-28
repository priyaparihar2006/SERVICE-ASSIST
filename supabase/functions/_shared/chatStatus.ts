export async function recomputeBookingChatStatus(
  supabaseClient: any,
  bookingId: number | bigint
): Promise<string> {
  try {
    const { data: booking, error: bErr } = await supabaseClient
      .from("bookings")
      .select("id, status, customer_id, professional_id")
      .eq("id", bookingId)
      .maybeSingle();

    if (bErr || !booking) {
      return "ACTIVE";
    }

    const isBookingActive = booking.status !== "COMPLETED" && booking.status !== "CANCELLED";
    const targetStatus = isBookingActive ? "ACTIVE" : "READ_ONLY";
    const closesAt = isBookingActive ? null : new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();

    await supabaseClient
      .from("chat_conversations")
      .update({
        status: targetStatus,
        closes_at: closesAt,
      })
      .eq("booking_id", booking.id)
      .neq("status", "CLOSED");

    return targetStatus;
  } catch (err) {
    console.error("recomputeBookingChatStatus error:", err);
    return "ACTIVE";
  }
}

export async function recomputePairChatStatus(
  supabaseClient: any,
  customerId: string,
  partnerId: string
): Promise<string> {
  try {
    const { data: activeBookings } = await supabaseClient
      .from("bookings")
      .select("id, status")
      .eq("customer_id", customerId)
      .eq("professional_id", partnerId)
      .not("status", "in", '("COMPLETED","CANCELLED")')
      .order("created_at", { ascending: false });

    if (activeBookings && activeBookings.length > 0) {
      for (const b of activeBookings) {
        await recomputeBookingChatStatus(supabaseClient, b.id);
      }
      return "ACTIVE";
    }

    return "READ_ONLY";
  } catch (err) {
    console.error("recomputePairChatStatus error:", err);
    return "ACTIVE";
  }
}
