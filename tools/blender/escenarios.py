# -*- coding: utf-8 -*-
"""
Escenarios 3D de GodziPet (capas parallax) -- Blender 5.x, Cycles.

Uso:  blender -b -P tools/blender/escenarios.py -- [ESCENARIO ...] [--solo dia|noche] [--capas 0,1,2]
      [--muestras N] [--prueba]   (sin argumentos: las 8 escenas, dia y noche, 5 capas)

Cada escena se construye de forma procedural (solo primitivas) en un unico mundo 3D con una
camara en perspectiva (lente 50 mm, a 4 m del suelo, horizonte al 50 % de la imagen):
    capa 0 = cielo (opaco: degradado + sol/luna + nubes/estrellas)
    capa 1 = horizonte lejano (montanas, mar), capa 2 = plano medio (ciudad/monumento),
    capa 3 = suelo cercano (cubre todo desde el 74 % de la altura hacia abajo),
    capa 4 = primer plano (solo en los bordes).
Cada capa se renderiza sola (el resto de colecciones se oculta) con fondo transparente (1-4).
La niebla atmosferica se hace en el material (mezcla con el color de bruma segun la profundidad
de camara), asi lo lejano sale mas claro/azulado. El desenfoque ligero de las capas 0-1 y la
conversion a WebP los hace tools/blender/postproceso.py (Pillow).
Los PNG se escriben en /tmp/claude-1000/godzi_render/<ESCENARIO>/<momento>_<capa>.png
"""
import bpy, math, random, sys, os, json, time
from mathutils import Vector
from mathutils import geometry as mgeo

ANCHO, ALTO = 1536, 1024
F_PX = ANCHO * 50.0 / 36.0          # focal en pixeles (lente 50 mm, sensor 36 mm)
CAM_H = 4.0                          # altura de la camara sobre el suelo (z=0)
TMP = '/tmp/claude-1000/godzi_render'


# ---------------------------------------------------------------- utilidades de vista
def vista(u, v, d):
    """Punto del mundo (x, z) que cae en la fraccion de imagen (u, v) a distancia d."""
    return (u - 0.5) * ANCHO / F_PX * d, CAM_H + (0.5 - v) * ALTO / F_PX * d

def v_suelo(d):
    """Fraccion vertical de imagen donde se ve el suelo (z=0) a distancia d."""
    return 0.5 + CAM_H / d * F_PX / ALTO

def d_suelo(v):
    return CAM_H * F_PX / ALTO / (v - 0.5)

def lin(c):
    """Color #rrggbb (sRGB) o tupla -> RGB lineal para Blender."""
    if isinstance(c, str):
        c = tuple(int(c[i:i + 2], 16) / 255.0 for i in (1, 3, 5))
    return tuple(((x + 0.055) / 1.055) ** 2.4 if x > 0.04045 else x / 12.92 for x in c)

def mezcla(a, b, t):
    a = lin(a) if isinstance(a, str) else a
    b = lin(b) if isinstance(b, str) else b
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))

def suave(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)

def ruido1d(x, semilla, escala=1.0):
    """Ruido 1D suave y determinista (suma de senos) en [-1, 1]."""
    r = random.Random(semilla)
    tot, amp, f = 0.0, 0.0, 1.0
    for _ in range(4):
        tot += math.sin(x * f / escala + r.uniform(0, 6.28)) * (0.5 ** _)
        amp += 0.5 ** _
        f *= 2.1
    return tot / amp


# ---------------------------------------------------------------- geometria acumulada
class Geo:
    """Acumula mallas por (capa, material) y las convierte en objetos al final."""
    def __init__(self, noche):
        self.noche = noche
        self.b = {}          # (capa, material) -> [vertices, caras]
        self.mats = {}       # nombre -> especificacion de material
        self.cfg = {}

    # --- materiales
    def mat(self, nombre, color, rug=0.85, emi=None, fuerza=0.0, ruido=0.0, color2=None,
            esc=2.0, bump=0.0, metal=0.0, bruma=1.0, spec=0.25):
        self.mats[nombre] = dict(color=color, rug=rug, emi=emi, fuerza=fuerza, ruido=ruido,
                                 color2=color2, esc=esc, bump=bump, metal=metal, bruma=bruma, spec=spec)
        return nombre

    def _add(self, L, m, vs, fs):
        v, f = self.b.setdefault((L, m), [[], []])
        o = len(v)
        v.extend(vs)
        f.extend([tuple(i + o for i in c) for c in fs])

    # --- primitivas (z = base del objeto salvo esfera, que es centro)
    def box(self, L, m, x, y, z, sx, sy, sz, rz=0.0):
        c, s = math.cos(rz), math.sin(rz)
        vs = []
        for dx in (-.5, .5):
            for dy in (-.5, .5):
                for dz in (0, 1):
                    px, py = dx * sx, dy * sy
                    vs.append((x + px * c - py * s, y + px * s + py * c, z + dz * sz))
        self._add(L, m, vs, [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)])

    def roof(self, L, m, x, y, z, w, d, h, rz=0.0, ov=0.0):
        """Tejado a dos aguas, cumbrera a lo largo de X local."""
        w, d = w + 2 * ov, d + 2 * ov
        c, s = math.cos(rz), math.sin(rz)
        loc = [(-w / 2, -d / 2, 0), (w / 2, -d / 2, 0), (w / 2, d / 2, 0), (-w / 2, d / 2, 0),
               (-w / 2, 0, h), (w / 2, 0, h)]
        vs = [(x + a * c - b * s, y + a * s + b * c, z + e) for a, b, e in loc]
        self._add(L, m, vs, [(0, 1, 5, 4), (2, 3, 4, 5), (3, 0, 4), (1, 2, 5), (0, 3, 2, 1)])

    def hip(self, L, m, x, y, z, w, d, h, top=0.0, rz=0.0):
        """Piramide/tronco de piramide rectangular (tejados de pagoda/castillo)."""
        c, s = math.cos(rz), math.sin(rz)
        loc = [(-w / 2, -d / 2, 0), (w / 2, -d / 2, 0), (w / 2, d / 2, 0), (-w / 2, d / 2, 0),
               (-w * top / 2, -d * top / 2, h), (w * top / 2, -d * top / 2, h),
               (w * top / 2, d * top / 2, h), (-w * top / 2, d * top / 2, h)]
        vs = [(x + a * c - b * s, y + a * s + b * c, z + e) for a, b, e in loc]
        self._add(L, m, vs, [(0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7), (4, 5, 6, 7), (0, 3, 2, 1)])

    def cyl(self, L, m, x, y, z, r, h, seg=12, r2=None, rot=0.0):
        r2 = r if r2 is None else r2
        vs, fs = [], []
        for i in range(seg):
            a = rot + 2 * math.pi * i / seg
            vs.append((x + r * math.cos(a), y + r * math.sin(a), z))
        for i in range(seg):
            a = rot + 2 * math.pi * i / seg
            vs.append((x + r2 * math.cos(a), y + r2 * math.sin(a), z + h))
        for i in range(seg):
            j = (i + 1) % seg
            fs.append((i, j, seg + j, seg + i))
        fs.append(tuple(range(seg - 1, -1, -1)))
        fs.append(tuple(range(seg, 2 * seg)))
        self._add(L, m, vs, fs)

    def sph(self, L, m, x, y, z, rx, ry=None, rz=None, seg=8, ani=4):
        ry = rx if ry is None else ry
        rz = rx if rz is None else rz
        vs, fs = [], []
        for j in range(1, ani):
            ph = math.pi * j / ani
            for i in range(seg):
                a = 2 * math.pi * i / seg
                vs.append((x + rx * math.sin(ph) * math.cos(a), y + ry * math.sin(ph) * math.sin(a), z + rz * math.cos(ph)))
        vs.append((x, y, z + rz)); vs.append((x, y, z - rz))
        top, bot = len(vs) - 2, len(vs) - 1
        for j in range(ani - 2):
            for i in range(seg):
                k = (i + 1) % seg
                fs.append((j * seg + i, j * seg + k, (j + 1) * seg + k, (j + 1) * seg + i))
        for i in range(seg):
            k = (i + 1) % seg
            fs.append((top, k, i))
            fs.append((bot, (ani - 2) * seg + i, (ani - 2) * seg + k))
        self._add(L, m, vs, fs)

    def beam(self, L, m, p0, p1, t=0.2, t2=None):
        """Viga/cable de seccion t x t2 entre dos puntos 3D."""
        t2 = t if t2 is None else t2
        p0, p1 = Vector(p0), Vector(p1)
        dr = (p1 - p0)
        if dr.length < 1e-6:
            return
        dr.normalize()
        ref = Vector((0, 1, 0)) if abs(dr.y) < 0.9 else Vector((1, 0, 0))
        a = dr.cross(ref).normalized() * (t / 2)
        b = dr.cross(a).normalized() * (t2 / 2)
        vs = []
        for p in (p0, p1):
            for sa, sb in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
                q = p + a * sa + b * sb
                vs.append((q.x, q.y, q.z))
        self._add(L, m, vs, [(0, 1, 2, 3), (4, 7, 6, 5), (0, 4, 5, 1), (1, 5, 6, 2), (2, 6, 7, 3), (3, 7, 4, 0)])

    def perfil(self, L, m, x0, x1, y0, y1, zb, fn, n=120):
        """Silueta (montanas/colinas) extruida en Y: fn(x)->altura."""
        xs = [x0 + (x1 - x0) * i / n for i in range(n + 1)]
        vs, fs = [], []
        for x in xs:
            z = fn(x)
            vs += [(x, y0, z), (x, y1, z), (x, y0, zb), (x, y1, zb)]
        for i in range(n):
            a, b = i * 4, (i + 1) * 4
            fs.append((a, a + 1, b + 1, b))          # cima
            fs.append((a + 2, b + 2, b, a))           # frente
        self._add(L, m, vs, fs)

    def terreno(self, L, m, x0, x1, y0, y1, nx, ny, fn):
        """Malla de superficie z = fn(x, y)."""
        vs, fs = [], []
        for j in range(ny + 1):
            y = y0 + (y1 - y0) * j / ny
            for i in range(nx + 1):
                x = x0 + (x1 - x0) * i / nx
                vs.append((x, y, fn(x, y)))
        for j in range(ny):
            for i in range(nx):
                a = j * (nx + 1) + i
                fs.append((a, a + 1, a + nx + 2, a + nx + 1))
        self._add(L, m, vs, fs)

    def poli(self, L, m, pts, y0, y1):
        """Poligono (x, z) extruido en Y."""
        pl = [[Vector((p[0], 0, p[1])) for p in pts]]
        tris = mgeo.tessellate_polygon(pl)
        n = len(pts)
        vs = [(p[0], y0, p[1]) for p in pts] + [(p[0], y1, p[1]) for p in pts]
        fs = [(t[0], t[1], t[2]) for t in tris] + [(t[2] + n, t[1] + n, t[0] + n) for t in tris]
        for i in range(n):
            j = (i + 1) % n
            fs.append((i, j, j + n, i + n))
        self._add(L, m, vs, fs)

    # --- construccion de objetos
    def construir(self):
        for nombre, e in self.mats.items():
            crear_material(nombre, e, self.cfg, self.noche)
        for l in range(5):
            col = bpy.data.collections.new('L%d' % l)
            bpy.context.scene.collection.children.link(col)
        for (L, m), (vs, fs) in self.b.items():
            me = bpy.data.meshes.new('%s_%d' % (m, L))
            me.from_pydata(vs, [], fs)
            me.update()
            ob = bpy.data.objects.new('%s_%d' % (m, L), me)
            ob.data.materials.append(bpy.data.materials[m])
            bpy.data.collections['L%d' % L].objects.link(ob)


