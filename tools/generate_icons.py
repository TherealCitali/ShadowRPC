"""Generate Android branding from the supplied artwork. Requires Pillow.

The supplied source PNGs remain unmodified in Assets/. The square artwork is
flattened, so adaptive foregrounds use that full composition over a black backing
rather than inventing separated character/background layers.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
SQUARE = Image.open(ROOT / 'Assets/ShadowRPC-Icon-Square.png').convert('RGBA')
ROUND = Image.open(ROOT / 'Assets/ShadowRPC-Icon.png').convert('RGBA')
LANCZOS = Image.Resampling.LANCZOS
DENSITIES = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

# Purpose-built, single-colour fox-mask reduction for Android tinting. Eyes,
# forehead diamond and inner ears are negative space, not coloured details.
OUTLINE = [(34,30),(46,41),(54,40),(62,41),(74,30),(73,53),
           (78,51),(74,65),(65,75),(54,84),(43,75),(34,65),(30,51),(35,53)]
CUTOUTS = [
    [(37,37),(43,43),(37,49)], [(71,37),(65,43),(71,49)],
    [(54,47),(57,52),(54,57),(51,52)],
    [(38,57),(45,60),(49,66),(42,63)],
    [(70,57),(63,60),(59,66),(66,63)],
]

def polygon_path(points):
    return 'M' + 'L'.join(f'{x},{y}' for x,y in points) + 'Z'

path = ' '.join(polygon_path(p) for p in [OUTLINE, *CUTOUTS])
(RES / 'drawable/ic_launcher_monochrome.xml').write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <!-- Simplified fox mask for wallpaper tinting; full-colour art is unchanged. -->
    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd"
        android:pathData="{path}" />
</vector>
''')
mask = Image.new('L', (1080,1080))
draw = ImageDraw.Draw(mask)
for i, polygon in enumerate([OUTLINE, *CUTOUTS]):
    draw.polygon([(x*10,y*10) for x,y in polygon], fill=255 if i == 0 else 0)
glyph = mask.crop((280,280,800,860))
for density, scale in DENSITIES.items():
    folder = RES / f'mipmap-{density}'
    for name, source, size in [
        ('ic_launcher', SQUARE, round(48*scale)),
        ('ic_launcher_round', ROUND, round(48*scale)),
        ('ic_launcher_foreground', SQUARE, round(108*scale)),
    ]:
        source.resize((size,size), LANCZOS).save(folder / f'{name}.png', optimize=True)
    size = round(24*scale)
    alpha = Image.new('L', (size,size))
    fit = glyph.copy()
    fit.thumbnail((round(20*scale),round(20*scale)), LANCZOS)
    alpha.paste(fit, ((size-fit.width)//2,(size-fit.height)//2))
    icon = Image.new('RGBA',(size,size),'white')
    icon.putalpha(alpha)
    icon.save(RES / f'drawable-{density}/ic_stat_shadow.png', optimize=True)

# Transparent inset keeps the supplied round composition inside splash masking.
splash = Image.new('RGBA',(864,864))
splash.alpha_composite(ROUND.resize((560,560),LANCZOS),(152,152))
splash.save(RES / 'drawable-nodpi/ic_splash.png', optimize=True)
ROUND.resize((256,256),LANCZOS).save(RES / 'drawable-nodpi/notification_artwork.png', optimize=True)
SQUARE.convert('RGB').resize((512,512),LANCZOS).save(ROOT / 'assets/discord_app_icon_512.png', optimize=True)
print('Generated launcher, themed, notification, splash and Developer Portal icon assets.')
