package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.cesba.godzipet.modelo.Escenario;

import java.util.Calendar;
import java.util.Random;

/**
 * Fondos japoneses: cielo según la hora real, agua estilo ukiyo-e, clima y los 8 escenarios
 * (estos últimos en {@link EscenariosJapon}). Los shaders se cachean para no asignar por cuadro.
 */
public final class EscenaPainter {
    private static final int PARTICULAS = 80;
    private static final int[][] CIELOS = {
            {0xFF4FA3E6, 0xFFA9D8F8, 0xFFE8F4FF},
            {0xFF3B2A6B, 0xFFC4587A, 0xFFFFB066},
            {0xFF050917, 0xFF0C1535, 0xFF1A2650}};
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final RectF puntaRect = new RectF();
    private final float[] estrellas = new float[90];
    private final float[] edificios;
    private final float[] px = new float[PARTICULAS], py = new float[PARTICULAS],
            pf = new float[PARTICULAS], pv = new float[PARTICULAS];
    final EscenariosJapon japon = new EscenariosJapon(this);
    private Shader cieloShader, aguaShader;
    private long cieloClave = -1, aguaClave = -1;

    public EscenaPainter() {
        Random r = new Random(90);
        for (int i = 0; i < estrellas.length; i++) estrellas[i] = r.nextFloat();
        edificios = new float[34 * 3];
        for (int i = 0; i < 34; i++) {
            edificios[i * 3] = r.nextFloat();         // altura relativa
            edificios[i * 3 + 1] = 0.6f + r.nextFloat() * 0.8f; // ancho relativo
            edificios[i * 3 + 2] = r.nextFloat();     // antena
        }
        for (int i = 0; i < PARTICULAS; i++) {
            px[i] = r.nextFloat();
            py[i] = r.nextFloat();
            pf[i] = r.nextFloat();
            pv[i] = 0.5f + r.nextFloat();
        }
    }

    /**
     * Dibuja el fondo COMPLETO del escenario japonés (cielo, horizonte, elementos icónicos, agua y
     * suelo) en un lienzo w×h, con las proporciones de {@link Escenas}. momento: 0 día, 1 atardecer,
     * 2 noche. t en segundos.
     */
    public void dibujarEscenario(Canvas c, float w, float h, Escenario e, float t, int momento, boolean noche) {
        japon.dibujar(c, w, h, e, t, noche || momento == 2 ? 2 : momento);
    }

    /** Partículas de clima (sakura, lluvia, nieve, sol) sobre toda la escena; sin estado mutable. */
    public void dibujarClima(Canvas c, float w, float h, Clima clima, float t) {
        if (clima == null) return;
        float d = Math.max(0.6f, w / 360f);
        switch (clima) {
            case SAKURA:
                for (int i = 0; i < 40; i++) {
                    float y = ((py[i] + t * 0.04f * pv[i]) % 1f) * (h + 20) - 10;
                    float x = px[i] * w + (float) Math.sin(t * 0.9f * pv[i] + pf[i] * 6.28f) * 26 * d;
                    float s = (3.5f + pf[i] * 3f) * d;
                    p.setColor(i % 2 == 0 ? 0xDDF8B8CC : 0xDDFFD3E0);
                    c.save();
                    c.translate(x, y);
                    c.rotate(t * 70f * pv[i] + pf[i] * 360f);
                    rf.set(-s, -s * 0.55f, s, s * 0.55f);
                    c.drawOval(rf, p);
                    c.restore();
                }
                break;
            case LLUVIA:
                p.setColor(0x1A203040);
                c.drawRect(0, 0, w, h, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.3f * d);
                p.setColor(0x99B8D4F0);
                for (int i = 0; i < PARTICULAS; i++) {
                    float y = ((py[i] + t * (0.9f + pv[i] * 0.5f)) % 1f) * (h + 30) - 15;
                    float x = (px[i] * (w + 80) + y * 0.22f) % (w + 80) - 20;
                    c.drawLine(x, y, x - 3 * d, y + 14 * d, p);
                }
                p.setStyle(Paint.Style.FILL);
                break;
            case NIEVE:
                p.setColor(0xDDFFFFFF);
                for (int i = 0; i < 70; i++) {
                    float y = ((py[i] + t * 0.05f * pv[i]) % 1f) * (h + 10) - 5;
                    float x = px[i] * w + (float) Math.sin(t * 0.7f + pf[i] * 6.28f) * 14 * d;
                    c.drawCircle(x, y, (1.4f + pf[i] * 2.2f) * d, p);
                }
                break;
            default:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(16 * d);
                for (int k = 0; k < 6; k++) {
                    float a = (float) Math.toRadians(100 + k * 13);
                    float alfa = 0.5f + 0.5f * (float) Math.sin(t * 0.8f + k * 1.3f);
                    p.setColor(0xFFFFF2B0);
                    p.setAlpha((int) (14 + 22 * alfa));
                    float cx = w * 0.95f, cy = -h * 0.02f;
                    c.drawLine(cx, cy, cx + (float) Math.cos(a) * h, cy + (float) Math.sin(a) * h, p);
                }
                p.setStyle(Paint.Style.FILL);
                break;
        }
    }

