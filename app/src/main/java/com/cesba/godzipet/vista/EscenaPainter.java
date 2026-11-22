package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import java.util.Calendar;
import java.util.Random;

/** Fondo de la bahía de Nueva York: cielo según la hora real, skyline y agua animada. */
public final class EscenaPainter {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] estrellas = new float[90];
    private float[] edificios;

    public EscenaPainter() {
        Random r = new Random(90);
        for (int i = 0; i < estrellas.length; i++) estrellas[i] = r.nextFloat();
        edificios = new float[34 * 3];
        for (int i = 0; i < 34; i++) {
            edificios[i * 3] = r.nextFloat();         // altura relativa
            edificios[i * 3 + 1] = 0.6f + r.nextFloat() * 0.8f; // ancho relativo
            edificios[i * 3 + 2] = r.nextFloat();     // antena
        }
    }

    /**
     * CONTRATO (lo implementa el agente de escenarios): dibuja el fondo COMPLETO del escenario
     * japonés (cielo, horizonte, elementos icónicos, agua y suelo) en un lienzo w×h, usando las
     * proporciones de {@link Escenas}. momento: 0 día, 1 atardecer, 2 noche. t en segundos.
     */
    public void dibujarEscenario(android.graphics.Canvas c, float w, float h,
                                 com.cesba.godzipet.modelo.Escenario e, float t, int momento, boolean noche) {
        cielo(c, w, h * Escenas.HORIZONTE, momento, t, noche);
        ciudad(c, w, h * Escenas.HORIZONTE, h * 0.22f, momento, noche);
        agua(c, w, h * Escenas.HORIZONTE, h * Escenas.INICIO_SUELO, t, noche || momento == 2);
        muelle(c, w, h * Escenas.INICIO_SUELO, h);
    }

    /** CONTRATO: partículas de clima (sakura, lluvia, nieve) sobre todo, con estado interno. */
    public void dibujarClima(android.graphics.Canvas c, float w, float h, Clima clima, float t) { }

    /** CONTRATO: rectángulo (en px) de la punta de la Torre de Tokio en ese escenario, o null. */
    public android.graphics.RectF puntaTorre(float w, float h, com.cesba.godzipet.modelo.Escenario e) {
        return null;
    }

    /** 0 = día, 1 = atardecer, 2 = noche, según la hora del teléfono. */
    public static int momento() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 7 && h < 17) return 0;
        if (h >= 17 && h < 20) return 1;
        return 2;
    }

    public void cielo(Canvas c, float w, float horizonte, int momento, float t, boolean forzarNoche) {
        int m = forzarNoche ? 2 : momento;
        int arriba, abajo;
        if (m == 0) { arriba = 0xFF4FA8E8; abajo = 0xFFBFE6FF; }
        else if (m == 1) { arriba = 0xFF3B2A6B; abajo = 0xFFFF9A5A; }
        else { arriba = 0xFF070B1F; abajo = 0xFF141E42; }
        p.setShader(new LinearGradient(0, 0, 0, horizonte, arriba, abajo, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, horizonte, p);
        p.setShader(null);
        if (m == 2) {
            for (int i = 0; i < estrellas.length; i += 3) {
                float brillo = 0.5f + 0.5f * (float) Math.sin(t * 2 + i);
                p.setColor(((int) (120 + 135 * brillo) << 24) | 0xFFFFFF);
                c.drawCircle(estrellas[i] * w, estrellas[i + 1] * horizonte * 0.8f, 1.2f + estrellas[i + 2] * 1.5f, p);
            }
            p.setColor(0xFFF4F1D0);
            c.drawCircle(w * 0.82f, horizonte * 0.2f, horizonte * 0.08f, p);
            p.setColor(arriba);
            c.drawCircle(w * 0.84f, horizonte * 0.18f, horizonte * 0.07f, p);
        } else {
            p.setColor(m == 0 ? 0xFFFFE46B : 0xFFFF7043);
            c.drawCircle(w * 0.8f, horizonte * (m == 0 ? 0.22f : 0.75f), horizonte * 0.09f, p);
            p.setColor(0xCCFFFFFF);
            float nube = (t * 6) % (w + 200) - 100;
            nube(c, nube, horizonte * 0.3f, horizonte * 0.08f);
            nube(c, (nube + w * 0.55f) % (w + 200) - 100, horizonte * 0.15f, horizonte * 0.06f);
        }
    }

    private void nube(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r, p);
        c.drawCircle(x + r * 1.1f, y - r * 0.4f, r * 1.2f, p);
        c.drawCircle(x + r * 2.3f, y, r * 0.9f, p);
        c.drawRect(x, y, x + r * 2.3f, y + r * 0.9f, p);
    }

    public void ciudad(Canvas c, float w, float horizonte, float altoMax, int momento, boolean forzarNoche) {
        boolean noche = forzarNoche || momento == 2;
        int color = noche ? 0xFF223066 : momento == 1 ? 0xFF3D2F55 : 0xFF6F86A6;
        float x = 0;
        int i = 0;
        float anchoBase = w / 22f;
        while (x < w) {
            int k = (i % 34) * 3;
            float bw = anchoBase * edificios[k + 1];
            float bh = altoMax * (0.25f + 0.75f * edificios[k]);
            p.setColor(color);
            c.drawRect(x, horizonte - bh, x + bw - 1, horizonte, p);
            if (edificios[k + 2] > 0.75f) c.drawRect(x + bw / 2 - 1, horizonte - bh - bh * 0.18f, x + bw / 2 + 1, horizonte - bh, p);
            if (noche) {
                p.setColor(0xFFFFD86B);
                for (float wy = horizonte - bh + 4; wy < horizonte - 4; wy += 7) {
                    for (float wx = x + 3; wx < x + bw - 4; wx += 6) {
                        if (((int) (wx * 7 + wy * 13)) % 5 == 0) c.drawRect(wx, wy, wx + 2.2f, wy + 3f, p);
                    }
                }
            }
            x += bw;
            i++;
        }
    }

    public void agua(Canvas c, float w, float y0, float y1, float t, boolean noche) {
        p.setShader(new LinearGradient(0, y0, 0, y1, noche ? 0xFF16305A : 0xFF2B7BB9,
                noche ? 0xFF0A1630 : 0xFF14467A, Shader.TileMode.CLAMP));
        c.drawRect(0, y0, w, y1, p);
        p.setShader(null);
        p.setColor(noche ? 0x55A9C8FF : 0x88FFFFFF);
        p.setStrokeWidth(2f);
        for (int fila = 0; fila < 4; fila++) {
            float y = y0 + (y1 - y0) * (0.15f + fila * 0.22f);
            float off = (t * (20 + fila * 8)) % 60;
            for (float x = -60 + off; x < w; x += 60) c.drawLine(x, y, x + 18 + fila * 4, y, p);
        }
    }

    /** Muelle de madera donde vive el kaiju. */
    public void muelle(Canvas c, float w, float y0, float y1) {
        p.setColor(0xFF5B4030);
        c.drawRect(0, y0, w, y1, p);
        p.setColor(0xFF7A5940);
        float tabla = (y1 - y0) / 4f;
        for (int i = 0; i < 4; i++) c.drawRect(0, y0 + i * tabla + 2, w, y0 + (i + 1) * tabla - 2, p);
        p.setColor(0xFF2B1E16);
        c.drawRect(0, y0, w, y0 + 4, p);
    }
}
