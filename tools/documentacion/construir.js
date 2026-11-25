// Genera el Word (.docx) y el HTML (para el PDF) a partir de contenido.js.
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Table, TableRow, TableCell, WidthType, AlignmentType,
  HeadingLevel, ShadingType, BorderStyle, LevelFormat, PageBreak, Footer, PageNumber, TableOfContents,
  VerticalAlign,
} = require('docx');
const C = require('./contenido');

const OUT = process.argv[2];
const NOMBRE = 'Tarea1_Documentacion_GodziPet90_Emilio_Garduno';
const AZUL = '0E1630', ROJO = 'C8102E', CELESTE = '3DE0FF';
const ANCHO = 9360; // ancho útil en DXA (carta con márgenes de 1")

// ---------------------------------------------------------------- utilidades
function tamImagen(src) {
  const b = fs.readFileSync(src);
  if (src.endsWith('.png')) return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), b, tipo: 'png' };
  let i = 2; // JPEG: buscar el marcador SOF
  while (i < b.length) {
    const m = b[i + 1], len = b.readUInt16BE(i + 2);
    if (m >= 0xC0 && m <= 0xC3) return { w: b.readUInt16BE(i + 7), h: b.readUInt16BE(i + 5), b, tipo: 'jpg' };
    i += 2 + len;
  }
  throw new Error('No se pudo leer ' + src);
}

function runs(texto, base = {}) {
  const partes = String(texto).split(/(\*\*[^*]+\*\*|\*[^*]+\*)/g).filter(Boolean);
  return partes.map((t) => {
    if (t.startsWith('**')) return new TextRun({ ...base, text: t.slice(2, -2), bold: true });
    if (t.startsWith('*')) return new TextRun({ ...base, text: t.slice(1, -1), italics: true });
    return new TextRun({ ...base, text: t });
  });
}

function imagen(src, anchoPulg) {
  const im = tamImagen(src);
  const w = Math.round(anchoPulg * 96), h = Math.round(w * im.h / im.w);
  return new ImageRun({ type: im.tipo, data: im.b, transformation: { width: w, height: h } });
}

const borde = { style: BorderStyle.SINGLE, size: 4, color: 'B8C2DA' };
const bordes = { top: borde, bottom: borde, left: borde, right: borde };

function celda(contenido, anchoDxa, opts = {}) {
  const hijos = Array.isArray(contenido) ? contenido : [new Paragraph({ children: runs(contenido, { size: opts.size || 19, color: opts.color }), alignment: opts.align })];
  return new TableCell({
    children: hijos, width: { size: anchoDxa, type: WidthType.DXA }, borders: bordes,
    shading: opts.fondo ? { type: ShadingType.CLEAR, color: 'auto', fill: opts.fondo } : undefined,
    margins: { top: 60, bottom: 60, left: 100, right: 100 }, verticalAlign: opts.va || VerticalAlign.TOP,
  });
}

function tablaDocx(colsPulg, enc, filas) {
  const total = colsPulg.reduce((a, b) => a + b, 0);
  const anchos = colsPulg.map((c) => Math.round(c / total * ANCHO));
  anchos[anchos.length - 1] += ANCHO - anchos.reduce((a, b) => a + b, 0);
  const filaEnc = new TableRow({ tableHeader: true, children: enc.map((t, i) => celda(t, anchos[i], { fondo: '22305E', color: 'FFFFFF', size: 19 })) });
  const resto = filas.map((f, k) => new TableRow({ cantSplit: true, children: f.map((t, i) => celda(t, anchos[i], { fondo: k % 2 ? 'F3F5FC' : undefined })) }));
  return new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: anchos, rows: [filaEnc, ...resto] });
}

function pie(texto) {
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: runs(texto, { size: 17, italics: true, color: '5A6794' }) });
}

