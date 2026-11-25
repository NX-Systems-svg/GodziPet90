# Generador de diagramas de flujo en SVG con simbología estándar (ANSI/ISO 5807):
# óvalo = inicio/fin, rectángulo = proceso, rombo = decisión, paralelogramo = entrada/salida.
import sys, html
OUT = sys.argv[1]
W_N, H_N = 230, 64          # tamaño base de nodo

def caja(n):
    x, y = n['x'], n['y']; w = n.get('w', W_N); h = n.get('h', H_N)
    if n['f'] == 'dec': h = n.get('h', 92); w = n.get('w', 250)
    return x - w/2, y - h/2, w, h

def borde(n, dx, dy):
    """Punto del borde del nodo en dirección (dx, dy) — aproximado por el rectángulo envolvente."""
    x0, y0, w, h = caja(n); cx, cy = n['x'], n['y']
    if dx == 0 and dy == 0: return cx, cy
    if n['f'] == 'dec':
        t = 1 / (abs(dx)/(w/2) + abs(dy)/(h/2))
    else:
        tx = (w/2)/abs(dx) if dx else 1e9; ty = (h/2)/abs(dy) if dy else 1e9; t = min(tx, ty)
    return cx + dx*t, cy + dy*t

def forma(n, colores):
    x0, y0, w, h = caja(n); f = n['f']; col = colores[f]
    s = f'stroke="#1F2A44" stroke-width="2.2" fill="{col}"'
    if f == 'ini':
        r = f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="{h/2}" {s}/>'
    elif f == 'proc':
        r = f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="4" {s}/>'
    elif f == 'pant':  # pantalla (diagrama de navegación)
        r = f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="12" {s}/>'
    elif f == 'dec':
        cx, cy = n['x'], n['y']
        r = f'<polygon points="{cx},{y0} {x0+w},{cy} {cx},{y0+h} {x0},{cy}" {s}/>'
    elif f == 'io':
        k = 18
        r = f'<polygon points="{x0+k},{y0} {x0+w},{y0} {x0+w-k},{y0+h} {x0},{y0+h}" {s}/>'
    lineas = n['t'].split('\n'); lh = 17
    y = n['y'] - (len(lineas)-1)*lh/2 + 5
    t = ''.join(f'<text x="{n["x"]}" y="{y+i*lh}" text-anchor="middle" font-family="Arial" font-size="14.5" fill="#111">{html.escape(l)}</text>' for i, l in enumerate(lineas))
    return r + t

def flecha(a, b, etiqueta=None, via=None):
    pts = [(a['x'], a['y'])] + (via or []) + [(b['x'], b['y'])]
    p0 = borde(a, pts[1][0]-pts[0][0], pts[1][1]-pts[0][1])
    p1 = borde(b, pts[-2][0]-pts[-1][0], pts[-2][1]-pts[-1][1])
    camino = [p0] + (via or []) + [p1]
    d = 'M ' + ' L '.join(f'{x:.1f},{y:.1f}' for x, y in camino)
    s = f'<path d="{d}" fill="none" stroke="#1F2A44" stroke-width="2" marker-end="url(#punta)"/>'
    if etiqueta:
        (x1, y1), (x2, y2) = camino[0], camino[1]
        lx, ly = (x1 + x2)/2, (y1 + y2)/2
        s += f'<rect x="{lx-16}" y="{ly-11}" width="32" height="19" rx="4" fill="#fff"/><text x="{lx}" y="{ly+4}" text-anchor="middle" font-family="Arial" font-size="13.5" font-weight="bold" fill="#C8102E">{etiqueta}</text>'
    return s

def svg(nodos, aristas, ancho, alto, titulo, colores=None):
    colores = colores or {'ini': '#BFE9C8', 'proc': '#DCEBFF', 'dec': '#FFE6A6', 'io': '#F6D3E6', 'pant': '#DCEBFF'}
    N = {n['id']: n for n in nodos}
    cuerpo = ''.join(flecha(N[a], N[b], *(e[2:3] or [None]), via=(e[3] if len(e) > 3 else None)) for e in aristas for a, b in [e[:2]])
    cuerpo += ''.join(forma(n, colores) for n in nodos)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{ancho}" height="{alto}" viewBox="0 0 {ancho} {alto}">
