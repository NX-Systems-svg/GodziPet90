package com.cesba.godzipet.vista;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Modo foto: marco de póster de película japonesa (sol rojo con rayos, título ゴジラ). */
public final class PosterPainter {
    private static final int W = 1080, H = 1350;
    private static final int CREMA = 0xFFF2E6C9;
    private static final int CREMA_OSC = 0xFFE7D6AC;
    private static final int ROJO = 0xFFC8102E;
    private static final int TINTA = 0xFF111111;
    private static final String TITULO = "ゴジラ";

    private PosterPainter() { }

    /**
     * Devuelve un bitmap nuevo de 1080x1350 con la escena dentro de un marco de póster japonés:
     * sol rojo con rayos, título ゴジラ, nombre del kaiju, su forma y el lugar.
     */
    public static Bitmap crear(Bitmap escena, String nombreKaiju, String forma, String lugar) {
        Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(CREMA);

        // Rayos y sol naciente (centrados detrás de la escena)
        float cx = W / 2f, cy = 640f;
        p.setColor(CREMA_OSC);
        Path ray = new Path();
        int rayos = 24;
        for (int i = 0; i < rayos; i++) {
            double a0 = Math.toRadians(i * 360.0 / rayos);
            double a1 = a0 + Math.toRadians(360.0 / rayos / 2.0);
            ray.reset();
            ray.moveTo(cx, cy);
            ray.lineTo(cx + (float) Math.cos(a0) * 1800, cy + (float) Math.sin(a0) * 1800);
            ray.lineTo(cx + (float) Math.cos(a1) * 1800, cy + (float) Math.sin(a1) * 1800);
            ray.close();
            c.drawPath(ray, p);
        }
        p.setColor(ROJO);
        c.drawCircle(cx, cy, 540f, p);

        // Escena recortada al centro dentro de un marco de tinta
        RectF marco = new RectF(80, 290, 1000, 1010);
        p.setColor(TINTA);
        c.drawRect(marco.left - 22, marco.top - 22, marco.right + 22, marco.bottom + 22, p);
        p.setColor(CREMA);
        c.drawRect(marco.left - 8, marco.top - 8, marco.right + 8, marco.bottom + 8, p);
        if (escena != null && escena.getWidth() > 0 && escena.getHeight() > 0) {
            Rect src = recorteCentral(escena.getWidth(), escena.getHeight(), marco.width() / marco.height());
            Paint pf = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            c.drawBitmap(escena, src, marco, pf);
        } else {
            p.setColor(TINTA);
            c.drawRect(marco, p);
        }

        // Título gigante: relleno rojo con contorno de tinta
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        t.setTextAlign(Paint.Align.CENTER);
        t.setStrokeJoin(Paint.Join.ROUND);
        t.setTextSize(100f);
        float tam = 100f * 900f / Math.max(1f, t.measureText(TITULO));
        t.setTextSize(Math.min(tam, 210f));
        float yTit = 245f;
        t.setStyle(Paint.Style.STROKE);
        t.setStrokeWidth(t.getTextSize() * 0.16f);
        t.setColor(TINTA);
        c.drawText(TITULO, cx + 6, yTit + 6, t);
        c.drawText(TITULO, cx, yTit, t);
        t.setStyle(Paint.Style.FILL);
        t.setColor(ROJO);
        c.drawText(TITULO, cx, yTit, t);

        // Nombre, forma y lugar
        p.setColor(0xEEF2E6C9); // franja crema para que el texto se lea sobre el sol rojo
        c.drawRect(80, 1040, 1000, 1250, p);
        texto(c, t, nombreKaiju, cx, 1112f, 78f, 900f, TINTA, true);
        texto(c, t, forma, cx, 1172f, 44f, 900f, ROJO, true);
        texto(c, t, lugar, cx, 1232f, 38f, 900f, TINTA, false);

        // Pie
        p.setColor(TINTA);
        c.drawRect(80, 1262, 1000, 1266, p);
        texto(c, t, "GODZI-PET 90 · 怪獣王", cx, 1315f, 34f, 900f, TINTA, true);
        return out;
    }

    private static Rect recorteCentral(int bw, int bh, float aspecto) {
        float aBmp = bw / (float) bh;
        if (aBmp > aspecto) {
            int nw = Math.round(bh * aspecto);
            int x = (bw - nw) / 2;
            return new Rect(x, 0, x + nw, bh);
        }
        int nh = Math.round(bw / aspecto);
        int y = (bh - nh) / 2;
        return new Rect(0, y, bw, y + nh);
    }

    /** Dibuja una línea centrada que se reduce para caber en maxAncho. */
    private static void texto(Canvas c, Paint t, String s, float x, float y, float tam, float maxAncho,
                              int color, boolean negrita) {
        if (s == null || s.isEmpty()) return;
        t.setStyle(Paint.Style.FILL);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, negrita ? Typeface.BOLD : Typeface.NORMAL));
        t.setTextSize(tam);
        float ancho = t.measureText(s);
        if (ancho > maxAncho) t.setTextSize(tam * maxAncho / ancho);
        t.setColor(color);
        c.drawText(s, x, y, t);
    }
}
