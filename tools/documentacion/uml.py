# Genera diagramas de clases UML (HTML + SVG) a partir de clases.json (extraído del código real).
import json, html, re, sys, subprocess, os

D = json.load(open('clases.json'))
OUT = 'uml'
os.makedirs(OUT, exist_ok=True)

VIS = {'public': '+', 'private': '-', 'protected': '#', '': '~'}


def params(s):
    if not s.strip():
        return ''
    out = []
    depth = 0; cur = ''
    for ch in s:
        if ch == '<': depth += 1
        if ch == '>': depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur); cur = ''
        else:
            cur += ch
    out.append(cur)
    res = []
    for p in out:
        p = p.strip().split()
        if len(p) >= 2:
            res.append(f'{p[-1]}: {" ".join(p[:-1])}')
    return ', '.join(res)


def fila_campo(f):
    nombres = [n.strip() for n in f['name'].split(',')]
    txt = f"{VIS[f['vis']]} {f['name']}: {f['type']}"
    # campos múltiples tipo "double a, b" (el parser deja el primero en name y el resto en tipo)
    m = re.match(r'(\w+(?:<[^>]*>)?(?:\[\])?)\s+(.*)', f['type'])
    if m and ',' in f['type']:
        txt = f"{VIS[f['vis']]} {m.group(2).rstrip(', ')}, {f['name']}: {m.group(1)}"
    if f['static'] and f.get('final'):
        txt = txt.replace(f"{VIS[f['vis']]} ", f"{VIS[f['vis']]} ", 1)
    return txt, f['static']


def fila_metodo(c, m):
    ret = '' if not m['ret'] else ': ' + m['ret']
    return f"{VIS[m['vis']]} {m['name']}({params(m['params'])}){ret}", m['static']


def caja(nombre, x, y, campos='all', metodos='all', ancho=None, nota=None, externo=False, este=None):
    """campos/metodos: 'all', lista de nombres o [] ; devuelve html"""
    if externo:
        return (f'<div class="box ext" id="{nombre}" style="left:{x}px;top:{y}px;{f"width:{ancho}px" if ancho else ""}">'
                f'<div class="hd"><i>{html.escape(este or "")}</i><b>{html.escape(nombre)}</b></div></div>')
    c = D[nombre]
    kind = c['kind']
    st = {'enum': '«enumeration»', 'interface': '«interface»'}.get(kind, '')
    if este: st = este
    filas_c = []
    if kind == 'enum':
        filas_c = [(v, False) for v in c['enum']]
    else:
        for f in c['fields']:
            if f['type'] in ('enum', 'interface'):
                continue
            if campos == 'all' or f['name'] in campos or any(n.strip() in campos for n in f['name'].split(',')):
                filas_c.append(fila_campo(f))
    filas_m = []
    omit = 0
    for m in c['methods']:
        if metodos == 'all' or m['name'] in metodos:
            filas_m.append(fila_metodo(c, m))
        else:
            omit += 1
    if campos != 'all' and kind != 'enum':
        oc = sum(1 for f in c['fields'] if f['type'] not in ('enum', 'interface')) - len(filas_c)
        if oc > 0: filas_c.append((f'… (+{oc} atributos)', False))
    if omit and metodos != []:
        filas_m.append((f'… (+{omit} métodos)', False))
    def sec(filas, cls):
        if not filas: return f'<div class="{cls} vacio"></div>'
        return f'<div class="{cls}">' + ''.join(
            f'<div class="{"st" if s else ""}">{html.escape(t)}</div>' for t, s in filas) + '</div>'
    hd = f'<div class="hd">{f"<i>{st}</i>" if st else ""}<b>{nombre}</b><span>{c["pkg"]}</span></div>'
    cuerpo = sec(filas_c, 'at') + ('' if kind == 'enum' and not filas_m else sec(filas_m, 'me'))
    w = f'width:{ancho}px;' if ancho else ''
    return f'<div class="box {kind}" id="{nombre}" style="left:{x}px;top:{y}px;{w}">{hd}{cuerpo}</div>'


CSS = '''
body{margin:0;background:#fff;font-family:"DejaVu Sans Mono",monospace;}
#lienzo{position:relative;}
.box{position:absolute;border:1.6px solid #1d2b44;background:#fbfcff;font-size:11.5px;line-height:1.33;
 box-shadow:3px 3px 0 #d8deea;border-radius:3px;min-width:150px;max-width:420px}
.box.enum{background:#fffaf0}.box.interface{background:#f3fff5}.box.ext{background:#eef0f4;border-style:dashed}
.hd{background:#1d2b44;color:#fff;text-align:center;padding:4px 8px;font-family:"DejaVu Sans",sans-serif}
.box.enum .hd{background:#8a4b08}.box.interface .hd{background:#1f6b35}.box.ext .hd{background:#6b7385}
.hd b{display:block;font-size:13.5px}.hd i{display:block;font-size:10.5px;font-style:normal;opacity:.85}
.hd span{display:block;font-size:9.5px;opacity:.7}
.at,.me{padding:4px 7px 4px 7px;border-top:1.4px solid #1d2b44}.at div,.me div{padding-left:1.4em;text-indent:-1.4em}.vacio{padding:2px}
.st{text-decoration:underline}
svg{position:absolute;left:0;top:0;overflow:visible}
.tit{position:absolute;left:20px;top:10px;font-family:"DejaVu Sans",sans-serif;font-size:22px;font-weight:bold;color:#1d2b44}
.ley{position:absolute;font-family:"DejaVu Sans",sans-serif;font-size:11px;color:#333;border:1px solid #bbb;padding:6px 10px;background:#fff}
.lbl{font-family:"DejaVu Sans",sans-serif;font-size:10.5px;fill:#333}
'''