def crear_material(nombre, e, cfg, noche):
    mt = bpy.data.materials.new(nombre)
    mt.use_nodes = True
    nt = mt.node_tree
    nt.nodes.clear()
    N = nt.nodes.new
    out = N('ShaderNodeOutputMaterial')
    bs = N('ShaderNodeBsdfPrincipled')
    nt.links.new(bs.outputs['BSDF'], out.inputs['Surface'])
    bs.inputs['Roughness'].default_value = e['rug']
    bs.inputs['Metallic'].default_value = e['metal']
    bs.inputs['Specular IOR Level'].default_value = e['spec']
    col = N('ShaderNodeRGB'); col.outputs[0].default_value = (*lin(e['color']), 1)
    cur = col.outputs[0]
    if e['ruido'] > 0:                       # variacion de color tipo "pintada"
        tc = N('ShaderNodeTexCoord'); tx = N('ShaderNodeTexNoise')
        tx.inputs['Scale'].default_value = e['esc']; tx.inputs['Detail'].default_value = 4
        nt.links.new(tc.outputs['Object'], tx.inputs['Vector'])
        c2 = N('ShaderNodeRGB')
        c2.outputs[0].default_value = (*(lin(e['color2']) if e['color2'] else mezcla(e['color'], '#000000', 0.35)), 1)
        mx = N('ShaderNodeMix'); mx.data_type = 'RGBA'
        mf = N('ShaderNodeMath'); mf.operation = 'MULTIPLY'; mf.inputs[1].default_value = e['ruido'] * 2.0
        nt.links.new(tx.outputs['Fac'], mf.inputs[0])
        nt.links.new(mf.outputs[0], mx.inputs['Factor'])
        nt.links.new(cur, mx.inputs['A']); nt.links.new(c2.outputs[0], mx.inputs['B'])
        cur = mx.outputs['Result']
        if e['bump'] > 0:
            bp = N('ShaderNodeBump'); bp.inputs['Strength'].default_value = e['bump']
            bp.inputs['Distance'].default_value = 0.3
            nt.links.new(tx.outputs['Fac'], bp.inputs['Height'])
            nt.links.new(bp.outputs['Normal'], bs.inputs['Normal'])
    # niebla atmosferica: fade = 1-exp(-profundidad/Dh), mezcla con color de bruma
    fade = None
    if e['bruma'] > 0:
        cd = N('ShaderNodeCameraData')
        m1 = N('ShaderNodeMath'); m1.operation = 'MULTIPLY'; m1.inputs[1].default_value = -1.0 / cfg['dh']
        pw = N('ShaderNodeMath'); pw.operation = 'POWER'; pw.inputs[0].default_value = 2.71828
        sb = N('ShaderNodeMath'); sb.operation = 'SUBTRACT'; sb.inputs[0].default_value = 1.0
        ml = N('ShaderNodeMath'); ml.operation = 'MULTIPLY'; ml.inputs[1].default_value = cfg['bruma_max'] * e['bruma']
        nt.links.new(cd.outputs['View Z Depth'], m1.inputs[0])
        nt.links.new(m1.outputs[0], pw.inputs[1])
        nt.links.new(pw.outputs[0], sb.inputs[1])
        nt.links.new(sb.outputs[0], ml.inputs[0])
        fade = ml.outputs[0]
        hz = N('ShaderNodeRGB'); hz.outputs[0].default_value = (*lin(cfg['bruma']), 1)
        mh = N('ShaderNodeMix'); mh.data_type = 'RGBA'
        nt.links.new(fade, mh.inputs['Factor'])
        nt.links.new(cur, mh.inputs['A']); nt.links.new(hz.outputs[0], mh.inputs['B'])
        cur = mh.outputs['Result']
    nt.links.new(cur, bs.inputs['Base Color'])
    if e['emi'] is not None and (e['fuerza'] > 0):
        bs.inputs['Emission Color'].default_value = (*lin(e['emi']), 1)
        if fade is not None:                  # lo emisivo lejano pierde fuerza
            inv = N('ShaderNodeMath'); inv.operation = 'MULTIPLY_ADD'
            inv.inputs[1].default_value = -0.8; inv.inputs[2].default_value = 1.0
            nt.links.new(fade, inv.inputs[0])
            st = N('ShaderNodeMath'); st.operation = 'MULTIPLY'; st.inputs[1].default_value = e['fuerza']
            nt.links.new(inv.outputs[0], st.inputs[0])
            nt.links.new(st.outputs[0], bs.inputs['Emission Strength'])
        else:
            bs.inputs['Emission Strength'].default_value = e['fuerza']


# ---------------------------------------------------------------- mundo, luces, cielo
def cfg_base(noche):
    if noche:
        return dict(top='#04081f', mid='#0f1d45', hor='#2a3f72', bruma='#1d2c55', dh=800.0, bruma_max=0.92,
                    amb='#2b3f78', amb_f=0.55, sol_dir=(-0.5, -0.6, 0.6), sol_col='#8fa8ff', sol_f=0.9,
                    fill_col='#304a9a', fill_f=0.25, luna=(0.72, 0.2), sol=None)
    return dict(top='#3f86dd', mid='#8fc4f2', hor='#f4ead6', bruma='#d3e3ee', dh=1500.0, bruma_max=0.9,
                amb='#a9cdf2', amb_f=0.75, sol_dir=(-0.55, -0.55, 0.62), sol_col='#fff0d4', sol_f=4.2,
                fill_col='#9ec6ff', fill_f=0.8, luna=None, sol=(0.74, 0.2))

def preparar_mundo(cfg):
    w = bpy.data.worlds.new('mundo')
    w.use_nodes = True
    nt = w.node_tree
    nt.nodes.clear()
    N = nt.nodes.new
    out = N('ShaderNodeOutputWorld')
    lp = N('ShaderNodeLightPath')
    tc = N('ShaderNodeTexCoord'); sp = N('ShaderNodeSeparateXYZ')
    cr = N('ShaderNodeValToRGB')
    el = cr.color_ramp.elements
    el[0].position, el[1].position = 0.0, 1.0
    cr.color_ramp.elements.new(0.5); cr.color_ramp.elements.new(0.72)
    cols = [cfg['bruma'], cfg['hor'], cfg['mid'], cfg['top']]
    for e, c in zip(cr.color_ramp.elements, cols):
        e.color = (*lin(c), 1)
    # posiciones: 0 bruma (bajo el horizonte) .5 horizonte .72 medio 1 cenit
    pos = [0.0, 0.5, 0.72, 1.0]
    for e, p in zip(cr.color_ramp.elements, pos):
        e.position = p
    sky = N('ShaderNodeBackground'); amb = N('ShaderNodeBackground')
    amb.inputs['Color'].default_value = (*lin(cfg['amb']), 1)
    amb.inputs['Strength'].default_value = cfg['amb_f']
    mix = N('ShaderNodeMixShader')
    nt.links.new(tc.outputs['Window'], sp.inputs['Vector'])
    nt.links.new(sp.outputs['Y'], cr.inputs['Fac'])
    nt.links.new(cr.outputs['Color'], sky.inputs['Color'])
    nt.links.new(lp.outputs['Is Camera Ray'], mix.inputs['Fac'])
    nt.links.new(amb.outputs['Background'], mix.inputs[1])
    nt.links.new(sky.outputs['Background'], mix.inputs[2])
    nt.links.new(mix.outputs['Shader'], out.inputs['Surface'])
    bpy.context.scene.world = w

def _sol(nombre, direccion, color, fuerza, ang):
    l = bpy.data.lights.new(nombre, 'SUN')
    l.color = lin(color); l.energy = fuerza; l.angle = ang
    ob = bpy.data.objects.new(nombre, l)
    bpy.context.scene.collection.objects.link(ob)
    ob.rotation_euler = (-Vector(direccion)).to_track_quat('-Z', 'Y').to_euler()

def preparar_luces(cfg):
    _sol('principal', cfg['sol_dir'], cfg['sol_col'], cfg['sol_f'], 0.06)
    d = cfg['sol_dir']
    _sol('relleno', (-d[0] * 0.8, -d[1] * 0.4, 0.5), cfg['fill_col'], cfg['fill_f'], 0.4)

def preparar_camara():
    cd = bpy.data.cameras.new('cam')
    cd.lens, cd.sensor_width, cd.sensor_fit = 50.0, 36.0, 'HORIZONTAL'
    cd.clip_start, cd.clip_end = 0.5, 30000.0
    ob = bpy.data.objects.new('cam', cd)
    bpy.context.scene.collection.objects.link(ob)
    ob.location = (0, 0, CAM_H)
    ob.rotation_euler = (math.radians(90), 0, 0)
    bpy.context.scene.camera = ob

def preparar_render(muestras):
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.render.resolution_x, sc.render.resolution_y, sc.render.resolution_percentage = ANCHO, ALTO, 100
    sc.render.image_settings.file_format = 'PNG'
    sc.render.image_settings.color_mode = 'RGBA'
    sc.view_settings.view_transform = 'Standard'
    sc.cycles.samples = muestras
    sc.cycles.use_denoising = True
    sc.cycles.max_bounces = 4
    sc.cycles.diffuse_bounces = 3
    sc.cycles.glossy_bounces = 2
    sc.cycles.transparent_max_bounces = 4
    try:
        sc.cycles.denoiser = 'OPENIMAGEDENOISE'
    except Exception:
        pass
    usado = 'CPU'
    try:
        pr = bpy.context.preferences.addons['cycles'].preferences
        pr.compute_device_type = 'CUDA'
        pr.refresh_devices()
        gpu = [d for d in pr.devices if d.type == 'CUDA']
        for d in pr.devices:
            d.use = d.type == 'CUDA'
        if gpu and not os.environ.get('GODZI_CPU'):
            sc.cycles.device = 'GPU'
            usado = 'CUDA ' + gpu[0].name
    except Exception as ex:
        print('GPU no disponible:', ex)
    return usado