    /** Rectángulo (en px) de la punta de la Torre de Tokio en ese escenario, o null. */
    public RectF puntaTorre(float w, float h, Escenario e) {
        if (e != Escenario.TORRE_TOKIO) return null;
        float lado = w * 0.12f, cx = EscenariosJapon.torreX(w), top = EscenariosJapon.torreArriba(h);
        float y0 = Math.max(0f, top - lado * 0.2f);
        puntaRect.set(cx - lado / 2, y0, cx + lado / 2, y0 + lado);
        return puntaRect;
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
        long clave = ((long) (horizonte * 4) << 3) | m;
        if (clave != cieloClave) {
            cieloClave = clave;
            cieloShader = new LinearGradient(0, 0, 0, horizonte, CIELOS[m], null, Shader.TileMode.CLAMP);
        }
        p.setShader(cieloShader);
        c.drawRect(0, 0, w, horizonte, p);
        p.setShader(null);
        if (m == 2) {
            for (int i = 0; i < estrellas.length; i += 3) {
                float brillo = 0.5f + 0.5f * (float) Math.sin(t * 2 + i);
                p.setColor(((int) (120 + 135 * brillo) << 24) | 0xFFFFFF);
                c.drawCircle(estrellas[i] * w, estrellas[i + 1] * horizonte * 0.8f, 1.2f + estrellas[i + 2] * 1.5f, p);
            }
            float lx = w * 0.82f, ly = horizonte * 0.2f, lr = horizonte * 0.08f;
            p.setColor(0x22F4F1D0);
            c.drawCircle(lx, ly, lr * 1.7f, p);
            p.setColor(0xFFF4F1D0);
            c.drawCircle(lx, ly, lr, p);
            p.setColor(0x33B8B090);
            c.drawCircle(lx - lr * 0.3f, ly - lr * 0.2f, lr * 0.22f, p);
            c.drawCircle(lx + lr * 0.35f, ly + lr * 0.3f, lr * 0.16f, p);
        } else if (m == 0) {
            p.setColor(0x33FFE46B);
            c.drawCircle(w * 0.8f, horizonte * 0.22f, horizonte * 0.15f, p);
            p.setColor(0xFFFFE46B);
            c.drawCircle(w * 0.8f, horizonte * 0.22f, horizonte * 0.09f, p);
        } else {
            p.setColor(0x44FF7043);
            c.drawCircle(w * 0.8f, horizonte * 0.75f, horizonte * 0.16f, p);
            p.setColor(0xFFE8442B);
            c.drawCircle(w * 0.8f, horizonte * 0.75f, horizonte * 0.09f, p);
        }
        // nubes a la deriva (más lentas cuanto más lejanas)
        p.setColor(m == 0 ? 0xDDFFFFFF : m == 1 ? 0xBBFFC9B0 : 0x22AEB8E0);
        for (int k = 0; k < 4; k++) {
            float ancho = w + 300;
            float x = ((estrellas[k] * ancho + t * (4 + k * 2.5f)) % ancho) - 150;
            nube(c, x, horizonte * (0.14f + 0.17f * k), horizonte * (0.05f + 0.01f * (k % 2)));
        }
    }

