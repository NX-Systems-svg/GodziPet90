package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.RutaEvolucion;

/**
 * Pantalla de título animada: sol naciente con rayos giratorios, horizonte de Tokio con la
 * torre, relámpagos, silueta de Godzilla con halo y ojos brillantes, y el título ゴジラ.
 */
public class TituloView extends View {
    public interface AlTocarLogo { void tocado(); }

    private static final String TITULO = "ゴジラ";
    private static final String SUBTITULO = "GODZI-PET 90";
    private static final String TOCA = "TOCA PARA COMENZAR";
    private static final int EDIFICIOS = 18;
    private static final int RAYOS = 18;
    private static final int PUNTOS_RAYO = 12;
    private static final int COL_TINTA = 0xFF070A16;
    private static final int COL_ROJO = 0xFFE01B2F;
    private static final int COL_ATOMICO = 0xFF3DE0FF;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCapa = new Paint();
    private final Paint pOscuro = new Paint();
    private final Paint pHalo = new Paint();
    private final Path ruta = new Path();
    private final Path torre = new Path();
    private final RectF rectTitulo = new RectF();
    private final RectF capa = new RectF();
    private final Rect tmp = new Rect();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final Apariencia apariencia = new Apariencia(Etapa.FINAL, RutaEvolucion.HEISEI,
            OrigenHuevo.ISLA_ODO, EstadoAnimo.NORMAL, 0, 90f);
    private final java.util.Random azar = new java.util.Random(90);

    private final float[] edX = new float[EDIFICIOS], edW = new float[EDIFICIOS], edH = new float[EDIFICIOS];
    private final float[] bolt = new float[PUNTOS_RAYO * 2];

    private Shader cielo, resplandor;
    private float w, h;
    private float tamTitulo;

    private boolean adjunta;
    private long inicioNs;
    private float proxRayo = 2.5f;
    private float tRayo = -10f;

    private AlTocarLogo alTocar;

    public TituloView(Context c) { this(c, null); }

    public TituloView(Context c, AttributeSet a) {
        super(c, a);
        pOscuro.setColorFilter(null);
        pOscuro.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        pOscuro.setColor(0xFF04020A);
        pHalo.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        pTexto.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        pTexto.setTextAlign(Paint.Align.CENTER);
        pTexto.setStrokeJoin(Paint.Join.ROUND);
        setClickable(true);
    }