# ---------------------------------------------------------------- cielo (capa 0)
def cielo(g, n, nubes=((0.15, 0.3), (0.45, 0.22), (0.8, 0.33), (0.62, 0.12), (0.3, 0.12)), semilla=3):
    """Sol/luna, nubes (dia) o estrellas (noche). Todo en capa 0, muy lejos."""
    r = random.Random(semilla)
    D = 5000.0
    if n:
        g.mat('estrella', '#ffffff', emi='#dfe8ff', fuerza=14.0, bruma=0)
        g.mat('luna', '#e8ecf5', emi='#e9edf8', fuerza=2.4, bruma=0)
        g.mat('nube', '#46568a', emi='#33447a', fuerza=0.35, bruma=0)
        for _ in range(240):
            x, z = vista(r.uniform(0.0, 1.0), r.uniform(0.0, 0.5), D)
            g.sph(0, 'estrella', x, D, z, r.uniform(2.0, 4.5), seg=4, ani=3)
        lu = g.cfg['luna']
        x, z = vista(lu[0], lu[1], D)
        g.sph(0, 'luna', x, D, z, 150, seg=16, ani=8)
    else:
        g.mat('sol', '#fff7dc', emi='#fff3c8', fuerza=9.0, bruma=0)
        g.mat('nube', '#fff6ea', emi='#dfe8ff', fuerza=0.22, bruma=0.0, rug=1.0)
        s = g.cfg['sol']
        x, z = vista(s[0], s[1], D)
        g.sph(0, 'sol', x, D, z, 130, seg=16, ani=8)
    Dn = 3600.0
    for (u, v) in nubes:
        cx, cz = vista(u, v, Dn)
        for _ in range(r.randint(4, 7)):
            g.sph(0, 'nube', cx + r.uniform(-380, 380), Dn + r.uniform(-60, 60), cz + r.uniform(-30, 40),
                  r.uniform(90, 190), r.uniform(60, 100), r.uniform(40, 75), seg=10, ani=5)


# ---------------------------------------------------------------- componentes reutilizables
def colinas(g, L, mat, d, y_esp, base, alt, semilla, escala=140.0, picos=1.0, x_ext=0.75):
    """Cordillera lejana: silueta de ruido a distancia d."""
    def fn(x):
        r = ruido1d(x, semilla, escala)
        return base + alt * (0.55 + 0.45 * r) * (1 + 0.25 * picos * abs(ruido1d(x * 1.7, semilla + 9, escala)))
    g.perfil(L, mat, -x_ext * d, x_ext * d, d, d + y_esp, -30, fn, 140)
    return fn

def ramas_pino(g, L, n, u0, v0, u1, v1, d=12.0, sem=1, mat_r='rama', mat_n='aguja', k=0.5, tufos=16):
    """Rama de pino en primer plano de (u0,v0) a (u1,v1) con penachos de agujas (k = escala)."""
    g.mat(mat_r, '#4a3524', rug=0.9, bruma=0.0)
    g.mat(mat_n, '#2f6b3a', rug=0.8, ruido=0.3, color2='#1b4a2a', esc=6, bruma=0.0)
    r = random.Random(sem)
    x0, z0 = vista(u0, v0, d); x1, z1 = vista(u1, v1, d)
    g.beam(L, mat_r, (x0, d, z0), (x1, d, z1), 0.3 * k, 0.3 * k)
    for i in range(1, tufos):
        t = i / float(tufos)
        px, pz = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
        for lado in (-1, 1):
            ex, ez = px + r.uniform(0.2, 0.6) * k, pz + lado * r.uniform(0.7, 1.4) * k
            g.beam(L, mat_r, (px, d, pz), (ex, d + r.uniform(-0.2, 0.2), ez), 0.1 * k, 0.1 * k)
            for _ in range(16):
                a = r.uniform(0, 6.28)
                ln = r.uniform(0.5, 0.9) * k
                g.beam(L, mat_n, (ex, d, ez), (ex + math.cos(a) * ln, d + r.uniform(-0.2, 0.2), ez + math.sin(a) * ln), 0.07 * k, 0.07 * k)

def hierba_borde(g, L, mat, d, u0, u1, v_base, alto, cuantos, sem, ancho=0.07):
    """Matas de hierba/cañas en el borde inferior (u0..u1)."""
    r = random.Random(sem)
    for _ in range(cuantos):
        x, z = vista(r.uniform(u0, u1), v_base + r.uniform(0, 0.04), d)
        h = alto * r.uniform(0.5, 1.0)
        inc = r.uniform(-0.4, 0.4) * h
        g.beam(L, mat, (x, d, z), (x + inc, d + r.uniform(-.3, .3), z + h), ancho, ancho * 0.4)

def casa(g, n, L, x, y, z, w=7.0, d=5.0, h=3.6, rz=0.0, pared='cas_pared', techo='cas_techo', ht=3.0, vent=True):
    """Cabana de pescadores con tejado a dos aguas de paja."""
    g.box(L, pared, x, y, z, w, d, h, rz)
    g.roof(L, techo, x, y, z + h - 0.1, w, d, ht, rz, ov=0.7)
    if vent:
        g.box(L, 'cas_puerta', x - w * 0.18, y - d / 2 - 0.02, z, 1.2, 0.2, 2.2, rz)
        g.box(L, 'cas_vent', x + w * 0.2, y - d / 2 - 0.02, z + 1.2, 1.3, 0.2, 1.1, rz)

def casas_lista(g, n, L, lst):
    g.mat('cas_pared', '#b98a5b', ruido=0.25, color2='#8c6340', esc=4)
    g.mat('cas_techo', '#cfa94d', ruido=0.35, color2='#8f6f2c', esc=5)
    g.mat('cas_puerta', '#4a3020')
    g.mat('cas_vent', '#2d2a33' if not n else '#ffc864', emi='#ffb84a' if n else None, fuerza=7.0 if n else 0)
    for c in lst:
        casa(g, n, L, *c)

def torii(g, L, x, y, z, ancho=12.0, alto=11.0):
    g.mat('torii', '#d2352b', rug=0.5, bruma=1.0)
    g.mat('torii_n', '#231a1a', rug=0.6)
    sep = ancho / 2 - 1.0
    for s in (-1, 1):
        g.cyl(L, 'torii', x + s * sep, y, z, 0.55, alto, 10, r2=0.45)
        g.box(L, 'torii_n', x + s * sep, y, z - 0.1, 1.3, 1.3, 0.4)
    g.box(L, 'torii', x, y, z + alto * 0.74, ancho * 0.95, 0.6, 0.55)           # nuki
    g.box(L, 'torii', x, y, z + alto * 0.98, ancho * 1.25, 0.8, 0.7)             # kasagi
    g.box(L, 'torii_n', x, y, z + alto * 1.0 + 0.6, ancho * 1.3, 1.0, 0.35)
    g.box(L, 'torii', x, y, z + alto * 0.74 - 0.9, 0.5, 0.5, 0.9)               # gakuzuka

def barco(g, n, L, x, y, z, largo=10.0, rz=0.0, vela=True, c_casco='#6b4b32'):
    g.mat('casco', c_casco, ruido=0.2, esc=3)
    g.mat('casco_f', '#e8e0cf')
    g.mat('vela', '#efe6d0', rug=0.95)
    g.mat('mastil', '#3a2a1c')
    l2 = largo / 2
    pts = [(-l2, 1.5), (-l2 * 0.75, 0.0), (l2 * 0.7, 0.0), (l2, 1.7), (l2 * 0.6, 1.2), (-l2 * 0.5, 1.0)]
    c, s = math.cos(rz), math.sin(rz)
    g.poli(L, 'casco', [(x + p[0], z + p[1] - 0.2) for p in pts], y - 0.9, y + 0.9)
    g.box(L, 'casco_f', x, y - 0.95, z + 0.9, largo * 0.8, 0.1, 0.18)
    if vela:
        g.cyl(L, 'mastil', x - 0.5, y, z + 0.5, 0.12, largo * 0.8, 6)
        g.poli(L, 'vela', [(x - 0.3, z + largo * 0.7), (x - 0.3, z + 1.5), (x + largo * 0.33, z + 1.5)], y - 0.1, y + 0.1)


# ================================================================ ESCENAS
META = {}      # datos para el postproceso (brillo del sol/luna, desvanecido de capa 3...)

def mar_lejano(g, color='#2d86b0', color2='#1d6a95'):
    g.mat('mar', color, rug=0.4, ruido=0.3, color2=color2, esc=0.22, bump=0.15, spec=0.06)
    g.terreno(1, 'mar', -7000, 7000, 14, 7000, 1, 1, lambda x, y: 0.0)

def arboles_cono(g, L, lista, mat='pino2', seg=7):
    """lista de (x, y, z, alto, radio)."""
    g.mat('tronco', '#5a4030')
    for (x, y, z, h, r) in lista:
        g.cyl(L, 'tronco', x, y, z, r * 0.18, h * 0.3, 5)
        g.cyl(L, mat, x, y, z + h * 0.18, r, h * 0.55, seg, r2=r * 0.35)
        g.cyl(L, mat, x, y, z + h * 0.5, r * 0.75, h * 0.5, seg, r2=0.05)

def rocas(g, L, mat, lista, sem=1):
    r = random.Random(sem)
    for (x, y, z, s) in lista:
        g.sph(L, mat, x, y, z + s * 0.35, s, s * r.uniform(0.8, 1.3), s * 0.6, seg=6, ani=4)

