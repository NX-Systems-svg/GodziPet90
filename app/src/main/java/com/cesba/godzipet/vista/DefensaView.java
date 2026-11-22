package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Minijuego de reflejos "Defensa de la Bahía": toca a los monstruos antes de que escapen. */
public class DefensaView extends View {
    public static final int DURACION_MS = 30_000;
    public static final int META_ACIERTOS = 15;
    public static final int PUNTOS_POR_ACIERTO = 10;

    public interface Oyente {
        void progreso(int aciertos, int escapes, int segundosRestantes);
        void fin(int aciertos, int puntaje);
        void acierto();
    }

    private static final class Monstruo {
        float x, y;
        long nace;
        long vida;
        int tipo;
    }

    private static final class Rayo {
        PointF destino;
        long inicio;
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final EscenaPainter escena = new EscenaPainter();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final List<Monstruo> monstruos = new ArrayList<>();
    private final List<Rayo> rayos = new ArrayList<>();
    private final Random azar = new Random();

    private Apariencia apariencia;
    private Oyente oyente;
    private boolean jugando;
    private long inicio, ultimoSpawn, pausaHasta, destelloHasta;
    private int aciertos, escapes;

    public DefensaView(Context c) { this(c, null); }

    public DefensaView(Context c, AttributeSet a) {
        super(c, a);
        setContentDescription("Área de juego: toca a los monstruos");
    }

    public void setApariencia(Apariencia a) { apariencia = a.conAnimo(EstadoAnimo.NORMAL); }
    public void setOyente(Oyente o) { oyente = o; }
    public boolean isJugando() { return jugando; }

    public void iniciar() {
        monstruos.clear();
        rayos.clear();
        aciertos = 0;
        escapes = 0;
        inicio = System.currentTimeMillis();
        ultimoSpawn = inicio;
        pausaHasta = 0;
        jugando = true;
        invalidate();
    }

    public void detener() {
        jugando = false;
    }

    /** Mecánica exclusiva: el Aliento Atómico arrasa con todos los monstruos visibles. */
    public int alientoAtomico() {
        if (!jugando) return 0;
        int n = monstruos.size();
        aciertos += n;
        monstruos.clear();
        long ahora = System.currentTimeMillis();
        destelloHasta = ahora + 700;
        pausaHasta = ahora + 1200;
        if (oyente != null) oyente.progreso(aciertos, escapes, restantes(ahora));
        return n;
    }

