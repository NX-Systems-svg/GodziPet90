package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;

import java.util.Random;

/**
 * Minijuego de reflejos "Defensa de la Bahía de Tokio": toca a los monstruos que emergen del agua antes de
 * que escapen. Escena nocturna con horizonte de Tokio. Sin asignaciones en onDraw (entidades en arreglos).
 */
public class DefensaView extends View {
    public static final int DURACION_MS = 30_000;
    public static final int META_ACIERTOS = 15;
    public static final int PUNTOS_POR_ACIERTO = 10;

    public interface Oyente {
        void progreso(int aciertos, int escapes, int segundosRestantes);
        void fin(int aciertos, int puntaje);
        void acierto();
    }

    private static final int MAX_ENEMIGOS = 6;
    private static final int MAX_RAYOS = 8;
    private static final int MAX_EXPLOSIONES = 10;
    private static final int ESTRELLAS = 46;
    private static final int EDIFICIOS = 24;
    private static final long DURA_RAYO = 280;
    private static final long DURA_EXPLOSION = 650;
    private static final long DURA_TEMBLOR = 260;
    private static final long PROGRESO_CADA_MS = 500;
    private static final int CAL = 0xFF3DE0FF; // azul-blanco del aliento atómico

    private static final float[] DIR_X = new float[12], DIR_Y = new float[12];
    static {
        for (int i = 0; i < 12; i++) {
            DIR_X[i] = (float) Math.cos(i * Math.PI / 6 + 0.3);
            DIR_Y[i] = (float) Math.sin(i * Math.PI / 6 + 0.3);
        }
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path torreCuerpo = new Path(), torreLineas = new Path();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final Random azar = new Random();

    // enemigos
    private final boolean[] eActivo = new boolean[MAX_ENEMIGOS];
    private final float[] eX = new float[MAX_ENEMIGOS], eY = new float[MAX_ENEMIGOS];
    private final long[] eNace = new long[MAX_ENEMIGOS], eVida = new long[MAX_ENEMIGOS];
    private final int[] eTipo = new int[MAX_ENEMIGOS];
    // rayos
    private final float[] rX = new float[MAX_RAYOS], rY = new float[MAX_RAYOS];
    private final long[] rInicio = new long[MAX_RAYOS];
    // explosiones
    private final float[] xX = new float[MAX_EXPLOSIONES], xY = new float[MAX_EXPLOSIONES];
    private final long[] xInicio = new long[MAX_EXPLOSIONES];
    // escena precalculada
    private final float[] estX = new float[ESTRELLAS], estY = new float[ESTRELLAS];
    private final float[] edX = new float[EDIFICIOS], edAncho = new float[EDIFICIOS], edAlto = new float[EDIFICIOS];
    private float[] ventanas = new float[0], ventanasCian = new float[0];
    private int nVentanas, nCian;
    private LinearGradient cielo, agua;

    private Apariencia apariencia;
    private Oyente oyente;
    private boolean jugando;
    private long inicio, ultimoSpawn, pausaHasta, destelloHasta, temblorHasta, barridoInicio, ultimoProgreso;
    private int aciertos, escapes;
    private boolean cambio;
    private float w, h, horizonte, tam, kaijuX, kaijuSuelo, bocaX, bocaY;

    public DefensaView(Context c) { this(c, null); }

    public DefensaView(Context c, AttributeSet a) {
        super(c, a);
        setContentDescription("Área de juego: toca a los monstruos");
    }

    public void setApariencia(Apariencia a) {
        apariencia = a.conAnimo(EstadoAnimo.NORMAL);
        recalcularKaiju();
        invalidate();
    }

    public void setOyente(Oyente o) { oyente = o; }
    public boolean isJugando() { return jugando; }

    public void iniciar() {
        for (int i = 0; i < MAX_ENEMIGOS; i++) eActivo[i] = false;
        for (int i = 0; i < MAX_RAYOS; i++) rInicio[i] = 0;
        for (int i = 0; i < MAX_EXPLOSIONES; i++) xInicio[i] = 0;
        aciertos = 0;
        escapes = 0;
        inicio = System.currentTimeMillis();
        ultimoSpawn = inicio;
        ultimoProgreso = inicio;
        pausaHasta = 0;
        destelloHasta = 0;
        temblorHasta = 0;
        barridoInicio = 0;
        jugando = true;
        invalidate();
    }

    public void detener() {
        jugando = false;
    }

    /** Mecánica exclusiva: el Aliento Atómico arrasa con todos los monstruos visibles. */
    public int alientoAtomico() {
        if (!jugando) return 0;
        long ahora = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < MAX_ENEMIGOS; i++) {
            if (!eActivo[i]) continue;
            eActivo[i] = false;
            nuevaExplosion(eX[i], eY[i], ahora);
            n++;
        }
        aciertos += n;
        destelloHasta = ahora + 700;
        barridoInicio = ahora;
        temblorHasta = ahora + 450;
        pausaHasta = ahora + 1200;
        if (oyente != null) oyente.progreso(aciertos, escapes, restantes(ahora));
        return n;
    }