// ---------------------------------------------------------------- Word
function bloqueDocx(b) {
  switch (b.t) {
    case 'h1': return [new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(b.v)] })];
    case 'h2': return [new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(b.v)] })];
    case 'p': return [new Paragraph({ children: runs(b.v), spacing: { after: 120 }, alignment: AlignmentType.JUSTIFIED })];
    case 'ul': return b.v.map((t) => new Paragraph({ numbering: { reference: 'vinetas', level: 0 }, children: runs(t), spacing: { after: 60 } }));
    case 'nota': return [new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: [ANCHO], rows: [new TableRow({ children: [celda([new Paragraph({ children: runs(b.v, { size: 20 }) })], ANCHO, { fondo: 'FFF4E0' })] })] }), new Paragraph({ text: '' })];
    case 'img': return [new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(b.src, b.ancho)] }), pie(b.pie)];
    case 'tabla': return [tablaDocx(b.cols, b.enc, b.filas), new Paragraph({ text: '', spacing: { after: 80 } })];
    case 'salto': return [new Paragraph({ children: [new PageBreak()] })];
    case 'paleta': {
      const anchos = [900, 1900, 6560];
      return [new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: anchos, rows: [
        new TableRow({ tableHeader: true, children: ['Color', 'Nombre y código', 'Uso'].map((t, i) => celda(t, anchos[i], { fondo: '22305E', color: 'FFFFFF' })) }),
        ...b.v.map(([hex, nom, uso]) => new TableRow({ children: [
          celda(' ', anchos[0], { fondo: hex.slice(1) }), celda(`**${nom}** ${hex}`, anchos[1]), celda(uso, anchos[2])] })),
      ] }), new Paragraph({ text: '' })];
    }
    case 'galeria': {
      const n = b.porFila, ancho = Math.floor(ANCHO / n), filas = [];
      for (let i = 0; i < b.items.length; i += n) {
        const grupo = b.items.slice(i, i + n);
        while (grupo.length < n) grupo.push(null);
        filas.push(new TableRow({ cantSplit: true, children: grupo.map((it) => celda(it ? [
          new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(it[0], b.ancho)] }),
          new Paragraph({ alignment: AlignmentType.CENTER, children: runs(it[1], { size: 18, bold: true }) })] : [new Paragraph('')], ancho, { align: AlignmentType.CENTER })) }));
      }
      return [new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: Array(n).fill(ancho), rows: filas }), new Paragraph({ text: '' })];
    }
    case 'minijuego': {
      const anchos = [1500, 4200, 3660];
      const filas = b.filas.map((f) => new TableRow({ cantSplit: true, children: [celda(`**${f[0]}**`, anchos[0], { fondo: 'EEF2FB' }), celda(f[1], anchos[1])] }));
      // la primera fila lleva además la celda de imágenes que abarca todas las filas (rowSpan)
      filas[0] = new TableRow({ cantSplit: true, children: [celda(`**${b.filas[0][0]}**`, anchos[0], { fondo: 'EEF2FB' }), celda(b.filas[0][1], anchos[1]),
        new TableCell({ rowSpan: b.filas.length, width: { size: anchos[2], type: WidthType.DXA }, borders: bordes, verticalAlign: VerticalAlign.CENTER,
          children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(b.img1, 1.45)] }),
            new Paragraph({ alignment: AlignmentType.CENTER, children: runs('Instrucciones', { size: 16, italics: true }) }),
            new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(b.img2, 1.45)] }),
            new Paragraph({ alignment: AlignmentType.CENTER, children: runs('Partida', { size: 16, italics: true }) })] })] });
      return [new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(b.nombre)] }),
        new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: anchos, rows: filas }), new Paragraph({ text: '' })];
    }
    case 'tienda': {
      const anchos = [800, 2200, 900, 1900, 3560];
      return [tablaDocxConImg(anchos, ['Imagen', 'Nombre', 'Precio', 'Tipo', 'Efecto sobre el personaje'], b.filas)];
    }
    case 'pantalla': {
      const anchos = [2700, 6660];
      return [new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: anchos, rows: [new TableRow({ cantSplit: true, children: [
        celda([new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(b.src, 1.65)] })], anchos[0], { va: VerticalAlign.CENTER }),
        celda([new Paragraph({ children: runs(b.titulo, { bold: true, size: 24, color: AZUL }), spacing: { after: 100 } }),
          new Paragraph({ children: runs(b.texto, { size: 20 }) })], anchos[1], { va: VerticalAlign.CENTER })] })] }),
        new Paragraph({ text: '', spacing: { after: 60 } })];
    }
    default: throw new Error('Bloque desconocido ' + b.t);
  }
}

