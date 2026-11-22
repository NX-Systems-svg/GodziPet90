package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.cesba.godzipet.modelo.Escenario;

import java.util.Calendar;

/**
 * Dibujo de los 8 escenarios japoneses (siluetas simples con Canvas) y de los objetos
 * icónicos reutilizables (torre de Tokio, reloj Wako). Sin asignaciones dentro de los métodos de dibujo.
 * Todos los colores se escriben "de día" y {@link #col(int)} los tiñe según el momento.
 */
final class EscenariosJapon {
    static final int ROJO = 0xFFC8102E, CREMA = 0xFFF2E6C9, ORO = 0xFFD4A017, ESPUMA = 0xFFF5F1E6;
    private static final int[] CONTENEDOR = {0xFFC8102E, 0xFF2E6FB0, 0xFFD4A017, 0xFF3C7A56};
    private static final int[] EDIF_GINZA = {0xFFC9B79A, 0xFFB5A58A, 0xFFD6C7A8, 0xFFBDAE96};
    private static final float[] ALTO_GINZA = {0.62f, 0.5f, 0.72f, 0.56f, 0.66f};

    private final EscenaPainter ep;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path path2 = new Path();
    private final RectF rf = new RectF();
    private final Calendar cal = Calendar.getInstance();
    /** Momento actual: 0 día, 1 atardecer, 2 noche. */
    int m;
    /** Tiempo en segundos del cuadro actual. */
    float t;

    EscenariosJapon(EscenaPainter ep) {
        this.ep = ep;
    }

    // ------------------------------------------------------------------ utilidades

    private static float sn(float a) { return (float) Math.sin(a); }

    private static float cs(float a) { return (float) Math.cos(a); }

    static int mezcla(int a, int b, float f) {
        int rr = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int gg = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bb = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | rr << 16 | gg << 8 | bb;
    }

    /** Tiñe un color de día según el momento (atardecer morado, noche azul marino). */
    int col(int dia) {
        if (m == 0) return dia;
        return m == 1 ? mezcla(dia, 0xFF7A3F66, 0.38f) : mezcla(dia, 0xFF0A1236, 0.66f);
    }

    private void r(Canvas c, float x0, float y0, float x1, float y1, int dia) {
        p.setColor(col(dia));
        c.drawRect(x0, y0, x1, y1, p);
    }

    private void tri(Canvas c, int dia, float x0, float y0, float x1, float y1, float x2, float y2) {
        path.reset();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        path.close();
        p.setColor(col(dia));
        c.drawPath(path, p);
    }

    private void cuad(Canvas c, int dia, float x0, float y0, float x1, float y1,
                      float x2, float y2, float x3, float y3) {
        path.reset();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        p.setColor(col(dia));
        c.drawPath(path, p);
    }