def isla_odo(g, n):
    cielo(g, n, semilla=2)
    # --- capa 1: mar + islas lejanas
    mar_lejano(g)
    g.mat('lejos1', '#4f8a78', ruido=0.2, color2='#3a6f66', esc=30)
    g.mat('lejos2', '#6c9aa0', ruido=0.2, color2='#4d7f86', esc=30)
    for c, w, p, d, m in ((-520, 380, 95, 2700, 'lejos2'), (330, 300, 70, 2300, 'lejos1'), (1100, 450, 110, 3300, 'lejos2')):
        g.perfil(1, m, -2600, 2600, d, d + 200, -20,
                 lambda x, c=c, w=w, p=p: p * math.exp(-((x - c) / w) ** 2) * (1 + 0.2 * ruido1d(x, 5, 60)))
    # --- capa 2: isla con aldea, torii y barcas
    yf = 150.0
    def alt(x, y):
        pico = 24 * math.exp(-((x + 30) / 30.0) ** 2) + 9 * math.exp(-((x + 78) / 16.0) ** 2) + 9 * math.exp(-((x - 42) / 14.0) ** 2)
        t = suave((y - (yf - 30)) / 75.0)
        return pico * t * (0.85 + 0.15 * ruido1d(x * 0.8 + y * 0.5, 11, 25))
    g.mat('hierba', '#5d9b4a', ruido=0.5, color2='#3f7a3a', esc=0.12)
    g.mat('arena', '#e2cb94', ruido=0.3, color2='#c9ad78', esc=0.2)
    g.terreno(2, 'hierba', -190, 190, yf - 30, yf + 50, 90, 16, alt)
    g.terreno(2, 'arena', -190, 190, yf - 30, yf + 50, 90, 16, lambda x, y: min(alt(x, y), 2.0) + 0.04)
    r = random.Random(7)
    chozas = []
    for i in range(11):
        x = -58 + i * 5.6 + r.uniform(-1.5, 1.5)
        y = yf + r.uniform(0, 34)
        chozas.append((x, y, alt(x, y) - 0.3, r.uniform(8.5, 11), r.uniform(6, 7.5), r.uniform(4, 5), r.uniform(-0.15, 0.15)))
    casas_lista(g, n, 2, chozas)
    g.mat('pino2', '#2f6b45', ruido=0.35, color2='#1d4a33', esc=1.2)
    arb = []
    for i in range(34):
        x = r.uniform(-80, 45); y = yf + r.uniform(25, 55)
        arb.append((x, y, alt(x, y) - 0.2, r.uniform(8, 14), r.uniform(2.4, 3.6)))
    arboles_cono(g, 2, arb)
    g.mat('roca', '#7b8088', ruido=0.3, color2='#575c66', esc=1.5)
    rocas(g, 2, 'roca', [(-62, yf - 18, 0, 4), (52, yf - 15, 0, 3.2), (56, yf - 12, 0, 2.2)])
    barco(g, n, 2, -30, 122, 0.0, 10, 0.04)
    barco(g, n, 2, 40, 128, 0.0, 9, -0.03, c_casco='#7a5a3e')
    # --- capa 3: playa de arena, aguas someras, torii y barcas (todo dentro de +-0.36*d del centro)
    ysh = lambda x: 40 + 3 * ruido1d(x, 21, 18)
    def suelo(x, y):
        z = 0.1 * ruido1d(x * 1.3 + y, 4, 4)
        return z - 0.9 * suave((y - ysh(x)) / 8.0)
    g.mat('arena3', '#ead7a3', ruido=0.5, color2='#c9ab72', esc=0.3, bump=0.3)
    g.mat('agua3', '#4fc0c6', rug=0.4, ruido=0.3, color2='#2a93aa', esc=0.35, bump=0.12, bruma=0.5, spec=0.06)
    g.mat('espuma', '#f6fbff', rug=0.9, bruma=0.5)
    g.terreno(3, 'arena3', -60, 60, 10, 100, 140, 45, suelo)
    g.terreno(3, 'agua3', -60, 60, 30, 112, 1, 1, lambda x, y: -0.45)
    for k in range(22):                       # franja de espuma en la orilla
        x = -50 + k * 4.6
        g.box(3, 'espuma', x, ysh(x) + 4.2 + 1.0 * ruido1d(x, 2), -0.42, r.uniform(2.6, 4.4), 0.5, 0.08)
    g.mat('roca3', '#807a70', ruido=0.35, color2='#5b564f', esc=1.0)
    rocas(g, 3, 'roca3', [(-17, 58, -0.2, 2.6), (-14, 60, -0.2, 1.6), (-20, 57, -0.1, 1.2), (30, 70, -0.3, 2.2), (33, 69, -0.3, 1.3)], 3)
    torii(g, 3, -16, 82, -0.4, 10, 9.5)
    barco(g, n, 3, 14, 92, -0.4, 9, 0.03)
    barco(g, n, 3, 24, 66, -0.4, 6.5, -0.04, vela=False, c_casco='#7a5a3e')
    barco(g, n, 3, 8, 24, 0.2, 7, 0.12, vela=False)           # barca varada
    g.mat('palo', '#6a4d36')
    g.mat('lampara', '#f5e6b8', emi='#ffc766' if n else None, fuerza=14.0 if n else 0.0, bruma=0.3)
    for px, py in ((-9, 28), (9, 52)):                         # faroles de pescadores sobre postes
        g.cyl(3, 'palo', px, py, 0, 0.18, 3.2, 6)
        g.box(3, 'lampara', px, py, 3.2, 0.7, 0.7, 0.9)
    # --- capa 4: pino arriba a la izquierda, hierba abajo a la derecha
    ramas_pino(g, 4, n, 0.0, 0.02, 0.30, 0.16, 12.0, 1)
    g.mat('hierba4', '#6a9c3c', ruido=0.3, color2='#41741f', esc=3, bruma=0)
    hierba_borde(g, 4, 'hierba4', 12.0, 0.8, 1.0, 0.98, 1.6, 40, 5)
    hierba_borde(g, 4, 'hierba4', 12.0, 0.0, 0.14, 0.99, 1.1, 18, 6)
    META['fade3'] = (0.572, 0.61)



# ---------------------------------------------------------------- ayudas comunes (resto de escenas)
def ventanas(g, L, n, x, y, z0, w, h, nx, nz, sem=0, prob=0.55, lado=0, tam=0.5):
    """Rejilla de ventanas en una fachada. lado=0 frente (cara -Y); +-1 cara lateral (x=cte, w es la profundidad)."""
    r = random.Random(sem)
    g.mat('vent_on', '#ffe3a0', emi='#ffc860', fuerza=6.0)
    g.mat('vent_off', '#2f3c55' if not n else '#161d33', rug=0.3, spec=0.5)
    for i in range(nx):
        for j in range(nz):
            m = 'vent_on' if (n and r.random() < prob) else 'vent_off'
            a = -w / 2 + (i + 0.5) * w / nx
            zz = z0 + (j + 0.45) * h / nz
            if lado == 0:
                g.box(L, m, x + a, y - 0.04, zz, w / nx * tam, 0.12, h / nz * 0.5)
            else:
                g.box(L, m, x - lado * 0.04, y + a, zz, 0.12, w / nx * tam, h / nz * 0.5)

def edificio(g, n, L, x, y, w, d, h, mat, nx=None, nz=None, sem=0, techo='techo_pl'):
    g.box(L, mat, x, y, 0, w, d, h)
    g.box(L, techo, x, y, h, w + 0.6, d + 0.6, 0.6)
    nx = nx or max(2, int(w / 3.2)); nz = nz or max(2, int(h / 3.4))
    ventanas(g, L, n, x, y - d / 2, 1.0, w, h - 1.5, nx, nz, sem)

def plano(g, L, mat, y0, y1, z=0.0, ext=0.62):
    g.terreno(L, mat, -ext * y1, ext * y1, y0, y1, 1, 1, lambda x, y: z)

def farola(g, n, L, x, y, h=6.0, brazo=0.0):
    g.mat('poste', '#2d3036')
    g.mat('lampara', '#f5e6b8', emi='#ffc766' if n else None, fuerza=14.0 if n else 0.0, bruma=0.3)
    g.cyl(L, 'poste', x, y, 0, h * 0.03 + 0.08, h, 6)
    g.box(L, 'lampara', x + brazo, y, h, 0.8, 0.8, 0.9)
    if brazo:
        g.beam(L, 'poste', (x, y, h - 0.2), (x + brazo, y, h - 0.2), 0.12)

def cables(g, L, d, lista, grosor=0.03):
    """Cables colgantes en primer plano: lista de (v_izq, v_der, caida_en_v)."""
    g.mat('cable', '#16181d', bruma=0)
    for (va, vb, cai) in lista:
        pts = []
        for k in range(17):
            t = k / 16.0
            u = -0.05 + 1.1 * t
            v = va + (vb - va) * t + cai * 4 * t * (1 - t)
            x, z = vista(u, v, d)
            pts.append((x, d, z))
        for a, b in zip(pts, pts[1:]):
            g.beam(L, 'cable', a, b, grosor, grosor)

def orilla(g, n, c1, c2, ca, cb, ysh0=40, espuma=True, sem=3, bump=0.3):
    """Capa 3 tipica: suelo (arena/hierba) + aguas someras al fondo. Devuelve y_orilla(x)."""
    r = random.Random(sem)
    ysh = lambda x: ysh0 + 3 * ruido1d(x, 21, 18)
    g.mat('suelo3', c1, ruido=0.5, color2=c2, esc=0.3, bump=bump)
    g.mat('agua3', ca, rug=0.4, ruido=0.3, color2=cb, esc=0.35, bump=0.12, bruma=0.5, spec=0.06)
    g.mat('espuma', '#f6fbff', rug=0.9, bruma=0.5)
    sue = lambda x, y: 0.1 * ruido1d(x * 1.3 + y, 4, 4) - 0.9 * suave((y - ysh(x)) / 8.0)
    g.terreno(3, 'suelo3', -60, 60, 10, 100, 140, 45, sue)
    g.terreno(3, 'agua3', -60, 60, 30, 112, 1, 1, lambda x, y: -0.45)
    if espuma:
        for k in range(22):
            x = -50 + k * 4.6
            g.box(3, 'espuma', x, ysh(x) + 4.2 + ruido1d(x, 2), -0.42, r.uniform(2.6, 4.4), 0.5, 0.08)
    META['fade3'] = (0.572, 0.61)
    return ysh

def fronda(g, L, m, x, y, z, ang, ln, w, cai):
    """Hoja de palmera: lamina curva en el plano XZ."""
    top, bot = [], []
    for k in range(9):
        t = k / 8.0
        cx = x + math.cos(ang) * ln * t
        cz = z + math.sin(ang) * ln * t - cai * t * t * ln
        ox = w * math.sin(math.pi * min(1, t * 1.05)) * 0.5
        top.append((cx, cz + ox)); bot.append((cx, cz - ox))
    g.poli(L, m, top + bot[::-1], y - 0.05, y + 0.05)

def palmera(g, L, x, y, h=9.0, incl=0.8, sem=1, esc=1.0):
    g.mat('tronco_p', '#8a6a46', ruido=0.3, color2='#5d4630', esc=1.5)
    g.mat('hoja_p', '#3f9a47', ruido=0.4, color2='#206b34', esc=2.0)
    r = random.Random(sem)
    px, pz = x, 0.0
    for k in range(6):
        nx_, nz_ = px + incl * h / 6 * (k + 1) / 6, pz + h / 6
        g.beam(L, 'tronco_p', (px, y, pz), (nx_, y, nz_), 0.55 * esc, 0.55 * esc)
        px, pz = nx_, nz_
    for k in range(9):
        a = math.pi * (k / 8.0)
        fronda(g, L, 'hoja_p', px, y + r.uniform(-.3, .3), pz, a, h * 0.55, h * 0.22, 0.55 + 0.1 * r.random())
    g.sph(L, 'tronco_p', px, y, pz - 0.2, 0.5 * esc, seg=6, ani=3)

def sakura(g, L, x, y, h=7.0, sem=1, mat='sakura'):
    g.mat('sakura', '#f6b9cb', ruido=0.4, color2='#e987a9', esc=0.8)
    g.mat('tronco', '#5a4030')
    r = random.Random(sem)
    g.cyl(L, 'tronco', x, y, 0, h * 0.06, h * 0.55, 6, r2=h * 0.035)
    for _ in range(9):
        g.sph(L, mat, x + r.uniform(-.45, .45) * h, y + r.uniform(-1, 1), h * 0.6 + r.uniform(-.1, .3) * h,
              h * r.uniform(.22, .34), h * 0.25, h * 0.2, seg=7, ani=4)

