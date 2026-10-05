"""Icône et écran de lancement, dessinés à partir des couleurs de l'application.

Produit les cinq images sources de ce dossier. Les déclinaisons par plateforme s'en déduisent
ensuite :

    python3 assets/dessiner-la-marque.py
    npx @capacitor/assets generate \
        --iconBackgroundColor '#0b231e' --iconBackgroundColorDark '#0b231e' \
        --splashBackgroundColor '#eef1f0' --splashBackgroundColorDark '#0b231e'

Le tracé est programmatique plutôt qu'un fichier de dessin : la marque se réduit à quelques
polygones, et la garder en code permet de la régénérer après un changement de palette sans
rouvrir d'outil graphique.

Tracé à quatre fois la taille finale puis réduit : PIL ne lisse pas les polygones, et un bord
en escalier se voit immédiatement sur une icône de téléphone.

La marque est dessinée sur un calque transparent, puis recadrée sur son contenu réel avant
d'être centrée. Centrer sur les coordonnées du plateau ne suffit pas : la calotte descend et
le gland déborde à droite, si bien que l'ensemble paraissait décalé vers le haut et la gauche.
"""
from PIL import Image, ImageDraw

VERT_FONCE = (11, 35, 30)      # --color-nav
VERT = (15, 92, 76)            # --tenant-primary
OR = (201, 162, 39)            # --tenant-secondary
CANEVAS = (238, 241, 240)      # --color-canvas
BLANC = (255, 255, 255)


def calque_marque(cote, couleur, gland):
    """Une toque de diplômé sur fond transparent : plateau, calotte, gland."""
    img = Image.new("RGBA", (cote, cote), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = cote / 2
    L = cote * 0.34
    plateau = [(cx, cy - 0.46 * L), (cx + L, cy), (cx, cy + 0.46 * L), (cx - L, cy)]
    calotte = [
        (cx - 0.52 * L, cy + 0.19 * L),
        (cx + 0.52 * L, cy + 0.19 * L),
        (cx + 0.46 * L, cy + 0.62 * L),
        (cx, cy + 0.78 * L),
        (cx - 0.46 * L, cy + 0.62 * L),
    ]
    d.polygon(calotte, fill=couleur)
    d.polygon(plateau, fill=couleur)
    epais = max(2, int(0.055 * L))
    d.line([(cx + L, cy), (cx + L, cy + 0.50 * L)], fill=gland, width=epais)
    r = 0.095 * L
    d.ellipse([cx + L - r, cy + 0.50 * L, cx + L + r, cy + 0.50 * L + 2 * r], fill=gland)
    return img.crop(img.getbbox())


def composer(cote, fond, couleur, gland, part):
    """Pose la marque, mise à l'échelle pour occuper `part` de la largeur, au centre exact."""
    grand = cote * 4
    img = (fond(grand) if callable(fond) else Image.new("RGBA", (grand, grand), fond + (255,))) \
        if fond is not None else Image.new("RGBA", (grand, grand), (0, 0, 0, 0))
    marque = calque_marque(grand, couleur, gland)
    largeur = int(grand * part)
    hauteur = round(marque.height * largeur / marque.width)
    marque = marque.resize((largeur, hauteur), Image.LANCZOS)
    img.alpha_composite(marque, ((grand - largeur) // 2, (grand - hauteur) // 2))
    return img.resize((cote, cote), Image.LANCZOS)


def degrade(cote):
    bande = Image.new("RGB", (1, cote))
    d = ImageDraw.Draw(bande)
    for y in range(cote):
        t = y / (cote - 1)
        d.point((0, y), fill=tuple(round(VERT[i] + (VERT_FONCE[i] - VERT[i]) * t) for i in range(3)))
    return bande.resize((cote, cote)).convert("RGBA")


composer(1024, degrade, BLANC, OR, 0.62).save("assets/icon.png")
# Le calque de premier plan est plein cadre : l'icône adaptative générée l'insère elle-même
# avec un retrait de 16,7 %, qui le ramène dans la zone sûre. Une marge ajoutée ici serait
# donc appliquée deux fois — la toque se retrouvait minuscule au milieu de son cercle.
composer(1024, None, BLANC, OR, 0.78).save("assets/icon-foreground.png")
Image.new("RGB", (1024, 1024), VERT_FONCE).save("assets/icon-background.png")
# Fond clair, celui du corps de l'application : un écran de lancement sombre suivi d'une
# interface claire produit un éclair au démarrage. L'écran est recadré selon l'appareil, d'où
# une marque modeste — seul le centre est garanti visible.
composer(2732, CANEVAS, VERT, OR, 0.22).convert("RGB").save("assets/splash.png")
composer(2732, VERT_FONCE, BLANC, OR, 0.22).convert("RGB").save("assets/splash-dark.png")
for f in ("icon", "icon-foreground", "icon-background", "splash", "splash-dark"):
    print("  écrit : assets/%s.png" % f)
