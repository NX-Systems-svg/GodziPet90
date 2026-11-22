package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.RutaEvolucion;

/**
 * Dibuja al kaiju con trazos vectoriales (estilo caricatura de los 90, contorno grueso).
 * Sistema local: unidades 0-100, origen en el suelo entre los pies, mirando a la derecha.
 */
public final class KaijuPainter {
    private static final int CONTORNO = 0xFF0B0F14;

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aux = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    /** Paleta por forma. */
    private static final class Paleta {
        int cuerpo, sombra, vientre, rayas, aletas, aletasBorde, ojo, pupila;
        boolean brillo;
    }

    public KaijuPainter() {
        trazo.setStyle(Paint.Style.STROKE);
        trazo.setStrokeJoin(Paint.Join.ROUND);
        trazo.setStrokeCap(Paint.Cap.ROUND);
        trazo.setColor(CONTORNO);
        relleno.setStyle(Paint.Style.FILL);
    }

    private Paleta paleta(Apariencia a) {
        Paleta p = new Paleta();
        p.cuerpo = 0xFF56695A; p.sombra = 0xFF2F3D33; p.vientre = 0xFF98A68F; p.rayas = 0xFF7B2D3B;
        p.aletas = 0xFFC9D4DD; p.aletasBorde = 0xFF8B9BAA; p.ojo = 0xFFFFC23D; p.pupila = 0xFF1A0E05;
        if (a.etapa == Etapa.FINAL && a.ruta == RutaEvolucion.ATOMICO) {
            p.cuerpo = 0xFF3A4352; p.sombra = 0xFF1C222C; p.vientre = 0xFF66758A; p.rayas = 0xFF2E8B57;
            p.aletas = 0xFF9BFF7A; p.aletasBorde = 0xFF3CCB5A; p.ojo = 0xFFD8FFD0; p.pupila = 0xFF0F3D1A;
            p.brillo = true;
        } else if (a.etapa == Etapa.FINAL && a.ruta == RutaEvolucion.SALVAJE) {
            p.cuerpo = 0xFF66413A; p.sombra = 0xFF35201C; p.vientre = 0xFFA57A62; p.rayas = 0xFF1E1110;
            p.aletas = 0xFFCF3B3B; p.aletasBorde = 0xFF7E1C1C; p.ojo = 0xFFFF4A3D; p.pupila = 0xFF1A0303;
        } else if (a.etapa == Etapa.FINAL) { // Guardián: tonos más brillantes y aletas azul hielo
            p.cuerpo = 0xFF5B7563; p.sombra = 0xFF2E4236; p.vientre = 0xFFAAB89E;
            p.aletas = 0xFFDDEBFA; p.aletasBorde = 0xFF7FA6D6;
        }
        if (a.animo == EstadoAnimo.ENFERMO) {
            p.cuerpo = mezclar(p.cuerpo, 0xFFA7C77E, 0.45f);
            p.vientre = mezclar(p.vientre, 0xFFD4E6A5, 0.45f);
        }
        return p;
    }

    static int mezclar(int a, int b, float t) {
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.argb(255, r, g, bl);
    }