# ================================================================ BAHIA_TOKIO
def bahia_tokio(g, n):
    cielo(g, n, semilla=5)
    mar_lejano(g, '#2a7fa8', '#1c5f88')
    r = random.Random(4)
    g.mat('lejos2', '#7fa0b4', ruido=0.2, color2='#5e8296', esc=30)
    g.perfil(1, 'lejos2', -3000, 3000, 2900, 3100, -20, lambda x: 120 * (0.5 + 0.5 * ruido1d(x, 8, 500)) * (x > 0) + 40)
    g.mat('urbe1', '#aebfcd', ruido=0.25, color2='#8da2b6', esc=15)
    for i in range(44):
        g.box(1, 'urbe1', -540 + i * 25 + r.uniform(-5, 5), 1500 + r.uniform(0, 80), 0, r.uniform(14, 26), 22, r.uniform(18, 75))
    g.mat('techo_pl', '#8a8f98'); g.mat('muro_a', '#c8c1b2', ruido=0.2, esc=3); g.mat('muro_b', '#a9a39a', ruido=0.2, esc=3)
    g.mat('muro_c', '#c9a68a', ruido=0.2, esc=3)
    for i, (x, h, m) in enumerate(((22, 20, 'muro_a'), (36, 28, 'muro_b'), (52, 18, 'muro_c'), (68, 32, 'muro_a'), (82, 22, 'muro_b'))):
        edificio(g, n, 2, x, 330 + r.uniform(-10, 10), 12, 10, h, m, sem=i)
    # buque de carga
    def carguero(x, y, L_, esc_):
        g.mat('casco_r', '#8c2f2a'); g.mat('casco_n', '#26303c'); g.mat('super', '#efe9dc'); g.mat('chim', '#d49a2a')
        pts = [(-L_ / 2, 7 * esc_), (-L_ / 2 + 2, 0), (L_ / 2 - 8, 0), (L_ / 2, 6 * esc_), (L_ / 2, 7 * esc_)]
        g.poli(2, 'casco_n', [(x + p[0], p[1] - 1.0) for p in pts], y - 5, y + 5)
        g.box(2, 'casco_r', x, y - 5.05, 0.3, L_ * 0.95, 0.1, 2.0 * esc_)
        g.box(2, 'super', x - L_ * 0.38, y, 7 * esc_ - 1, L_ * 0.14, 8, 9 * esc_)
        g.box(2, 'super', x - L_ * 0.38, y, 7 * esc_ + 8 * esc_, L_ * 0.12, 7, 3 * esc_)
        ventanas(g, 2, n, x - L_ * 0.38, y - 4, 7 * esc_ + 2, L_ * 0.14, 6 * esc_, 5, 2, 3)
        g.cyl(2, 'chim', x - L_ * 0.3, y, 7 * esc_ + 8, 1.4 * esc_, 5 * esc_, 8)
        cols = ['#d1443a', '#2f6fb5', '#e0b13a', '#3f8c58', '#c9c9c9']
        for k in range(10):
            for j in range(2 if k % 3 else 3):
                g.mat('cont%d' % ((k + j) % 5), cols[(k + j) % 5])
                g.box(2, 'cont%d' % ((k + j) % 5), x - L_ * 0.18 + k * (L_ * 0.058), y, 7 * esc_ - 1 + j * 2.6 * esc_, L_ * 0.052, 6, 2.5 * esc_)
    carguero(-30, 190, 70, 1.0)
    carguero(55, 270, 50, 1.0)
    # grua portico
    g.mat('grua', '#c7452e', metal=0.3, rug=0.5)
    for dx in (-9, 9):
        for dy in (-3, 3):
            g.beam(2, 'grua', (88 + dx, 150 + dy, 0), (88 + dx * 0.8, 150 + dy, 26), 0.8)
    g.beam(2, 'grua', (70, 147, 27), (118, 147, 27), 1.2, 2.0); g.beam(2, 'grua', (70, 153, 27), (118, 153, 27), 1.2, 2.0)
    g.beam(2, 'grua', (79, 147, 0), (97, 147, 26), 0.4); g.beam(2, 'grua', (97, 147, 0), (79, 147, 26), 0.4)
    # faro rojiblanco sobre escollera
    g.mat('rompeolas', '#7d7f84', ruido=0.4, esc=2)
    g.box(2, 'rompeolas', -75, 150, 0, 40, 8, 2.2)
    g.mat('faro_r', '#cf3a32'); g.mat('faro_b', '#f4f1ea'); g.mat('faro_l', '#ffe9a8', emi='#ffe08a', fuerza=22.0 if n else 1.0)
    for k in range(5):
        g.cyl(2, 'faro_r' if k % 2 == 0 else 'faro_b', -75, 150, 2.2 + k * 4.2, 3.2 - k * 0.2, 4.2, 12, r2=3.0 - k * 0.2)
    g.cyl(2, 'faro_l', -75, 150, 2.2 + 21, 2.0, 3.0, 10)
    g.cyl(2, 'faro_r', -75, 150, 2.2 + 24, 2.8, 2.2, 10, r2=0.1)
    # capa 3: muelle de hormigon
    g.mat('hormigon', '#a7a9a8', ruido=0.4, color2='#83857f', esc=0.4, bump=0.2)
    g.mat('junta', '#6b6d6c', bruma=0.4)
    g.mat('agua3', '#2c7ea0', rug=0.4, ruido=0.3, color2='#1e5f82', esc=0.35, bump=0.12, bruma=0.5, spec=0.06)
    g.terreno(3, 'hormigon', -60, 60, 10, 44, 1, 1, lambda x, y: 0.0)
    g.terreno(3, 'agua3', -60, 60, 40, 112, 1, 1, lambda x, y: -2.0)
    for x in range(-60, 61, 8):
        g.beam(3, 'junta', (x * 0.3, 10, 0.02), (x * 1.6, 44, 0.02), 0.08, 0.02)
    for y in (18, 27, 36):
        g.box(3, 'junta', 0, y, 0.0, 120, 0.1, 0.03)
    g.mat('bolardo', '#2b2e33'); g.mat('madera', '#8f6a40', ruido=0.3, esc=3); g.mat('barril', '#3f5f8c')
    for x in range(-26, 27, 6):
        g.cyl(3, 'bolardo', x * 0.9, 40.5, 0, 0.55, 1.0, 8, r2=0.4); g.cyl(3, 'bolardo', x * 0.9, 40.5, 1.0, 0.75, 0.25, 8)
    for (x, y, k) in ((-11, 33, 2), (-8.5, 34, 1), (-12, 28, 1)):
        for j in range(k):
            g.box(3, 'madera', x, y, j * 1.6, 2.4, 2.4, 1.6, r.uniform(-.2, .2))
    for x in (9.5, 11, 10.2):
        g.cyl(3, 'barril', x, 33 - (x % 3), 0, 0.5, 1.2, 8)
    farola(g, n, 3, -14, 36, 6.5, 1.5); farola(g, n, 3, 15, 36, 6.5, -1.5)
    META['fade3'] = (0.572, 0.61)
    # capa 4: cables electricos arriba y bolardo grande abajo a la derecha
    cables(g, 4, 12, [(0.0, 0.03, 0.045), (0.02, 0.07, 0.05), (-0.01, 0.0, 0.05)])
    px, pz = vista(0.04, 0.5, 12)
    g.mat('poste_m', '#4a3a2c', bruma=0)
    g.cyl(4, 'poste_m', px, 12, vista(0, 1.2, 12)[1], 0.4, 14, 8)
    g.beam(4, 'poste_m', (px - 0.6, 12, vista(0, 0.04, 12)[1]), (px + 1.0, 12, vista(0, 0.04, 12)[1]), 0.12)
    bx, bz = vista(0.9, 1.15, 12)
    g.mat('bolardo4', '#26292e', bruma=0)
    g.cyl(4, 'bolardo4', bx, 12, bz, 0.9, 2.0, 10, r2=0.65); g.cyl(4, 'bolardo4', bx, 12, bz + 2.0, 1.15, 0.4, 10)

# ================================================================ GINZA
def ginza(g, n):
    g.cfg['sol'] = (0.8, 0.18) if not n else None
    cielo(g, n, semilla=6)
    r = random.Random(9)
    g.mat('urbe1', '#aab4c4', ruido=0.25, color2='#8b97aa', esc=15)
    for i in range(40):
        g.box(1, 'urbe1', -420 + i * 22 + r.uniform(-4, 4), 700 + r.uniform(0, 60), 0, r.uniform(14, 24), 20, r.uniform(25, 70))
    g.mat('asfalto', '#55585f', ruido=0.4, color2='#42454c', esc=0.5, bump=0.15)
    g.mat('acera', '#b9b4a8', ruido=0.3, esc=0.4)
    g.mat('techo_pl', '#7d7d82')
    plano(g, 2, 'asfalto', 15, 600)
    mats = [('crema', '#d9c9a3'), ('ladri', '#a85a42'), ('gris', '#a9a8a2'), ('ocre', '#c69a5e'), ('verde', '#7a8d78')]
    for nm, c in mats:
        g.mat(nm, c, ruido=0.25, color2=mezcla(c, '#000000', 0.3), esc=3)
    # hileras de edificios a ambos lados de la avenida (capas 3 y 2 segun profundidad)
    for sg in (-1, 1):
        for k in range(8):
            y = 38 + k * 21 + r.uniform(-1, 1)
            L = 3 if y < 105 else 2
            h = r.uniform(13, 22)
            w = 16
            xc = sg * (13.5 + w / 2)
            m = mats[(k + (2 if sg > 0 else 0)) % 5][0]
            g.box(L, m, xc, y, 0, w, 18, h)
            g.box(L, 'techo_pl', xc, y, h, w + 0.6, 18.6, 0.7)
            ventanas(g, L, n, xc - sg * w / 2, y, 1.2, 18, h - 2, 5, max(3, int(h / 3.4)), k + sg, lado=-sg)
            if k == 0 or True:
                ventanas(g, L, n, xc, y - 9, 1.2, w, h - 2, 4, max(3, int(h / 3.4)), k + 20 * sg)
            g.box(L, 'acera', sg * 12.3, y, 0, 2.6, 21, 0.25)
    # Wako al fondo
    g.mat('wako', '#e6dcc4', ruido=0.2, color2='#c9bd9f', esc=3)
    g.mat('cupula', '#4e8f7f', rug=0.4, metal=0.3)
    g.mat('reloj', '#f4f0e4', rug=0.5)
    g.mat('oro', '#d9b24a', metal=0.6, rug=0.4)
    wy = 215.0
    g.box(2, 'wako', 0, wy, 0, 44, 18, 17)
    g.box(2, 'techo_pl', 0, wy, 17, 45, 19, 0.8)
    ventanas(g, 2, n, 0, wy - 9, 1.5, 40, 14, 10, 3, 77)
    for i in range(-5, 6):
        g.box(2, 'wako', i * 3.9, wy - 9.4, 0, 0.9, 1.0, 15)          # pilastras
    g.box(2, 'wako', 0, wy, 17.8, 11, 11, 11)                           # cuerpo de la torre
    g.box(2, 'oro', 0, wy, 28.6, 12.2, 12.2, 0.6)
    cx, cz = 0.0, 24.0
    g.poli(2, 'reloj', [(cx + 4.0 * math.cos(a * math.pi / 12), cz + 4.0 * math.sin(a * math.pi / 12)) for a in range(24)], wy - 5.9, wy - 5.7)
    g.sph(2, 'cupula', 0, wy, 29.2, 5.8, 5.8, 5.2, seg=14, ani=6)
    g.cyl(2, 'oro', 0, wy, 34.2, 0.18, 3.2, 6)
    META['reloj'] = [0.5 + cx / (0.72 * (wy - 5.8)), 0.5 - (cz - CAM_H) / (wy - 5.8) * F_PX / ALTO, 4.0 / (wy - 5.8) * F_PX / ANCHO]
    # tranvia y rieles
    g.mat('riel', '#8c8f94', metal=0.6, rug=0.4, bruma=0.6)
    for xr in (-3.2, -1.8, 1.8, 3.2):
        g.beam(3, 'riel', (xr * 0.8, 12, 0.05), (xr, 105, 0.05), 0.14, 0.06)
        g.beam(2, 'riel', (xr, 105, 0.05), (xr, 215, 0.05), 0.14, 0.06)
    g.mat('tranvia', '#3d8a5a', rug=0.5); g.mat('tranvia_c', '#f0e6c8')
    g.box(2, 'tranvia', 2.5, 150, 0.7, 4.2, 12, 3.6); g.box(2, 'tranvia_c', 2.5, 150, 3.0, 4.3, 12.1, 1.3)
    g.box(2, 'poste', 2.5, 150, 4.3, 0.2, 0.2, 2.6)
    ventanas(g, 2, n, 2.5, 144, 3.0, 4.2, 1.6, 2, 1, 4, prob=0.9)
    for yy in (40, 70, 100, 130, 160):
        farola(g, n, 3 if yy < 105 else 2, -11.2, yy, 6.0, 1.4); farola(g, n, 3 if yy < 105 else 2, 11.2, yy, 6.0, -1.4)
    # cebra y lineas
    g.mat('pintura', '#c4c2b8', bruma=0.5)
    for i in range(-6, 7):
        g.box(3, 'pintura', i * 1.7, 24, 0.02, 0.9, 5, 0.02)
    META['fade3'] = None
    # primer plano: cables, farola a la izquierda, rama a la derecha
    cables(g, 4, 12, [(0.0, 0.035, 0.05), (0.03, 0.01, 0.045)])
    fx, fz = vista(0.05, 1.2, 12)
    g.mat('poste4', '#25272c', bruma=0)
    g.cyl(4, 'poste4', fx, 12, fz, 0.35, 13, 8)
    g.beam(4, 'poste4', (fx, 12, vista(0, 0.1, 12)[1]), (fx + 1.0, 12, vista(0, 0.08, 12)[1]), 0.1)
    g.mat('lampara4', '#fff0c0', emi='#ffd070', fuerza=14.0 if n else 0.0, bruma=0)
    g.box(4, 'lampara4', fx + 1.0, 12, vista(0, 0.08, 12)[1] - 0.5, 0.5, 0.5, 0.5)
    g.mat('hoja_g', '#2e7a3c', ruido=0.4, color2='#1a5a2a', esc=5, bruma=0)
    ramas_pino(g, 4, n, 1.0, 0.02, 0.76, 0.14, 12.0, 3, mat_n='hoja_g', tufos=12)

