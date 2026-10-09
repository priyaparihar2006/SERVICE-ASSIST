import re

# 1. Fix PartnerBookingsScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\PartnerBookingsScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'// Dynamic theme colors[^\n]*\n(?:private val [^\n]+\n)+', '', c)
with open(r"app\src\main\java\com\example\ui\screens\PartnerBookingsScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

# 2. Fix PartnerEarningsScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\PartnerEarningsScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'(?:private val ServoraTheme[^\n]+\n)+', '', c)
c = re.sub(r'(?:private val Partner[^\n]+\n)+', '', c)
with open(r"app\src\main\java\com\example\ui\screens\PartnerEarningsScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

# 3. Fix PartnerJobsScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\PartnerJobsScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'// Modern Brand Theme Palette[^\n]*\n', '', c)
c = re.sub(r'(?:private val [^\n]+\n)+', '', c)
with open(r"app\src\main\java\com\example\ui\screens\PartnerJobsScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

# 4. Fix PartnerSettlementHistoryScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\PartnerSettlementHistoryScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'(?:private val ServoraTheme[^\n]+\n)+', '', c)
c = re.sub(r'(?:private val Partner[^\n]+\n)+', '', c)
with open(r"app\src\main\java\com\example\ui\screens\PartnerSettlementHistoryScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

# 5. Fix PaymentCollectionScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\PaymentCollectionScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'// Dynamic theme colors\n', '', c)
# Add top-level fallbacks if referenced
top_defs = """
private val BrandDarkGreen = Color(0xFF0F5132)
private val BrandMintBg = Color(0xFFEEF9F3)
"""
c = top_defs + c
with open(r"app\src\main\java\com\example\ui\screens\PaymentCollectionScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

# 6. Fix ServiceDetailScreen.kt
with open(r"app\src\main\java\com\example\ui\screens\ServiceDetailScreen.kt", "r", encoding="utf-8") as f:
    c = f.read()
c = re.sub(r'// Light Green & White Theme Palette\n// Dynamic brand theme colors\n+', '', c)
c = """
private val brandDarkGreen = Color(0xFF0F5132)
private val brandMintBg = Color(0xFFEEF9F3)
private val brandMintSubtle = Color(0xFFF2FAF5)
""" + c
with open(r"app\src\main\java\com\example\ui\screens\ServiceDetailScreen.kt", "w", encoding="utf-8") as f:
    f.write(c)

print("Files fixed cleanly.")
