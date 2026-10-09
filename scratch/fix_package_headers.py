import re

for filename, consts in [
    (r"app\src\main\java\com\example\ui\screens\ServiceDetailScreen.kt", """
private val brandDarkGreen = Color(0xFF0F5132)
private val brandMintBg = Color(0xFFEEF9F3)
private val brandMintSubtle = Color(0xFFF2FAF5)
"""),
    (r"app\src\main\java\com\example\ui\screens\PaymentCollectionScreen.kt", """
private val BrandDarkGreen = Color(0xFF0F5132)
private val BrandMintBg = Color(0xFFEEF9F3)
""")
]:
    with open(filename, "r", encoding="utf-8") as f:
        c = f.read()
    # Remove any misplaced constants before package
    c = re.sub(r'^(?:\s*private val [^\n]+\n)+', '', c)
    # Find after imports
    match = re.search(r'(import [^\n]+\n)(?!\s*import)', c)
    if match:
        idx = match.end()
        c = c[:idx] + consts + c[idx:]
    with open(filename, "w", encoding="utf-8") as f:
        f.write(c)

print("Placed package and constants correctly.")