    /**
     * @param cx     centro horizontal en pantalla
     * @param suelo  coordenada Y del suelo
     * @param tam    tamaño en px de 100 unidades
     * @param t      tiempo en segundos (animación)
     * @param mandibula apertura extra de la mandíbula 0-1 (rugir/comer)
     */
    public void dibujar(Canvas c, float cx, float suelo, float tam, Apariencia a, float t, float mandibula,
                        boolean silueta) {
        if (a.etapa == Etapa.HUEVO) {
            dibujarHuevo(c, cx, suelo, tam, a, t);
            return;
        }
        float u = tam / 100f * a.escalaEtapa();
        Paleta p = paleta(a);
        if (silueta) {
            p.cuerpo = p.sombra = p.vientre = p.rayas = p.aletas = p.aletasBorde = Color.WHITE;
            p.brillo = false;
        }
        EstadoAnimo animo = a.animo;
        float respira = 1f + 0.018f * (float) Math.sin(t * 2.2);
        float rebote = animo == EstadoAnimo.FELIZ ? -Math.abs((float) Math.sin(t * 5)) * 4f : 0f;

        c.save();
        c.translate(cx, suelo);
        c.scale(u, u);
        c.translate(0, rebote);
        c.scale(1f, respira, 0, 0);
        trazo.setStrokeWidth(1.7f);

        float escalaAletas = a.etapa == Etapa.CRIA ? 0.5f : a.etapa == Etapa.JUVENIL ? 0.78f
                : a.ruta == RutaEvolucion.SALVAJE ? 1.3f : 1.12f;
        float escalaCabeza = a.etapa == Etapa.CRIA ? 1.38f : a.etapa == Etapa.JUVENIL ? 1.15f : 1f;

        if (p.brillo) dibujarHaloAtomico(t);
        dibujarCola(c, p);
        dibujarAletas(c, p, escalaAletas, a.ruta == RutaEvolucion.SALVAJE && a.etapa == Etapa.FINAL, t);
        dibujarPiernaTrasera(c, p);
        dibujarCuerpo(c, p);
        dibujarPiernaDelantera(c, p);
        dibujarRayas(c, p, silueta);
        if (a.animo == EstadoAnimo.SUCIO && !silueta) dibujarManchas(c);

        // cabeza
        float inclinacion = animo == EstadoAnimo.DORMIDO ? 14f : animo == EstadoAnimo.TRISTE
                || animo == EstadoAnimo.CANSADO ? 7f : (float) Math.sin(t * 1.1) * 2f;
        float apertura = Math.max(mandibula, aperturaPorAnimo(animo));
        c.save();
        c.rotate(inclinacion, 6, -68);
        c.scale(escalaCabeza, escalaCabeza, 10, -70);
        dibujarCuello(c, p);
        dibujarCabeza(c, p, apertura, a, t, silueta);
        c.restore();
        dibujarBrazo(c, p);
        c.restore();
    }

    private float aperturaPorAnimo(EstadoAnimo a) {
        switch (a) {
            case FELIZ: return 0.45f;
            case HAMBRIENTO: return 0.2f;
            default: return 0f;
        }
    }

    // ------------------------------------------------------------------ partes

    private void dibujarHaloAtomico(float t) {
        // se dibuja en el lienzo actual (ya transformado)
    }

    private void llenarYContornear(Canvas c, Path ruta, int color) {
        relleno.setShader(null);
        relleno.setColor(color);
        c.drawPath(ruta, relleno);
        c.drawPath(ruta, trazo);
    }

    private void llenarDegradado(Canvas c, Path ruta, int oscuro, int claro, float x0, float x1) {
        relleno.setShader(new LinearGradient(x0, 0, x1, 0, oscuro, claro, Shader.TileMode.CLAMP));
        c.drawPath(ruta, relleno);
        relleno.setShader(null);
        c.drawPath(ruta, trazo);
    }

    private void dibujarCola(Canvas c, Paleta p) {
        path.reset();
        path.moveTo(-12, -42);
        path.cubicTo(-30, -40, -46, -26, -64, -5);
        path.cubicTo(-64, -2, -62, -1, -59, -2);
        path.cubicTo(-42, -12, -26, -18, -6, -20);
        path.close();
        llenarDegradado(c, path, p.sombra, p.cuerpo, -60, -10);
    }

    private void dibujarAletas(Canvas c, Paleta p, float escala, boolean filosas, float t) {
        // base x, base y, altura, ancho
        float[][] aletas = {
                {4, -87, 9, 6}, {-3, -79, 14, 8}, {-10, -68, 18, 10}, {-16, -56, 18, 10},
                {-21, -44, 15, 9}, {-31, -34, 11, 7}, {-43, -23, 8, 6}, {-53, -13, 5, 4}
        };
        if (p.brillo) {
            float pulso = 0.6f + 0.4f * (float) Math.sin(t * 4);
            aux.setShader(new RadialGradient(-12, -60, 46,
                    Color.argb((int) (150 * pulso), 120, 255, 110), Color.argb(0, 120, 255, 110),
                    Shader.TileMode.CLAMP));
            c.drawCircle(-12, -60, 46, aux);
            aux.setShader(null);
        }
        for (float[] f : aletas) {
            float h = f[2] * escala, w = f[3] * Math.max(0.7f, escala);
            dibujarAleta(c, p, f[0], f[1], h, w, filosas);
        }
    }

