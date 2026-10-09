import re

files_and_defs = [
    (r"app\src\main\java\com\example\ui\screens\PartnerBookingsScreen.kt", """
private val BrandDarkGreen = Color(0xFF0B5433)
private val BrandMintBg = Color(0xFFE6F5EE)
private val BrandBorder = Color(0xFFCCEBDC)
"""),
    (r"app\src\main\java\com\example\ui\screens\PartnerEarningsScreen.kt", """
private val PartnerMintBg = Color(0xFFE6F5EE)
private val PartnerCardBg = Color(0xFFFAFCFA)
"""),
    (r"app\src\main\java\com\example\ui\screens\PartnerJobsScreen.kt", """
private val brandEmeraldSoft = Color(0xFFE6F5EE)
private val brandAmber = Color(0xFFF59E0B)
"""),
    (r"app\src\main\java\com\example\ui\screens\PartnerSettlementHistoryScreen.kt", """
private val PartnerMintBg = Color(0xFFE6F5EE)
""")
]

for filename, consts in files_and_defs:
    with open(filename, "r", encoding="utf-8") as f:
        c = f.read()
    # Find last import
    match = re.search(r'(import [^\n]+\n)(?!\s*import)', c)
    if match:
        idx = match.end()
        c = c[:idx] + consts + c[idx:]
    with open(filename, "w", encoding="utf-8") as f:
        f.write(c)

print("Added required top-level color definitions.")