    private int restantes(long ahora) {
        return (int) Math.max(0, (DURACION_MS - (ahora - inicio) + 999) / 1000);
    }

    // ---------------------------------------------------------------- geometría

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        super.onSizeChanged(nw, nh, ow, oh);
        w = nw;
        h = nh;
        horizonte = h * 0.34f;
        kaijuX = w * 0.16f;
        kaijuSuelo = h * 1.02f;
        cielo = new LinearGradient(0, 0, 0, horizonte, new int[]{0xFF050818, 0xFF141B44, 0xFF3A2D6B},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        agua = new LinearGradient(0, horizonte, 0, h, 0xFF16275A, 0xFF050C22, Shader.TileMode.CLAMP);
        Random r = new Random(90);
        for (int i = 0; i < ESTRELLAS; i++) {
            estX[i] = r.nextFloat() * w;
            estY[i] = r.nextFloat() * horizonte * 0.85f;
        }
        construirCiudad(r);
        construirTorre();
        recalcularKaiju();
    }

    private void recalcularKaiju() {
        if (w == 0) return;
        if (apariencia == null) {
            tam = h * 0.42f;
            bocaX = w * 0.3f;
            bocaY = h * 0.7f;
            return;
        }
        tam = h * 0.27f / apariencia.escalaEtapa();
        PointF b = KaijuPainter.boca(kaijuX, kaijuSuelo, tam, apariencia);
        bocaX = b.x;
        bocaY = b.y;
    }

    /** Siluetas de edificios y ventanas encendidas (se calculan una sola vez por tamaño). */
    private void construirCiudad(Random r) {
        float x = -w * 0.02f;
        int n = 0;
        float[] vt = new float[EDIFICIOS * 40 * 2];
        float[] vc = new float[EDIFICIOS * 6 * 2];
        int nc = 0;
        float paso = Math.max(4f, w * 0.012f);
        for (int i = 0; i < EDIFICIOS; i++) {
            edX[i] = x;
            edAncho[i] = w * (0.03f + r.nextFloat() * 0.05f);
            edAlto[i] = h * (0.035f + r.nextFloat() * 0.1f);
            if (i % 7 == 3) edAlto[i] *= 1.6f;
            for (float wy = horizonte - edAlto[i] + paso; wy < horizonte - paso * 0.5f; wy += paso * 1.3f) {
                for (float wx = x + paso * 0.5f; wx < x + edAncho[i] - paso * 0.5f; wx += paso) {
                    if (r.nextFloat() < 0.38f && n < vt.length / 2) {
                        vt[n * 2] = wx;
                        vt[n * 2 + 1] = wy;
                        n++;
                    } else if (r.nextFloat() < 0.03f && nc < vc.length / 2) {
                        vc[nc * 2] = wx;
                        vc[nc * 2 + 1] = wy;
                        nc++;
                    }
                }
            }
            x += edAncho[i] * (0.9f + r.nextFloat() * 0.2f);
        }
        ventanas = vt;
        ventanasCian = vc;
        nVentanas = n * 2;
        nCian = nc * 2;
    }

