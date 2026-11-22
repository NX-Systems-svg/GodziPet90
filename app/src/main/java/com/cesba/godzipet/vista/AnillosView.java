package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;

import java.util.Random;

/**
 * Minijuego "Anillos de Humo": homenaje a Minilla (El Hijo de Godzilla, 1967), que soplaba anillos de humo
 * en lugar de aliento atómico. Toca para lanzar un anillo contra los insectos gigantes de la isla.
 * Sin asignaciones de memoria en onDraw: todas las entidades viven en arreglos fijos.
 */
public class AnillosView extends View {
    public static final int DURACION_MS = 30_000;
    public static final int META_ACIERTOS = 10;
    public static final int PUNTOS_POR_ACIERTO = 10;

    public interface Oyente {
        void progreso(int aciertos, int fallos, int segundosRestantes);
        void fin(int aciertos, int puntaje);
        void acierto();
    }

    private static final int MAX_INSECTOS = 7;
    private static final int MAX_PUFFS = 24;
    private static final float ENFRIAMIENTO = 0.35f;
    private static final long PROGRESO_CADA_MS = 500;

    // estados de insecto
    private static final int LIBRE = 0, VIVO = 1, CAYENDO = 2;
    private static final int MANTIS = 0, ESCARABAJO = 1;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final Random azar = new Random();

    // insectos
    private final float[] ix = new float[MAX_INSECTOS], iy = new float[MAX_INSECTOS];
    private final float[] iBase = new float[MAX_INSECTOS], iAmp = new float[MAX_INSECTOS];
    private final float[] iVx = new float[MAX_INSECTOS], iFase = new float[MAX_INSECTOS];
    private final float[] iFreq = new float[MAX_INSECTOS], iTam = new float[MAX_INSECTOS];
    private final float[] iGiro = new float[MAX_INSECTOS], iEdad = new float[MAX_INSECTOS];
    private final int[] iTipo = new int[MAX_INSECTOS], iEstado = new int[MAX_INSECTOS];

    // anillo en vuelo
    private boolean anilloActivo;
    private float aX, aY, aDx, aDy, aDestX, aDestY, aEdad, aRadio;
    private float enfriamiento;

    // puffs y "+1"
    private final float[] pfX = new float[MAX_PUFFS], pfY = new float[MAX_PUFFS], pfEdad = new float[MAX_PUFFS];
    private final float[] pfTam = new float[MAX_PUFFS];
    private final int[] pfTipo = new int[MAX_PUFFS]; // 0 libre, 1 humo, 2 "+1"

    private Apariencia apariencia;
    private Oyente oyente;
    private boolean jugando;
    private long inicio, ultimoCuadro, ultimoProgreso;
    private float tSpawn, mandibulaHasta;
    private int aciertos, fallos;
    private boolean cambio;
    private float tiempoJuego;

    // geometría dependiente del tamaño
    private float w, h, horizonte, suelo, tamKaiju, bocaX, bocaY, kaijuX;
    private LinearGradient cielo, mar, arena;

    public AnillosView(Context c) { this(c, null); }

    public AnillosView(Context c, AttributeSet a) {
        super(c, a);
        setContentDescription("Área de juego: toca para lanzar anillos de humo a los insectos");
    }

    public void setApariencia(Apariencia a) {
        apariencia = a == null ? null : new Apariencia(Etapa.PEQUENO, a.ruta, a.origen, EstadoAnimo.NORMAL,
                a.toquesHuevo, a.radiacion);
        recalcularBoca();
        invalidate();
    }

    public void setOyente(Oyente o) { oyente = o; }
    public boolean isJugando() { return jugando; }

    public void iniciar() {
        for (int i = 0; i < MAX_INSECTOS; i++) iEstado[i] = LIBRE;
        for (int i = 0; i < MAX_PUFFS; i++) pfTipo[i] = 0;
        anilloActivo = false;
        enfriamiento = 0;
        aciertos = 0;
        fallos = 0;
        tSpawn = 0.4f;
        tiempoJuego = 0;
        inicio = SystemClock.uptimeMillis();
        ultimoCuadro = inicio;
        ultimoProgreso = inicio;
        jugando = true;
        cambio = true;
        invalidate();
    }

    public void detener() { jugando = false; }