    private void dibujarAleta(Canvas c, Paleta p, float bx, float by, float h, float w, boolean filosa) {
        // dirección hacia arriba-atrás y tangente perpendicular
        float dx = -0.42f, dy = -0.91f;
        float tx = 0.91f, ty = -0.42f;
        path.reset();
        path.moveTo(bx - tx * w / 2, by - ty * w / 2);
        path.lineTo(bx + dx * h * 0.55f - tx * w * 0.55f, by + dy * h * 0.55f - ty * w * 0.55f);
        path.lineTo(bx + dx * h * 0.5f - tx * w * 0.22f, by + dy * h * 0.5f - ty * w * 0.22f);
        path.lineTo(bx + dx * h, by + dy * h); // punta
        path.lineTo(bx + dx * h * 0.62f + tx * w * 0.12f, by + dy * h * 0.62f + ty * w * 0.12f);
        path.lineTo(bx + dx * h * (filosa ? 0.88f : 0.78f) + tx * w * 0.45f,
                by + dy * h * (filosa ? 0.88f : 0.78f) + ty * w * 0.45f);
        path.lineTo(bx + dx * h * 0.35f + tx * w * 0.3f, by + dy * h * 0.35f + ty * w * 0.3f);
        path.lineTo(bx + tx * w / 2, by + ty * w / 2);
        path.close();
        relleno.setShader(new LinearGradient(bx, by, bx + dx * h, by + dy * h, p.aletasBorde, p.aletas,
                Shader.TileMode.CLAMP));
        c.drawPath(path, relleno);
        relleno.setShader(null);
        c.drawPath(path, trazo);
    }

    private void dibujarPiernaTrasera(Canvas c, Paleta p) {
        rect.set(-20, -32, -2, -4);
        path.reset();
        path.addOval(rect, Path.Direction.CW);
        llenarYContornear(c, path, p.sombra);
        pie(c, -22, -6, 20, p.sombra);
    }

    private void dibujarPiernaDelantera(Canvas c, Paleta p) {
        path.reset();
        path.moveTo(-4, -30);
        path.cubicTo(-4, -40, 16, -40, 18, -28);
        path.cubicTo(19, -20, 14, -10, 12, -5);
        path.lineTo(2, -5);
        path.cubicTo(2, -12, -4, -18, -4, -30);
        path.close();
        llenarDegradado(c, path, p.sombra, p.cuerpo, -4, 18);
        pie(c, -1, -6, 24, p.cuerpo);
    }

    private void pie(Canvas c, float x, float y, float largo, int color) {
        rect.set(x, y, x + largo, y + 6);
        path.reset();
        path.addRoundRect(rect, 3, 3, Path.Direction.CW);
        llenarYContornear(c, path, color);
        relleno.setColor(0xFFF1EAD8);
        for (int i = 0; i < 3; i++) {
            float gx = x + largo - 1 + i * 0.2f;
            float gy = y + 1 + i * 2f;
            path.reset();
            path.moveTo(gx, gy);
            path.lineTo(gx + 3.5f, gy + 1.2f);
            path.lineTo(gx, gy + 2f);
            path.close();
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        }
    }