JS = r'''
const rels = REL; const cols = COLS;
let cx=20; let cima=null; let y0=70;
if (cols.length && cols[0][0]==='@top'){ cima=document.getElementById(cols[0][1]); cols.shift(); y0=70+cima.getBoundingClientRect().height+110; }
for (const col of cols){ let y=y0, mw=0; const xs=cx;
  for (const id of col){ const e=document.getElementById(id); e.style.left=xs+'px'; e.style.top=y+'px';
    const r=e.getBoundingClientRect(); y+=r.height+70; mw=Math.max(mw,r.width);} cx+=mw+110; }
if (cima){ cima.style.top="70px"; cima.style.left=((cx-110)/2-cima.getBoundingClientRect().width/2)+'px'; }

const svg = document.querySelector('svg');
function R(id){const e=document.getElementById(id); if(!e) throw id; const r=e.getBoundingClientRect(); return {x:r.left+scrollX,y:r.top+scrollY,w:r.width,h:r.height};}
function borde(a, hacia){ // punto del borde de a en dirección del centro "hacia"
  const cx=a.x+a.w/2, cy=a.y+a.h/2, dx=hacia.x-cx, dy=hacia.y-cy;
  const sx = dx===0?1e9:(a.w/2)/Math.abs(dx), sy = dy===0?1e9:(a.h/2)/Math.abs(dy); const s=Math.min(sx,sy);
  return {x:cx+dx*s, y:cy+dy*s};}
let maxX=0,maxY=0;
document.querySelectorAll('.box,.ley').forEach(b=>{const r=b.getBoundingClientRect(); maxX=Math.max(maxX,r.right+scrollX); maxY=Math.max(maxY,r.bottom+scrollY);});
svg.setAttribute('width',maxX+20); svg.setAttribute('height',maxY+20);
document.getElementById('lienzo').style.width=(maxX+30)+'px'; document.getElementById('lienzo').style.height=(maxY+30)+'px';
const NS='http://www.w3.org/2000/svg';
for (const [a,b,tipo,etq,oa,ob] of rels){
  const A=R(a),B=R(b); const ca={x:A.x+A.w/2+(oa||0),y:A.y+A.h/2}, cb={x:B.x+B.w/2+(ob||0),y:B.y+B.h/2};
  const p1=borde(A,cb), p2=borde(B,ca);
  const l=document.createElementNS(NS,'line'); l.setAttribute('x1',p1.x);l.setAttribute('y1',p1.y);l.setAttribute('x2',p2.x);l.setAttribute('y2',p2.y);
  l.setAttribute('stroke', tipo==='dep'?'#7a7f8c':'#1d2b44'); l.setAttribute('stroke-width','1.5');
  if (tipo==='dep'||tipo==='real') l.setAttribute('stroke-dasharray','6 4');
  const fin={her:'url(#tri)',real:'url(#tri)',dep:'url(#flecha)',asoc:'url(#flecha)',comp:null,agr:null}[tipo];
  if (fin) l.setAttribute('marker-end',fin);
  if (tipo==='comp') l.setAttribute('marker-start','url(#rombo)');
  if (tipo==='agr') l.setAttribute('marker-start','url(#romboV)');
  svg.appendChild(l);
  if (etq){const t=document.createElementNS(NS,'text'); t.setAttribute('x',(p1.x+p2.x)/2+4); t.setAttribute('y',(p1.y+p2.y)/2-4); t.setAttribute('class','lbl'); t.textContent=etq; svg.appendChild(t);}
}
document.title='listo';
'''

DEFS = '''<defs>
<marker id="tri" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="14" markerHeight="14" orient="auto-start-reverse" markerUnits="userSpaceOnUse"><path d="M0,0 L12,6 L0,12 Z" fill="#fff" stroke="#1d2b44" stroke-width="1.3"/></marker>
<marker id="flecha" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto-start-reverse" markerUnits="userSpaceOnUse"><path d="M0,0 L12,6 L0,12" fill="none" stroke="#1d2b44" stroke-width="1.5"/></marker>
<marker id="rombo" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="18" markerHeight="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M0,5 L8,0 L16,5 L8,10 Z" fill="#1d2b44"/></marker>
<marker id="romboV" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="18" markerHeight="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M0,5 L8,0 L16,5 L8,10 Z" fill="#fff" stroke="#1d2b44"/></marker>
</defs>'''

LEY = ('<div class="ley" id="leyenda" style="left:{x}px;top:{y}px"><b>Notación UML</b><br>'
       '◆── composición (la parte vive dentro del todo) &nbsp; ◇── agregación<br>'
       '──▷ herencia (extends) &nbsp; - - ▷ realización (implements)<br>'
       '──&gt; asociación &nbsp; - - &gt; dependencia (usa)<br>'
       '+ público · − privado · # protegido · ~ paquete · <u>subrayado</u> = static</div>')


def pagina(nombre, titulo, cajas, rels, cols):
    ids = set(re.findall(r'id="(\w+)"', ''.join(cajas))) | {'leyenda'}
    rels = [r for r in rels if r[0] in ids and r[1] in ids]
    cols = [[c for c in col if c in ids or c == '@top'] for col in cols]
    h = (f'<!doctype html><html><head><meta charset="utf-8"><style>{CSS}</style></head><body><div id="lienzo">'
         f'<div class="tit">{html.escape(titulo)}</div><svg>{DEFS}</svg>' + ''.join(cajas) + LEY.format(x=0, y=0) +
         f'</div><script>{JS.replace("REL", json.dumps(rels)).replace("COLS", json.dumps(cols))}</script></body></html>')
    open(f'{OUT}/{nombre}.html', 'w').write(h)


exec(open('uml_specs.py').read())