    private void linea(Canvas c, float x0, float y0, float x1, float y1, float ancho, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(ancho);
        p.setColor(color);
        c.drawLine(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Ventana: encendida de noche, cristal tenue de día. */
    private void luz(Canvas c, float x, float y, float w, float h) {
        p.setColor(m == 2 ? 0xFFFFD86B : m == 1 ? 0xFFE8B070 : col(0xFF35465A));
        c.drawRect(x, y, x + w, y + h, p);
    }

    private void edificio(Canvas c, float x, float base, float bw, float bh, int dia, float paso) {
        r(c, x, base - bh, x + bw, base, dia);
        int fi = 0;
        for (float wy = base - bh + paso * 0.5f; wy < base - paso; wy += paso, fi++) {
            int ci = 0;
            for (float wx = x + paso * 0.4f; wx < x + bw - paso * 0.6f; wx += paso * 0.8f, ci++) {
                boolean on = ((fi * 5 + ci * 3 + (int) (x * 0.1f)) % 4) == 0;
                p.setColor(m == 2 ? (on ? 0xFFFFD86B : 0xFF1A2447) : col(0xFF5B6E86));
                c.drawRect(wx, wy, wx + paso * 0.4f, wy + paso * 0.5f, p);
            }
        }
    }

    private void montes(Canvas c, float w, float base, float alto, int dia, float fase, float vel) {
        path.reset();
        path.moveTo(0, base);
        float off = t * vel;
        for (float x = 0; x <= w + 10; x += 10) {
            float u = (x + off) / w * 8.2f + fase;
            path.lineTo(x, base - alto * (0.55f + 0.3f * sn(u) + 0.15f * sn(u * 2.3f + 1f)));
        }
        path.lineTo(w + 10, base);
        path.close();
        p.setColor(col(dia));
        c.drawPath(path, p);
    }

    private void suelo(Canvas c, float w, float h, float y, int dia, int bordeDia) {
        r(c, 0, y, w, h, dia);
        r(c, 0, y, w, y + (h - y) * 0.07f, bordeDia);
        p.setColor(0x14000000);
        c.drawRect(0, y + (h - y) * 0.6f, w, h, p);
    }

    private void bruma(Canvas c, float w, float y, float alto, float alfa) {
        p.setColor(0xFFFFFFFF);
        p.setAlpha((int) (alfa * 255));
        for (int k = 0; k < 3; k++) {
            float x = ((k * 0.37f + t * 0.01f) % 1f) * (w + 200) - 100;
            rf.set(x - w * 0.3f, y + k * alto * 0.3f, x + w * 0.3f, y + alto + k * alto * 0.3f);
            c.drawOval(rf, p);
        }
    }

    // ------------------------------------------------------------------ objetos

    private void pino(Canvas c, float x, float base, float s) {
        r(c, x - s * 0.04f, base - s * 0.3f, x + s * 0.04f, base, 0xFF4A3426);
        for (int i = 0; i < 3; i++) {
            float by = base - s * (0.2f + 0.22f * i), ty = base - s * (0.55f + 0.225f * i), hw = s * (0.32f - 0.07f * i);
            tri(c, i == 1 ? 0xFF3A6E44 : 0xFF2F5E3A, x - hw, by, x + hw, by, x, ty);
        }
    }

    private void sakuraArbol(Canvas c, float x, float base, float s) {
        r(c, x - s * 0.04f, base - s * 0.5f, x + s * 0.04f, base, 0xFF5A3F30);
        for (int i = 0; i < 5; i++) {
            float ox = (i - 2) * s * 0.17f, oy = (i % 2 == 0 ? 0.62f : 0.78f) * s;
            p.setColor(col(i % 2 == 0 ? 0xFFF7C1D2 : 0xFFF0A5BD));
            c.drawCircle(x + ox, base - oy, s * 0.27f, p);
        }
    }

    private void casa(Canvas c, float cx, float base, float w, float h) {
        r(c, cx - w / 2, base - h * 0.55f, cx + w / 2, base, 0xFFB89A6A);
        tri(c, 0xFF4A3A30, cx - w * 0.68f, base - h * 0.5f, cx + w * 0.68f, base - h * 0.5f, cx, base - h);
        luz(c, cx - w * 0.12f, base - h * 0.4f, w * 0.24f, h * 0.18f);
    }

    private void torii(Canvas c, float cx, float base, float s) {
        r(c, cx - s * 0.42f, base - s, cx - s * 0.33f, base, ROJO);
        r(c, cx + s * 0.33f, base - s, cx + s * 0.42f, base, ROJO);
        r(c, cx - s * 0.46f, base - s * 0.72f, cx + s * 0.46f, base - s * 0.64f, ROJO);
        cuad(c, 0xFF1A1A1A, cx - s * 0.66f, base - s * 1.12f, cx + s * 0.66f, base - s * 1.12f,
                cx + s * 0.52f, base - s * 0.98f, cx - s * 0.52f, base - s * 0.98f);
        r(c, cx - s * 0.56f, base - s * 0.98f, cx + s * 0.56f, base - s * 0.9f, ROJO);
    }

    private void barco(Canvas c, float x, float y, float s) {
        float by = y + sn(t * 1.4f + x * 0.01f) * s * 0.03f;
        cuad(c, 0xFF3E2C22, x - s * 0.5f, by - s * 0.18f, x + s * 0.5f, by - s * 0.18f,
                x + s * 0.3f, by, x - s * 0.34f, by);
        r(c, x - s * 0.02f, by - s * 0.75f, x + s * 0.02f, by - s * 0.18f, 0xFF5A4636);
        tri(c, CREMA, x + s * 0.04f, by - s * 0.72f, x + s * 0.04f, by - s * 0.24f, x + s * 0.4f, by - s * 0.24f);
        luz(c, x - s * 0.4f, by - s * 0.3f, s * 0.07f, s * 0.09f);
    }

    private void buque(Canvas c, float x, float y, float s, int dia) {
        cuad(c, dia, x - s / 2, y - s * 0.12f, x + s / 2, y - s * 0.12f, x + s * 0.42f, y, x - s * 0.46f, y);
        for (int i = 0; i < 5; i++) {
            float cx = x - s * 0.42f + i * s * 0.12f;
            r(c, cx, y - s * 0.2f, cx + s * 0.11f, y - s * 0.12f, CONTENEDOR[i % 4]);
        }
        r(c, x + s * 0.22f, y - s * 0.3f, x + s * 0.38f, y - s * 0.12f, 0xFFEDEDED);
        luz(c, x + s * 0.25f, y - s * 0.26f, s * 0.1f, s * 0.04f);
        r(c, x + s * 0.28f, y - s * 0.38f, x + s * 0.33f, y - s * 0.3f, ROJO);
    }

    private void grua(Canvas c, float x, float base, float s) {
        r(c, x - s * 0.2f, base - s * 0.8f, x - s * 0.16f, base, 0xFFD9A441);
        r(c, x + s * 0.16f, base - s * 0.8f, x + s * 0.2f, base, 0xFFD9A441);
        r(c, x - s * 0.7f, base - s * 0.86f, x + s * 0.45f, base - s * 0.8f, 0xFFD9A441);
        tri(c, 0xFFD9A441, x - s * 0.7f, base - s * 0.86f, x - s * 0.46f, base - s * 0.86f, x - s * 0.58f, base - s * 0.98f);
        linea(c, x - s * 0.55f, base - s * 0.8f, x - s * 0.55f, base - s * 0.5f, 1.5f, col(0xFF333333));
    }

    private void faro(Canvas c, float x, float base, float s) {
        tri(c, 0xFF5A5448, x - s * 0.5f, base, x + s * 0.5f, base, x, base - s * 0.18f);
        for (int i = 0; i < 3; i++) {
            float y0 = base - s * 0.1f - i * s * 0.3f, y1 = y0 - s * 0.3f;
            float a0 = s * (0.2f - i * 0.045f), a1 = s * (0.2f - (i + 1) * 0.045f);
            cuad(c, i % 2 == 0 ? ROJO : ESPUMA, x - a0, y0, x + a0, y0, x + a1, y1, x - a1, y1);
        }
        float ty = base - s * 1.0f;
        r(c, x - s * 0.17f, ty - s * 0.03f, x + s * 0.17f, ty, 0xFF222222);
        luz(c, x - s * 0.08f, ty - s * 0.15f, s * 0.16f, s * 0.12f);
        tri(c, ROJO, x - s * 0.14f, ty - s * 0.15f, x + s * 0.14f, ty - s * 0.15f, x, ty - s * 0.3f);
        if (m == 2) {
            float a = sn(t * 0.9f) * 0.5f;
            p.setColor(0xFFFFF2A0);
            p.setAlpha(60);
            path.reset();
            path.moveTo(x, ty - s * 0.09f);
            path.lineTo(x - s * 4f, ty - s * (0.9f + a));
            path.lineTo(x - s * 4f, ty - s * (0.2f + a));
            path.close();
            c.drawPath(path, p);
        }
    }

    private void pagoda(Canvas c, float cx, float base, float s) {
        for (int i = 0; i < 5; i++) {
            float bw = s * (0.5f - i * 0.07f), y = base - i * s * 0.2f;
            r(c, cx - bw * 0.4f, y - s * 0.1f, cx + bw * 0.4f, y, ROJO);
            luz(c, cx - bw * 0.08f, y - s * 0.085f, bw * 0.16f, s * 0.05f);
            cuad(c, 0xFF2F3A3A, cx - bw * 0.7f, y - s * 0.1f, cx + bw * 0.7f, y - s * 0.1f,
                    cx + bw * 0.32f, y - s * 0.2f, cx - bw * 0.32f, y - s * 0.2f);
        }
        r(c, cx - s * 0.01f, base - s * 1.18f, cx + s * 0.01f, base - s * 1.0f, 0xFF2F3A3A);
    }

    private void palma(Canvas c, float x, float base, float s, float dir) {
        path.reset();
        path.moveTo(x, base);
        path.quadTo(x + dir * s * 0.25f, base - s * 0.5f, x + dir * s * 0.1f, base - s);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.07f);
        p.setColor(col(0xFF6B4A32));
        c.drawPath(path, p);
        p.setColor(col(0xFF2E7D3F));
        p.setStrokeWidth(s * 0.05f);
        float tx = x + dir * s * 0.1f, ty = base - s;
        for (int k = 0; k < 6; k++) {
            float a = (float) Math.toRadians(190 + k * 32);
            float ex = tx + cs(a) * s * 0.5f, ey = ty + sn(a) * s * 0.3f + s * 0.18f + sn(t * 1.5f + k) * 2f;
            path.reset();
            path.moveTo(tx, ty);
            path.quadTo((tx + ex) / 2, ty - s * 0.2f, ex, ey);
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(col(0xFF5A3A22));
        c.drawCircle(tx, ty + s * 0.04f, s * 0.04f, p);
    }

    /** Torre de Tokio: celosía roja y blanca con dos plataformas y antena. */
    void torre(Canvas c, float cx, float baseY, float topY) {
        float hh = baseY - topY, bw = hh * 0.26f, cuerpo = hh * 0.62f;
        int rojo = m == 2 ? 0xFFE0602B : col(ROJO), blanco = m == 2 ? 0xFFFFE2B0 : col(ESPUMA);
        path.reset();
        path.moveTo(cx - ancho(0, bw), baseY);
        for (int i = 1; i <= 10; i++) path.lineTo(cx - ancho(i / 10f, bw), baseY - cuerpo * i / 10f);
        for (int i = 10; i >= 0; i--) path.lineTo(cx + ancho(i / 10f, bw), baseY - cuerpo * i / 10f);
        path.close();
        p.setColor(rojo);
        c.drawPath(path, p);
        c.save();
        c.clipPath(path);
        p.setColor(blanco);
        c.drawRect(cx - bw, baseY - cuerpo * 0.2f, cx + bw, baseY - cuerpo * 0.1f, p);
        c.drawRect(cx - bw, baseY - cuerpo * 0.55f, cx + bw, baseY - cuerpo * 0.4f, p);
        c.drawRect(cx - bw, baseY - cuerpo * 0.9f, cx + bw, baseY - cuerpo * 0.75f, p);
        for (int i = 0; i < 10; i++) {
            float f0 = i / 10f, f1 = (i + 1) / 10f;
            int tinta = m == 2 ? 0x66401808 : 0x88601020;
            linea(c, cx - ancho(f0, bw), baseY - cuerpo * f0, cx + ancho(f1, bw), baseY - cuerpo * f1, 1.4f, tinta);
            linea(c, cx + ancho(f0, bw), baseY - cuerpo * f0, cx - ancho(f1, bw), baseY - cuerpo * f1, 1.4f, tinta);
        }
        c.restore();
        // plataformas
        p.setColor(blanco);
        float y1 = baseY - cuerpo * 0.38f, y2 = baseY - cuerpo;
        c.drawRect(cx - ancho(0.38f, bw) - bw * 0.08f, y1 - hh * 0.022f, cx + ancho(0.38f, bw) + bw * 0.08f, y1, p);
        p.setColor(rojo);
        c.drawRect(cx - ancho(1f, bw) - bw * 0.07f, y2 - hh * 0.02f, cx + ancho(1f, bw) + bw * 0.07f, y2, p);
        // antena a franjas
        float seg = (y2 - topY) / 6f, aw = Math.max(1.2f, bw * 0.035f);
        for (int i = 0; i < 6; i++) {
            p.setColor(i % 2 == 0 ? rojo : blanco);
            c.drawRect(cx - aw * (1.6f - i * 0.2f), y2 - seg * (i + 1), cx + aw * (1.6f - i * 0.2f), y2 - seg * i, p);
        }
        p.setColor(((int) (t * 2)) % 2 == 0 ? 0xFFFF3B3B : 0xFF7A1A1A);
        c.drawCircle(cx, topY, aw * 1.8f, p);
    }

    /** Semiancho de la torre a la fracción f (0 base, 1 plataforma alta). */
    private static float ancho(float f, float bw) {
        float u = 1f - f;
        return bw * 0.5f * u * u * u + bw * 0.04f;
    }

    /** Reloj estilo Wako con la hora real del teléfono. */
    void reloj(Canvas c, float cx, float cy, float rad) {
        cal.setTimeInMillis(System.currentTimeMillis());
        float min = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f;
        float hor = cal.get(Calendar.HOUR) + min / 60f;
        p.setColor(0xFF6B5B3E);
        c.drawCircle(cx, cy, rad * 1.14f, p);
        p.setColor(m == 2 ? 0xFFFFF0B8 : CREMA);
        c.drawCircle(cx, cy, rad, p);
        for (int i = 0; i < 12; i++) {
            float a = (float) Math.toRadians(i * 30);
            linea(c, cx + sn(a) * rad * 0.8f, cy - cs(a) * rad * 0.8f,
                    cx + sn(a) * rad * 0.94f, cy - cs(a) * rad * 0.94f, Math.max(1f, rad * 0.06f), 0xFF222222);
        }
        float ah = (float) Math.toRadians(hor / 12f * 360f), am = (float) Math.toRadians(min / 60f * 360f);
        p.setStrokeCap(Paint.Cap.ROUND);
        linea(c, cx, cy, cx + sn(ah) * rad * 0.5f, cy - cs(ah) * rad * 0.5f, Math.max(1.5f, rad * 0.12f), 0xFF1A1A1A);
        linea(c, cx, cy, cx + sn(am) * rad * 0.82f, cy - cs(am) * rad * 0.82f, Math.max(1f, rad * 0.07f), 0xFF1A1A1A);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(ROJO);
        c.drawCircle(cx, cy, rad * 0.07f, p);
    }

    // ------------------------------------------------------------------ escenarios

    static float torreX(float w) { return w * 0.24f; }

    static float torreArriba(float h) { return h * 0.06f; }

    static float torreBase(float h) { return h * 0.54f; }

    void dibujar(Canvas c, float w, float h, Escenario e, float tiempo, int momento) {
        this.m = momento;
        this.t = tiempo;
        float hor = h * Escenas.HORIZONTE, suelo = h * Escenas.INICIO_SUELO;
        ep.cielo(c, w, hor, momento, tiempo, false);
        switch (e == null ? Escenario.ISLA_ODO : e) {
            case BAHIA_TOKIO: bahia(c, w, h, hor, suelo); break;
            case GINZA: ginza(c, w, h, hor, suelo); break;
            case MONTE_FUJI: fuji(c, w, h, hor, suelo); break;
            case TORRE_TOKIO: torreTokio(c, w, h, hor, suelo); break;
            case OSAKA: osaka(c, w, h, hor, suelo); break;
            case KAMAKURA: kamakura(c, w, h, hor, suelo); break;
            case ISLA_MONSTRUOS: monstruos(c, w, h, hor, suelo); break;
            default: odo(c, w, h, hor, suelo); break;
        }
    }

    private float colinaY(float x, float w, float hor, float sa) {
        float u = (x - w * 0.3f) / (w * 0.45f);
        if (u <= -1f || u >= 1f) return hor + sa * 0.3f;
        float k = 0.5f + 0.5f * cs((float) Math.PI * u);
        return hor + sa * 0.3f - hor * 0.75f * (float) Math.pow(k, 1.2);
    }

    private void odo(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor;
        montes(c, w, hor, hor * 0.2f, 0xFF8FA9BF, 1.2f, 1.5f);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF4F7FA3), col(0xFF1F4568));
        bruma(c, w, hor - h * 0.02f, h * 0.06f, 0.18f);
        path.reset();
        path.moveTo(0, hor + sa * 0.3f);
        for (float x = 0; x <= w; x += 8) path.lineTo(x, colinaY(x, w, hor, sa));
        path.lineTo(w, hor + sa * 0.3f);
        path.close();
        p.setColor(col(0xFF3F6B3A));
        c.drawPath(path, p);
        for (int i = 0; i < 5; i++) {
            float x = w * (0.1f + i * 0.085f);
            casa(c, x, colinaY(x, w, hor, sa) + h * 0.03f, w * 0.07f, h * 0.07f);
        }
        pino(c, w * 0.5f, colinaY(w * 0.5f, w, hor, sa) + h * 0.02f, h * 0.1f);
        torii(c, w * 0.58f, hor + sa * 0.22f, h * 0.1f);
        barco(c, w * 0.72f + sn(t * 0.2f) * 6, hor + sa * 0.3f, w * 0.12f);
        barco(c, w * 0.88f, hor + sa * 0.55f, w * 0.15f);
        barco(c, w * 0.62f, hor + sa * 0.82f, w * 0.18f);
        bruma(c, w, hor + sa * 0.5f, h * 0.05f, 0.1f);
        suelo(c, w, h, suelo, 0xFFD2B47C, 0xFFBFA06A);
        r(c, 0, suelo - 3, w, suelo + 3, ESPUMA);
    }