    private void dibujarCuerpo(Canvas c, Paleta p) {
        path.reset();
        path.moveTo(-18, -22);
        path.cubicTo(-27, -46, -14, -68, 2, -70);
        path.cubicTo(16, -72, 22, -50, 18, -34);
        path.cubicTo(16, -21, 4, -14, -8, -14);
        path.cubicTo(-14, -14, -17, -17, -18, -22);
        path.close();
        llenarDegradado(c, path, p.sombra, p.cuerpo, -24, 14);
        // vientre con placas
        path.reset();
        path.moveTo(4, -66);
        path.cubicTo(15, -60, 19, -40, 13, -20);
        path.cubicTo(10, -17, 5, -19, 3, -23);
        path.cubicTo(7, -40, 7, -54, 2, -64);
        path.close();
        llenarYContornear(c, path, p.vientre);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeWidth(0.9f);
        aux.setColor(mezclar(p.vientre, CONTORNO, 0.45f));
        for (int i = 0; i < 6; i++) {
            float y = -58 + i * 6.5f;
            c.drawLine(6f + i * 0.3f, y, 14.5f - Math.abs(i - 3) * 0.6f, y + 1.2f, aux);
        }
        aux.setStyle(Paint.Style.FILL);
    }

    private void dibujarRayas(Canvas c, Paleta p, boolean silueta) {
        if (silueta) return;
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(2.4f);
        aux.setColor(p.rayas);
        float[][] rayas = {{-15, -52, -6, -49}, {-18, -42, -6, -38}, {-15, -31, -5, -28},
                {-30, -32, -25, -25}, {-41, -22, -37, -15}, {-51, -12, -48, -7}};
        for (float[] r : rayas) {
            path.reset();
            path.moveTo(r[0], r[1]);
            path.quadTo((r[0] + r[2]) / 2 + 1.5f, (r[1] + r[3]) / 2 - 2.5f, r[2], r[3]);
            c.drawPath(path, aux);
        }
        aux.setStyle(Paint.Style.FILL);
    }

    private void dibujarManchas(Canvas c) {
        aux.setColor(0xAA5A3D1E);
        c.drawCircle(-10, -40, 4, aux);
        c.drawCircle(4, -28, 3, aux);
        c.drawCircle(-38, -16, 3, aux);
        c.drawCircle(10, -60, 2.5f, aux);
    }

    private void dibujarCuello(Canvas c, Paleta p) {
        path.reset();
        path.moveTo(-4, -64);
        path.cubicTo(-4, -80, 2, -88, 13, -89);
        path.lineTo(20, -73);
        path.cubicTo(14, -68, 8, -62, 4, -58);
        path.close();
        llenarDegradado(c, path, p.sombra, p.cuerpo, -4, 18);
    }

    private void dibujarCabeza(Canvas c, Paleta p, float apertura, Apariencia a, float t, boolean silueta) {
        float angulo = apertura * 24f;
        // interior de la boca
        if (angulo > 1f) {
            relleno.setColor(0xFF5E1020);
            path.reset();
            path.moveTo(12, -75);
            path.lineTo(42, -76);
            PointF q = rotar(40, -73, 12, -74, angulo);
            path.lineTo(q.x, q.y);
            path.close();
            c.drawPath(path, relleno);
            relleno.setColor(0xFFE0566E); // lengua
            PointF l = rotar(28, -72, 12, -74, angulo * 0.7f);
            c.drawOval(l.x - 7, l.y - 2, l.x + 5, l.y + 2, relleno);
        }
        // mandíbula inferior
        c.save();
        c.rotate(angulo, 12, -74);
        path.reset();
        path.moveTo(10, -75);
        path.lineTo(40, -76);
        path.cubicTo(43, -75, 42, -71, 37, -70);
        path.cubicTo(28, -68, 18, -67, 11, -69);
        path.close();
        llenarYContornear(c, path, p.vientre);
        if (angulo > 4f) dientes(c, -75.2f, false);
        c.restore();
        // cráneo y hocico
        path.reset();
        path.moveTo(4, -82);
        path.cubicTo(6, -95, 18, -99, 28, -95);
        path.cubicTo(37, -92, 45, -86, 47, -80);
        path.cubicTo(47, -77, 45, -76, 41, -76);
        path.lineTo(18, -74.5f);
        path.cubicTo(10, -73, 4, -76, 4, -82);
        path.close();
        llenarDegradado(c, path, p.sombra, p.cuerpo, 0, 44);
        if (angulo > 4f) dientes(c, -75.5f, true);
        // crestas de la ceja y fosa nasal
        relleno.setColor(CONTORNO);
        c.drawCircle(43, -81, 0.9f, relleno);
        if (!silueta) dibujarOjo(c, p, a.animo, t, a.etapa);
        if (!silueta && a.animo == EstadoAnimo.FELIZ) {
            aux.setColor(0x66FF6F91);
            c.drawCircle(33, -79, 3f, aux);
        }
    }