# ================================================================ MONTE_FUJI
def monte_fuji(g, n):
    g.cfg['sol'] = (0.2, 0.2) if not n else None
    g.cfg['luna'] = (0.22, 0.2) if n else None
    g.cfg['dh'] = 5000.0
    cielo(g, n, semilla=8)
    g.mat('lago', '#3b82b0', rug=0.4, ruido=0.3, color2='#27669a', esc=0.2, bump=0.1, spec=0.06)
    g.terreno(1, 'lago', -7000, 7000, 14, 7000, 1, 1, lambda x, y: 0.0)
    d = 2900.0; H = 420.0; cx = 140.0
    def fuji(x):
        t = abs(x - cx) / 1250.0
        return H * max(0.0, 1 - t) ** 1.7 * (1 + 0.02 * ruido1d(x, 3, 40)) if t < 1 else 0.0
    g.mat('fuji', '#5f72a6', ruido=0.3, color2='#46588f', esc=40)
    g.mat('nieve', '#f4f7ff', rug=0.7, ruido=0.2, color2='#d8e2f5', esc=25)
    g.perfil(1, 'fuji', cx - 1250, cx + 1250, d, d + 300, -10, fuji, 160)
    xs = [cx - 360 + i * 720 / 60 for i in range(61)]
    top = [(x, fuji(x)) for x in xs]
    bot = [(x, min(fuji(x) - 2, 0.62 * H + 30 * ruido1d(x * 0.7, 5, 14) + 18 * abs(math.sin((x - cx) * 0.045)))) for x in xs]
    g.poli(1, 'nieve', top + bot[::-1], d - 4, d + 5)
    g.mat('colina1', '#4a7c6a', ruido=0.3, color2='#355f55', esc=30)
    g.perfil(1, 'colina1', -2300, 2300, 1900, 2100, -10, lambda x: 90 * (0.5 + 0.5 * ruido1d(x, 12, 250)) + 20)
    # capa 2: orilla boscosa, pagoda de cinco pisos, cerezos
    yb = 170.0
    g.mat('orilla2', '#5f9a52', ruido=0.5, color2='#3f7a3d', esc=0.15)
    alt = lambda x, y: (3.0 + 10 * math.exp(-((x + 55) / 30.0) ** 2) + 6 * math.exp(-((x - 60) / 25.0) ** 2)) * suave((y - (yb - 30)) / 40.0)
    g.terreno(2, 'orilla2', -140, 140, yb - 30, yb + 70, 70, 10, alt)
    g.mat('p_rojo', '#c9382d', rug=0.6); g.mat('p_teja', '#3e4b47', rug=0.5); g.mat('p_oro', '#d9b24a', metal=0.5); g.mat('p_piedra', '#9a978f', ruido=0.3, esc=2)
    px, py = -16.0, yb + 12
    g.box(2, 'p_piedra', px, py, 0, 17, 14, 2.6)
    z = 2.6; ancho = 11.5
    for k in range(5):
        g.box(2, 'p_rojo', px, py, z, ancho, ancho * 0.75, 3.0)
        ventanas(g, 2, n, px, py - ancho * 0.375, z + 0.5, ancho * 0.8, 2.2, 3, 1, k, prob=0.8)
        g.hip(2, 'p_teja', px, py, z + 2.8, ancho + 5.5, ancho * 0.75 + 5.5, 2.4, top=0.35)
        z += 5.0; ancho *= 0.86
    g.cyl(2, 'p_oro', px, py, z - 0.6, 0.25, 6.5, 6)
    for i in range(4):
        g.cyl(2, 'p_oro', px, py, z + 1.0 + i * 1.2, 1.0 - i * 0.15, 0.35, 8)
    r = random.Random(21)
    for i in range(14):
        x = r.uniform(-110, 110)
        if abs(x - px) < 10: continue
        y = yb + r.uniform(0, 45)
        sakura(g, 2, x, y, r.uniform(8, 12), i) if i % 3 else arboles_cono(g, 2, [(x, y, alt(x, y), 10, 2.8)])
    g.mat('pino2', '#2f6b45', ruido=0.35, color2='#1d4a33', esc=1.2)
    # capa 3: ribera con cesped y agua somera
    orilla(g, n, '#7fb85a', '#4f8f3c', '#3f86b5', '#2a679a', 40, False, 5, 0.15)
    g.mat('roca3', '#807a70', ruido=0.35, color2='#5b564f', esc=1.0)
    rocas(g, 3, 'roca3', [(-17, 54, -0.2, 2.2), (-14, 55, -0.2, 1.3), (22, 60, -0.3, 1.8)], 3)
    sakura(g, 3, -17, 32, 10, 5); sakura(g, 3, 16, 48, 9, 6)
    rr = random.Random(3)
    g.mat('petalo', '#fbd0dc', bruma=0.2)
    for i in range(110):
        yy = rr.uniform(14, 46); xx = rr.uniform(-0.34, 0.34) * yy * 1.6 * 0.72 * 1.4
        g.box(3, 'petalo', xx, yy, 0.03, 0.18, 0.12, 0.02, rr.uniform(0, 3))
    # capa 4: rama de cerezo arriba a la derecha, hierba abajo
    g.mat('rama4', '#4a3524', bruma=0); g.mat('flor4', '#f7bccd', ruido=0.4, color2='#e787a8', esc=8, bruma=0)
    x0, z0 = vista(1.02, 0.03, 12); x1, z1 = vista(0.72, 0.18, 12)
    g.beam(4, 'rama4', (x0, 12, z0), (x1, 12, z1), 0.16)
    for i in range(1, 16):
        t = i / 16.0
        bx, bz = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
        for sg in (-1, 1):
            ex, ez = bx - rr.uniform(0.1, 0.6), bz + sg * rr.uniform(0.3, 0.9)
            g.beam(4, 'rama4', (bx, 12, bz), (ex, 12, ez), 0.06)
            for _ in range(9):
                g.sph(4, 'flor4', ex + rr.uniform(-.5, .5), 12 + rr.uniform(-.3, .3), ez + rr.uniform(-.5, .5), rr.uniform(.07, .12), seg=5, ani=3)
    g.mat('hierba4', '#6a9c3c', ruido=0.3, color2='#41741f', esc=3, bruma=0)
    hierba_borde(g, 4, 'hierba4', 12.0, 0.0, 0.16, 0.99, 1.1, 20, 6)