    private int restantes(long ahora) {
        return (int) Math.max(0, (DURACION_MS - (ahora - inicio) + 999) / 1000);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!jugando || e.getAction() != MotionEvent.ACTION_DOWN) return true;
        float radio = getWidth() * 0.09f;
        long ahora = System.currentTimeMillis();
        Iterator<Monstruo> it = monstruos.iterator();
        while (it.hasNext()) {
            Monstruo m = it.next();
            if (Math.hypot(e.getX() - m.x, e.getY() - m.y) < radio) {
                it.remove();
                aciertos++;
                Rayo r = new Rayo();
                r.destino = new PointF(m.x, m.y);
                r.inicio = ahora;
                rayos.add(r);
                if (oyente != null) {
                    oyente.acierto();
                    oyente.progreso(aciertos, escapes, restantes(ahora));
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w == 0) return;
        long ahora = System.currentTimeMillis();
        float t = ahora / 1000f;
        float horizonte = h * 0.32f;
        escena.cielo(c, w, horizonte, 2, t, true);
        escena.ciudad(c, w, horizonte, h * 0.14f, 2, true);
        escena.agua(c, w, horizonte, h, t, true);

        if (jugando) actualizar(ahora, w, h, horizonte);

        for (Monstruo m : monstruos) dibujarMonstruo(c, m, ahora, w);

        // el kaiju defiende desde la esquina
        float tam = apariencia == null ? h * 0.42f : h * 0.27f / apariencia.escalaEtapa();
        if (apariencia != null) kaiju.dibujar(c, w * 0.16f, h * 1.02f, tam, apariencia, t, rayos.isEmpty() ? 0 : 0.8f, false);
        PointF boca = apariencia == null ? new PointF(w * 0.3f, h * 0.7f)
                : KaijuPainter.boca(w * 0.16f, h * 1.02f, tam, apariencia);
        Iterator<Rayo> it = rayos.iterator();
        while (it.hasNext()) {
            Rayo r = it.next();
            float f = (ahora - r.inicio) / 260f;
            if (f >= 1) {
                it.remove();
                continue;
            }
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(w * 0.03f * (1 - f));
            p.setColor(Color.argb((int) (230 * (1 - f)), 150, 255, 130));
            c.drawLine(boca.x, boca.y, r.destino.x, r.destino.y, p);
            p.setColor(Color.argb((int) (255 * (1 - f)), 255, 240, 120));
            c.drawCircle(r.destino.x, r.destino.y, w * 0.05f + f * w * 0.08f, p);
        }
        if (ahora < destelloHasta) {
            float f = (destelloHasta - ahora) / 700f;
            p.setColor(Color.argb((int) (230 * f), 160, 255, 140));
            c.drawRect(0, 0, w, h, p);
        }
        if (jugando) postInvalidateOnAnimation();
    }

    private void actualizar(long ahora, float w, float h, float horizonte) {
        long transcurrido = ahora - inicio;
        if (transcurrido >= DURACION_MS) {
            jugando = false;
            monstruos.clear();
            if (oyente != null) oyente.fin(aciertos, aciertos * PUNTOS_POR_ACIERTO);
            return;
        }
        float dificultad = transcurrido / (float) DURACION_MS;
        long intervalo = (long) (850 - 380 * dificultad);
        if (ahora > pausaHasta && ahora - ultimoSpawn > intervalo && monstruos.size() < 5) {
            Monstruo m = new Monstruo();
            m.x = w * (0.38f + azar.nextFloat() * 0.56f);
            m.y = horizonte + (h - horizonte) * (0.15f + azar.nextFloat() * 0.6f);
            m.nace = ahora;
            m.vida = (long) (1700 - 650 * dificultad);
            m.tipo = azar.nextInt(3);
            monstruos.add(m);
            ultimoSpawn = ahora;
        }
        boolean cambio = false;
        Iterator<Monstruo> it = monstruos.iterator();
        while (it.hasNext()) {
            Monstruo m = it.next();
            if (ahora - m.nace > m.vida) {
                it.remove();
                escapes++;
                cambio = true;
            }
        }
        if (oyente != null && (cambio || ahora % 500 < 20)) oyente.progreso(aciertos, escapes, restantes(ahora));
    }

    private void dibujarMonstruo(Canvas c, Monstruo m, long ahora, float w) {
        float f = (ahora - m.nace) / (float) m.vida;
        float aparicion = Math.min(1f, f * 5);
        float r = w * 0.055f * aparicion;
        float y = m.y + (1 - aparicion) * r * 2;
        // anillo de agua
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(0x88FFFFFF);
        c.drawOval(m.x - r * 1.5f, y + r * 0.6f, m.x + r * 1.5f, y + r * 1.1f, p);
        // barra de tiempo restante
        p.setColor(f > 0.7f ? 0xFFFF4D5E : 0xFFFFD23F);
        p.setStrokeWidth(5);
        c.drawArc(m.x - r * 1.3f, y - r * 1.3f, m.x + r * 1.3f, y + r * 1.3f, -90, 360 * (1 - f), false, p);
        p.setStyle(Paint.Style.FILL);
        int color = m.tipo == 0 ? 0xFFE0533D : m.tipo == 1 ? 0xFF8E5BD8 : 0xFFF07FB0;
        p.setColor(color);
        if (m.tipo == 0) { // cangrejo
            c.drawOval(m.x - r, y - r * 0.7f, m.x + r, y + r * 0.6f, p);
            c.drawCircle(m.x - r * 1.2f, y - r * 0.6f, r * 0.45f, p);
            c.drawCircle(m.x + r * 1.2f, y - r * 0.6f, r * 0.45f, p);
        } else if (m.tipo == 1) { // murciélago
            path.reset();
            float aleteo = (float) Math.sin(ahora / 70.0) * r * 0.4f;
            path.moveTo(m.x, y);
            path.lineTo(m.x - r * 1.8f, y - r * 0.6f + aleteo);
            path.lineTo(m.x - r * 1.1f, y + r * 0.2f);
            path.lineTo(m.x + r * 1.1f, y + r * 0.2f);
            path.lineTo(m.x + r * 1.8f, y - r * 0.6f + aleteo);
            path.close();
            c.drawPath(path, p);
            c.drawCircle(m.x, y, r * 0.7f, p);
        } else { // calamar
            c.drawOval(m.x - r * 0.8f, y - r * 1.2f, m.x + r * 0.8f, y + r * 0.3f, p);
            p.setStrokeWidth(r * 0.25f);
            p.setStrokeCap(Paint.Cap.ROUND);
            for (int i = -2; i <= 2; i++) {
                c.drawLine(m.x + i * r * 0.3f, y + r * 0.2f,
                        m.x + i * r * 0.4f + (float) Math.sin(ahora / 120.0 + i) * r * 0.3f, y + r, p);
            }
        }
        p.setColor(Color.WHITE);
        c.drawCircle(m.x - r * 0.35f, y - r * 0.3f, r * 0.25f, p);
        c.drawCircle(m.x + r * 0.35f, y - r * 0.3f, r * 0.25f, p);
        p.setColor(Color.BLACK);
        c.drawCircle(m.x - r * 0.3f, y - r * 0.28f, r * 0.12f, p);
        c.drawCircle(m.x + r * 0.4f, y - r * 0.28f, r * 0.12f, p);
    }
}