function tablaDocxConImg(anchos, enc, filas) {
  const IT = (n) => __dirname + '/img/items/' + n + '.png';
  return new Table({ width: { size: ANCHO, type: WidthType.DXA }, columnWidths: anchos, rows: [
    new TableRow({ tableHeader: true, children: enc.map((t, i) => celda(t, anchos[i], { fondo: '22305E', color: 'FFFFFF' })) }),
    ...filas.map((f, k) => new TableRow({ cantSplit: true, children: [
      celda([new Paragraph({ alignment: AlignmentType.CENTER, children: [imagen(IT(f[0]), 0.38)] })], anchos[0], { va: VerticalAlign.CENTER, fondo: k % 2 ? 'F3F5FC' : undefined }),
      celda(`**${f[1]}**`, anchos[1], { fondo: k % 2 ? 'F3F5FC' : undefined, va: VerticalAlign.CENTER }),
      celda(typeof f[2] === 'number' ? `${f[2]} 🪙` : f[2], anchos[2], { fondo: k % 2 ? 'F3F5FC' : undefined, va: VerticalAlign.CENTER }),
      celda(f[3], anchos[3], { fondo: k % 2 ? 'F3F5FC' : undefined, va: VerticalAlign.CENTER }),
      celda(f[4], anchos[4], { fondo: k % 2 ? 'F3F5FC' : undefined, va: VerticalAlign.CENTER })] })),
  ] });
}

function portadaDocx(p) {
  const c = (t, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: o.after || 120 }, children: [new TextRun({ text: t, size: o.size || 24, bold: o.bold, color: o.color || AZUL, font: 'Arial' })] });
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 }, children: [imagen(p.logoInst, 3.3)] }),
    c(p.universidad, { size: 26, bold: true }),
    c(p.materia, { size: 24 }),
    ...p.trabajo.split('\n').map((t, i) => c(t, { size: i ? 22 : 28, bold: !i, color: i ? '5A6794' : ROJO })),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 120 }, children: [imagen(p.logoApp, 2.2)] }),
    c(p.app, { size: 40, bold: true, color: AZUL, after: 300 }),
    c('Estudiante: ' + p.estudiante, { size: 24, bold: true }),
    c(p.grupo, { size: 22 }),
    c('Docente: ' + p.docente, { size: 22 }),
    c(p.fecha, { size: 22, bold: true, color: ROJO }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

async function word() {
  const hijos = [
    ...portadaDocx(C.portada),
    new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun('Índice')] }),
    new TableOfContents('Índice', { hyperlink: true, headingStyleRange: '1-2' }),
    new Paragraph({ children: runs('*(Si el índice aparece vacío, en Word pulse clic derecho → «Actualizar campos».)*', { size: 16, color: '5A6794' }) }),
    new Paragraph({ children: [new PageBreak()] }),
    ...C.bloques.flatMap(bloqueDocx),
  ];
  const doc = new Document({
    creator: C.portada.estudiante, title: 'Documentación funcional y diseño · Godzi-Pet 90',
    styles: {
      default: { document: { run: { font: 'Arial', size: 21 } } },
      paragraphStyles: [
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run: { font: 'Arial', size: 32, bold: true, color: AZUL }, paragraph: { spacing: { before: 320, after: 140 }, keepNext: true, outlineLevel: 0,
            border: { bottom: { style: BorderStyle.SINGLE, size: 12, color: CELESTE, space: 4 } } } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
          run: { font: 'Arial', size: 25, bold: true, color: ROJO }, paragraph: { spacing: { before: 220, after: 100 }, keepNext: true, outlineLevel: 1 } },
      ],
    },
    numbering: { config: [{ reference: 'vinetas', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
      style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
    features: { updateFields: true },
    sections: [{
      properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, bottom: 1440, left: 1440, right: 1440 } } },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [
        new TextRun({ text: 'Godzi-Pet 90 · Documentación funcional · ', size: 16, color: '5A6794' }), new TextRun({ children: [PageNumber.CURRENT], size: 16, color: '5A6794' })] })] }) },
      children: hijos,
    }],
  });
  const buf = await Packer.toBuffer(doc);
  fs.writeFileSync(path.join(OUT, NOMBRE + '.docx'), buf);
}