    private void nube(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r, p);
        c.drawCircle(x + r * 1.1f, y - r * 0.4f, r * 1.2f, p);
        c.drawCircle(x + r * 2.3f, y, r * 0.9f, p);
        c.drawRect(x, y, x + r * 2.3f, y + r * 0.9f, p);
    }

    public void ciudad(Canvas c, float w, float horizonte, float altoMax, int momento, boolean forzarNoche) {
        ciudad(c, w, horizonte, altoMax, momento, forzarNoche, true);
    }

    /** Skyline japonés de los años 50; con la Torre de Tokio si {@code conTorre}. */
    void ciudad(Canvas c, float w, float horizonte, float altoMax, int momento, boolean forzarNoche, boolean conTorre) {
        int m = forzarNoche ? 2 : momento;
        boolean noche = m == 2;
        int color = noche ? 0xFF223066 : m == 1 ? 0xFF3D2F55 : 0xFF6F86A6;
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
        if (conTorre) {
            japon.m = m;
            japon.t = System.currentTimeMillis() % 100000L / 1000f;
            japon.torre(c, w * 0.8f, horizonte, horizonte - altoMax * 1.8f);
        }
    }

    public void agua(Canvas c, float w, float y0, float y1, float t, boolean noche) {
        aguaUkiyo(c, w, y0, y1, t, noche ? 2 : 0, noche ? 0xFF16305A : 0xFF2B7BB9, noche ? 0xFF0A1630 : 0xFF14467A);
    }

    /** Agua estilo ukiyo-e: degradado, filas de olas rizadas con espuma y deriva lenta. */
    public void aguaUkiyo(Canvas c, float w, float y0, float y1, float t, int m, int arriba, int abajo) {
        long clave = (((long) (y0 * 4) * 31 + (long) (y1 * 4)) * 31 + arriba) * 31 + abajo;
        if (clave != aguaClave) {
            aguaClave = clave;
            aguaShader = new LinearGradient(0, y0, 0, y1, arriba, abajo, Shader.TileMode.CLAMP);
        }
        p.setShader(aguaShader);
        c.drawRect(0, y0, w, y1, p);
        p.setShader(null);
        int ola = m == 2 ? 0x77A9C8FF : m == 1 ? 0x99FFD9B8 : 0xAAFFFFFF;
        int espuma = m == 2 ? 0xAAC8DCFF : 0xFFF5F1E6;
        int filas = 6;
        for (int f = 0; f < filas; f++) {
            float k = (f + 1f) / filas;
            float y = y0 + (y1 - y0) * (0.08f + 0.86f * k * k);
            float esc = 0.6f + k * 1.4f;
            float paso = 44f * esc;
            float off = (t * (8 + f * 5) * (f % 2 == 0 ? 1 : -1)) % paso;
            for (float x = -paso + off; x < w + paso; x += paso) {
                rf.set(x, y - 8 * esc, x + paso * 0.8f, y + 8 * esc);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.2f + esc);
                p.setColor(ola);
                c.drawArc(rf, 200, 140, false, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(espuma);
                c.drawCircle(rf.left + rf.width() * 0.04f, rf.centerY() - rf.height() * 0.17f, esc * 1.5f, p);
                c.drawCircle(rf.left + rf.width() * 0.96f, rf.centerY() - rf.height() * 0.17f, esc * 1.1f, p);
            }
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