    /** Torre de Tokio: silueta naranja con plataformas, entramado y antena. */
    private void construirTorre() {
        float tx = w * 0.64f, base = horizonte, alto = h * 0.3f, bw = alto * 0.36f;
        torreCuerpo.reset();
        torreCuerpo.moveTo(tx - bw / 2, base);
        torreCuerpo.lineTo(tx - bw * 0.1f, base - alto * 0.4f);
        torreCuerpo.lineTo(tx - bw * 0.07f, base - alto * 0.44f);
        torreCuerpo.lineTo(tx - bw * 0.04f, base - alto * 0.72f);
        torreCuerpo.lineTo(tx - bw * 0.012f, base - alto * 0.76f);
        torreCuerpo.lineTo(tx, base - alto);
        torreCuerpo.lineTo(tx + bw * 0.012f, base - alto * 0.76f);
        torreCuerpo.lineTo(tx + bw * 0.04f, base - alto * 0.72f);
        torreCuerpo.lineTo(tx + bw * 0.07f, base - alto * 0.44f);
        torreCuerpo.lineTo(tx + bw * 0.1f, base - alto * 0.4f);
        torreCuerpo.lineTo(tx + bw / 2, base);
        torreCuerpo.close();
        torreLineas.reset();
        for (int i = 0; i < 6; i++) {
            float f0 = i * 0.066f, f1 = f0 + 0.066f;
            float a0 = bw * (0.5f - 0.4f * f0 / 0.4f * 1f), a1 = bw * (0.5f - 0.4f * f1 / 0.4f * 1f);
            torreLineas.moveTo(tx - a0, base - alto * f0);
            torreLineas.lineTo(tx + a1, base - alto * f1);
            torreLineas.moveTo(tx + a0, base - alto * f0);
            torreLineas.lineTo(tx - a1, base - alto * f1);
        }
        // plataformas
        torreLineas.addRect(tx - bw * 0.2f, base - alto * 0.44f, tx + bw * 0.2f, base - alto * 0.4f, Path.Direction.CW);
        torreLineas.addRect(tx - bw * 0.11f, base - alto * 0.74f, tx + bw * 0.11f, base - alto * 0.71f,
                Path.Direction.CW);
    }

