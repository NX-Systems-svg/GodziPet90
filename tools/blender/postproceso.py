#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Postproceso de las capas renderizadas con Blender (usa solo Pillow, sin numpy).

  python3 tools/blender/postproceso.py [ESCENARIO ...] [--prueba]

Para cada <ESCENARIO>/<momento>_<capa>.png en /tmp/claude-1000/godzi_render:
  * capa 0: halo del sol/luna + desenfoque ligero;  capa 1: desenfoque ligero respetando el alfa
  * capa 3: desvanecido opcional del borde superior (meta 'fade3') y relleno de seguridad del 74 % abajo
  * guarda WebP con alfa (calidad ~78, < 180 KB) en app/src/main/assets/escenas/
  * genera la previsualizacion preview_<ESCENARIO>_<momento>.png (recorte centrado 1.1:1)
"""
import os, sys, json, glob
from PIL import Image, ImageFilter, ImageDraw, ImageChops

TMP = '/tmp/claude-1000/godzi_render'
RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SAL = os.path.join(RAIZ, 'app', 'src', 'main', 'assets', 'escenas')
ESC = ['ISLA_ODO', 'BAHIA_TOKIO', 'GINZA', 'MONTE_FUJI', 'TORRE_TOKIO', 'OSAKA', 'KAMAKURA', 'ISLA_MONSTRUOS']
MAX_BYTES = 180 * 1024


def halo(img, uv, color, radio, fuerza):
    """Suma un resplandor suave alrededor de (u, v) a una imagen opaca."""
    w, h = img.size
    cx, cy = uv[0] * w, uv[1] * h
    capa = Image.new('RGB', img.size, (0, 0, 0))
    d = ImageDraw.Draw(capa)
    for k, f in ((1.0, 0.25), (0.6, 0.45), (0.3, 0.9)):
        r = radio * k
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=tuple(int(c * f * fuerza) for c in color))
    capa = capa.filter(ImageFilter.GaussianBlur(radio * 0.35))
    return ImageChops.add(img, capa)


def desenfocar_alfa(img, radio, bruma):
    """Desenfoca una imagen RGBA sin halos oscuros: rellena lo transparente con el color de bruma."""
    fondo = Image.new('RGBA', img.size, bruma + (255,))
    rgb = Image.alpha_composite(fondo, img).convert('RGB').filter(ImageFilter.GaussianBlur(radio))
    a = img.getchannel('A').filter(ImageFilter.GaussianBlur(radio))
    rgb.putalpha(a)
    return rgb


def desvanecer(img, v0, v1):
    """Alfa 0 en v0 (arriba) -> 1 en v1: funde el borde superior de la capa 3 con lo que hay detras."""
    w, h = img.size
    y0, y1 = int(v0 * h), int(v1 * h)
    mask = Image.new('L', (1, h), 255)
    for y in range(h):
        if y < y0:
            mask.putpixel((0, y), 0)
        elif y < y1:
            mask.putpixel((0, y), int(255 * (y - y0) / (y1 - y0)))
    mask = mask.resize((w, h))
    a = ImageChops.multiply(img.getchannel('A'), mask)
    img = img.copy(); img.putalpha(a)
    return img


def rellenar_suelo(img, v=0.74):
    """Seguridad: todo pixel por debajo del 74 % debe ser opaco."""
    w, h = img.size
    y0 = int(v * h) - 2
    inf = img.crop((0, y0, w, h))
    a = inf.getchannel('A')
    if a.getextrema()[0] == 255:
        return img, 0
    medio = inf.resize((1, 1), Image.BOX).getpixel((0, 0))[:3]
    fondo = Image.new('RGBA', inf.size, medio + (255,))
    out = img.copy()
    out.paste(Image.alpha_composite(fondo, inf), (0, y0))
    return out, 1


def guardar_webp(img, ruta, calidad=78):
    q = calidad
    while True:
        img.save(ruta, 'WEBP', quality=q, method=6)
        if os.path.getsize(ruta) <= MAX_BYTES or q <= 40:
            return os.path.getsize(ruta), q
        q -= 6


def procesar(esc, prueba=False):
    carpeta = os.path.join(TMP, esc)
    os.makedirs(os.path.join(SAL, esc), exist_ok=True)
    for momento in ('dia', 'noche'):
        mp = os.path.join(carpeta, 'meta_%s.json' % momento)
        if not os.path.exists(mp):
            continue
        meta = json.load(open(mp))
        capas = {}
        for l in range(5):
            p = os.path.join(carpeta, '%s_%d.png' % (momento, l))
            if not os.path.exists(p):
                continue
            img = Image.open(p).convert('RGBA')
            if prueba:
                img = img.resize((1536, 1024))
            if l == 0:
                rgb = img.convert('RGB')
                if meta.get('sol'):
                    rgb = halo(rgb, meta['sol'], (255, 236, 190), 170, 0.85)
                if meta.get('luna'):
                    rgb = halo(rgb, meta['luna'], (150, 175, 255), 130, 0.55)
                img = rgb.filter(ImageFilter.GaussianBlur(1.2)).convert('RGBA')
            elif l == 1:
                img = desenfocar_alfa(img, 1.6, (211, 227, 238) if momento == 'dia' else (29, 44, 85))
            elif l == 3:
                if meta.get('fade3'):
                    img = desvanecer(img, *meta['fade3'])
                img, fix = rellenar_suelo(img)
                if fix:
                    print('  aviso: capa 3 con huecos bajo el 74%%, rellenada (%s %s)' % (esc, momento))
            elif l == 4:
                img = desenfocar_alfa(img, 0.8, (60, 70, 50))
            capas[l] = img
            if l == 0:
                img = img.convert('RGB')
            if not prueba:
                tam, q = guardar_webp(img, os.path.join(SAL, esc, '%s_%d.webp' % (momento, l)))
                print('  %s_%d.webp %d KB q%d' % (momento, l, tam // 1024, q))
        if capas:
            comp = Image.new('RGBA', (1536, 1024), (0, 0, 0, 255))
            for l in sorted(capas):
                comp = Image.alpha_composite(comp, capas[l])
            ancho = int(1024 * 1.1)
            x0 = (1536 - ancho) // 2
            comp.convert('RGB').crop((x0, 0, x0 + ancho, 1024)).save(os.path.join(TMP, 'preview_%s_%s.png' % (esc, momento)))


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    for e in (args or ESC):
        print(e)
        procesar(e, '--prueba' in sys.argv)