    private void bahia(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor;
        ep.ciudad(c, w, hor, hor * 0.3f, m, false, false);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF3B6E94), col(0xFF1B3F66));
        grua(c, w * 0.08f, hor + sa * 0.15f, h * 0.2f);
        grua(c, w * 0.3f, hor + sa * 0.1f, h * 0.16f);
        faro(c, w * 0.9f, hor + sa * 0.3f, h * 0.2f);
        float x1 = ((t * 6f) % (w * 1.6f)) - w * 0.3f;
        buque(c, x1, hor + sa * 0.45f, w * 0.34f, 0xFF3A4F66);
        buque(c, w * 0.7f - sn(t * 0.1f) * 8, hor + sa * 0.85f, w * 0.42f, 0xFF7A2B2B);
        suelo(c, w, h, suelo, 0xFF8A8F96, 0xFF6F747B);
        r(c, 0, suelo + (h - suelo) * 0.18f, w, suelo + (h - suelo) * 0.22f, 0xFFE0B72E);
        for (int i = 0; i < 5; i++) {
            p.setColor(col(0xFF2B2E33));
            c.drawCircle(w * (0.1f + i * 0.2f), suelo + (h - suelo) * 0.12f, h * 0.012f, p);
        }
    }

    private void ginza(Canvas c, float w, float h, float hor, float suelo) {
        float paso = Math.max(5f, h * 0.022f);
        // fila lejana
        for (int i = 0; i < 9; i++) {
            float bw = w * 0.13f, x = i * w * 0.115f - w * 0.03f;
            edificio(c, x, suelo, bw, hor * (0.5f + 0.35f * ALTO_GINZA[i % 5]) , 0xFFA9A396, paso * 0.8f);
        }
        // cercanas: izquierda y derecha
        for (int i = 0; i < 3; i++) {
            float bw = w * 0.15f, x = i * w * 0.15f - w * 0.02f;
            float bh = hor * (0.85f + ALTO_GINZA[i] * 0.4f);
            edificio(c, x, suelo, bw, bh, EDIF_GINZA[i], paso);
            r(c, x - 2, suelo - bh - 4, x + bw + 2, suelo - bh + 2, 0xFF6B5E4A);
            r(c, x, suelo - h * 0.07f, x + bw, suelo - h * 0.05f, i % 2 == 0 ? ROJO : 0xFF2E5C8A);
        }
        for (int i = 0; i < 2; i++) {
            float bw = w * 0.14f, x = w * 0.6f + i * w * 0.2f;
            edificio(c, x, suelo, bw, hor * (0.9f + ALTO_GINZA[i + 3] * 0.3f), EDIF_GINZA[i + 1], paso);
        }
        // torre del reloj Wako
        float cx = w * 0.74f, bw = w * 0.2f, yc = h * 0.34f;
        r(c, cx - bw / 2, h * 0.46f, cx + bw / 2, suelo, 0xFFD8CBB0);
        for (int i = 0; i < 4; i++) {
            luz(c, cx - bw * 0.4f + i * bw * 0.22f, h * 0.5f, bw * 0.12f, h * 0.06f);
            luz(c, cx - bw * 0.4f + i * bw * 0.22f, h * 0.6f, bw * 0.12f, h * 0.06f);
        }
        r(c, cx - bw * 0.4f, h * 0.2f, cx + bw * 0.4f, h * 0.46f, 0xFFCFC2A6);
        r(c, cx - bw * 0.46f, h * 0.45f, cx + bw * 0.46f, h * 0.47f, 0xFF8B7B5E);
        reloj(c, cx, yc, Math.min(bw * 0.3f, h * 0.075f));
        path.reset();
        rf.set(cx - bw * 0.32f, h * 0.1f, cx + bw * 0.32f, h * 0.3f);
        p.setColor(col(0xFF4A8F80));
        c.drawArc(rf, 180, 180, true, p);
        r(c, cx - 1.5f, h * 0.04f, cx + 1.5f, h * 0.11f, 0xFF3A3A3A);
        p.setColor(col(ORO));
        c.drawCircle(cx, h * 0.055f, 3.5f, p);
        // calle y tranvía
        suelo(c, w, h, suelo, 0xFF3E4047, 0xFF8B8478);
        float ty = suelo + (h - suelo) * 0.62f, tl = w * 0.3f, th = h * 0.075f;
        float tx = ((t * 24f) % (w + tl * 2)) - tl;
        linea(c, 0, ty + 2, w, ty + 2, 2f, col(0xFF8A8F96));
        linea(c, 0, ty - th - 10, w, ty - th - 10, 1.4f, col(0xFF222222));
        r(c, tx, ty - th, tx + tl, ty, 0xFF3C7A56);
        r(c, tx, ty - th * 0.45f, tx + tl, ty - th * 0.3f, CREMA);
        for (int i = 0; i < 5; i++) luz(c, tx + tl * (0.06f + i * 0.18f), ty - th * 0.88f, tl * 0.12f, th * 0.3f);
        r(c, tx + tl * 0.45f, ty - th - 6, tx + tl * 0.47f, ty - th, 0xFF222222);
        for (int i = 0; i < 6; i++) r(c, w * (0.05f + i * 0.18f), suelo + (h - suelo) * 0.35f, w * (0.1f + i * 0.18f), suelo + (h - suelo) * 0.37f, 0xFFE6E0C8);
    }

    private void fujiForma(Path pa, float cx, float w, float hor, float pk) {
        float top = hor - pk;
        pa.reset();
        pa.moveTo(cx - 0.04f * w, top);
        for (int i = 1; i <= 14; i++) {
            float f = i / 14f;
            pa.lineTo(cx - (0.04f * w + 0.5f * w * (float) Math.pow(f, 1.8)), top + pk * f);
        }
        for (int i = 14; i >= 0; i--) {
            float f = i / 14f;
            pa.lineTo(cx + (0.04f * w + 0.5f * w * (float) Math.pow(f, 1.8)), top + pk * f);
        }
        pa.close();
    }

    private void fuji(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor, cx = w * 0.5f, pk = hor * 0.8f, top = hor - pk;
        fujiForma(path2, cx, w, hor, pk);
        p.setColor(col(0xFF5C7699));
        c.drawPath(path2, p);
        c.save();
        c.clipPath(path2);
        p.setColor(0x16000000);
        c.drawRect(cx, 0, w, hor, p);
        path.reset();
        path.moveTo(cx - 0.4f * w, top - 4);
        path.lineTo(cx + 0.4f * w, top - 4);
        for (int k = 8; k >= 0; k--) {
            path.lineTo(cx - 0.4f * w + k * 0.1f * w, top + pk * (0.16f + ((k % 2) == 0 ? 0.1f : 0f) + 0.03f * (k % 3)));
        }
        path.close();
        p.setColor(col(ESPUMA));
        c.drawPath(path, p);
        c.restore();
        montes(c, w, hor, hor * 0.16f, 0xFF4F7A5A, 3.3f, 2f);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF5A8DB5), col(0xFF2A5580));
        // reflejo del Fuji en el lago
        c.save();
        rf.set(0, hor, w, suelo);
        c.clipRect(rf);
        c.scale(1f, -1f, 0f, hor);
        p.setColor(col(0xFF5C7699));
        p.setAlpha(0x55);
        c.drawPath(path2, p);
        c.restore();
        // orilla: pagoda y sakuras
        p.setColor(col(0xFF4F8A4A));
        rf.set(w * 0.62f, hor + sa * 0.15f, w * 1.05f, hor + sa * 0.5f);
        c.drawOval(rf, p);
        pagoda(c, w * 0.82f, hor + sa * 0.32f, h * 0.17f);
        sakuraArbol(c, w * 0.06f, hor + sa * 0.5f, h * 0.2f);
        sakuraArbol(c, w * 0.2f, hor + sa * 0.38f, h * 0.14f);
        pino(c, w * 0.95f, hor + sa * 0.5f, h * 0.12f);
        suelo(c, w, h, suelo, 0xFF7FA05A, 0xFF6B8F4A);
        p.setColor(col(0xFFF4B6C8));
        for (int i = 0; i < 14; i++) c.drawCircle(w * ((i * 0.137f) % 1f), suelo + (h - suelo) * (0.2f + 0.7f * ((i * 0.31f) % 1f)), 2.2f, p);
    }

    private void torreTokio(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor, paso = Math.max(5f, h * 0.02f);
        ep.ciudad(c, w, hor, hor * 0.34f, m, false, false);
        torre(c, torreX(w), torreBase(h), torreArriba(h));
        r(c, 0, hor, w, suelo, 0xFF5F7A55);
        for (int i = 0; i < 9; i++) {
            float x = i * w * 0.12f - w * 0.02f, bh = sa * (0.45f + 0.2f * ALTO_GINZA[i % 5]);
            edificio(c, x, hor + sa * 0.62f, w * 0.1f, bh, EDIF_GINZA[(i + 1) % 4], paso);
        }
        for (int i = 0; i < 6; i++) {
            float x = i * w * 0.2f + w * 0.02f;
            edificio(c, x, suelo, w * 0.16f, sa * (0.45f + 0.15f * ALTO_GINZA[(i + 2) % 5]), EDIF_GINZA[i % 4], paso);
            p.setColor(col(0xFF3F7A45));
            c.drawCircle(x + w * 0.18f, suelo - sa * 0.1f, sa * 0.1f, p);
        }
        suelo(c, w, h, suelo, 0xFF8C8F99, 0xFF6E717B);
        for (int i = 0; i < 6; i++) {
            float x = w * (0.04f + i * 0.17f);
            r(c, x, suelo + (h - suelo) * 0.3f, x + w * 0.1f, suelo + (h - suelo) * 0.35f, 0xFFD8D8D0);
        }
    }

    private void osaka(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor, cx = w * 0.6f, yb = hor + sa * 0.08f;
        montes(c, w, hor, hor * 0.2f, 0xFF8FA8A0, 2.1f, 1f);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF3F7A7A), col(0xFF1E4A55));
        // base de piedra
        float yt = yb - hor * 0.22f;
        cuad(c, 0xFF8E8C84, cx - w * 0.33f, yb, cx + w * 0.33f, yb, cx + w * 0.26f, yt, cx - w * 0.26f, yt);
        for (int i = 1; i < 5; i++) {
            float yy = yb - (yb - yt) * i / 5f, ins = w * 0.07f * i / 5f;
            linea(c, cx - w * 0.33f + ins, yy, cx + w * 0.33f - ins, yy, 1.2f, col(0xFF5E5C56));
        }
        // torres del castillo
        float[] anchos = {0.4f, 0.34f, 0.27f, 0.2f, 0.13f};
        float y = yt;
        for (int i = 0; i < 5; i++) {
            float tw = w * anchos[i] * 0.85f, wall = hor * 0.07f, roof = hor * 0.06f;
            r(c, cx - tw * 0.4f, y - wall, cx + tw * 0.4f, y, 0xFFF4F1E6);
            luz(c, cx - tw * 0.08f, y - wall * 0.75f, tw * 0.16f, wall * 0.4f);
            y -= wall;
            cuad(c, 0xFF3F8F7A, cx - tw * 0.6f, y, cx + tw * 0.6f, y, cx + tw * 0.36f, y - roof, cx - tw * 0.36f, y - roof);
            tri(c, 0xFF3F8F7A, cx - tw * 0.6f, y, cx - tw * 0.68f, y - roof * 0.5f, cx - tw * 0.5f, y - roof * 0.25f);
            tri(c, 0xFF3F8F7A, cx + tw * 0.6f, y, cx + tw * 0.68f, y - roof * 0.5f, cx + tw * 0.5f, y - roof * 0.25f);
            r(c, cx - tw * 0.6f, y - 1.5f, cx + tw * 0.6f, y + 1.5f, ORO);
            y -= roof * 0.7f;
        }
        tri(c, ORO, cx - w * 0.015f, y + hor * 0.01f, cx + w * 0.015f, y + hor * 0.01f, cx, y - hor * 0.07f);
        pino(c, w * 0.08f, hor + sa * 0.5f, h * 0.15f);
        pino(c, w * 0.2f, hor + sa * 0.42f, h * 0.11f);
        pino(c, w * 0.95f, hor + sa * 0.5f, h * 0.13f);
        suelo(c, w, h, suelo, 0xFFA29C8E, 0xFF8A8478);
        for (int i = 0; i < 4; i++) {
            float yy = suelo + (h - suelo) * (0.25f + i * 0.2f);
            linea(c, 0, yy, w, yy, 1.4f, col(0xFF7C766A));
            for (float x = (i % 2) * w * 0.05f; x < w; x += w * 0.1f) linea(c, x, yy, x, yy + (h - suelo) * 0.2f, 1.4f, col(0xFF7C766A));
        }
    }

    private void buda(Canvas c, float cx, float base, float hb) {
        int bronce = 0xFF5F8A72;
        r(c, cx - hb * 0.5f, base - hb * 0.05f, cx + hb * 0.5f, base, 0xFF8E8C84);
        rf.set(cx - hb * 0.46f, base - hb * 0.36f, cx + hb * 0.46f, base - hb * 0.02f);
        p.setColor(col(bronce));
        c.drawOval(rf, p);
        cuad(c, bronce, cx - hb * 0.32f, base - hb * 0.24f, cx - hb * 0.2f, base - hb * 0.66f,
                cx + hb * 0.2f, base - hb * 0.66f, cx + hb * 0.32f, base - hb * 0.24f);
        p.setColor(col(bronce));
        c.drawCircle(cx, base - hb * 0.78f, hb * 0.13f, p);
        c.drawCircle(cx, base - hb * 0.92f, hb * 0.055f, p);
        rf.set(cx - hb * 0.17f, base - hb * 0.82f, cx - hb * 0.12f, base - hb * 0.66f);
        c.drawOval(rf, p);
        rf.set(cx + hb * 0.12f, base - hb * 0.82f, cx + hb * 0.17f, base - hb * 0.66f);
        c.drawOval(rf, p);
        rf.set(cx - hb * 0.1f, base - hb * 0.36f, cx + hb * 0.1f, base - hb * 0.28f);
        p.setColor(col(0xFF4A6F5B));
        c.drawOval(rf, p);
        linea(c, cx - hb * 0.07f, base - hb * 0.78f, cx - hb * 0.03f, base - hb * 0.78f, 1.5f, col(0xFF2F4A3C));
        linea(c, cx + hb * 0.03f, base - hb * 0.78f, cx + hb * 0.07f, base - hb * 0.78f, 1.5f, col(0xFF2F4A3C));
        linea(c, cx - hb * 0.15f, base - hb * 0.5f, cx - hb * 0.05f, base - hb * 0.3f, 1.5f, col(0xFF3E5E4D));
        linea(c, cx + hb * 0.15f, base - hb * 0.5f, cx + hb * 0.05f, base - hb * 0.3f, 1.5f, col(0xFF3E5E4D));
    }

    private void kamakura(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor, cx = w * 0.3f;
        montes(c, w, hor, hor * 0.34f, 0xFF55775A, 0.7f, 1.2f);
        montes(c, w, hor, hor * 0.2f, 0xFF3F6648, 2.4f, 2f);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF3F7FA8), col(0xFF1B4A78));
        p.setColor(col(0xFF4B7048));
        rf.set(cx - hor * 0.5f, hor - sa * 0.02f, cx + hor * 0.5f, hor + sa * 0.3f);
        c.drawOval(rf, p);
        buda(c, cx, hor + sa * 0.12f, hor * 0.75f);
        pino(c, cx - hor * 0.62f, hor + sa * 0.15f, h * 0.12f);
        pino(c, cx + hor * 0.6f, hor + sa * 0.14f, h * 0.1f);
        suelo(c, w, h, suelo, 0xFFE4D2A5, 0xFFD8C08C);
        float fy = suelo + sn(t * 0.8f) * (h - suelo) * 0.03f;
        p.setColor(ESPUMA);
        p.setAlpha(170);
        c.drawRect(0, fy - 4, w, fy + 3, p);
    }

    private void monstruos(Canvas c, float w, float h, float hor, float suelo) {
        float sa = suelo - hor, cx = w * 0.68f, pk = hor * 0.62f, top = hor - pk;
        cuad(c, 0xFF5A3B33, cx - w * 0.36f, hor, cx - w * 0.06f, top, cx + w * 0.06f, top, cx + w * 0.36f, hor);
        p.setColor(0xFFFF7A2B);
        rf.set(cx - w * 0.06f, top - 3, cx + w * 0.06f, top + 5);
        c.drawOval(rf, p);
        linea(c, cx - w * 0.01f, top + 4, cx - w * 0.05f, top + pk * 0.45f, 2.4f, 0xFFFF5A1F);
        linea(c, cx + w * 0.02f, top + 4, cx + w * 0.07f, top + pk * 0.35f, 2f, 0xFFFF5A1F);
        for (int k = 0; k < 5; k++) {
            float f = (t * 0.12f + k * 0.2f) % 1f;
            p.setColor(col(0xFF9A9A9A));
            p.setAlpha((int) (150 * (1 - f)));
            c.drawCircle(cx + f * w * 0.1f + sn(t + k) * 4, top - f * hor * 0.5f, hor * (0.04f + 0.1f * f), p);
        }
        montes(c, w, hor, hor * 0.14f, 0xFF3E7A45, 4.1f, 2f);
        ep.aguaUkiyo(c, w, hor, suelo, t, m, col(0xFF35C0C4), col(0xFF117F94));
        p.setColor(col(0xFF4F9A4C));
        rf.set(-w * 0.1f, hor + sa * 0.25f, w * 0.34f, hor + sa * 0.7f);
        c.drawOval(rf, p);
        rf.set(w * 0.8f, hor + sa * 0.3f, w * 1.1f, hor + sa * 0.7f);
        c.drawOval(rf, p);
        palma(c, w * 0.08f, hor + sa * 0.5f, h * 0.24f, 1f);
        palma(c, w * 0.2f, hor + sa * 0.55f, h * 0.18f, -1f);
        palma(c, w * 0.93f, hor + sa * 0.55f, h * 0.2f, -1f);
        suelo(c, w, h, suelo, 0xFF6FA24A, 0xFFE9D79F);
        p.setColor(col(0xFFE0415A));
        for (int i = 0; i < 8; i++) c.drawCircle(w * ((i * 0.173f + 0.05f) % 1f), suelo + (h - suelo) * (0.3f + 0.6f * ((i * 0.37f) % 1f)), 2.6f, p);
    }
}
