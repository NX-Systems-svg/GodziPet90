package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Sistema de partículas con pool fijo: no crea objetos por cuadro. Las vistas llaman a
 * {@link #dibujar} en cada onDraw.
 */
public final class Particulas {
    public enum Tipo { ESCOMBRO, CHISPA, HUMO, PETALO, ATOMICO, MONEDA, CORAZON, BURBUJA, NIEVE, LLUVIA, VAPOR }

    private static final int MAX = 400;
    private static final int MAX_TEXTOS = 24;
    private static final float DT_MAX = 0.05f;
    private static final float DUR_TEXTO = 1.1f;
    private static final Tipo[] TIPOS = Tipo.values();

    // Estado de partículas (arreglos paralelos)
    private final boolean[] vivo = new boolean[MAX];
    private final byte[] tipo = new byte[MAX];
    private final float[] x = new float[MAX], y = new float[MAX];
    private final float[] vx = new float[MAX], vy = new float[MAX];
    private final float[] vida = new float[MAX], vidaMax = new float[MAX];
    private final float[] tam = new float[MAX];
    private final float[] rot = new float[MAX], vrot = new float[MAX];
    private final float[] fase = new float[MAX];
    private final int[] color = new int[MAX];
    private int cursor;
    private int cuenta;

    // Textos flotantes
    private final boolean[] tVivo = new boolean[MAX_TEXTOS];
    private final String[] tTexto = new String[MAX_TEXTOS];
    private final float[] tx = new float[MAX_TEXTOS], ty = new float[MAX_TEXTOS], tVida = new float[MAX_TEXTOS];
    private final int[] tColor = new int[MAX_TEXTOS];
    private final float[] tTam = new float[MAX_TEXTOS];
    private int tCursor;

    private final java.util.Random azar = new java.util.Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path corazon = new Path();
    private final Path petalo = new Path();

    public Particulas() {
        pt.setTextAlign(Paint.Align.CENTER);
        pt.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        // Corazón unitario centrado en (0,0), mide ~2 de ancho
        corazon.moveTo(0, 0.9f);
        corazon.cubicTo(-1.3f, 0f, -0.9f, -1f, 0f, -0.35f);
        corazon.cubicTo(0.9f, -1f, 1.3f, 0f, 0, 0.9f);
        corazon.close();
        // Pétalo unitario
        petalo.moveTo(0, -1f);
        petalo.cubicTo(0.9f, -0.6f, 0.8f, 0.7f, 0, 1f);
        petalo.cubicTo(-0.8f, 0.7f, -0.9f, -0.6f, 0, -1f);
        petalo.close();
    }

    private float rnd() { return azar.nextFloat(); }

    private float rango(float a, float b) { return a + (b - a) * azar.nextFloat(); }

    /** Emite {@code n} partículas en (x, y). {@code escala} = px por unidad (tamaño relativo). */
    public void emitir(Tipo t, float px, float py, int n, float escala) {
        float e = Math.max(0.5f, escala);
        for (int k = 0; k < n; k++) {
            int i = reservar();
            tipo[i] = (byte) t.ordinal();
            x[i] = px;
            y[i] = py;
            rot[i] = rnd() * 360f;
            vrot[i] = rango(-360f, 360f);
            fase[i] = rnd() * 6.283f;
            iniciar(i, t, e);
            vidaMax[i] = vida[i];
        }
    }

    private void iniciar(int i, Tipo t, float e) {
        float ang = rnd() * 6.283f;
        float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
        switch (t) {
            case ESCOMBRO:
                float v = rango(2f, 7f) * e * 14f;
                vx[i] = cs * v; vy[i] = -Math.abs(sn) * v - e * 20f;
                tam[i] = rango(0.8f, 2f) * e;
                vida[i] = rango(0.7f, 1.4f);
                color[i] = pick(0xFF6E6A66, 0xFF8A7B6A, 0xFF4F4B48, 0xFF9A9288);
                break;
            case CHISPA:
                float vc = rango(5f, 12f) * e * 14f;
                vx[i] = cs * vc; vy[i] = sn * vc;
                tam[i] = rango(0.8f, 1.6f) * e;
                vida[i] = rango(0.18f, 0.4f);
                color[i] = rnd() < 0.5f ? 0xFFFFE94A : 0xFF7DF3FF;
                break;
            case HUMO:
                vx[i] = rango(-1f, 1f) * e * 6f; vy[i] = -rango(1.5f, 3.5f) * e * 8f;
                tam[i] = rango(1.5f, 3f) * e;
                vida[i] = rango(1f, 1.9f);
                color[i] = pick(0xFF8A8A92, 0xFF6A6A72, 0xFFA5A5AD);
                break;
            case PETALO:
                vx[i] = rango(-1f, 1f) * e * 6f; vy[i] = rango(1f, 3f) * e * 6f;
                tam[i] = rango(0.8f, 1.4f) * e;
                vida[i] = rango(2.5f, 4f);
                color[i] = pick(0xFFFFB7C9, 0xFFFF9DB8, 0xFFFFD1DC);
                break;
            case ATOMICO:
                float va = rango(0.5f, 3.5f) * e * 8f;
                vx[i] = cs * va; vy[i] = sn * va - e * 8f;
                tam[i] = rango(0.6f, 1.3f) * e;
                vida[i] = rango(0.8f, 1.6f);
                color[i] = 0xFF3DE0FF;
                break;
            case MONEDA:
                vx[i] = rango(-2.5f, 2.5f) * e * 10f; vy[i] = -rango(7f, 12f) * e * 12f;
                tam[i] = rango(1.2f, 1.8f) * e;
                vida[i] = rango(1.1f, 1.6f);
                color[i] = 0xFFFFC82E;
                break;
            case CORAZON:
                vx[i] = rango(-1.2f, 1.2f) * e * 6f; vy[i] = -rango(2f, 4f) * e * 8f;
                tam[i] = rango(1f, 1.8f) * e;
                vida[i] = rango(1.2f, 2f);
                color[i] = pick(0xFFFF5C8A, 0xFFFF86A8, 0xFFFF3D71);
                break;
            case BURBUJA:
                vx[i] = rango(-0.5f, 0.5f) * e * 4f; vy[i] = -rango(2f, 4.5f) * e * 8f;
                tam[i] = rango(0.8f, 2.2f) * e;
                vida[i] = rango(1.5f, 2.8f);
                color[i] = 0xFFFFFFFF;
                break;
            case NIEVE:
                vx[i] = rango(-1f, 1f) * e * 5f; vy[i] = rango(2f, 4f) * e * 8f;
                tam[i] = rango(0.5f, 1.2f) * e;
                vida[i] = rango(2.5f, 4.5f);
                color[i] = 0xFFFFFFFF;
                break;
            case LLUVIA:
                vx[i] = -e * 18f; vy[i] = rango(28f, 40f) * e * 3f;
                tam[i] = rango(0.8f, 1.4f) * e;
                vida[i] = rango(0.5f, 0.9f);
                color[i] = 0xFFB6D4F2;
                break;
            default: // VAPOR
                vx[i] = rango(-0.8f, 0.8f) * e * 5f; vy[i] = -rango(2f, 4f) * e * 8f;
                tam[i] = rango(1.2f, 2.4f) * e;
                vida[i] = rango(0.9f, 1.6f);
                color[i] = 0xFFFFFFFF;
                break;
        }
    }

    private int pick(int a, int b, int c) {
        float r = rnd();
        return r < 0.33f ? a : (r < 0.66f ? b : c);
    }

    private int pick(int a, int b, int c, int d) {
        return rnd() < 0.5f ? pick(a, b, c) : d;
    }

    /** Busca un hueco libre (si el pool está lleno, recicla el más antiguo del cursor). */
    private int reservar() {
        for (int k = 0; k < MAX; k++) {
            int i = (cursor + k) % MAX;
            if (!vivo[i]) {
                cursor = (i + 1) % MAX;
                vivo[i] = true;
                cuenta++;
                return i;
            }
        }
        int i = cursor;
        cursor = (cursor + 1) % MAX;
        return i;
    }

    /** Texto flotante que sube y se desvanece (p. ej. "+10 🪙"). */
    public void texto(String s, float px, float py, int col, float tamPx) {
        int i = tCursor;
        tCursor = (tCursor + 1) % MAX_TEXTOS;
        tVivo[i] = true;
        tTexto[i] = s;
        tx[i] = px;
        ty[i] = py;
        tColor[i] = col;
        tTam[i] = tamPx;
        tVida[i] = DUR_TEXTO;
    }

    /** Avanza la simulación dt segundos y dibuja. */
    public void dibujar(Canvas c, float dtIn) {
        float dt = Math.max(0f, Math.min(DT_MAX, dtIn));
        for (int i = 0; i < MAX; i++) {
            if (!vivo[i]) continue;
            vida[i] -= dt;
            if (vida[i] <= 0f) {
                vivo[i] = false;
                cuenta--;
                continue;
            }
            actualizar(i, dt);
            dibujarUna(c, i);
        }
        dibujarTextos(c, dt);
    }

    private void actualizar(int i, float dt) {
        Tipo t = TIPOS[tipo[i]];
        float edad = vidaMax[i] - vida[i];
        switch (t) {
            case ESCOMBRO:
                vy[i] += 900f * dt;
                break;
            case MONEDA:
                vy[i] += 1100f * dt;
                break;
            case PETALO:
                vx[i] += (float) Math.sin(edad * 3f + fase[i]) * 60f * dt;
                break;
            case BURBUJA:
                x[i] += (float) Math.sin(edad * 5f + fase[i]) * tam[i] * 0.6f * dt * 10f;
                break;
            case NIEVE:
                x[i] += (float) Math.sin(edad * 2f + fase[i]) * 12f * dt;
                break;
            case HUMO:
            case VAPOR:
                x[i] += (float) Math.sin(edad * 2.5f + fase[i]) * 10f * dt;
                break;
            case CORAZON:
                x[i] += (float) Math.sin(edad * 4f + fase[i]) * 14f * dt;
                break;
            case ATOMICO:
                vy[i] -= 20f * dt;
                break;
            default:
                break;
        }
        x[i] += vx[i] * dt;
        y[i] += vy[i] * dt;
        rot[i] += vrot[i] * dt;
    }

    private void dibujarUna(Canvas c, int i) {
        Tipo t = TIPOS[tipo[i]];
        float f = vida[i] / vidaMax[i]; // 1 -> 0
        float s = tam[i];
        float e = s;
        p.setStyle(Paint.Style.FILL);
        switch (t) {
            case ESCOMBRO:
                p.setColor(conAlfa(color[i], Math.min(1f, f * 2f)));
                c.save();
                c.translate(x[i], y[i]);
                c.rotate(rot[i]);
                c.drawRect(-s * 1.2f, -s, s * 1.2f, s, p);
                c.restore();
                break;
            case CHISPA:
                p.setColor(conAlfa(color[i], f));
                p.setStrokeWidth(Math.max(1.5f, s * 0.8f));
                p.setStrokeCap(Paint.Cap.ROUND);
                float vl = (float) Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]) + 0.001f;
                float len = s * 3f;
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(x[i], y[i], x[i] - vx[i] / vl * len, y[i] - vy[i] / vl * len, p);
                p.setStyle(Paint.Style.FILL);
                break;
            case HUMO:
                p.setColor(conAlfa(color[i], f * 0.45f));
                c.drawCircle(x[i], y[i], s * (2.2f - f) * 1.6f, p);
                break;
            case VAPOR:
                p.setColor(conAlfa(color[i], f * 0.3f));
                c.drawCircle(x[i], y[i], s * (2f - f) * 1.4f, p);
                break;
            case PETALO:
                p.setColor(conAlfa(color[i], Math.min(1f, f * 3f)));
                c.save();
                c.translate(x[i], y[i]);
                c.rotate(rot[i] * 0.5f);
                c.scale(e * 3f, e * 3f * (0.4f + 0.6f * Math.abs((float) Math.sin(rot[i] * 0.03f))));
                c.drawPath(petalo, p);
                c.restore();
                break;
            case ATOMICO:
                p.setColor(conAlfa(0xFF3DE0FF, f * 0.25f));
                c.drawCircle(x[i], y[i], s * 3.2f, p);
                p.setColor(conAlfa(0xFFB8F6FF, f));
                c.drawCircle(x[i], y[i], s * 1.1f, p);
                break;
            case MONEDA:
                float giro = Math.abs((float) Math.cos(rot[i] * 0.03f));
                c.save();
                c.translate(x[i], y[i]);
                c.scale(Math.max(0.15f, giro), 1f);
                p.setColor(conAlfa(0xFFB07A00, Math.min(1f, f * 3f)));
                c.drawCircle(0, 0, s * 3f, p);
                p.setColor(conAlfa(0xFFFFD34A, Math.min(1f, f * 3f)));
                c.drawCircle(0, 0, s * 2.4f, p);
                p.setColor(conAlfa(0xFFFFF3B0, Math.min(1f, f * 3f)));
                c.drawCircle(-s * 0.8f, -s * 0.8f, s * 0.7f, p);
                c.restore();
                break;
            case CORAZON:
                p.setColor(conAlfa(color[i], Math.min(1f, f * 2.5f)));
                c.save();
                c.translate(x[i], y[i]);
                c.scale(s * 4f, s * 4f);
                c.drawPath(corazon, p);
                c.restore();
                break;
            case BURBUJA:
                float a = Math.min(1f, f * 2.5f);
                p.setColor(conAlfa(0xFFFFFFFF, a * 0.15f));
                c.drawCircle(x[i], y[i], s * 3f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.5f, s * 0.5f));
                p.setColor(conAlfa(0xFFFFFFFF, a * 0.9f));
                c.drawCircle(x[i], y[i], s * 3f, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(conAlfa(0xFFFFFFFF, a));
                c.drawCircle(x[i] - s, y[i] - s, s * 0.6f, p);
                break;
            case NIEVE:
                p.setColor(conAlfa(0xFFFFFFFF, Math.min(1f, f * 3f) * 0.9f));
                c.drawCircle(x[i], y[i], s * 2f, p);
                break;
            case LLUVIA:
                p.setColor(conAlfa(0xFFB6D4F2, Math.min(1f, f * 3f) * 0.8f));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.5f, s));
                c.drawLine(x[i], y[i], x[i] + vx[i] * 0.03f, y[i] - vy[i] * 0.03f, p);
                p.setStyle(Paint.Style.FILL);
                break;
            default:
                break;
        }
    }

    private void dibujarTextos(Canvas c, float dt) {
        for (int i = 0; i < MAX_TEXTOS; i++) {
            if (!tVivo[i]) continue;
            tVida[i] -= dt;
            if (tVida[i] <= 0f) {
                tVivo[i] = false;
                continue;
            }
            float edad = DUR_TEXTO - tVida[i];
            ty[i] -= (60f + 40f * (1f - edad / DUR_TEXTO)) * dt;
            float f = Math.min(1f, tVida[i] / 0.4f);
            float pop = edad < 0.15f ? 0.7f + edad / 0.15f * 0.45f : 1.15f - Math.min(0.15f, (edad - 0.15f));
            pt.setTextSize(tTam[i] * pop);
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeWidth(Math.max(3f, tTam[i] * 0.18f));
            pt.setStrokeJoin(Paint.Join.ROUND);
            pt.setColor(conAlfa(0xFF070A16, f));
            c.drawText(tTexto[i], tx[i], ty[i], pt);
            pt.setStyle(Paint.Style.FILL);
            pt.setColor(conAlfa(tColor[i], f));
            c.drawText(tTexto[i], tx[i], ty[i], pt);
        }
    }

    private static int conAlfa(int col, float f) {
        int a = (int) (Color.alpha(col) * Math.max(0f, Math.min(1f, f)));
        return (col & 0x00FFFFFF) | (a << 24);
    }

    public boolean activas() {
        if (cuenta > 0) return true;
        for (int i = 0; i < MAX_TEXTOS; i++) if (tVivo[i]) return true;
        return false;
    }

    public void limpiar() {
        for (int i = 0; i < MAX; i++) vivo[i] = false;
        for (int i = 0; i < MAX_TEXTOS; i++) tVivo[i] = false;
        cuenta = 0;
    }
}