// ---------------------------------------------------------------- HTML (para PDF)
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const md = (s) => esc(s).replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>').replace(/\*([^*]+)\*/g, '<i>$1</i>');
const src = (p) => 'file://' + p;
let indice = [];
function bloqueHtml(b) {
  switch (b.t) {
    case 'h1': indice.push([1, b.v]); return `<h1>${esc(b.v)}</h1>`;
    case 'h2': indice.push([2, b.v]); return `<h2>${esc(b.v)}</h2>`;
    case 'p': return `<p>${md(b.v)}</p>`;
    case 'ul': return '<ul>' + b.v.map((t) => `<li>${md(t)}</li>`).join('') + '</ul>';
    case 'nota': return `<div class="nota">${md(b.v)}</div>`;
    case 'img': return `<figure><img src="${src(b.src)}" style="width:${b.ancho}in"><figcaption>${md(b.pie)}</figcaption></figure>`;
    case 'tabla': return '<table><tr>' + b.enc.map((e) => `<th>${md(e)}</th>`).join('') + '</tr>' + b.filas.map((f) => '<tr>' + f.map((c) => `<td>${md(c)}</td>`).join('') + '</tr>').join('') + '</table>';
    case 'salto': return '<div class="salto"></div>';
    case 'paleta': return '<table><tr><th>Color</th><th>Nombre y código</th><th>Uso</th></tr>' + b.v.map(([h, n, u]) => `<tr><td style="background:${h};width:.7in"></td><td><b>${esc(n)}</b> ${h}</td><td>${esc(u)}</td></tr>`).join('') + '</table>';
    case 'galeria': return `<div class="galeria g${b.porFila}">` + b.items.map(([s, t]) => `<figure><img src="${src(s)}" style="width:${b.ancho}in"><figcaption><b>${esc(t)}</b></figcaption></figure>`).join('') + '</div>';
    case 'minijuego': indice.push([2, b.nombre]); return `<h2>${esc(b.nombre)}</h2><div class="mini"><table>` + b.filas.map((f) => `<tr><th class="lat">${esc(f[0])}</th><td>${md(f[1])}</td></tr>`).join('') + `</table><div class="mimg"><img src="${src(b.img1)}"><span>Instrucciones</span><img src="${src(b.img2)}"><span>Partida</span></div></div>`;
    case 'tienda': return '<table class="tienda"><tr><th>Imagen</th><th>Nombre</th><th>Precio</th><th>Tipo</th><th>Efecto sobre el personaje</th></tr>' + b.filas.map((f) => `<tr><td class="c"><img src="${src(__dirname + '/img/items/' + f[0] + '.png')}" style="width:.36in"></td><td><b>${esc(f[1])}</b></td><td>${typeof f[2] === 'number' ? f[2] + ' 🪙' : f[2]}</td><td>${esc(f[3])}</td><td>${esc(f[4])}</td></tr>`).join('') + '</table>';
    case 'pantalla': return `<div class="pant"><img src="${src(b.src)}"><div><h3>${esc(b.titulo)}</h3><p>${md(b.texto)}</p></div></div>`;
    default: throw new Error(b.t);
  }
}
function html() {
  const p = C.portada;
  const cuerpo = C.bloques.map(bloqueHtml).join('\n');
  const toc = indice.map(([n, t]) => `<div class="toc${n}">${esc(t)}</div>`).join('');
  const doc = `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Documentación funcional · Godzi-Pet 90</title><style>
@page { size: Letter; margin: 0.8in 0.8in 0.9in; }
body { font-family: Arial, 'Noto Sans', sans-serif; font-size: 10.5pt; color: #1b2340; line-height: 1.45; }
h1 { color: #0E1630; font-size: 17pt; border-bottom: 3px solid #3DE0FF; padding-bottom: 4px; margin-top: 22px; page-break-after: avoid; }
h2 { color: #C8102E; font-size: 13pt; margin: 16px 0 6px; page-break-after: avoid; }
h3 { color: #0E1630; margin: 0 0 6px; font-size: 12pt; }
p { text-align: justify; margin: 0 0 8px; }
table { border-collapse: collapse; width: 100%; margin: 6px 0 12px; font-size: 9.4pt; }
th { background: #22305E; color: #fff; text-align: left; padding: 5px 7px; }
td { border: 1px solid #c8d0e4; padding: 4px 7px; vertical-align: top; }
tr:nth-child(even) td { background: #F3F5FC; }
tr, figure, .pant, .mini { page-break-inside: avoid; }
th.lat { background: #EEF2FB; color: #0E1630; width: 1.1in; border: 1px solid #c8d0e4; }
.nota { background: #FFF4E0; border-left: 5px solid #FF8A1F; padding: 8px 12px; margin: 8px 0 14px; }
figure { margin: 8px 0 14px; text-align: center; } figcaption { font-size: 8.8pt; color: #5A6794; font-style: italic; }
figure img { border: 1px solid #c8d0e4; }
.galeria { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px; } .galeria figure { margin: 4px; }
.mini { display: flex; gap: 10px; align-items: flex-start; } .mini table { flex: 1; }
.mimg { width: 1.5in; text-align: center; font-size: 8pt; color: #5A6794; } .mimg img { width: 1.45in; border: 1px solid #c8d0e4; display: block; margin: 0 auto 2px; }
.tienda td { vertical-align: middle; } .c { text-align: center; }
.pant { display: flex; gap: 16px; align-items: center; border: 1px solid #c8d0e4; border-radius: 8px; padding: 8px 12px; margin: 8px 0; }
.pant img { width: 1.55in; border: 1px solid #c8d0e4; border-radius: 6px; }
.salto { page-break-after: always; }
.portada { text-align: center; color: #0E1630; height: 9.2in; display: flex; flex-direction: column; justify-content: center; page-break-after: always; }
.portada .u { font-size: 15pt; font-weight: bold; } .portada .t { font-size: 16pt; font-weight: bold; color: #C8102E; margin-top: 18px; }
.portada .s { color: #5A6794; } .portada .app { font-size: 26pt; font-weight: bold; margin: 8px 0 18px; }
.toc1 { font-weight: bold; margin-top: 6px; } .toc2 { margin-left: 22px; color: #3a4670; }
</style></head><body>
<div class="portada"><div><img src="${src(p.logoInst)}" style="width:3.3in"></div><div class="u">${esc(p.universidad)}</div><div>${esc(p.materia)}</div>
${p.trabajo.split('\n').map((t, i) => `<div class="${i ? 's' : 't'}">${esc(t)}</div>`).join('')}
<div style="margin-top:18px"><img src="${src(p.logoApp)}" style="width:2.1in;border-radius:10px"></div><div class="app">${esc(p.app)}</div>
<div><b>Estudiante: ${esc(p.estudiante)}</b></div><div>${esc(p.grupo)}</div><div>Docente: ${esc(p.docente)}</div><div style="color:#C8102E;font-weight:bold;margin-top:8px">${esc(p.fecha)}</div></div>
<h1>Índice</h1>${toc}<div class="salto"></div>
${cuerpo}</body></html>`;
  fs.writeFileSync(path.join(OUT, NOMBRE + '.html'), doc);
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  await word();
  html();
  console.log('ok', NOMBRE);
})();
