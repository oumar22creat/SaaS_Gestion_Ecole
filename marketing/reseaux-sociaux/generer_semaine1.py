# Visuels de la semaine 1 (publications 1 à 3 du PLAN.md), aux couleurs de School Manager.
# Formats : 1080x1080 (annonce) et 1080x1350 (4:5, Instagram et Facebook).
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

RACINE = Path(__file__).resolve().parents[2]
SORTIE = Path(__file__).resolve().parent / "visuels"
CAPTURES = RACINE / "marketing/play-store"
ICONE = CAPTURES / "icone-512.png"
POLICE = "/System/Library/Fonts/Avenir Next.ttc"
HAUT, BAS = (16, 94, 78), (9, 40, 33)
OR, CREME = (201, 162, 39), (238, 232, 218)

def police(t, gras=True):
    return ImageFont.truetype(POLICE, t, index=0 if gras else 5)

def fond(l, h):
    img = Image.new("RGB", (l, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        k = y / (h - 1)
        d.line([(0, y), (l, y)], fill=tuple(round(a + (b - a) * k) for a, b in zip(HAUT, BAS)))
    return img

def entete(img):
    # icône + nom en haut à gauche
    ic = Image.open(ICONE).convert("RGBA").resize((72, 72), Image.LANCZOS)
    m = Image.new("L", ic.size, 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, 71, 71], 16, fill=255)
    img.paste(ic, (60, 56), m)
    ImageDraw.Draw(img).text((150, 70), "School Manager", font=police(38), fill="white")

def telephone(capture, hauteur):
    # capture iPhone (fond vert déjà présent sur les fichiers Play) recadrée sur l'écran, coins arrondis
    im = Image.open(capture).convert("RGB")
    ecran = im.crop((60, 40, im.width - 60, im.height - 40))
    l = round(ecran.width * hauteur / ecran.height)
    ecran = ecran.resize((l, hauteur), Image.LANCZOS)
    cadre = Image.new("RGBA", (l + 24, hauteur + 24), (0, 0, 0, 0))
    ImageDraw.Draw(cadre).rounded_rectangle([0, 0, l + 23, hauteur + 23], 48, fill=(5, 18, 15))
    m = Image.new("L", ecran.size, 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, l - 1, hauteur - 1], 38, fill=255)
    cadre.paste(ecran, (12, 12), m)
    return cadre

def coller(img, cadre, x, y):
    ombre = Image.new("RGBA", cadre.size, (0, 0, 0, 0))
    ImageDraw.Draw(ombre).rounded_rectangle([0, 0, cadre.width - 1, cadre.height - 1], 48, fill=(0, 0, 0, 110))
    ombre = ombre.filter(ImageFilter.GaussianBlur(18))
    img.paste(ombre, (x + 10, y + 18), ombre)
    img.paste(cadre, (x, y), cadre)

def badge_app_store(img, x, y):
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([x, y, x + 300, y + 92], 18, fill="black", outline=(166, 166, 166), width=2)
    d.text((x + 28, y + 14), "Télécharger dans", font=police(22, False), fill="white")
    d.text((x + 28, y + 40), "l'App Store", font=police(38), fill="white")

def texte_centre(d, y, txt, f, l, couleur="white"):
    w = d.textlength(txt, font=f); d.text(((l - w) / 2, y), txt, font=f, fill=couleur)

# Publication 1 — annonce App Store, 1080x1080
img = fond(1080, 1080); entete(img); d = ImageDraw.Draw(img)
d.text((60, 190), "Disponible sur", font=police(64), fill="white")
d.text((60, 268), "l'App Store.", font=police(64), fill=OR)
d.text((60, 370), "Absences, notes, emploi du temps", font=police(32, False), fill=CREME)
d.text((60, 412), "et scolarité, sur votre téléphone.", font=police(32, False), fill=CREME)
badge_app_store(img, 60, 500)
d.text((60, 630), "Android : bientôt.", font=police(30, False), fill=CREME)
coller(img, telephone(CAPTURES / "capture-4-accueil.png", 860), 640, 140)
img.save(SORTIE / "p01-annonce-app-store-1080.png")

# Publication 2 — parents, absence le jour même, 1080x1350
img = fond(1080, 1350); entete(img); d = ImageDraw.Draw(img)
texte_centre(d, 190, "Vous le savez", police(72), 1080)
texte_centre(d, 276, "le jour même.", police(72), 1080, OR)
texte_centre(d, 380, "L'école saisit l'absence, vous êtes prévenu.", police(32, False), 1080, CREME)
coller(img, telephone(CAPTURES / "capture-4-accueil.png", 820), 335, 470)
img.save(SORTIE / "p02-absence-jour-meme-1350.png")

# Publication 3 — carrousel directeurs, bulletins, 3 x 1080x1350
img = fond(1080, 1350); entete(img); d = ImageDraw.Draw(img)
for i, l in enumerate(["Trois semaines", "pour sortir", "les bulletins ?"]):
    d.text((80, 430 + i * 110), l, font=police(96), fill=OR if i == 0 else "white")
d.text((80, 820), "Cahier, tableur, moyennes refaites à la main.", font=police(34, False), fill=CREME)
d.text((80, 1180), "Faites défiler  →", font=police(34), fill=OR)
img.save(SORTIE / "p03-bulletins-1-1350.png")

img = fond(1080, 1350); entete(img); d = ImageDraw.Draw(img)
texte_centre(d, 190, "Les notes saisies", police(64), 1080)
texte_centre(d, 268, "pendant le trimestre…", police(64), 1080, OR)
coller(img, telephone(CAPTURES / "capture-2-notes.png", 900), 330, 390)
img.save(SORTIE / "p03-bulletins-2-1350.png")

img = fond(1080, 1350); entete(img); d = ImageDraw.Draw(img)
for i, l in enumerate(["… alimentent", "directement", "le bulletin."]):
    d.text((80, 360 + i * 110), l, font=police(96), fill=OR if i == 2 else "white")
for i, l in enumerate(["Moyennes, coefficients, absences de la période.", "PDF à l'en-tête de votre école."]):
    d.text((80, 760 + i * 52), l, font=police(34, False), fill=CREME)
d.rounded_rectangle([80, 1000, 700, 1100], 50, fill=OR)
d.text((128, 1024), "Essai gratuit 30 jours", font=police(42), fill=(9, 40, 33))
d.text((80, 1150), "schoolmanager.tech", font=police(36, False), fill="white")
img.save(SORTIE / "p03-bulletins-3-1350.png")
print("ok")