    /** Se llama cada vez que el jugador toca el título ゴジラ (7 toques → modo profesor). */
    public void setAlTocarLogo(AlTocarLogo l) { alTocar = l; }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        adjunta = true;
        inicioNs = System.nanoTime();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        adjunta = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        super.onSizeChanged(nw, nh, ow, oh);
        w = nw;
        h = nh;
        cielo = new LinearGradient(0, 0, 0, h, new int[] {0xFF05060F, 0xFF2A0A1E, 0xFF7A1226, 0xFFC4262F},
                new float[] {0f, 0.45f, 0.78f, 1f}, Shader.TileMode.CLAMP);
        float r = w * 0.55f;
        resplandor = new RadialGradient(0, 0, r, new int[] {0xFFFF5A3C, 0x66E01B2F, 0x00E01B2F},
                new float[] {0.3f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        // Horizonte de Tokio
        java.util.Random rr = new java.util.Random(11);
        float x = -w * 0.02f;
        for (int i = 0; i < EDIFICIOS; i++) {
            edW[i] = w * (0.05f + rr.nextFloat() * 0.07f);
            edH[i] = h * (0.04f + rr.nextFloat() * 0.10f);
            edX[i] = x;
            x += edW[i] * 0.92f;
        }
        construirTorre();
    }

    private void construirTorre() {
        float cx = w * 0.84f, base = h * 0.80f, alto = h * 0.30f, ancho = w * 0.10f;
        torre.reset();
        torre.moveTo(cx - ancho / 2, base);
        torre.lineTo(cx - ancho * 0.28f, base - alto * 0.38f);
        torre.lineTo(cx - ancho * 0.30f, base - alto * 0.42f);
        torre.lineTo(cx + ancho * 0.30f, base - alto * 0.42f);
        torre.lineTo(cx + ancho * 0.28f, base - alto * 0.38f);
        torre.lineTo(cx + ancho / 2, base);
        torre.lineTo(cx + ancho * 0.12f, base);
        torre.lineTo(cx + ancho * 0.05f, base - alto * 0.30f);
        torre.lineTo(cx - ancho * 0.05f, base - alto * 0.30f);
        torre.lineTo(cx - ancho * 0.12f, base);
        torre.close();
        // Mástil superior y antena
        torre.moveTo(cx - ancho * 0.16f, base - alto * 0.42f);
        torre.lineTo(cx - ancho * 0.05f, base - alto * 0.80f);
        torre.lineTo(cx - ancho * 0.015f, base - alto);
        torre.lineTo(cx + ancho * 0.015f, base - alto);
        torre.lineTo(cx + ancho * 0.05f, base - alto * 0.80f);
        torre.lineTo(cx + ancho * 0.16f, base - alto * 0.42f);
        torre.close();
    }

    private static float easeOut(float x) {
        float v = Math.max(0f, Math.min(1f, x));
        return 1f - (1f - v) * (1f - v) * (1f - v);
    }

    private static float easeOutBack(float x) {
        float v = Math.max(0f, Math.min(1f, x));
        float c1 = 2.2f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(v - 1, 3) + c1 * (float) Math.pow(v - 1, 2);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (w <= 0) return;
        float t = (System.nanoTime() - inicioNs) / 1e9f;
        actualizarRayo(t);

        // Cielo
        p.setShader(cielo);
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        float sol = easeOut(t / 2.4f);
        float solY = h * (0.78f - 0.34f * sol);
        dibujarSol(c, t, solY);
        dibujarGodzilla(c, t);
        dibujarCiudad(c, t);
        dibujarRelampago(c, t);
        dibujarTitulos(c, t);

        if (adjunta) postInvalidateOnAnimation();
    }

    private void actualizarRayo(float t) {
        if (t < proxRayo) return;
        tRayo = t;
        proxRayo = t + 3f + azar.nextFloat() * 3f;
        float x = w * (0.15f + azar.nextFloat() * 0.7f), y = 0;
        float fin = h * (0.45f + azar.nextFloat() * 0.2f);
        for (int i = 0; i < PUNTOS_RAYO; i++) {
            bolt[i * 2] = x;
            bolt[i * 2 + 1] = y;
            x += (azar.nextFloat() - 0.5f) * w * 0.16f;
            y += fin / PUNTOS_RAYO;
        }
    }

    private void dibujarSol(Canvas c, float t, float solY) {
        float cx = w / 2f;
        float r = w * 0.30f;
        c.save();
        c.translate(cx, solY);
        // Rayos de póster giratorios
        float rot = t * 4f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x30FF3A4A);
        float largo = Math.max(w, h) * 1.4f;
        for (int i = 0; i < RAYOS; i++) {
            double a0 = Math.toRadians(rot + i * (360.0 / RAYOS));
            double a1 = a0 + Math.toRadians(360.0 / RAYOS / 2.0);
            ruta.reset();
            ruta.moveTo(0, 0);
            ruta.lineTo((float) Math.cos(a0) * largo, (float) Math.sin(a0) * largo);
            ruta.lineTo((float) Math.cos(a1) * largo, (float) Math.sin(a1) * largo);
            ruta.close();
            c.drawPath(ruta, p);
        }
        // Resplandor y disco
        p.setShader(resplandor);
        c.drawCircle(0, 0, w * 0.55f, p);
        p.setShader(null);
        float pulso = 1f + 0.015f * (float) Math.sin(t * 2f);
        p.setColor(COL_ROJO);
        c.drawCircle(0, 0, r * pulso, p);
        p.setColor(0x33FFFFFF);
        c.drawCircle(-r * 0.2f, -r * 0.25f, r * 0.55f, p);
        c.restore();
    }

    private void dibujarGodzilla(Canvas c, float t) {
        float subida = easeOut(t / 2.6f);
        float suelo = h * (1.15f - 0.33f * subida);
        float tam = w * 0.72f;
        float cx = w * 0.46f;
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 2.4f);
        capa.set(0, 0, w, h);
        kaiju.setCargaPlacas(pulso);

