# Génère les ressources images du site vitrine (landing/assets/) à partir des sources du dépôt :
# photos CC0 (marketing/site-vitrine/photos/), captures et icône de la fiche store
# (marketing/play-store/). Voir docs/refonte-site/SPEC.md §8.
#
# Usage : python3 marketing/site-vitrine/generer.py   (Pillow 11+ avec AVIF et WebP)
# Le script est idempotent : il écrase les fichiers produits à chaque exécution.
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, features

RACINE = Path(__file__).resolve().parents[2]
PHOTOS_SRC = Path(__file__).resolve().parent / "photos"
STORE = RACINE / "marketing/play-store"
ASSETS = RACINE / "landing/assets"
FONTS = ASSETS / "fonts"

AVIF = features.check("avif")


def sortie(*parties):
    chemin = ASSETS.joinpath(*parties)
    chemin.parent.mkdir(parents=True, exist_ok=True)
    return chemin


# ---------------------------------------------------------------- Photos (SPEC §8.1)
# Trois largeurs ; le 1200 est un redimensionnement de la source 1280 (jamais d'agrandissement).
PHOTOS = {
    "classe": "hero-classe.jpg",
    "eleves-lecture": "scolarite.jpg",
    "cahiers": "bulletins.jpg",
}
for nom, fichier in PHOTOS.items():
    source = Image.open(PHOTOS_SRC / fichier).convert("RGB")
    for largeur in (480, 800, 1200):
        hauteur = round(source.height * largeur / source.width)
        img = source.resize((largeur, hauteur), Image.LANCZOS)
        img.save(sortie("photos", f"{nom}-{largeur}.jpg"), "JPEG", quality=78, progressive=True, optimize=True)
        img.save(sortie("photos", f"{nom}-{largeur}.webp"), "WEBP", quality=75, method=6)
        if AVIF:
            img.save(sortie("photos", f"{nom}-{largeur}.avif"), "AVIF", quality=50, speed=4)

# -------------------------------------------------------------- Captures (SPEC §8.2)
# Aucun recadrage : c'est le cadre CSS (aspect-ratio + object-fit) qui recadre.
CAPTURES = {
    "appel": "capture-1-feuille-appel.png",
    "notes": "capture-2-notes.png",
    "emploi-du-temps": "capture-3-emploi-du-temps.png",
    "accueil": "capture-4-accueil.png",
    "connexion": "capture-5-connexion.png",
}
for nom, fichier in CAPTURES.items():
    source = Image.open(STORE / fichier).convert("RGB")
    for largeur, hauteur in ((540, 960), (360, 640)):
        source.resize((largeur, hauteur), Image.LANCZOS).save(
            sortie("captures", f"{nom}-{largeur}.webp"), "WEBP", quality=80, method=6
        )

# ------------------------------------------------------- Marque et favicons (SPEC §8.5)
icone = Image.open(STORE / "icone-512.png").convert("RGBA")
icone.resize((32, 32), Image.LANCZOS).save(sortie("brand", "favicon-32.png"), optimize=True)
icone.resize((72, 72), Image.LANCZOS).save(sortie("brand", "icone-72.png"), optimize=True)
# apple-touch-icon opaque : iOS remplit la transparence en noir.
icone.convert("RGB").resize((180, 180), Image.LANCZOS).save(RACINE / "landing/apple-touch-icon.png", optimize=True)
icone.save(RACINE / "landing/favicon.ico", sizes=[(32, 32)])

# --------------------------------------------------------- Image de partage (SPEC §8.5)
L, H, MARGE = 1200, 630, 72
NUIT, TEXTE, TEXTE_2, OR = (11, 35, 30), (241, 245, 243), (185, 204, 197), (201, 162, 39)
og = Image.new("RGB", (L, H), NUIT)
d = ImageDraw.Draw(og)

# Capture à droite, 300 px de large, coins arrondis en haut, coupée par le bas de l'image.
cap = Image.open(STORE / "capture-1-feuille-appel.png").convert("RGB")
cap = cap.resize((300, round(cap.height * 300 / cap.width)), Image.LANCZOS)
# Départ assez bas pour que la capture (533 px de haut) déborde du bas de l'image.
cap_x, cap_y = L - MARGE - 300, 140
cap = cap.crop((0, 0, 300, H - cap_y))
masque = Image.new("L", cap.size, 0)
ImageDraw.Draw(masque).rounded_rectangle((0, 0, 300, cap.height + 40), radius=24, fill=255)
og.paste(cap, (cap_x, cap_y), masque)

# Icône 140 px en haut à gauche.
ico = icone.resize((140, 140), Image.LANCZOS)
masque_ico = Image.new("L", (140, 140), 0)
ImageDraw.Draw(masque_ico).rounded_rectangle((0, 0, 139, 139), radius=32, fill=255)
og.paste(ico.convert("RGB"), (MARGE, MARGE), masque_ico)

titre = ImageFont.truetype(str(FONTS / "outfit-600.woff2"), 72)
sous = ImageFont.truetype(str(FONTS / "inter-400.woff2"), 34)
y_titre = MARGE + 140 + 56
d.text((MARGE, y_titre), "School Manager", font=titre, fill=TEXTE)
y_filet = y_titre + 72 + 26
d.rectangle((MARGE, y_filet, MARGE + 96, y_filet + 6), fill=OR)
# Sous-titre sur deux lignes pour rester à gauche de la capture.
y_sous = y_filet + 36
for i, ligne in enumerate(("La gestion scolaire des écoles", "privées du Mali")):
    d.text((MARGE, y_sous + i * 46), ligne, font=sous, fill=TEXTE_2)
og.save(sortie("og", "school-manager-1200x630.jpg"), "JPEG", quality=82, progressive=True, optimize=True)

print("Ressources générées dans", ASSETS, "(AVIF :", "oui" if AVIF else "non", ")")