    private void dientes(Canvas c, float y, boolean arriba) {
        relleno.setColor(0xFFFFF8E8);
        for (float x = 20; x < 40; x += 4.2f) {
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + 2.4f, y);
            path.lineTo(x + 1.2f, arriba ? y + 2.8f : y - 2.8f);
            path.close();
            c.drawPath(path, relleno);
        }
    }

    private void dibujarOjo(Canvas c, Paleta p, EstadoAnimo animo, float t, Etapa etapa) {
        float ox = 27, oy = -87;
        float grande = etapa == Etapa.CRIA ? 1.45f : etapa == Etapa.JUVENIL ? 1.2f : 1f;
        float rx = 4.2f * grande, ry = 3.3f * grande;
        boolean parpadeo = (t % 4.2f) < 0.13f;
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(1.6f);
        aux.setColor(CONTORNO);

        if (animo == EstadoAnimo.DORMIDO || parpadeo) {
            path.reset();
            path.moveTo(ox - rx, oy);
            path.quadTo(ox, oy + 2.4f, ox + rx, oy);
            c.drawPath(path, aux);
        } else if (animo == EstadoAnimo.FELIZ) {
            path.reset();
            path.moveTo(ox - rx, oy + 1);
            path.quadTo(ox, oy - 4.5f, ox + rx, oy + 1);
            c.drawPath(path, aux);
        } else {
            relleno.setColor(p.ojo);
            c.drawOval(ox - rx, oy - ry, ox + rx, oy + ry, relleno);
            float mirar = animo == EstadoAnimo.TRISTE ? 1.3f : 0f;
            relleno.setColor(p.pupila);
            c.drawOval(ox + 0.4f - 1.0f * grande, oy - ry + 0.6f + mirar, ox + 0.4f + 1.0f * grande,
                    oy + ry - 0.6f + mirar, relleno);
            relleno.setColor(Color.WHITE);
            c.drawCircle(ox + 1.6f * grande, oy - 1.3f * grande, 0.8f * grande, relleno);
            c.drawOval(ox - rx, oy - ry, ox + rx, oy + ry, aux);
            if (animo == EstadoAnimo.ENFERMO || animo == EstadoAnimo.CANSADO) { // párpado caído
                relleno.setColor(mezclar(p.cuerpo, CONTORNO, 0.2f));
                c.drawArc(ox - rx, oy - ry, ox + rx, oy + ry, 180, 180, true, relleno);
                c.drawLine(ox - rx, oy, ox + rx, oy, aux);
            }
        }
        // ceja según ánimo
        aux.setStrokeWidth(2.2f);
        switch (animo) {
            case TRISTE:
                c.drawLine(ox - rx - 1, oy - ry - 1.2f, ox + rx, oy - ry - 3.8f, aux);
                break;
            case HAMBRIENTO:
            case SUCIO:
                c.drawLine(ox - rx - 1, oy - ry - 3.8f, ox + rx + 1, oy - ry - 1.4f, aux);
                break;
            default:
                c.drawLine(ox - rx - 1, oy - ry - 2.5f, ox + rx + 1, oy - ry - 2.8f, aux);
        }
        aux.setStyle(Paint.Style.FILL);
    }

    private void dibujarBrazo(Canvas c, Paleta p) {
        path.reset();
        path.moveTo(6, -58);
        path.cubicTo(16, -57, 23, -52, 26, -45);
        path.lineTo(22, -43);
        path.cubicTo(18, -48, 12, -50, 5, -51);
        path.close();
        llenarYContornear(c, path, p.cuerpo);
        relleno.setColor(0xFFF1EAD8);
        for (int i = 0; i < 3; i++) {
            float x = 23.5f + i * 1.3f, y = -44f + i * 0.9f;
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + 1.4f, y + 3.2f);
            path.lineTo(x - 0.6f, y + 1.2f);
            path.close();
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        }
    }

    // ------------------------------------------------------------------ huevo

    private void dibujarHuevo(Canvas c, float cx, float suelo, float tam, Apariencia a, float t) {
        float u = tam / 100f * a.escalaEtapa();
        float tambaleo = (float) Math.sin(t * (2 + a.toquesHuevo)) * (2 + a.toquesHuevo * 2.5f);
        c.save();
        c.translate(cx, suelo);
        c.scale(u, u);
        c.rotate(tambaleo, 0, 0);
        trazo.setStrokeWidth(1.8f);
        rect.set(-24, -64, 24, 0);
        relleno.setShader(new LinearGradient(-24, 0, 24, 0, a.origen.colorOscuro, a.origen.colorClaro,
                Shader.TileMode.CLAMP));
        c.drawOval(rect, relleno);
        relleno.setShader(null);
        c.drawOval(rect, trazo);
        // manchas escamosas
        relleno.setColor(a.origen.colorOscuro);
        float[][] manchas = {{-8, -48, 4}, {9, -38, 5}, {-12, -24, 3.5f}, {6, -16, 3}, {12, -54, 2.5f}};
        for (float[] m : manchas) c.drawCircle(m[0], m[1], m[2], relleno);
        // brillo
        relleno.setColor(0x55FFFFFF);
        c.drawOval(-16, -56, -8, -40, relleno);
        // grietas según toques
        trazo.setStrokeWidth(1.3f);
        if (a.toquesHuevo >= 1) linea(c, -6, -60, -2, -54, -7, -49);
        if (a.toquesHuevo >= 2) linea(c, 10, -46, 4, -42, 9, -36);
        if (a.toquesHuevo >= 3) linea(c, -18, -30, -11, -32, -14, -26);
        if (a.toquesHuevo >= 4) linea(c, -2, -54, 3, -50, 1, -44);
        c.restore();
    }

    private void linea(Canvas c, float... p) {
        path.reset();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        c.drawPath(path, trazo);
    }

    // ------------------------------------------------------------------ utilidades

    private static PointF rotar(float x, float y, float px, float py, float grados) {
        double r = Math.toRadians(grados);
        float dx = x - px, dy = y - py;
        return new PointF((float) (px + dx * Math.cos(r) - dy * Math.sin(r)),
                (float) (py + dx * Math.sin(r) + dy * Math.cos(r)));
    }

    /** Punto aproximado de la boca en pantalla (para efectos de comer / aliento). */
    public static PointF boca(float cx, float suelo, float tam, Apariencia a) {
        float u = tam / 100f * a.escalaEtapa();
        float cabeza = a.etapa == Etapa.CRIA ? 1.38f : a.etapa == Etapa.JUVENIL ? 1.15f : 1f;
        float bx = 10 + (44 - 10) * cabeza, by = -70 + (-76 + 70) * cabeza;
        return new PointF(cx + bx * u, suelo + by * u);
    }

    /** Punto aproximado de la cabeza (para burbujas de pensamiento / Zzz). */
    public static PointF cabeza(float cx, float suelo, float tam, Apariencia a) {
        float u = tam / 100f * a.escalaEtapa();
        float cabeza = a.etapa == Etapa.CRIA ? 1.38f : a.etapa == Etapa.JUVENIL ? 1.15f : 1f;
        if (a.etapa == Etapa.HUEVO) return new PointF(cx, suelo - 64 * tam / 100f);
        return new PointF(cx + (10 + 16 * cabeza) * u, suelo + (-70 - 26 * cabeza) * u);
    }
}