    // ---------------------------------------------------------------- entrada

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!jugando || e.getAction() != MotionEvent.ACTION_DOWN) return true;
        float radio = getWidth() * 0.09f;
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < MAX_ENEMIGOS; i++) {
            if (!eActivo[i]) continue;
            if (Math.hypot(e.getX() - eX[i], e.getY() - eY[i]) < radio) {
                eActivo[i] = false;
                aciertos++;
                nuevoRayo(eX[i], eY[i], ahora);
                nuevaExplosion(eX[i], eY[i], ahora);
                temblorHasta = ahora + DURA_TEMBLOR;
                if (oyente != null) {
                    oyente.acierto();
                    oyente.progreso(aciertos, escapes, restantes(ahora));
                }
                break;
            }
        }
        return true;
    }

    private void nuevoRayo(float x, float y, long ahora) {
        int k = 0;
        for (int i = 0; i < MAX_RAYOS; i++) {
            if (ahora - rInicio[i] >= DURA_RAYO) { k = i; break; }
            if (rInicio[i] < rInicio[k]) k = i;
        }
        rX[k] = x;
        rY[k] = y;
        rInicio[k] = ahora;
    }

    private void nuevaExplosion(float x, float y, long ahora) {
        int k = 0;
        for (int i = 0; i < MAX_EXPLOSIONES; i++) {
            if (ahora - xInicio[i] >= DURA_EXPLOSION) { k = i; break; }
            if (xInicio[i] < xInicio[k]) k = i;
        }
        xX[k] = x;
        xY[k] = y;
        xInicio[k] = ahora;
    }

    // ---------------------------------------------------------------- lógica

    private void actualizar(long ahora) {
        long transcurrido = ahora - inicio;
        if (transcurrido >= DURACION_MS) {
            jugando = false;
            for (int i = 0; i < MAX_ENEMIGOS; i++) eActivo[i] = false;
            if (oyente != null) oyente.fin(aciertos, aciertos * PUNTOS_POR_ACIERTO);
            return;
        }
        float dificultad = transcurrido / (float) DURACION_MS;
        long intervalo = (long) (850 - 380 * dificultad);
        int activos = 0, libre = -1;
        for (int i = 0; i < MAX_ENEMIGOS; i++) {
            if (eActivo[i]) activos++; else if (libre < 0) libre = i;
        }
        if (ahora > pausaHasta && ahora - ultimoSpawn > intervalo && activos < 5 && libre >= 0) {
            eActivo[libre] = true;
            eX[libre] = w * (0.38f + azar.nextFloat() * 0.56f);
            eY[libre] = horizonte + (h - horizonte) * (0.15f + azar.nextFloat() * 0.6f);
            eNace[libre] = ahora;
            eVida[libre] = (long) (1700 - 650 * dificultad);
            eTipo[libre] = azar.nextInt(3);
            ultimoSpawn = ahora;
        }
        boolean huboCambio = cambio;
        cambio = false;
        for (int i = 0; i < MAX_ENEMIGOS; i++) {
            if (eActivo[i] && ahora - eNace[i] > eVida[i]) {
                eActivo[i] = false;
                escapes++;
                huboCambio = true;
            }
        }
        if (oyente != null && (huboCambio || ahora - ultimoProgreso >= PROGRESO_CADA_MS)) {
            ultimoProgreso = ahora;
            oyente.progreso(aciertos, escapes, restantes(ahora));
        }
    }

    // ---------------------------------------------------------------- dibujo

    @Override
    protected void onDraw(Canvas c) {
        if (w == 0) return;
        long ahora = System.currentTimeMillis();
        float t = ahora / 1000f;
        if (jugando) actualizar(ahora);

        c.save();
        if (ahora < temblorHasta) {
            float f = (temblorHasta - ahora) / (float) DURA_TEMBLOR;
            f = Math.min(1f, f);
            c.translate((float) Math.sin(ahora * 0.09) * w * 0.012f * f, (float) Math.cos(ahora * 0.11) * w * 0.01f * f);
        }
        dibujarCielo(c, t);
        dibujarCiudad(c, t);
        dibujarAgua(c, t);
        for (int i = 0; i < MAX_ENEMIGOS; i++) {
            if (eActivo[i]) dibujarEnemigo(c, i, ahora);
        }
        boolean disparando = false;
        for (int i = 0; i < MAX_RAYOS; i++) {
            if (ahora - rInicio[i] < DURA_RAYO) disparando = true;
        }
        if (apariencia != null) {
            kaiju.dibujar(c, kaijuX, kaijuSuelo, tam, apariencia, t, disparando ? 0.9f : 0f, false);
        }
        for (int i = 0; i < MAX_RAYOS; i++) {
            if (ahora - rInicio[i] < DURA_RAYO) dibujarRayo(c, rX[i], rY[i], (ahora - rInicio[i]) / (float) DURA_RAYO);
        }
        for (int i = 0; i < MAX_EXPLOSIONES; i++) {
            if (ahora - xInicio[i] < DURA_EXPLOSION) {
                dibujarExplosion(c, xX[i], xY[i], (ahora - xInicio[i]) / (float) DURA_EXPLOSION);
            }
        }
        if (ahora - barridoInicio < 700) dibujarBarrido(c, (ahora - barridoInicio) / 700f);
        c.restore();
        if (ahora < destelloHasta) {
            float f = (destelloHasta - ahora) / 700f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (210 * f) << 24) | 0x3DE0FF);
            c.drawRect(0, 0, w, h, p);
        }
        if (jugando) postInvalidateOnAnimation();
    }

    private void dibujarCielo(Canvas c, float t) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(cielo);
        c.drawRect(-w * 0.05f, -h * 0.05f, w * 1.05f, horizonte + 1, p);
        p.setShader(null);
        for (int i = 0; i < ESTRELLAS; i++) {
            int a = 120 + (int) (110 * Math.sin(t * 2 + i * 1.9));
            p.setColor((Math.max(40, Math.min(255, a)) << 24) | 0xFFFFFF);
            c.drawCircle(estX[i], estY[i], i % 5 == 0 ? 2.2f : 1.3f, p);
        }
        // luna con halo y cráteres
        float lx = w * 0.82f, ly = h * 0.12f, lr = w * 0.07f;
        p.setColor(0x22FFF3C4);
        c.drawCircle(lx, ly, lr * 2.4f, p);
        p.setColor(0x33FFF3C4);
        c.drawCircle(lx, ly, lr * 1.6f, p);
        p.setColor(0xFFFFF3C4);
        c.drawCircle(lx, ly, lr, p);
        p.setColor(0x33B39A5A);
        c.drawCircle(lx - lr * 0.3f, ly - lr * 0.2f, lr * 0.22f, p);
        c.drawCircle(lx + lr * 0.35f, ly + lr * 0.25f, lr * 0.16f, p);
        c.drawCircle(lx + lr * 0.1f, ly - lr * 0.5f, lr * 0.1f, p);
    }

    private void dibujarCiudad(Canvas c, float t) {
        // resplandor de la ciudad
        p.setColor(0x33FF9A5A);
        c.drawRect(-w * 0.05f, horizonte - h * 0.04f, w * 1.05f, horizonte, p);
        p.setColor(0xFF0A0E26);
        for (int i = 0; i < EDIFICIOS; i++) {
            c.drawRect(edX[i], horizonte - edAlto[i], edX[i] + edAncho[i], horizonte + 1, p);
            if (i % 4 == 1) c.drawRect(edX[i] + edAncho[i] * 0.45f, horizonte - edAlto[i] - h * 0.02f,
                    edX[i] + edAncho[i] * 0.52f, horizonte - edAlto[i], p);
        }
        // Torre de Tokio
        p.setColor(0xFFC4481F);
        c.drawPath(torreCuerpo, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, w * 0.004f));
        p.setColor(0xCCFFC27A);
        c.drawPath(torreLineas, p);
        p.setStyle(Paint.Style.FILL);
        // luz roja intermitente en la antena
        if ((int) (t * 1.5f) % 2 == 0) {
            p.setColor(0xFFFF3030);
            c.drawCircle(w * 0.64f, horizonte - h * 0.3f, Math.max(2f, w * 0.006f), p);
        }
        // ventanas encendidas
        float lado = Math.max(2.5f, w * 0.0055f);
        p.setStrokeWidth(lado);
        p.setStrokeCap(Paint.Cap.SQUARE);
        p.setColor(0xFFFFD27A);
        c.drawPoints(ventanas, 0, nVentanas, p);
        p.setColor(0xFF8FEFFF);
        c.drawPoints(ventanasCian, 0, nCian, p);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    private void dibujarAgua(Canvas c, float t) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(agua);
        c.drawRect(-w * 0.05f, horizonte, w * 1.05f, h * 1.05f, p);
        p.setShader(null);
        // reflejo de las luces de la ciudad
        p.setColor(0x33FFB45A);
        for (int i = 0; i < 9; i++) {
            float x = w * (0.08f + i * 0.11f) + (float) Math.sin(t * 1.5f + i) * w * 0.01f;
            c.drawRect(x, horizonte + 2, x + w * 0.012f, horizonte + h * (0.05f + 0.02f * (i % 3)), p);
        }
        // columna de luz de la luna
        p.setColor(0x44FFF3C4);
        for (int i = 0; i < 7; i++) {
            float ancho = w * (0.05f + 0.012f * i) * (0.8f + 0.2f * (float) Math.sin(t * 2.2f + i));
            float y = horizonte + h * (0.015f + i * 0.045f);
            c.drawRect(w * 0.82f - ancho, y, w * 0.82f + ancho, y + h * 0.01f, p);
        }
        // olas animadas
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, h * 0.004f));
        p.setColor(0x44A8D8FF);
        for (int f = 0; f < 6; f++) {
            float y = horizonte + (h - horizonte) * (0.1f + f * 0.16f);
            float amp = h * (0.004f + 0.002f * f);
            path.reset();
            path.moveTo(0, y);
            for (int s = 1; s <= 22; s++) {
                path.lineTo(w * s / 22f, y + (float) Math.sin(t * (1.2f + f * 0.15f) + s * 0.8f + f * 2) * amp);
            }
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void dibujarRayo(Canvas c, float dx, float dy, float f) {
        float fade = 1 - f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w * 0.07f * fade + 2);
        p.setColor(((int) (90 * fade) << 24) | 0x3DE0FF);
        c.drawLine(bocaX, bocaY, dx, dy, p);
        p.setStrokeWidth(w * 0.035f * fade + 1.5f);
        p.setColor(((int) (200 * fade) << 24) | (CAL & 0xFFFFFF));
        c.drawLine(bocaX, bocaY, dx, dy, p);
        p.setStrokeWidth(w * 0.014f * fade + 1f);
        p.setColor(((int) (255 * fade) << 24) | 0xFFFFFF);
        c.drawLine(bocaX, bocaY, dx, dy, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(((int) (160 * fade) << 24) | (CAL & 0xFFFFFF));
        c.drawCircle(bocaX, bocaY, w * 0.035f * (0.6f + fade * 0.6f), p);
    }

    /** Estallido: bola de fuego, onda expansiva, chispas y escombros con gravedad. */
    private void dibujarExplosion(Canvas c, float x, float y, float f) {
        float s = w * 0.09f, fade = 1 - f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(((int) (170 * fade) << 24) | 0xFF8A30);
        c.drawCircle(x, y, s * (0.35f + f * 1.1f), p);
        p.setColor(((int) (210 * fade) << 24) | 0x8FF0FF);
        c.drawCircle(x, y, s * (0.25f + f * 0.7f), p);
        p.setColor(((int) (255 * fade) << 24) | 0xFFFFFF);
        c.drawCircle(x, y, s * 0.5f * fade, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, s * 0.12f * fade));
        p.setColor(((int) (200 * fade) << 24) | 0x3DE0FF);
        c.drawCircle(x, y, s * (0.5f + f * 2.2f), p);
        // chispas
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.5f, s * 0.06f));
        p.setColor(((int) (255 * fade) << 24) | 0xFFE680);
        for (int i = 0; i < 12; i++) {
            float d0 = s * f * (1.4f + 0.5f * (i % 3));
            float d1 = d0 + s * 0.35f * fade;
            c.drawLine(x + DIR_X[i] * d0, y + DIR_Y[i] * d0, x + DIR_X[i] * d1, y + DIR_Y[i] * d1, p);
        }
        // escombros
        p.setStyle(Paint.Style.FILL);
        p.setColor(((int) (240 * fade) << 24) | 0x40363A);
        for (int i = 0; i < 6; i++) {
            float d = s * f * (0.8f + 0.35f * (i % 3));
            float dx = x + DIR_X[i * 2] * d, dy = y + DIR_Y[i * 2] * d * 0.6f + s * 2.2f * f * f;
            float r = s * 0.07f * (1 + i % 2);
            c.save();
            c.rotate(f * 540 * (i % 2 == 0 ? 1 : -1), dx, dy);
            c.drawRect(dx - r, dy - r, dx + r, dy + r, p);
            c.restore();
        }
    }

    /** Barrido del Aliento Atómico: un haz que recorre la bahía de abajo hacia arriba. */
    private void dibujarBarrido(Canvas c, float f) {
        float ty = h * 0.95f - f * h * 0.8f;
        float fade = 1 - f * 0.6f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w * 0.09f);
        p.setColor(((int) (90 * fade) << 24) | 0x3DE0FF);
        c.drawLine(bocaX, bocaY, w * 1.05f, ty, p);
        p.setStrokeWidth(w * 0.045f);
        p.setColor(((int) (220 * fade) << 24) | 0x3DE0FF);
        c.drawLine(bocaX, bocaY, w * 1.05f, ty, p);
        p.setStrokeWidth(w * 0.018f);
        p.setColor(((int) (255 * fade) << 24) | 0xFFFFFF);
        c.drawLine(bocaX, bocaY, w * 1.05f, ty, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void dibujarEnemigo(Canvas c, int i, long ahora) {
        float f = (ahora - eNace[i]) / (float) eVida[i];
        float aparicion = Math.min(1f, f * 5);
        float r = w * 0.055f * aparicion;
        float x = eX[i];
        float y = eY[i] + (1 - aparicion) * r * 2;
        // anillos de salpicadura sobre el agua
        float ts = Math.min(1f, (ahora - eNace[i]) / 700f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        for (int k = 0; k < 2; k++) {
            float g = Math.min(1f, ts + k * 0.0f) * (1 - k * 0.35f);
            p.setColor(((int) (170 * (1 - ts)) << 24) | 0xFFFFFF);
            float rr = r * (1.2f + 1.4f * g);
            c.drawOval(x - rr, eY[i] + r * 0.55f - rr * 0.22f, x + rr, eY[i] + r * 0.55f + rr * 0.22f, p);
        }
        // arco de tiempo restante
        p.setColor(f > 0.7f ? 0xFFFF4D5E : 0xFFFFD23F);
        p.setStrokeWidth(5);
        c.drawArc(x - r * 1.45f, y - r * 1.45f, x + r * 1.45f, y + r * 1.45f, -90, 360 * (1 - f), false, p);
        p.setStyle(Paint.Style.FILL);
        float aleteo = (float) Math.sin(ahora / 70.0) * r * 0.4f;
        if (eTipo[i] == 0) dibujarMantis(c, x, y, r, aleteo);
        else if (eTipo[i] == 1) dibujarPteranodon(c, x, y, r, aleteo);
        else dibujarLangosta(c, x, y, r, ahora);
        // espuma sobre la base mientras emerge
        if (aparicion < 1f) {
            p.setColor(0x99FFFFFF);
            c.drawOval(x - r * 1.1f, eY[i] + r * 0.3f, x + r * 1.1f, eY[i] + r * 0.8f, p);
        }
    }

    /** Mantis gigante: cuerpo verde, cabeza triangular y guadañas. */
    private void dibujarMantis(Canvas c, float x, float y, float r, float aleteo) {
        p.setColor(0x55C8FFD8);
        c.drawOval(x - r * 1.4f, y - r * 1.4f + aleteo, x - r * 0.1f, y + r * 0.2f, p);
        c.drawOval(x + r * 0.1f, y - r * 1.4f + aleteo, x + r * 1.4f, y + r * 0.2f, p);
        p.setColor(0xFF4FA83A);
        c.drawOval(x - r * 0.4f, y - r * 0.5f, x + r * 0.4f, y + r * 0.9f, p);
        p.setColor(0xFF74D04E);
        path.reset();
        path.moveTo(x - r * 0.55f, y - r * 0.95f);
        path.lineTo(x + r * 0.55f, y - r * 0.95f);
        path.lineTo(x, y - r * 0.35f);
        path.close();
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.2f);
        p.setColor(0xFF8AE05A);
        for (int s = -1; s <= 1; s += 2) {
            path.reset();
            path.moveTo(x + s * r * 0.3f, y);
            path.lineTo(x + s * r * 1.2f, y - r * 0.9f);
            path.lineTo(x + s * r * 0.9f, y + r * 0.1f);
            c.drawPath(path, p);
        }
        p.setStrokeWidth(r * 0.06f);
        c.drawLine(x - r * 0.2f, y - r * 0.95f, x - r * 0.6f, y - r * 1.4f, p);
        c.drawLine(x + r * 0.2f, y - r * 0.95f, x + r * 0.6f, y - r * 1.4f, p);
        p.setStyle(Paint.Style.FILL);
        ojosBrillantes(c, x - r * 0.3f, x + r * 0.3f, y - r * 0.75f, r * 0.17f, 0xFFFFE066);
    }

    /** Pteranodon: alas de membrana púrpura, pico largo y cresta. */
    private void dibujarPteranodon(Canvas c, float x, float y, float r, float aleteo) {
        p.setColor(0xFF7A4A8E);
        path.reset();
        path.moveTo(x, y - r * 0.2f);
        path.lineTo(x - r * 2.1f, y - r * 0.9f + aleteo * 1.6f);
        path.lineTo(x - r * 1.2f, y + r * 0.25f);
        path.lineTo(x - r * 0.5f, y + r * 0.05f);
        path.lineTo(x + r * 0.5f, y + r * 0.05f);
        path.lineTo(x + r * 1.2f, y + r * 0.25f);
        path.lineTo(x + r * 2.1f, y - r * 0.9f + aleteo * 1.6f);
        path.close();
        c.drawPath(path, p);
        p.setColor(0xFF9A5B3E);
        c.drawOval(x - r * 0.5f, y - r * 0.6f, x + r * 0.5f, y + r * 0.6f, p);
        path.reset();
        path.moveTo(x - r * 0.3f, y - r * 0.6f);
        path.lineTo(x - r * 0.2f, y - r * 1.1f);
        path.lineTo(x + r * 0.9f, y - r * 1.5f);
        path.lineTo(x + r * 0.35f, y - r * 0.7f);
        path.close();
        c.drawPath(path, p);
        p.setColor(0xFFF0C070);
        path.reset();
        path.moveTo(x - r * 0.2f, y - r * 0.95f);
        path.lineTo(x - r * 0.95f, y - r * 0.8f);
        path.lineTo(x - r * 0.2f, y - r * 0.75f);
        path.close();
        c.drawPath(path, p);
        ojosBrillantes(c, x, x, y - r * 0.9f, r * 0.12f, 0xFFFF4040);
    }

    /** Langosta mutante: caparazón rojo, pinzas enormes y ojos sobre pedúnculos. */
    private void dibujarLangosta(Canvas c, float x, float y, float r, long ahora) {
        float pinza = (float) Math.sin(ahora / 140.0) * 0.15f;
        p.setColor(0xFFC23826);
        for (int k = 0; k < 3; k++) { // cola segmentada
            c.drawOval(x - r * (0.55f - k * 0.1f), y + r * (0.4f + k * 0.28f),
                    x + r * (0.55f - k * 0.1f), y + r * (0.75f + k * 0.28f), p);
        }
        p.setColor(0xFFE0533D);
        c.drawOval(x - r * 0.75f, y - r * 0.6f, x + r * 0.75f, y + r * 0.6f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.18f);
        for (int s = -1; s <= 1; s += 2) {
            c.drawLine(x + s * r * 0.55f, y - r * 0.2f, x + s * r * 1.15f, y - r * 0.8f, p);
            c.drawLine(x + s * r * 0.5f, y + r * 0.2f, x + s * r * 1.1f, y + r * 0.55f, p);
            c.drawLine(x + s * r * 0.1f, y - r * 0.55f, x + s * r * 0.35f, y - r * 1.2f, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFF05A3C);
        for (int s = -1; s <= 1; s += 2) {
            c.drawCircle(x + s * r * 1.25f, y - r * (0.95f + pinza), r * 0.42f, p);
            path.reset();
            path.moveTo(x + s * r * 1.25f, y - r * (1.35f + pinza));
            path.lineTo(x + s * r * 1.75f, y - r * (1.5f + pinza * 2));
            path.lineTo(x + s * r * 1.5f, y - r * 0.9f);
            path.close();
            c.drawPath(path, p);
        }
        ojosBrillantes(c, x - r * 0.35f, x + r * 0.35f, y - r * 1.2f, r * 0.14f, 0xFFFFE066);
    }

    private void ojosBrillantes(Canvas c, float x1, float x2, float y, float r, int color) {
        p.setColor(color);
        c.drawCircle(x1, y, r, p);
        if (x2 != x1) c.drawCircle(x2, y, r, p);
        p.setColor(0xFF101010);
        c.drawCircle(x1, y, r * 0.45f, p);
        if (x2 != x1) c.drawCircle(x2, y, r * 0.45f, p);
    }
}
