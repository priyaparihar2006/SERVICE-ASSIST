import os
import re

partner_files = [
    r"app\src\main\java\com\example\ui\screens\PartnerBookingsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PaymentCollectionScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerJobDetailScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerEarningsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerJobsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerSettlementHistoryScreen.kt",
    r"app\src\main\java\com\example\ui\screens\ServiceDetailScreen.kt"
]

for p in partner_files:
    if not os.path.exists(p):
        continue
    with open(p, "r", encoding="utf-8") as f:
        content = f.read()

    # Replace hardcoded brand green constants with ServoraTheme colors
    if "PartnerBookingsScreen.kt" in p:
        content = content.replace("if (isDark) ServoraTheme.colors.success else BrandGreen", "ServoraTheme.colors.primary")
        content = content.replace("if (isDark) ServoraTheme.colors.brandGradientStart else BrandGreen", "ServoraTheme.colors.brandGradientStart")
        content = content.replace("Color(0xFFCCEBDC)", "ServoraTheme.colors.primaryContainer")
        content = content.replace("Color(0xFFE6F7EF)", "ServoraTheme.colors.primaryContainer.copy(alpha = 0.5f)")
        content = content.replace("private val BrandGreen = Color(0xFF009051)", "// Dynamic theme colors")

    if "PaymentCollectionScreen.kt" in p:
        content = content.replace("if (isDark) ServoraTheme.colors.success else BrandGreen", "ServoraTheme.colors.primary")
        content = content.replace("private val BrandGreen = Color(0xFF009051)", "// Dynamic theme colors")
        content = content.replace("private val BrandDarkGreen = Color(0xFF0F5132)", "")
        content = content.replace("private val BrandMintBg = Color(0xFFEEF9F3)", "")

    if "PartnerJobDetailScreen.kt" in p:
        content = content.replace("if (isDark) colors.success else Color(0xFF009051)", "colors.primary")
        content = content.replace("Color(0xFF009051)", "colors.primary")
        content = content.replace("Color(0xFF0B5433)", "colors.onPrimaryContainer")

    if "PartnerEarningsScreen.kt" in p:
        content = content.replace("PartnerRevenueGreen", "ServoraTheme.colors.primary")
        content = content.replace("PartnerRevenueDarkGreen", "ServoraTheme.colors.onPrimaryContainer")
        content = content.replace("PartnerBorderColor", "ServoraTheme.colors.primaryContainer")
        content = content.replace("private val PartnerRevenueGreen = Color(0xFF009051) // theme-invariant", "")
        content = content.replace("private val PartnerRevenueDarkGreen = Color(0xFF0B5433) // theme-invariant", "")
        content = content.replace("private val PartnerBorderColor = Color(0xFFCCEBDC) // theme-invariant", "")

    if "PartnerJobsScreen.kt" in p:
        content = content.replace("brandEmeraldPrimary", "ServoraTheme.colors.primary")
        content = content.replace("brandEmeraldDark", "ServoraTheme.colors.onPrimaryContainer")
        content = content.replace("Color(0xFF009051)", "ServoraTheme.colors.primary")
        content = content.replace("Color(0xFF0B5433)", "ServoraTheme.colors.onPrimaryContainer")
        content = content.replace("private val brandEmeraldPrimary = Color(0xFF009051) // theme-invariant", "")
        content = content.replace("private val brandEmeraldDark = Color(0xFF0B5433) // theme-invariant", "")

    if "PartnerSettlementHistoryScreen.kt" in p:
        content = content.replace("PartnerRevenueGreen", "ServoraTheme.colors.primary")
        content = content.replace("PartnerRevenueDarkGreen", "ServoraTheme.colors.onPrimaryContainer")
        content = content.replace("PartnerBorderColor", "ServoraTheme.colors.primaryContainer")
        content = content.replace("private val PartnerRevenueGreen = Color(0xFF009051)", "")
        content = content.replace("private val PartnerRevenueDarkGreen = Color(0xFF0B5433)", "")
        content = content.replace("private val PartnerBorderColor = Color(0xFFCCEBDC)", "")

    if "ServiceDetailScreen.kt" in p:
        content = content.replace("private val brandDarkGreen = Color(0xFF0F5132) // theme-invariant", "")
        content = content.replace("private val brandMintBg = Color(0xFFEEF9F3)", "")
        content = content.replace("private val brandMintSubtle = Color(0xFFF2FAF5)", "")

    with open(p, "w", encoding="utf-8") as f:
        f.write(content)

print("Partner & payment screens color updates applied.")
