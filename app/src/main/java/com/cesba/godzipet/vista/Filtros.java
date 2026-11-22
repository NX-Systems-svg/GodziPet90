package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * Aplica un filtro de pantalla completa.
 * Uso: {@code int s = Filtros.inicio(c, w, h, f); ...dibujar escena...; Filtros.fin(c, s, w, h, f, t);}
 */
public final class Filtros {
    private static final int PUNTOS = 260;
    private static final int RAYAS = 4;

    private static final Paint capaBN = new Paint();
    private static final Paint capaPoster = new Paint();
    private static final Paint pen = new Paint();
    private static final float[] gx = new float[PUNTOS], gy = new float[PUNTOS];
    private static final float[] rayasX = new float[RAYAS];
    private static final java.util.Random azar = new java.util.Random(7);

    private static RadialGradient vinBN, vinPoster, vinCRT;
    private static float vinW = -1, vinH = -1;
    private static float[] puntosGrano;
    private static long ultimoGrano = -1;
    private static boolean listo;

    private Filtros() { }

    private static void preparar() {
        if (listo) return;
        listo = true;
        // Escala de grises con ligero contraste
        ColorMatrix g = new ColorMatrix();
        g.setSaturation(0f);
        ColorMatrix k = new ColorMatrix(new float[] {
                1.18f, 0, 0, 0, -14,
                0, 1.18f, 0, 0, -14,
                0, 0, 1.18f, 0, -14,
                0, 0, 0, 1, 0});
        g.postConcat(k);
        capaBN.setColorFilter(new ColorMatrixColorFilter(g));
        // Tonos cálidos de póster: gris -> sepia/crema
        ColorMatrix s = new ColorMatrix();
        s.setSaturation(0.55f);
        ColorMatrix tono = new ColorMatrix(new float[] {
                1.10f, 0.08f, 0, 0, 10,
                0.02f, 1.0f, 0.04f, 0, 6,
                0, 0.02f, 0.78f, 0, -6,
                0, 0, 0, 1, 0});
        s.postConcat(tono);
        capaPoster.setColorFilter(new ColorMatrixColorFilter(s));
        puntosGrano = new float[PUNTOS * 2];
    }

    /** Abre una capa si el filtro lo necesita (blanco y negro / sepia). Devuelve el id de save. */
    public static int inicio(Canvas c, float w, float h, Filtro f) {
        preparar();
        if (f == Filtro.B_N_1954) return c.saveLayer(0, 0, w, h, capaBN);
        if (f == Filtro.POSTER) return c.saveLayer(0, 0, w, h, capaPoster);
        return c.save();
    }

    /** Cierra la capa y dibuja grano, vineta o líneas de barrido. t = segundos. */
    public static void fin(Canvas c, int save, float w, float h, Filtro f, float t) {
        c.restoreToCount(save);
        if (f == null || f == Filtro.NINGUNO) return;
        preparar();
        cachearVinetas(w, h);
        pen.reset();
        pen.setAntiAlias(false);
        switch (f) {
            case B_N_1954:
                dibujarB_N(c, w, h, t);
                break;
            case POSTER:
                dibujarPoster(c, w, h, t);
                break;
            case CRT:
                dibujarCRT(c, w, h, t);
                break;
            default:
                break;
        }
    }

    private static void cachearVinetas(float w, float h) {
        if (w == vinW && h == vinH) return;
        vinW = w;
        vinH = h;
        float r = (float) Math.hypot(w, h) * 0.5f;
        vinBN = new RadialGradient(w / 2, h / 2, r, new int[] {0x00000000, 0x00000000, 0xB8000000},
                new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        vinPoster = new RadialGradient(w / 2, h / 2, r, new int[] {0x00000000, 0x00000000, 0x6A3A2208},
                new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        vinCRT = new RadialGradient(w / 2, h / 2, r, new int[] {0x00000000, 0x00000000, 0x80000000},
                new float[] {0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
        for (int i = 0; i < PUNTOS; i++) {
            gx[i] = azar.nextFloat();
            gy[i] = azar.nextFloat();
        }
    }

    private static void vineta(Canvas c, float w, float h, RadialGradient g) {
        pen.setShader(g);
        c.drawRect(0, 0, w, h, pen);
        pen.setShader(null);
    }

    private static void dibujarB_N(Canvas c, float w, float h, float t) {
        // Parpadeo de brillo del proyector
        long fotograma = (long) (t * 24f);
        float flick = (float) Math.sin(t * 37f) * 0.5f + (float) Math.sin(t * 13f) * 0.5f;
        int alfa = (int) (Math.max(0f, flick) * 14f);
        if (alfa > 0) {
            pen.setColor(Color.argb(alfa, 255, 255, 255));
            c.drawRect(0, 0, w, h, pen);
        }
        // Grano: puntos preasignados que se desplazan cada fotograma
        if (fotograma != ultimoGrano) {
            ultimoGrano = fotograma;
            float ox = azar.nextFloat() * w, oy = azar.nextFloat() * h;
            for (int i = 0; i < PUNTOS; i++) {
                puntosGrano[i * 2] = (gx[i] * w + ox) % w;
                puntosGrano[i * 2 + 1] = (gy[i] * h + oy) % h;
            }
            for (int i = 0; i < RAYAS; i++) rayasX[i] = azar.nextFloat() * w;
        }
        float px = Math.max(1.5f, w / 360f);
        pen.setStrokeWidth(px);
        pen.setStrokeCap(Paint.Cap.SQUARE);
        pen.setColor(0x66FFFFFF);
        c.drawPoints(puntosGrano, 0, PUNTOS, pen);
        pen.setColor(0x55000000);
        c.drawPoints(puntosGrano, PUNTOS, PUNTOS, pen);
        // Arañazos verticales ocasionales
        if ((fotograma / 6) % 5 < 2) {
            pen.setStrokeWidth(Math.max(1f, px * 0.6f));
            pen.setColor(0x88FFFFFF);
            c.drawLine(rayasX[0], 0, rayasX[0] + 2f, h, pen);
            if ((fotograma / 6) % 5 == 1) c.drawLine(rayasX[1], h * 0.1f, rayasX[1] - 1f, h * 0.8f, pen);
        }
        vineta(c, w, h, vinBN);
    }

    private static void dibujarPoster(Canvas c, float w, float h, float t) {
        float px = Math.max(1.5f, w / 300f);
        pen.setStrokeWidth(px);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setColor(0x403A2A14);
        for (int i = 0; i < PUNTOS / 2; i++) {
            puntosGrano[i * 2] = gx[i] * w;
            puntosGrano[i * 2 + 1] = gy[i] * h;
        }
        c.drawPoints(puntosGrano, 0, PUNTOS, pen);
        vineta(c, w, h, vinPoster);
    }

    private static void dibujarCRT(Canvas c, float w, float h, float t) {
        float paso = 3f * Math.max(1f, h / 900f);
        pen.setColor(0x2E000000);
        for (float y = 0; y < h; y += paso) c.drawRect(0, y, w, y + paso * 0.45f, pen);
        // Barra de refresco que desciende lentamente
        float by = ((t * 0.12f) % 1.4f - 0.2f) * h;
        pen.setColor(0x10FFFFFF);
        c.drawRect(0, by, w, by + h * 0.12f, pen);
        vineta(c, w, h, vinCRT);
    }
}