    // ---------------------------------------------------------------- geometría

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        super.onSizeChanged(nw, nh, ow, oh);
        w = nw;
        h = nh;
        horizonte = h * 0.58f;
        suelo = h * 0.97f;
        kaijuX = w * 0.15f;
        tamKaiju = h * 0.24f / 0.64f;
        cielo = new LinearGradient(0, 0, 0, horizonte, new int[]{0xFF2A1B52, 0xFF8E3F7E, 0xFFFF7E4A, 0xFFFFC46B},
                new float[]{0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP);
        mar = new LinearGradient(0, horizonte, 0, h * 0.84f, 0xFFE0703A, 0xFF16406E, Shader.TileMode.CLAMP);
        arena = new LinearGradient(0, h * 0.8f, 0, h, 0xFFF0CE92, 0xFFB98A55, Shader.TileMode.CLAMP);
        recalcularBoca();
    }

    private void recalcularBoca() {
        if (apariencia == null || w == 0) return;
        PointF b = KaijuPainter.boca(kaijuX, suelo, tamKaiju, apariencia);
        bocaX = b.x;
        bocaY = b.y;
    }

    // ---------------------------------------------------------------- entrada

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!jugando || e.getAction() != MotionEvent.ACTION_DOWN) return true;
        if (anilloActivo || enfriamiento > 0) return true;
        float dx = e.getX() - bocaX, dy = e.getY() - bocaY;
        float d = (float) Math.hypot(dx, dy);
        if (d < 1f) return true;
        float vel = w * 1.15f;
        aX = bocaX;
        aY = bocaY;
        aDx = dx / d * vel;
        aDy = dy / d * vel;
        aDestX = e.getX();
        aDestY = e.getY();
        aEdad = 0;
        aRadio = w * 0.022f;
        anilloActivo = true;
        enfriamiento = ENFRIAMIENTO;
        mandibulaHasta = tiempoJuego + 0.3f;
        return true;
    }

    // ---------------------------------------------------------------- lógica

    private int restantes(long ahora) {
        return (int) Math.max(0, (DURACION_MS - (ahora - inicio) + 999) / 1000);
    }

    private void avisarProgreso(long ahora) {
        if (oyente != null) oyente.progreso(aciertos, fallos, restantes(ahora));
        ultimoProgreso = ahora;
        cambio = false;
    }

    private void actualizar(long ahora) {
        float dt = Math.min(0.05f, (ahora - ultimoCuadro) / 1000f);
        ultimoCuadro = ahora;
        long transcurrido = ahora - inicio;
        tiempoJuego = transcurrido / 1000f;
        if (transcurrido >= DURACION_MS) {
            jugando = false;
            avisarProgreso(ahora);
            if (oyente != null) oyente.fin(aciertos, aciertos * PUNTOS_POR_ACIERTO);
            return;
        }
        float dificultad = transcurrido / (float) DURACION_MS;
        if (enfriamiento > 0) enfriamiento -= dt;
        generar(dt, dificultad);
        moverInsectos(dt);
        moverAnillo(dt);
        envejecerPuffs(dt);
        if (cambio || ahora - ultimoProgreso >= PROGRESO_CADA_MS) avisarProgreso(ahora);
    }

    private void generar(float dt, float dificultad) {
        tSpawn -= dt;
        if (tSpawn > 0) return;
        int vivos = 0, libre = -1;
        for (int i = 0; i < MAX_INSECTOS; i++) {
            if (iEstado[i] == LIBRE) { if (libre < 0) libre = i; } else if (iEstado[i] == VIVO) vivos++;
        }
        int tope = 3 + (int) (dificultad * 3.5f);
        tSpawn = 1.15f - 0.65f * dificultad + azar.nextFloat() * 0.35f;
        if (libre < 0 || vivos >= tope) return;
        int i = libre;
        iEstado[i] = VIVO;
        iTipo[i] = azar.nextFloat() < 0.55f ? MANTIS : ESCARABAJO;
        iTam[i] = w * (iTipo[i] == MANTIS ? 0.05f : 0.062f);
        iBase[i] = h * (0.14f + azar.nextFloat() * 0.34f);
        iAmp[i] = h * (0.025f + azar.nextFloat() * 0.06f);
        float rapidez = (0.11f + azar.nextFloat() * 0.1f) * (1f + 0.9f * dificultad);
        iVx[i] = -w * rapidez * (iTipo[i] == ESCARABAJO ? 0.8f : 1f);
        iFase[i] = azar.nextFloat() * 6.28f;
        iFreq[i] = 1.6f + azar.nextFloat() * 1.8f;
        ix[i] = w + iTam[i] * 2;
        iy[i] = iBase[i];
        iGiro[i] = 0;
        iEdad[i] = 0;
    }

    private void moverInsectos(float dt) {
        float salida = w * 0.3f - w * 0.1f;
        for (int i = 0; i < MAX_INSECTOS; i++) {
            if (iEstado[i] == VIVO) {
                ix[i] += iVx[i] * dt;
                iFase[i] += iFreq[i] * dt;
                iy[i] = iBase[i] + iAmp[i] * (float) Math.sin(iFase[i]);
                if (ix[i] < salida) {
                    iEstado[i] = LIBRE;
                    fallos++;
                    cambio = true;
                }
            } else if (iEstado[i] == CAYENDO) {
                iEdad[i] += dt;
                iGiro[i] += 720f * dt;
                iy[i] += h * (0.25f + iEdad[i] * 0.9f) * dt;
                ix[i] += iVx[i] * 0.2f * dt;
                if (iEdad[i] > 0.9f || iy[i] > horizonte + h * 0.1f) iEstado[i] = LIBRE;
            }
        }
    }

    private void moverAnillo(float dt) {
        if (!anilloActivo) return;
        aX += aDx * dt;
        aY += aDy * dt;
        aEdad += dt;
        aRadio = w * 0.022f + w * 0.05f * Math.min(1f, aEdad / 0.7f);
        for (int i = 0; i < MAX_INSECTOS; i++) {
            if (iEstado[i] != VIVO) continue;
            if (Math.hypot(aX - ix[i], aY - iy[i]) < aRadio + iTam[i] * 0.75f) {
                derribar(i);
                anilloActivo = false;
                nuevoPuff(aX, aY, 1, w * 0.07f);
                return;
            }
        }
        // llegó al punto tocado sin acertar: se disuelve
        float restoX = aDestX - aX, restoY = aDestY - aY;
        if (restoX * aDx + restoY * aDy <= 0) {
            anilloActivo = false;
            nuevoPuff(aX, aY, 1, w * 0.06f);
        }
    }

    private void derribar(int i) {
        iEstado[i] = CAYENDO;
        iEdad[i] = 0;
        aciertos++;
        cambio = true;
        nuevoPuff(ix[i], iy[i], 1, w * 0.09f);
        nuevoPuff(ix[i], iy[i] - iTam[i], 2, w * 0.045f);
        if (oyente != null) oyente.acierto();
    }

    private void nuevoPuff(float x, float y, int tipo, float tam) {
        int k = -1;
        for (int i = 0; i < MAX_PUFFS; i++) {
            if (pfTipo[i] == 0) { k = i; break; }
        }
        if (k < 0) return;
        pfX[k] = x;
        pfY[k] = y;
        pfTam[k] = tam;
        pfEdad[k] = 0;
        pfTipo[k] = tipo;
    }

    private void envejecerPuffs(float dt) {
        for (int i = 0; i < MAX_PUFFS; i++) {
            if (pfTipo[i] == 0) continue;
            pfEdad[i] += dt;
            if (pfEdad[i] > (pfTipo[i] == 2 ? 0.9f : 0.6f)) pfTipo[i] = 0;
        }
    }

    // ---------------------------------------------------------------- dibujo

    @Override
    protected void onDraw(Canvas c) {
        if (w == 0) return;
        long ahora = SystemClock.uptimeMillis();
        if (jugando) actualizar(ahora);
        float t = ahora / 1000f;
        dibujarEscena(c, t);
        for (int i = 0; i < MAX_INSECTOS; i++) {
            if (iEstado[i] != LIBRE) dibujarInsecto(c, i, t);
        }
        dibujarPalmas(c, t);
        dibujarKaiju(c, t);
        if (anilloActivo) dibujarAnillo(c, t);
        dibujarPuffs(c);
        if (jugando) postInvalidateOnAnimation();
    }

    private void dibujarKaiju(Canvas c, float t) {
        if (apariencia == null) return;
        float mand = tiempoJuego < mandibulaHasta && jugando ? 0.9f : 0f;
        kaiju.dibujar(c, kaijuX, suelo, tamKaiju, apariencia, t, mand, false);
    }

    private void dibujarEscena(Canvas c, float t) {
        // cielo del atardecer
        p.setStyle(Paint.Style.FILL);
        p.setShader(cielo);
        c.drawRect(0, 0, w, horizonte + 1, p);
        p.setShader(null);
        // nubes rosadas a la deriva
        p.setColor(0x55FFB0A0);
        for (int i = 0; i < 3; i++) {
            float cx = ((t * (6 + i * 3) + i * w * 0.4f) % (w * 1.4f)) - w * 0.2f;
            float cy = h * (0.1f + i * 0.09f);
            c.drawOval(cx - w * 0.14f, cy - h * 0.018f, cx + w * 0.14f, cy + h * 0.018f, p);
        }
        // sol sobre el horizonte
        float sx = w * 0.3f;
        p.setColor(0x33FFD27A);
        c.drawCircle(sx, horizonte, w * 0.2f, p);
        p.setColor(0x55FFE09A);
        c.drawCircle(sx, horizonte, w * 0.13f, p);
        p.setColor(0xFFFFF0B0);
        c.drawCircle(sx, horizonte, w * 0.085f, p);
        dibujarVolcan(c, t);
        // mar
        p.setShader(mar);
        c.drawRect(0, horizonte, w, h * 0.84f, p);
        p.setShader(null);
        // reflejo del sol y destellos
        for (int i = 0; i < 7; i++) {
            float y = horizonte + h * 0.012f + i * h * 0.03f;
            float ancho = w * (0.1f + 0.012f * i) * (0.8f + 0.2f * (float) Math.sin(t * 2 + i));
            p.setColor(Color_alfa(0xFFFFD27A, 190 - i * 20));
            c.drawRect(sx - ancho, y, sx + ancho, y + h * 0.008f, p);
        }
        // olas
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, h * 0.004f));
        p.setColor(0x44FFFFFF);
        for (int f = 0; f < 3; f++) {
            float y = h * (0.67f + f * 0.055f);
            path.reset();
            path.moveTo(0, y);
            for (int s = 1; s <= 24; s++) {
                float x = w * s / 24f;
                path.lineTo(x, y + (float) Math.sin(t * 1.6f + s * 0.9f + f) * h * 0.008f);
            }
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
        // playa con espuma
        p.setShader(arena);
        c.drawRect(0, h * 0.8f, w, h, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(h * 0.012f);
        p.setColor(0xCCFFFFFF);
        path.reset();
        float yo = h * 0.8f + (float) Math.sin(t * 1.2f) * h * 0.006f;
        path.moveTo(0, yo);
        for (int s = 1; s <= 20; s++) {
            path.lineTo(w * s / 20f, yo + (float) Math.sin(t * 1.2f + s * 0.7f) * h * 0.008f);
        }
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static int Color_alfa(int color, int alfa) {
        return (Math.max(0, Math.min(255, alfa)) << 24) | (color & 0xFFFFFF);
    }

    private void dibujarVolcan(Canvas c, float t) {
        float cx = w * 0.8f, ancho = w * 0.2f, alto = h * 0.2f, cima = horizonte - alto;
        path.reset();
        path.moveTo(cx - ancho * 1.4f, horizonte);
        path.lineTo(cx - ancho * 0.16f, cima);
        path.lineTo(cx + ancho * 0.16f, cima);
        path.lineTo(cx + ancho * 1.4f, horizonte);
        path.close();
        p.setColor(0xFF4A2142);
        c.drawPath(path, p);
        p.setColor(0xFF6A2E4A);
        c.drawOval(cx - ancho * 0.18f, cima - h * 0.008f, cx + ancho * 0.18f, cima + h * 0.012f, p);
        p.setColor(0xCCFF7A2E);
        c.drawOval(cx - ancho * 0.12f, cima - h * 0.004f, cx + ancho * 0.12f, cima + h * 0.008f, p);
        // humo que sube y se ensancha
        for (int i = 0; i < 6; i++) {
            float f = (t * 0.07f + i / 6f) % 1f;
            float px = cx + (float) Math.sin(t * 0.5f + i * 2f) * ancho * 0.12f + f * ancho * 0.5f;
            float py = cima - f * h * 0.26f;
            p.setColor(Color_alfa(0xFF6E5A6E, (int) (150 * (1 - f))));
            c.drawCircle(px, py, w * 0.018f + f * w * 0.05f, p);
        }
    }

    private void dibujarPalmas(Canvas c, float t) {
        palma(c, w * 0.93f, h * 0.9f, h * 0.5f, -0.18f, t, 0f);
        palma(c, w * 0.04f, h * 0.88f, h * 0.36f, 0.22f, t, 1.7f);
    }

    /** Palma en silueta: tronco curvo y frondas que se mecen con la brisa. */
    private void palma(Canvas c, float x, float baseY, float alto, float inclina, float t, float fase) {
        float topX = x + inclina * alto, topY = baseY - alto;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(alto * 0.055f);
        p.setColor(0xFF2B1D22);
        path.reset();
        path.moveTo(x, baseY);
        path.quadTo(x - inclina * alto * 0.2f, baseY - alto * 0.55f, topX, topY);
        c.drawPath(path, p);
        p.setStrokeWidth(alto * 0.035f);
        p.setColor(0xFF1B3A2E);
        float brisa = (float) Math.sin(t * 1.3f + fase);
        for (int i = 0; i < 7; i++) {
            float ang = (float) Math.toRadians(-170 + i * 28 + brisa * 4);
            float largo = alto * (0.38f + 0.06f * (i % 2));
            float ex = topX + (float) Math.cos(ang) * largo;
            float ey = topY + (float) Math.sin(ang) * largo * 0.5f + largo * 0.45f;
            path.reset();
            path.moveTo(topX, topY);
            path.quadTo(topX + (ex - topX) * 0.5f, topY - alto * 0.12f + (float) Math.sin(ang) * largo * 0.4f, ex, ey);
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF2B1D22);
        c.drawCircle(topX, topY + alto * 0.03f, alto * 0.03f, p);
        c.drawCircle(topX + alto * 0.04f, topY + alto * 0.05f, alto * 0.028f, p);
    }

    private void dibujarInsecto(Canvas c, int i, float t) {
        float r = iTam[i];
        float alfa = iEstado[i] == CAYENDO ? 1f - iEdad[i] / 0.9f : 1f;
        float aleteo = (float) Math.sin(t * 38 + i * 1.7f);
        float inclina = iEstado[i] == VIVO ? (float) Math.cos(iFase[i]) * 14f : iGiro[i];
        c.save();
        c.translate(ix[i], iy[i]);
        c.rotate(inclina);
        p.setStyle(Paint.Style.FILL);
        if (iTipo[i] == MANTIS) dibujarMantis(c, r, aleteo, alfa);
        else dibujarEscarabajo(c, r, aleteo, alfa);
        c.restore();
    }

    /** Mantis verde original: cuerpo alargado, cabeza triangular y garras segadoras. Mira a la izquierda. */
    private void dibujarMantis(Canvas c, float r, float aleteo, float alfa) {
        int a = (int) (255 * alfa);
        // alas
        p.setColor(Color_alfa(0xFFD8FFE0, (int) (110 * alfa)));
        c.drawOval(-r * 0.2f, -r * (0.5f + 0.5f * aleteo), r * 1.7f, r * 0.05f, p);
        c.drawOval(-r * 0.2f, -r * 0.05f * aleteo, r * 1.5f, r * 0.55f * (0.6f + 0.4f * aleteo), p);
        // abdomen y tórax
        p.setColor(Color_alfa(0xFF4FA83A, a));
        c.drawOval(-r * 0.1f, -r * 0.28f, r * 1.5f, r * 0.28f, p);
        c.drawOval(-r * 0.75f, -r * 0.22f, r * 0.2f, r * 0.22f, p);
        // cabeza
        p.setColor(Color_alfa(0xFF74D04E, a));
        path.reset();
        path.moveTo(-r * 0.7f, -r * 0.3f);
        path.lineTo(-r * 1.15f, 0);
        path.lineTo(-r * 0.7f, r * 0.3f);
        path.close();
        c.drawPath(path, p);
        p.setColor(Color_alfa(0xFFFFE066, a));
        c.drawCircle(-r * 0.85f, -r * 0.16f, r * 0.12f, p);
        p.setColor(Color_alfa(0xFF202020, a));
        c.drawCircle(-r * 0.88f, -r * 0.16f, r * 0.05f, p);
        // garras segadoras y patas
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.12f);
        p.setColor(Color_alfa(0xFF3C8A2E, a));
        path.reset();
        path.moveTo(-r * 0.55f, r * 0.1f);
        path.lineTo(-r * 1.05f, r * 0.6f);
        path.lineTo(-r * 0.7f, r * 0.15f);
        c.drawPath(path, p);
        p.setStrokeWidth(r * 0.07f);
        c.drawLine(-r * 0.3f, r * 0.2f, -r * 0.1f, r * 0.75f, p);
        c.drawLine(r * 0.3f, r * 0.2f, r * 0.55f, r * 0.8f, p);
        c.drawLine(-r * 1.0f, -r * 0.15f, -r * 1.35f, -r * 0.5f, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Escarabajo gigante original: élitros oscuros brillantes, cuernos y alas de membrana. */
    private void dibujarEscarabajo(Canvas c, float r, float aleteo, float alfa) {
        int a = (int) (255 * alfa);
        p.setColor(Color_alfa(0xFFE6D8FF, (int) (120 * alfa)));
        c.drawOval(-r * 0.7f, -r * (0.9f + 0.4f * aleteo), r * 0.9f, -r * 0.1f, p);
        // patas
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.1f);
        p.setColor(Color_alfa(0xFF2A1A3A, a));
        for (int k = -1; k <= 1; k++) c.drawLine(k * r * 0.4f, r * 0.4f, k * r * 0.55f - r * 0.1f, r * 0.95f, p);
        p.setStyle(Paint.Style.FILL);
        // élitros
        p.setColor(Color_alfa(0xFF4B2A7A, a));
        c.drawOval(-r * 0.75f, -r * 0.6f, r * 0.95f, r * 0.55f, p);
        p.setColor(Color_alfa(0x88C0A0FF, a));
        c.drawOval(-r * 0.45f, -r * 0.5f, r * 0.55f, -r * 0.1f, p);
        p.setStrokeWidth(r * 0.05f);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color_alfa(0xFF20103A, a));
        c.drawLine(r * 0.1f, -r * 0.6f, r * 0.1f, r * 0.55f, p);
        p.setStyle(Paint.Style.FILL);
        // cabeza con cuerno
        p.setColor(Color_alfa(0xFF2A1A3A, a));
        c.drawCircle(-r * 0.95f, 0, r * 0.32f, p);
        path.reset();
        path.moveTo(-r * 1.1f, -r * 0.1f);
        path.lineTo(-r * 1.6f, -r * 0.45f);
        path.lineTo(-r * 1.2f, -r * 0.0f);
        path.close();
        c.drawPath(path, p);
        p.setColor(Color_alfa(0xFFFF5A4A, a));
        c.drawCircle(-r * 1.0f, -r * 0.1f, r * 0.08f, p);
    }

    private void dibujarAnillo(Canvas c, float t) {
        float f = Math.min(1f, aEdad / 0.7f);
        float osc = (float) Math.sin(t * 20);
        float rx = aRadio * (1f + 0.1f * osc), ry = aRadio * (0.85f - 0.1f * osc);
        float ang = (float) Math.toDegrees(Math.atan2(aDy, aDx));
        // estela de humo
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x33DDDDDD);
        c.drawCircle(aX - aDx * 0.03f, aY - aDy * 0.03f, aRadio * 1.1f, p);
        c.drawCircle(aX - aDx * 0.06f, aY - aDy * 0.06f, aRadio * 0.8f, p);
        c.save();
        c.translate(aX, aY);
        c.rotate(ang);
        // bocanada suave
        p.setColor(Color_alfa(0xFFEDEDED, (int) (60 + 40 * f)));
        c.drawOval(-rx, -ry * 1.5f, rx, ry * 1.5f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(aRadio * 0.38f);
        p.setColor(0xCCF4F4F4);
        c.drawOval(-rx * 0.45f, -ry, rx * 0.45f, ry, p);
        p.setStrokeWidth(aRadio * 0.14f);
        p.setColor(0xAA9A9AA8);
        c.drawOval(-rx * 0.5f, -ry * 1.12f, rx * 0.5f, ry * 1.12f, p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
    }

    private void dibujarPuffs(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MAX_PUFFS; i++) {
            if (pfTipo[i] == 0) continue;
            if (pfTipo[i] == 1) {
                float f = pfEdad[i] / 0.6f;
                float r = pfTam[i] * (0.4f + f);
                p.setColor(Color_alfa(0xFFEEEEEE, (int) (170 * (1 - f))));
                c.drawCircle(pfX[i], pfY[i], r, p);
                c.drawCircle(pfX[i] - r * 0.7f, pfY[i] + r * 0.2f, r * 0.6f, p);
                c.drawCircle(pfX[i] + r * 0.7f, pfY[i] - r * 0.1f, r * 0.6f, p);
                c.drawCircle(pfX[i], pfY[i] - r * 0.7f, r * 0.55f, p);
            } else {
                float f = pfEdad[i] / 0.9f;
                p.setTextAlign(Paint.Align.CENTER);
                p.setFakeBoldText(true);
                p.setTextSize(pfTam[i] * (1f + 0.3f * f));
                p.setColor(Color_alfa(0xFFFFFFFF, (int) (255 * (1 - f))));
                c.drawText("+1", pfX[i], pfY[i] - f * h * 0.08f, p);
                p.setFakeBoldText(false);
            }
        }
    }
}
