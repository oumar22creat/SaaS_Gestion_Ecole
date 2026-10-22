# Génère l'icône (512x512) et le bandeau (1024x500) de la fiche Google Play
# à partir de l'icône iOS publiée, pour garder une identité identique sur les deux stores.
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

RACINE = Path(__file__).resolve().parents[2]
SORTIE = Path(__file__).resolve().parent
ICONE_IOS = RACINE / "mobile/ios/App/App/Assets.xcassets/AppIcon.appiconset/AppIcon-512@2x.png"
POLICE = "/System/Library/Fonts/Avenir Next.ttc"

HAUT, BAS = (16, 94, 78), (9, 40, 33)   # dégradé vert de l'icône
OR = (201, 162, 39)
CREME = (238, 232, 218)

def degrade(l, h):
    img = Image.new("RGB", (l, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (l, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(HAUT, BAS)))
    return img

# Icône : PNG 32 bits exigé par Play (canal alpha présent, entièrement opaque)
icone = Image.open(ICONE_IOS).convert("RGBA").resize((512, 512), Image.LANCZOS)
icone.save(SORTIE / "icone-512.png")

# Bandeau : la toque à gauche, le nom et la promesse à droite
l, h = 1024, 500
bandeau = degrade(l, h)
# On découpe la toque de l'icône (zone centrale) pour la poser sur le bandeau
toque = Image.open(ICONE_IOS).convert("RGB").crop((170, 300, 850, 720)).resize((280, 173), Image.LANCZOS)
masque = Image.new("L", toque.size, 0)
px, pm = toque.load(), masque.load()
for x in range(toque.size[0]):
    for y in range(toque.size[1]):
        r, g, b = px[x, y]
        # garde le blanc de la toque et l'or du gland, laisse passer le fond vert
        if (r > 200 and g > 200 and b > 200) or (r > 150 and g > 120 and b < 90):
            pm[x, y] = 255
bandeau.paste(toque, (80, 152), masque)

d = ImageDraw.Draw(bandeau)
# Taille du titre ajustée pour tenir entre x=410 et la marge droite de 60 px
X, MARGE = 410, 60
taille = 76
while ImageFont.truetype(POLICE, taille, index=0).getlength("School Manager") > l - X - MARGE:
    taille -= 2
titre = ImageFont.truetype(POLICE, taille, index=0)  # Avenir Next Bold
sous = ImageFont.truetype(POLICE, 32, index=5)        # Avenir Next Medium
d.text((X, 150), "School Manager", font=titre, fill="white")
d.rectangle([X + 2, 262, X + 82, 268], fill=OR)
d.text((X, 292), "L'école et la famille,", font=sous, fill=CREME)
d.text((X, 336), "dans le même téléphone.", font=sous, fill=CREME)
bandeau.save(SORTIE / "bandeau-1024x500.png")
print("ok")