# ================================================================ TORRE_TOKIO
def torre_tokio(g, n):
    g.cfg['sol'] = (0.8, 0.2) if not n else None
    g.cfg['luna'] = (0.8, 0.2) if n else None
    cielo(g, n, semilla=11)
    r = random.Random(12)
    g.mat('urbe1', '#aeb9c8', ruido=0.25, color2='#8d9bb0', esc=15)
    for i in range(60):
        g.box(1, 'urbe1', -900 + i * 30 + r.uniform(-6, 6), 2200 + r.uniform(0, 100), 0, r.uniform(20, 36), 30, r.uniform(25, 110))
    g.mat('lejos2', '#7a95b0', ruido=0.2, esc=30)
    g.perfil(1, 'lejos2', -3500, 3500, 3800, 4000, -20, lambda x: 160 * (0.5 + 0.5 * ruido1d(x, 31, 600)) + 40)
    g.mat('asfalto', '#585b62', ruido=0.4, color2='#44474e', esc=0.5)
    plano(g, 2, 'asfalto', 15, 900)
    # Torre de Tokio: antena en (u=0.30, v=0.12)
    D = 650.0
    tx, zt = vista(0.30, 0.12, D)
    Hh = zt
    g.mat('t_rojo', '#e2492b', rug=0.5); g.mat('t_blanco', '#f4efe6', rug=0.5)
    g.mat('t_luz', '#ffd9a0', emi='#ffb860', fuerza=5.0 if n else 0.0)
    bw = Hh * 0.16
    def ancho(z):
        return bw * (1 - min(1.0, z / (Hh * 0.52))) ** 0.9 + Hh * 0.012
    niv = [Hh * 0.52 * i / 14.0 for i in range(15)]
    for i in range(14):
        za, zb = niv[i], niv[i + 1]
        m = 't_rojo' if (i // 2) % 2 == 0 else 't_blanco'
        wa, wb = ancho(za), ancho(zb)
        for sx in (-1, 1):
            for sy in (-1, 1):
                g.beam(2, m, (tx + sx * wa, D + sy * wa, za), (tx + sx * wb, D + sy * wb, zb), Hh * 0.011)
        for sg in (-1, 1):
            g.beam(2, m, (tx - wa, D + sg * wa, za), (tx + wb, D + sg * wb, zb), Hh * 0.005)
            g.beam(2, m, (tx + wa, D + sg * wa, za), (tx - wb, D + sg * wb, zb), Hh * 0.005)
        for sg in (-1, 1):
            g.beam(2, m, (tx + sg * wa, D - wa, za), (tx + sg * wa, D + wa, za), Hh * 0.003)
        g.box(2, m, tx, D, zb - 0.2, 2 * wb, 2 * wb, Hh * 0.004)
    for (zc, wd, hh) in ((Hh * 0.205, 1.55, Hh * 0.04), (Hh * 0.34, 1.0, Hh * 0.03)):
        w2 = ancho(zc) * wd + Hh * 0.015
        g.box(2, 't_rojo', tx, D, zc, 2 * w2, 2 * w2, hh)
        g.box(2, 't_luz', tx, D - w2 - 0.1, zc + hh * 0.3, 2 * w2 * 0.9, 0.4, hh * 0.35)
    zt0 = Hh * 0.52
    for k in range(7):
        g.cyl(2, 't_rojo' if k % 2 == 0 else 't_blanco', tx, D, zt0 + (Hh - zt0) * k / 7.0, Hh * 0.012 * (1 - k * 0.09) + 0.1, (Hh - zt0) / 7.0, 8)
    g.cyl(2, 't_luz', tx, D, Hh - 2, 0.3, 2, 6)
    # edificios de oficinas (capa 2) y torres de iluminacion
    g.mat('techo_pl', '#808389')
    for nm, c in (('b1', '#c8c4b8'), ('b2', '#a4abb7'), ('b3', '#b9a58f'), ('b4', '#8fa0a8')):
        g.mat(nm, c, ruido=0.25, color2=mezcla(c, '#000000', 0.3), esc=3)
    for i in range(26):
        x = -150 + i * 12 + r.uniform(-2, 2)
        if abs(x - tx) < 10: continue
        edificio(g, n, 2, x, 300 + r.uniform(-20, 40), r.uniform(9, 14), 12, r.uniform(8, 13), 'b%d' % (1 + i % 4), sem=i)
    # capa 3: plaza
    g.mat('plaza', '#b8b2a6', ruido=0.4, color2='#9a948a', esc=0.4, bump=0.1)
    g.mat('junta', '#7c776e', bruma=0.4)
    g.mat('seto', '#3f7a43', ruido=0.4, color2='#285c2f', esc=1.5)
    g.terreno(3, 'plaza', -60, 60, 10, 90, 1, 1, lambda x, y: 0.0)
    for x in range(-60, 61, 6):
        g.beam(3, 'junta', (x * 0.3, 10, 0.02), (x * 1.7, 90, 0.02), 0.08, 0.02)
    for y in (18, 26, 36, 50, 68):
        g.box(3, 'junta', 0, y, 0.0, 130, 0.1, 0.03)
    g.mat('pintura', '#e8e6dc', bruma=0.5)
    for i in range(-5, 6):
        g.box(3, 'pintura', i * 1.9, 74, 0.02, 1.0, 5, 0.02)
    for (x, y) in ((-20, 80), (-9, 80), (9, 80), (20, 80)):
        g.box(3, 'seto', x, y, 0, 8, 3, 1.5)
    for x, y in ((-17, 44), (17, 50), (-5, 88), (6, 88)):
        sakura(g, 3, x, y, 7, int(x)) if False else arboles_cono(g, 3, [(x, y, 0, 8, 2.6)])
    g.mat('pino2', '#2f6b45', ruido=0.35, color2='#1d4a33', esc=1.2)
    for x in (-14, 14, -26, 26):
        farola(g, n, 3, x * 0.9, 38 + abs(x), 6.5, 1.5 if x < 0 else -1.5)
    META['fade3'] = None
    cables(g, 4, 12, [(0.0, 0.025, 0.05), (0.03, 0.06, 0.05), (0.0, 0.0, 0.045)])
    fx, fz = vista(0.95, 1.2, 12)
    g.mat('poste4', '#3c3a38', bruma=0)
    g.cyl(4, 'poste4', fx, 12, fz, 0.35, 13, 8)
    g.beam(4, 'poste4', (fx - 0.8, 12, vista(0, 0.05, 12)[1]), (fx + 0.4, 12, vista(0, 0.05, 12)[1]), 0.12)
    g.mat('hoja_g', '#2e7a3c', ruido=0.4, color2='#1a5a2a', esc=5, bruma=0)
    ramas_pino(g, 4, n, 0.0, 0.1, 0.2, 0.26, 12.0, 6, mat_n='hoja_g', tufos=10)

# ================================================================ OSAKA
def osaka(g, n):
    g.cfg['sol'] = (0.2, 0.2) if not n else None
    g.cfg['luna'] = (0.2, 0.2) if n else None
    cielo(g, n, semilla=13)
    g.mat('colina1', '#5f8f7a', ruido=0.3, color2='#46735f', esc=30)
    g.perfil(1, 'colina1', -2800, 2800, 2400, 2600, -20, lambda x: 120 * (0.5 + 0.5 * ruido1d(x, 17, 500)) + 25)
    g.mat('urbe1', '#aab7c6', ruido=0.25, color2='#8b9bb0', esc=15)
    r = random.Random(14)
    for i in range(34):
        g.box(1, 'urbe1', -460 + i * 28 + r.uniform(-5, 5), 1400 + r.uniform(0, 80), 0, r.uniform(16, 26), 22, r.uniform(14, 50))
    g.mat('cesped', '#6aa04d', ruido=0.5, color2='#4a7f38', esc=0.2)
    plano(g, 2, 'cesped', 15, 800, ext=0.8)
    g.mat('piedra', '#a7a69d', ruido=0.4, color2='#7d7c74', esc=1.0, bump=0.2)
    g.mat('o_blanco', '#f1ede2', ruido=0.2, color2='#d8d2c0', esc=3); g.mat('o_verde', '#4a8c78', rug=0.5, metal=0.2)
    g.mat('o_oro', '#e0b84a', metal=0.7, rug=0.35); g.mat('o_negro', '#2b2a2e'); g.mat('techo_pl', '#4a8c78')
    g.mat('foso', '#3f7f8a', rug=0.4, ruido=0.3, color2='#2c6270', esc=0.3, bump=0.1, spec=0.08)
    cy = 250.0
    g.terreno(2, 'foso', -120, 120, 150, 232, 1, 1, lambda x, y: -0.6)
    g.box(2, 'piedra', 0, 236, -0.6, 160, 6, 6.2)                      # muro ishigaki del foso
    g.hip(2, 'piedra', 0, cy, 0, 56, 36, 10, top=0.78)                  # base de piedra del castillo
    z = 10.0; w, d = 36.0, 22.0
    for k in range(5):
        h = 4.6 if k < 4 else 3.2
        g.box(2, 'o_blanco', 0, cy, z, w, d, h)
        ventanas(g, 2, n, 0, cy - d / 2, z + 0.6, w * 0.8, h * 0.6, max(2, int(w / 6)), 1, k, prob=0.5)
        g.hip(2, 'o_verde', 0, cy, z + h - 0.2, w + 6, d + 5, 3.2 if k < 4 else 4.2, top=0.5 if k < 4 else 0.1)
        g.box(2, 'o_oro', 0, cy - d / 2 - 3.0, z + h + 0.8, 5, 0.6, 1.4)              # karahafu dorado
        g.box(2, 'o_oro', 0, cy, z + h + 3.0, w * 0.5, 0.5, 0.4) if k == 4 else None
        z += h + 2.6; w *= 0.78; d *= 0.8
    for sx in (-1, 1):
        g.sph(2, 'o_oro', sx * 3.2, cy, z + 1.0, 0.7, 0.4, 1.4, seg=6, ani=4)          # shachihoko
    # pinos y arboles
    g.mat('pino2', '#2f6b45', ruido=0.35, color2='#1d4a33', esc=1.2)
    arboles_cono(g, 2, [(x, y, 0, r.uniform(11, 16), r.uniform(3, 4.2)) for x in (-80, -62, -48, 52, 66, 82, 96, -98) for y in (r.uniform(215, 262),)])
    # capa 3: camino de piedra con seto y muro bajo al fondo
    g.mat('camino', '#b4ada0', ruido=0.4, color2='#928b7e', esc=0.4, bump=0.15)
    g.mat('junta', '#6e695f', bruma=0.4)
    g.terreno(3, 'camino', -60, 60, 10, 96, 1, 1, lambda x, y: 0.0)
    for x in range(-60, 61, 5):
        g.beam(3, 'junta', (x * 0.3, 10, 0.02), (x * 1.8, 96, 0.02), 0.08, 0.02)
    for y in (16, 22, 30, 40, 52, 66, 82):
        g.box(3, 'junta', 0, y, 0.0, 140, 0.1, 0.03)
    g.mat('seto', '#3f7a43', ruido=0.4, color2='#285c2f', esc=1.5)
    g.box(3, 'piedra', 0, 96, 0, 130, 3.0, 1.9)
    for x in range(-60, 61, 12):
        g.box(3, 'seto', x * 0.95, 95.5, 1.9, 8, 2.6, 1.5)
    # linternas de piedra
    def linterna(L, x, y, s=1.0):
        g.mat('linterna', '#8f8f86', ruido=0.4, esc=3, bruma=0.3)
        g.mat('l_luz', '#ffe2a0', emi='#ffb85a', fuerza=12.0 if n else 0.0, bruma=0.3)
        g.cyl(L, 'linterna', x, y, 0, 0.5 * s, 1.0 * s, 8); g.cyl(L, 'linterna', x, y, 1.0 * s, 0.25 * s, 1.0 * s, 8)
        g.box(L, 'linterna', x, y, 2.0 * s, 1.1 * s, 1.1 * s, 0.2 * s); g.box(L, 'l_luz', x, y, 2.2 * s, 0.7 * s, 0.7 * s, 0.8 * s)
        g.hip(L, 'linterna', x, y, 3.0 * s, 1.8 * s, 1.8 * s, 0.9 * s, top=0.15)
    linterna(3, -14, 40, 1.6); linterna(3, 14, 46, 1.6); linterna(3, -24, 70, 1.6); linterna(3, 25, 76, 1.6)
    pino_p = [(-20, 30), (21, 34)]
    arboles_cono(g, 3, [(x, y, 0, 11, 3.0) for x, y in pino_p])
    META['fade3'] = None
    ramas_pino(g, 4, n, 0.0, 0.02, 0.3, 0.17, 12.0, 4)
    lx, lz = vista(0.94, 1.08, 12)
    g.mat('linterna4', '#8f8f86', ruido=0.4, esc=3, bruma=0); g.mat('l_luz4', '#ffe2a0', emi='#ffb85a', fuerza=12.0 if n else 0.0, bruma=0)
    g.cyl(4, 'linterna4', lx, 12, lz, 0.35, 0.7, 8); g.cyl(4, 'linterna4', lx, 12, lz + 0.7, 0.18, 0.7, 8)
    g.box(4, 'linterna4', lx, 12, lz + 1.4, 0.8, 0.8, 0.15); g.box(4, 'l_luz4', lx, 12, lz + 1.55, 0.5, 0.5, 0.55)
    g.hip(4, 'linterna4', lx, 12, lz + 2.1, 1.3, 1.3, 0.6, top=0.15)

# ================================================================ KAMAKURA
def kamakura(g, n):
    g.cfg['sol'] = (0.25, 0.2) if not n else None
    g.cfg['luna'] = (0.25, 0.2) if n else None
    cielo(g, n, semilla=15)
    mar_lejano(g, '#2f88b3', '#1f6c98')
    g.mat('colina1', '#4f8a72', ruido=0.3, color2='#386f5c', esc=30)
    g.perfil(1, 'colina1', -2800, 0, 2200, 2400, -20, lambda x: 130 * (0.5 + 0.5 * ruido1d(x, 19, 400)) + 30)
    g.perfil(1, 'colina1', -1600, 1600, 1500, 1700, -20, lambda x: max(0, 70 * math.exp(-((x + 900) / 500.0) ** 2) * 1.2))
    # capa 2: colinas boscosas y Gran Buda
    yb = 190.0
    g.mat('monte', '#4f8c50', ruido=0.5, color2='#2f6a3a', esc=0.12)
    alt = lambda x, y: (6 + 32 * math.exp(-((x + 55) / 38.0) ** 2) + 30 * math.exp(-((x - 55) / 40.0) ** 2) - 5 * math.exp(-(x / 18.0) ** 2) * suave((y - yb) / 50.0)) * suave((y - (yb - 40)) / 60.0)
    g.terreno(2, 'monte', -140, 140, yb - 40, yb + 70, 90, 12, alt)
    g.mat('pino2', '#2f6b45', ruido=0.35, color2='#1d4a33', esc=1.2)
    r = random.Random(23)
    arboles_cono(g, 2, [(x, y, alt(x, y) - 0.3, r.uniform(8, 14), r.uniform(2.4, 3.6)) for x, y in [(r.uniform(-120, 120), yb + r.uniform(-10, 60)) for _ in range(70)] if abs(x) > 16 or y > yb + 25])
    g.mat('bronce', '#5f8f78', ruido=0.3, color2='#3f6c58', esc=1.2, rug=0.55, metal=0.4)
    g.mat('piedra', '#9a978f', ruido=0.3, esc=2)
    k = 1.5
    by = yb + 6; gz = alt(0, by) + 0.5
    g.box(2, 'piedra', 0, by, gz - 0.5, 26, 16, 3.0)
    g.sph(2, 'bronce', 0, by, gz + 5.0, 10 * k * 0.8, 5.5, 3.4 * k * 0.8, seg=14, ani=6)      # piernas cruzadas
    g.sph(2, 'bronce', 0, by, gz + 9.5, 6.0 * k * 0.75, 4.2, 6.5 * k * 0.75, seg=12, ani=6)     # tronco
    g.sph(2, 'bronce', 0, by, gz + 14.0, 5.0 * k * 0.7, 3.6, 3.4 * k * 0.7, seg=12, ani=6)      # hombros
    g.sph(2, 'bronce', 0, by, gz + 19.6, 3.0 * k * 0.9, 3.0, 3.2 * k * 0.9, seg=12, ani=6)      # cabeza
    for i in range(9):
        for j in range(4):
            g.sph(2, 'bronce', -3.0 + i * 0.75, by - 2.7, gz + 20.6 + j * 0.95, 0.5, seg=4, ani=3)  # rizos
    g.sph(2, 'bronce', 0, by - 0.4, gz + 24.0, 1.3, 1.3, 1.6, seg=8, ani=4)                     # ushnisha
    g.box(2, 'bronce', -2.4, by - 3.2, gz + 7.6, 4.8, 2.2, 1.6)                                    # manos
    for sx in (-1, 1):
        g.sph(2, 'bronce', sx * 3.9, by, gz + 19.0, 0.8, 1.0, 2.6, seg=6, ani=4)                 # orejas
        g.beam(2, 'bronce', (sx * 5.2, by - 0.5, gz + 13.4), (sx * 5.8, by - 2.2, gz + 8.0), 2.4, 2.6)   # brazos
    # capa 3: playa
    orilla(g, n, '#e8d3a0', '#c8aa72', '#4fbcc6', '#2d93aa', 40, True, 7, 0.3)
    g.mat('roca3', '#807a70', ruido=0.35, color2='#5b564f', esc=1.0)
    rocas(g, 3, 'roca3', [(15, 56, -0.2, 2.4), (18, 57, -0.2, 1.4), (-22, 62, -0.3, 2.0)], 3)
    g.mat('madero', '#8a7156', ruido=0.3, esc=3)
    g.beam(3, 'madero', (-12, 30, 0.35), (-5, 33, 0.45), 0.7)
    g.beam(3, 'madero', (8, 24, 0.3), (14, 22, 0.4), 0.5)
    arboles_cono(g, 3, [(-26, 36, 0, 10, 3.0), (-22, 46, 0, 8, 2.4)])
    ramas_pino(g, 4, n, 1.0, 0.02, 0.72, 0.15, 12.0, 8)
    g.mat('hierba4', '#6a9c3c', ruido=0.3, color2='#41741f', esc=3, bruma=0)
    hierba_borde(g, 4, 'hierba4', 12.0, 0.0, 0.18, 0.99, 1.2, 22, 9)

# ================================================================ ISLA_MONSTRUOS
def isla_monstruos(g, n):
    g.cfg['sol'] = (0.2, 0.2) if not n else None
    g.cfg['luna'] = (0.2, 0.2) if n else None
    if not n:
        g.cfg['top'] = '#2f8ee8'; g.cfg['hor'] = '#fdeccb'
    cielo(g, n, semilla=16)
    mar_lejano(g, '#1fa3b8', '#1479a0')
    g.mat('lejos1', '#4f8a78', ruido=0.2, color2='#3a6f66', esc=30)
    g.perfil(1, 'lejos1', -2600, 2600, 2600, 2800, -20, lambda x: 60 * math.exp(-((x + 700) / 260.0) ** 2) + 35 * math.exp(-((x - 900) / 200.0) ** 2))
    yb = 190.0
    cr = 60.0
    def alt(x, y):
        dx = (x - 20)
        base = 7 * math.exp(-((x + 20) / 95.0) ** 2) * suave((y - (yb - 40)) / 50.0)
        v = 40 * max(0.0, 1 - abs(dx) / 62.0) ** 1.5 * suave((y - (yb - 20)) / 60.0)
        if abs(dx) < 7:
            v = min(v, 35 - 4 * (1 - abs(dx) / 7))
        return base + v
    g.mat('volcan', '#6a5a50', ruido=0.4, color2='#3d322e', esc=0.15)
    g.mat('selva', '#3f8f45', ruido=0.5, color2='#245f2f', esc=0.12)
    g.terreno(2, 'selva', -150, 150, yb - 40, yb + 80, 100, 14, lambda x, y: alt(x, y) if alt(x, y) < 14 else 14 + (alt(x, y) - 14) * 0.2)
    g.terreno(2, 'volcan', -150, 150, yb - 40, yb + 80, 100, 14, lambda x, y: alt(x, y) + 0.05 if alt(x, y) >= 14 else alt(x, y) - 0.6)
    g.mat('arena', '#ead7a3', ruido=0.3, esc=0.2)
    g.terreno(2, 'arena', -150, 150, yb - 40, yb + 80, 100, 14, lambda x, y: min(alt(x, y), 0.9) + 0.03)
    g.mat('lava', '#ff6a1a', emi='#ff5a10', fuerza=9.0 if n else 1.2, bruma=0.2)
    g.box(2, 'lava', 20, yb + 22, 34.4, 12, 10, 0.8)
    for dx, ln in ((-3, 5), (2, 9), (5, 4)):                       # coladas de lava por la ladera
        g.box(2, 'lava', 20 + dx, yb + 5, 34.0 - ln, 0.9, 0.5, ln)
    g.mat('humo', '#7a7578' if not n else '#3a3a48', rug=1.0, emi='#ff7a30' if n else None, fuerza=0.15 if n else 0.0, bruma=0.6)
    for k in range(7):
        g.sph(2, 'humo', 20 + k * 4 - 3, yb + 24, 40 + k * 6, 5.0 + k * 2.0, 4 + k, 4 + k * 1.3, seg=8, ani=4)
    r = random.Random(33)
    for i in range(22):
        x = r.uniform(-110, 90); y = yb + r.uniform(-25, 40)
        z = alt(x, y)
        if z < 14:
            palmera(g, 2, x, y, r.uniform(8, 12), r.uniform(-.5, .8), i)
    # capa 3: arena de selva con palmeras y helechos
    orilla(g, n, '#e1cc92', '#b79c62', '#35b8c4', '#1f8ca6', 40, True, 11, 0.3)
    palmera(g, 3, -16, 34, 12, 0.7, 1); palmera(g, 3, 18, 52, 11, -0.6, 2); palmera(g, 3, -14, 56, 9, 0.5, 3)
    g.mat('roca3', '#6f6a60', ruido=0.35, color2='#4b473f', esc=1.0)
    rocas(g, 3, 'roca3', [(12, 38, -0.1, 1.6), (13.5, 39, -0.1, 1.0), (-8, 62, -0.3, 2.0)], 3)
    # capa 4: hojas de palmera arriba y hoja grande abajo
    g.mat('hoja4', '#2f9a3e', ruido=0.4, color2='#176d2a', esc=4, bruma=0)
    x0, z0 = vista(1.0, 0.0, 10)
    for k in range(5):
        fronda(g, 4, 'hoja4', x0, 10 + k * 0.05, z0 + 0.3, math.pi + 0.35 + k * 0.28, 2.4, 0.7, 0.5)
    x1, z1 = vista(0.0, 1.0, 10)
    for k in range(4):
        fronda(g, 4, 'hoja4', x1, 10 + k * 0.05, z1 - 0.1, 0.55 + k * 0.28, 1.8, 0.6, 0.3)
    g.mat('rama4', '#4a3524', bruma=0)

ESCENAS = {'ISLA_ODO': isla_odo, 'BAHIA_TOKIO': bahia_tokio, 'GINZA': ginza, 'MONTE_FUJI': monte_fuji,
           'TORRE_TOKIO': torre_tokio, 'OSAKA': osaka, 'KAMAKURA': kamakura, 'ISLA_MONSTRUOS': isla_monstruos}



# ================================================================ renderizado
def generar(nombre, momento, capas, muestras, prueba=False):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    noche = momento == 'noche'
    META.clear()
    g = Geo(noche)
    g.cfg = cfg_base(noche)
    ESCENAS[nombre](g, noche)
    g.construir()
    preparar_mundo(g.cfg)
    preparar_luces(g.cfg)
    preparar_camara()
    motor = preparar_render(muestras)
    sc = bpy.context.scene
    if prueba:
        sc.render.resolution_percentage = 33
    carpeta = os.path.join(TMP, nombre)
    os.makedirs(carpeta, exist_ok=True)
    for l in capas:
        for k in range(5):
            bpy.data.collections['L%d' % k].hide_render = (k != l)
        sc.render.film_transparent = (l != 0)
        sc.render.filepath = os.path.join(carpeta, '%s_%d.png' % (momento, l))
        t0 = time.time()
        bpy.ops.render.render(write_still=True)
        print('[%s %s capa %d] %s %.1fs' % (nombre, momento, l, motor, time.time() - t0), flush=True)
    meta = dict(META)
    meta['sol'] = g.cfg.get('sol'); meta['luna'] = g.cfg.get('luna')
    with open(os.path.join(carpeta, 'meta_%s.json' % momento), 'w') as f:
        json.dump(meta, f)


def main():
    args = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    escenas = [a for a in args if a in ESCENAS] or list(ESCENAS)
    momentos = ['dia', 'noche']
    capas, muestras, prueba = [0, 1, 2, 3, 4], 48, '--prueba' in args
    if '--solo' in args:
        momentos = [args[args.index('--solo') + 1]]
    if '--capas' in args:
        capas = [int(c) for c in args[args.index('--capas') + 1].split(',')]
    if '--muestras' in args:
        muestras = int(args[args.index('--muestras') + 1])
    for e in escenas:
        for m in momentos:
            generar(e, m, capas, muestras, prueba)

main()