        // Halo atómico: silueta ligeramente mayor teñida de cian
        int alfaHalo = (int) ((55 + 75 * pulso) * subida);
        pHalo.setColor(Color.argb(alfaHalo, 61, 224, 255));
        int s1 = c.saveLayer(capa, pCapa);
        c.save();
        c.scale(1.035f, 1.035f, cx, suelo);
        kaiju.dibujar(c, cx, suelo, tam, apariencia, t, 0f, true);
        c.restore();
        c.drawRect(capa, pHalo);
        c.restoreToCount(s1);

        // Silueta oscura
        int s2 = c.saveLayer(capa, pCapa);
        kaiju.dibujar(c, cx, suelo, tam, apariencia, t, 0f, true);
        c.drawRect(capa, pOscuro);
        c.restoreToCount(s2);

        // Ojos brillantes
        float u = tam / 100f;
        float ex = cx + 31f * u, ey = suelo - 99f * u;
        float brillo = Math.min(1f, subida * 1.5f) * (0.75f + 0.25f * pulso);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb((int) (70 * brillo), 61, 224, 255));
        c.drawCircle(ex, ey, u * 6f, p);
        p.setColor(Color.argb((int) (255 * brillo), 190, 250, 255));
        c.drawCircle(ex, ey, u * 1.8f, p);
    }

    private void dibujarCiudad(Canvas c, float t) {
        float base = h * 0.80f;
        // Fila trasera (más clara por la bruma)
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < EDIFICIOS; i++) {
            p.setColor(0xFF1A0A1A);
            float x = edX[i] + edW[i] * 0.4f;
            c.drawRect(x, base - edH[i] * 1.5f, x + edW[i], h, p);
        }
        p.setColor(0xFF220C1C);
        c.drawPath(torre, p);
        // Fila delantera
        for (int i = 0; i < EDIFICIOS; i++) {
            p.setColor(0xFF05030A);
            c.drawRect(edX[i], base - edH[i], edX[i] + edW[i], h, p);
        }
        c.drawRect(0, base, w, h, p);
        // Ventanas encendidas parpadeantes
        for (int i = 0; i < EDIFICIOS; i++) {
            for (int k = 0; k < 6; k++) {
                int semilla = i * 31 + k * 17;
                if (((semilla + (int) (t * 0.7f)) % 5) < 2) continue;
                float wx = edX[i] + edW[i] * (0.15f + 0.22f * (semilla % 4));
                float wy = base - edH[i] + edH[i] * 0.12f * (1 + (k * 3 + i) % 6);
                if (wy > base) continue;
                p.setColor((semilla & 1) == 0 ? 0xFFFFD27A : 0xFFFFA64A);
                c.drawRect(wx, wy, wx + edW[i] * 0.1f, wy + edH[i] * 0.06f, p);
            }
        }
        // Luz roja de la torre
        float lat = (float) Math.sin(t * 3f) * 0.5f + 0.5f;
        p.setColor(Color.argb((int) (120 + 135 * lat), 255, 60, 60));
        c.drawCircle(w * 0.84f, base - h * 0.30f, w * 0.008f, p);
    }

    private void dibujarRelampago(Canvas c, float t) {
        float dt = t - tRayo;
        if (dt < 0f || dt > 0.5f) return;
        // Doble destello
        float f = dt < 0.1f ? 1f - dt / 0.1f : (dt < 0.18f ? 0f : (dt < 0.3f ? 0.7f * (1f - (dt - 0.18f) / 0.12f) : 0f));
        if (dt < 0.3f) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int) (140 * Math.max(f, 0.1f * (1f - dt / 0.3f))), 255, 255, 255));
            c.drawRect(0, 0, w, h, p);
            ruta.reset();
            ruta.moveTo(bolt[0], bolt[1]);
            for (int i = 1; i < PUNTOS_RAYO; i++) ruta.lineTo(bolt[i * 2], bolt[i * 2 + 1]);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(w * 0.018f);
            p.setColor(Color.argb((int) (120 * Math.max(f, 0.2f)), 160, 200, 255));
            c.drawPath(ruta, p);
            p.setStrokeWidth(w * 0.006f);
            p.setColor(Color.WHITE);
            c.drawPath(ruta, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private void dibujarTitulos(Canvas c, float t) {
        float tIn = (t - 0.5f) / 1.0f;
        float escala = easeOutBack(tIn);
        if (tIn <= 0f) escala = 0f;
        float cx = w / 2f, cy = h * 0.22f;

        // Ajusta el tamaño de texto para que el título ocupe ~78% del ancho
        if (tamTitulo == 0f) {
            pTexto.setTextSize(100f);
            tamTitulo = 100f * (w * 0.78f) / Math.max(1f, pTexto.measureText(TITULO));
        }
        pTexto.setTextSize(tamTitulo);
        pTexto.getTextBounds(TITULO, 0, TITULO.length(), tmp);
        float tw = pTexto.measureText(TITULO);
        float th = tmp.height();
        rectTitulo.set(cx - tw / 2f * escala, cy - th * 0.8f * escala, cx + tw / 2f * escala, cy + th * 0.5f * escala);

        if (escala > 0.01f) {
            c.save();
            c.translate(cx, cy);
            c.scale(escala, escala);
            float bal = (float) Math.sin(t * 1.6f) * 0.01f;
            c.rotate(bal * 57f);
            pTexto.setStyle(Paint.Style.STROKE);
            pTexto.setStrokeWidth(tamTitulo * 0.14f);
            pTexto.setColor(COL_TINTA);
            c.drawText(TITULO, 0, tmp.height() * 0.28f + 6f, pTexto);
            pTexto.setStrokeWidth(tamTitulo * 0.085f);
            c.drawText(TITULO, 0, 0, pTexto);
            pTexto.setStyle(Paint.Style.FILL);
            pTexto.setColor(COL_ROJO);
            c.drawText(TITULO, 0, 0, pTexto);
            pTexto.setColor(0x55FFFFFF);
            c.drawText(TITULO, -tamTitulo * 0.01f, -tamTitulo * 0.015f, pTexto);
            c.restore();
        }

        // Subtítulo
        float sIn = easeOut((t - 1.2f) / 0.7f);
        if (sIn > 0f) {
            float ts = w * 0.075f;
            pTexto.setTextSize(ts);
            pTexto.setLetterSpacing(0.18f);
            pTexto.setStyle(Paint.Style.STROKE);
            pTexto.setStrokeWidth(ts * 0.22f);
            pTexto.setColor(Color.argb((int) (255 * sIn), 7, 10, 22));
            float sy = cy + th * 0.5f * escala + ts * 1.6f;
            c.drawText(SUBTITULO, cx, sy, pTexto);
            pTexto.setStyle(Paint.Style.FILL);
            pTexto.setColor(Color.argb((int) (255 * sIn), 255, 210, 63));
            c.drawText(SUBTITULO, cx, sy, pTexto);
            pTexto.setLetterSpacing(0f);
        }

        // "TOCA PARA COMENZAR" parpadeante
        float bIn = (t - 2.4f);
        if (bIn > 0f) {
            float parp = 0.55f + 0.45f * (float) Math.sin(t * 4f);
            float tt = w * 0.05f;
            pTexto.setTextSize(tt);
            pTexto.setLetterSpacing(0.1f);
            pTexto.setStyle(Paint.Style.STROKE);
            pTexto.setStrokeWidth(tt * 0.25f);
            pTexto.setColor(Color.argb((int) (255 * parp), 7, 10, 22));
            float by = h - Math.max(h * 0.06f, tt * 2f);
            c.drawText(TOCA, cx, by, pTexto);
            pTexto.setStyle(Paint.Style.FILL);
            pTexto.setColor(Color.argb((int) (255 * parp), 243, 246, 255));
            c.drawText(TOCA, cx, by, pTexto);
            pTexto.setLetterSpacing(0f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) return true;
        if (a == MotionEvent.ACTION_UP) {
            if (rectTitulo.contains(e.getX(), e.getY())) {
                if (alTocar != null) alTocar.tocado();
            } else {
                performClick();
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