<defs><marker id="punta" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><polygon points="0,0 10,4 0,8" fill="#1F2A44"/></marker></defs>
<rect width="100%" height="100%" fill="#fff"/>
<text x="{ancho/2}" y="34" text-anchor="middle" font-family="Arial" font-size="21" font-weight="bold" fill="#0E1630">{html.escape(titulo)}</text>
{cuerpo}</svg>'''

def n(id, f, t, x, y, **k): d = dict(id=id, f=f, t=t, x=x, y=y); d.update(k); return d

# ---------------------------------------------------------------- 1. Alimentar
alim = [n('a','ini','Inicio',360,80), n('b','io','El jugador toca «Comer»',360,165),
        n('c','dec','¿Tiene comida en\nel inventario?',360,275), n('c2','proc','Ofrecer ir a la tienda',680,275),
        n('d','io','Elige la comida\nen la lista',360,390), n('e','io','Arrastra la comida\nhasta la boca',360,490),
        n('f','dec','¿Llegó a la boca?',360,600), n('f2','proc','La comida regresa\na su lugar',680,600),
        n('g','dec','¿Dormido o\nalimentación ≥ 95?',360,725), n('g2','io','Mensaje de error\n(no come)',680,725),
        n('h','proc','Consumir 1 unidad del inventario\ny sumar sus efectos (0-100)',360,845, w=300),
        n('i','proc','Contadores +1 · +5 XP\nanimación «¡ÑAM!»',360,950, w=260), n('j','proc','Guardar la partida (Room)',360,1045),
        n('k','ini','Fin',360,1135)]
alim_e = [('a','b'),('b','c'),('c','d','Sí'),('c','c2','No'),('d','e'),('e','f'),('f','g','Sí'),('f','f2','No'),('f2','e',None,[(680,490)]),
          ('g','h','No'),('g','g2','Sí'),('h','i'),('i','j'),('j','k'),('c2','k',None,[(830,275),(830,1135)]),('g2','k',None,[(680,1135)])]
# ---------------------------------------------------------------- 2. Comprar
comp = [n('a','ini','Inicio',360,80), n('b','io','Toca el precio de\nun artículo en la tienda',360,170, w=260),
        n('c','dec','¿Es el Destructor\nde Oxígeno?',360,285), n('c2','proc','Desbloquear secreto\n«no está a la venta»',690,285, w=240),
        n('d','dec','¿Nivel suficiente?',360,405), n('e','dec','¿Decoración que\nya tiene?',360,525),
        n('f','dec','¿Monedas ≥ precio?',360,645), n('x','io','Mensaje de error\n(compra bloqueada)',690,525, w=240),
        n('g','proc','Restar monedas y agregar\nal inventario (máx. 99)',360,765, w=280), n('h','proc','Sonido de monedas y\nguardar la partida',360,865, w=260),
        n('k','ini','Fin',360,960)]
comp_e = [('a','b'),('b','c'),('c','c2','Sí'),('c','d','No'),('d','e','Sí'),('d','x','No',[(690,405)]),('e','x','Sí'),('e','f','No'),
          ('f','g','Sí'),('f','x','No',[(690,645)]),('g','h'),('h','k'),('x','k',None,[(690,960)]),('c2','k',None,[(860,285),(860,960)])]
# ---------------------------------------------------------------- 3. Reclamar misión
mis = [n('a','ini','Inicio',360,80), n('b','io','Toca «Reclamar»\nen una misión',360,170),
       n('c','proc','Leer el progreso real\n(contadores de la partida)',360,275, w=270),
       n('d','dec','¿Progreso ≥ meta?',360,385), n('e','dec','¿Ya reclamada?\n(hoy / logro)',360,505),
       n('x','io','Mensaje de error\n(no se paga dos veces)',690,445, w=250),
       n('f','proc','Marcar reclamada\n(día o «LOGRO»)',360,625), n('g','proc','Sumar monedas y XP\n(+10 si nivel 6)',360,725),
       n('h','dec','¿Subió de nivel?',360,835), n('i','proc','Revisar evolución\n(ver diagrama 4)',690,835, w=220),
       n('j','proc','Guardar la partida',360,945), n('k','ini','Fin',360,1035)]
mis_e = [('a','b'),('b','c'),('c','d'),('d','e','Sí'),('d','x','No',[(690,385)]),('e','x','Sí',[(690,505)]),('e','f','No'),('f','g'),('g','h'),
         ('h','i','Sí'),('h','j','No'),('i','j',None,[(690,945)]),('j','k'),('x','k',None,[(860,445),(860,1035)])]
# ---------------------------------------------------------------- 4. Evolución
evo = [n('a','ini','Gana experiencia',380,80), n('b','proc','nivel = nivelPara(XP)',380,170),
       n('c','dec','¿Baby y nivel ≥ 2?',380,280), n('c2','proc','Evoluciona a\nLittle Godzilla',730,280),
       n('d','dec','¿Little y nivel ≥ 3?',380,400), n('d2','proc','Evoluciona a\nGodzilla Junior',730,400),
       n('e','dec','¿Junior y nivel ≥ 5?',380,520),
       n('f','dec','¿Bienestar\npromedio < 45?',380,645), n('f2','proc','Shin Godzilla',730,645),
       n('g','dec','¿Radiación\npromedio ≥ 72?',380,770), n('g2','proc','Burning Godzilla',730,770),
       n('h','dec','¿Entrenos ≥ 6 y\n2×entrenos ≥ cuidados?',380,895, w=280), n('h2','proc','Godzilla Heisei',730,895),
       n('i','proc','Godzilla Showa',380,1010), n('j','proc','Animación de evolución +\nguardar forma en Kaiju-pedia',380,1110, w=300),
       n('k','ini','Fin',380,1200)]
evo_e = [('a','b'),('b','c'),('c','c2','Sí'),('c','d','No'),('c2','d',None,[(730,340),(380,340)]),('d','d2','Sí'),('d','e','No'),('d2','e',None,[(730,460),(380,460)]),
         ('e','f','Sí'),('e','k','No',[(160,520),(160,1200)]),('f','f2','Sí'),('f','g','No'),('g','g2','Sí'),('g','h','No'),('h','h2','Sí'),('h','i','No'),('i','j'),
         ('f2','j',None,[(900,645),(900,1110)]),('g2','j',None,[(880,770),(880,1095)]),('h2','j',None,[(860,895),(860,1080)]),('j','k')]
# ---------------------------------------------------------------- 5. Radiación y fusión (mecánica exclusiva)
fus = [n('a','ini','Cada minuto de juego',380,80), n('b','proc','Radiación −3/h (dormido −1.5)\nBurning: +1/h',380,175, w=290),
       n('c','dec','¿Radiación ≥ 95 y\netapa Junior o final?',380,300, w=280), n('c2','proc','Medidor de fusión\nbaja (enfría)',730,300),
       n('d','proc','Medidor de fusión +1 min\naviso «¡ALERTA DE FUSIÓN!»',380,420, w=290),
       n('e','dec','¿Fusión ≥ 30 min?',380,540), n('f','proc','Fusión nuclear:\nnace Godzilla Junior',380,660, w=250),
       n('g','proc','Generación +1 · bono XP +10%\n(máx. +30%) · secreto «Meltdown»',380,770, w=310),
       n('h','ini','Fin del minuto',380,880)]
fus_e = [('a','b'),('b','c'),('c','d','Sí'),('c','c2','No'),('d','e'),('e','f','Sí'),('e','h','No',[(160,540),(160,880)]),('f','g'),('g','h'),('c2','h',None,[(730,880)])]
# ---------------------------------------------------------------- 6. Navegación
P = 'pant'
nav = [n('t',P,'Título ゴジラ',650,90), n('l',P,'Inicio de sesión\n/ Registro',650,195), n('i',P,'Inicio de partida',650,300),
       n('aj',P,'Configuración',1080,300, w=200), n('pr',P,'Panel del profesor\n(código GOJIRA1954)',1080,420, w=220),
       n('s',P,'Selección de huevo\n+ nombre',300,420),
       n('m',P,'Pantalla principal\n(barra superior + 5 pestañas)',650,545, w=300),
       n('h',P,'🏯 Hogar',110,710, w=160), n('j',P,'🎮 Juegos',300,710, w=160), n('ti',P,'🛒 Tienda /\nInventario',490,710, w=160),
       n('g',P,'🎎 Guarida',680,710, w=160), n('mi',P,'🎯 Misiones',870,710, w=160), n('pg',P,'⭐ Progreso y\nevolución',1060,710, w=160),
       n('kp',P,'📖 Kaiju-pedia',1250,710, w=160),
       n('ma',P,'Mapa de Japón',150,840, w=150), n('ca',P,'Cámara AR',150,935, w=150), n('po',P,'Póster\n(compartir)',150,1030, w=150),
       n('d',P,'Defensa de Tokio',360,840, w=170), n('an',P,'Anillos de Humo',360,935, w=170), n('me',P,'Memoria Fuerza G',360,1030, w=170),
       n('ba',P,'Batalla Mecha-Kaiju',360,1125, w=170)]
nav_e = [('t','l'),('l','i'),('i','s'),('s','m'),('i','m'),('i','aj'),('aj','pr'),('m','aj',None,[(1235,545),(1235,300)]),
         ('m','h'),('m','j'),('m','ti'),('m','g'),('m','mi'),('m','pg'),('m','kp'),
         ('h','ma',None,[(55,710),(55,840)]),('h','ca',None,[(55,710),(55,935)]),('h','po',None,[(55,710),(55,1030)]),
         ('j','d',None,[(250,710),(250,840)]),('j','an',None,[(250,710),(250,935)]),('j','me',None,[(250,710),(250,1030)]),('j','ba',None,[(250,710),(250,1125)])]

graficas = {
  'flujo_alimentar': (alim, alim_e, 900, 1180, 'Diagrama de flujo 1 · Alimentar al kaiju'),
  'flujo_comprar': (comp, comp_e, 920, 1000, 'Diagrama de flujo 2 · Comprar un objeto en la tienda'),
  'flujo_mision': (mis, mis_e, 920, 1080, 'Diagrama de flujo 3 · Completar y reclamar una misión'),
  'flujo_evolucion': (evo, evo_e, 960, 1240, 'Diagrama de flujo 4 · Evolución del personaje'),
  'flujo_fusion': (fus, fus_e, 900, 920, 'Diagrama de flujo 5 · Radiación y fusión nuclear (mecánica exclusiva)'),
  'navegacion': (nav, nav_e, 1360, 1180, 'Diagrama de navegación de Godzi-Pet 90'),
}
for nombre, (ns, es, w, h, t) in graficas.items():
    open(f'{OUT}/{nombre}.svg', 'w').write(svg(ns, es, w, h, t))
    print(nombre)
