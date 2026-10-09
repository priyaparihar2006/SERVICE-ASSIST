import os
import re

files_to_fix = [
    r"app\src\main\java\com\example\ui\screens\ServiceDetailScreen.kt",
    r"app\src\main\java\com\example\ui\screens\HomeScreen.kt",
    r"app\src\main\java\com\example\ui\screens\BookingConfirmationScreen.kt",
    r"app\src\main\java\com\example\ui\screens\BookingFlowScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerJobsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerBookingsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerJobDetailScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerEarningsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PaymentCollectionScreen.kt",
    r"app\src\main\java\com\example\ui\screens\NotificationsScreen.kt",
    r"app\src\main\java\com\example\ui\screens\OffersScreen.kt",
    r"app\src\main\java\com\example\ui\screens\ProfileScreen.kt",
    r"app\src\main\java\com\example\ui\screens\LoginScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerToolkitScreen.kt",
    r"app\src\main\java\com\example\ui\screens\PartnerSettlementHistoryScreen.kt",
    r"app\src\main\java\com\example\ui\screens\AdminDashboardScreen.kt",
    r"app\src\main\java\com\example\ui\screens\ChatThreadScreen.kt",
    r"app\src\main\java\com\example\ui\components\FloatingCartBar.kt"
]

for file_rel in files_to_fix:
    if not os.path.exists(file_rel):
        continue
    with open(file_rel, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Replace `if (isDark) ServoraTheme.colors.primary else BrandGreen` or similar with `ServoraTheme.colors.primary`
    content = re.sub(r'if\s*\(\s*isDark\s*\)\s*ServoraTheme\.colors\.primary\s*else\s*(?:BrandGreen|brandGreen|EmeraldGreen|emeraldGreen|brandEmeraldPrimary)', 'ServoraTheme.colors.primary', content)
    content = re.sub(r'if\s*\(\s*isDark\s*\)\s*ServoraTheme\.colors\.primary\s*else\s*Color\(0xFF009051\)', 'ServoraTheme.colors.primary', content)

    # 2. In ServiceDetailScreen.kt
    if "ServiceDetailScreen.kt" in file_rel:
        content = re.sub(r'private val brandGreen = Color\(0xFF009051\)[^\n]*', '// Dynamic brand theme colors', content)
        content = re.sub(r'val primaryColor = if \(isDark\) ServoraTheme\.colors\.primary else brandGreen', 'val primaryColor = ServoraTheme.colors.primary\n    val brandMintBg = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer.copy(alpha = 0.5f)\n    val brandDarkGreen = if (isDark) ServoraTheme.colors.textPrimary else ServoraTheme.colors.onPrimaryContainer', content)
        content = re.sub(r'val primaryColor = if \(isDark\) ServoraTheme\.colors\.primary else brandGreen', 'val primaryColor = ServoraTheme.colors.primary', content)
        content = re.sub(r'listOf\(brandDarkGreen, brandGreen\)', 'listOf(ServoraTheme.colors.primary, ServoraTheme.colors.primary)', content)

    # 3. In HomeScreen.kt
    if "HomeScreen.kt" in file_rel:
        content = re.sub(r'val emeraldGreen = if \(isDark\) ServoraTheme\.colors\.primary else BrandGreen', 'val emeraldGreen = ServoraTheme.colors.primary', content)
        content = re.sub(r'val brandGreen = if \(isDark\) ServoraTheme\.colors\.primary else BrandGreen', 'val brandGreen = ServoraTheme.colors.primary', content)
        content = re.sub(r'val brandLightGreen = if \(isDark\) ServoraTheme\.colors\.surfaceVariant else BrandLightGreen', 'val brandLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer', content)
        content = re.sub(r'val brandVeryLightGreen = if \(isDark\) ServoraTheme\.colors\.surfaceVariant else BrandVeryLightGreen', 'val brandVeryLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer.copy(alpha = 0.45f)', content)
        content = re.sub(r'val pillText = if \(isDark\) brandGreen else Color\(0xFF1B633D\)', 'val pillText = if (isDark) brandGreen else ServoraTheme.colors.onPrimaryContainer', content)
        content = re.sub(r'val pillBg = if \(isDark\) surfaceVariant else Color\(0xFFEAF7F0\)', 'val pillBg = if (isDark) surfaceVariant else ServoraTheme.colors.primaryContainer.copy(alpha = 0.45f)', content)
        content = re.sub(r'val circleIconBg = if \(isDark\) surfaceVariant else Color\(0xFFE2F7ED\)', 'val circleIconBg = if (isDark) surfaceVariant else ServoraTheme.colors.primaryContainer.copy(alpha = 0.5f)', content)
        content = re.sub(r'val imgContainerBg = if \(isDark\) ServoraTheme\.colors\.surfaceVariant else Color\(0xFFEBF5F0\)', 'val imgContainerBg = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer.copy(alpha = 0.45f)', content)

    with open(file_rel, "w", encoding="utf-8") as f:
        f.write(content)

print("Applied theme dynamic color updates successfully.")
