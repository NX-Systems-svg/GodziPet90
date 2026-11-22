package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;

import java.util.Random;

/**
 * Minijuego "Batalla contra Mecha-Kaiju" (60 s): toca los misiles para destruirlos, mantén presionado para
 * cargar el aliento atómico y suéltalo para disparar. Tiempo de juego propio (hit-stop y cámara lenta).
 * Sin asignaciones en onDraw: todo vive en arreglos y objetos reutilizables.
 */
public class BatallaView extends View {
    public static final int DURACION_S = 60;
    private static final float CARGA_S = 1.3f;          // segundos para carga completa
    private static final float DANO_CARGA_COMPLETA = 22f; // % de vida del jefe
    private static final float DANO_ALIENTO_MAX = 34f;
    private static final float DANO_MISIL = 6f, DANO_MISIL_FURIA = 8f, DANO_LASER = 12f;
    private static final float CARGA_MIN = 0.12f;
    private static final float HITSTOP_S = 0.08f;
    private static final float RAYO_DUR = 0.45f;
    private static final float LASER_AVISO = 1.0f, LASER_DUR = 0.3f;
    private static final float FIN_DUR = 1.0f, FIN_LENTA = 0.6f;
    private static final int MAX_MISILES = 8, MAX_EXPL = 10, MAX_EDIF = 20, MAX_FUEGOS = 7;
    private static final int CAL = 0xFF3DE0FF;
    private static final int[] RAYO_COLS = {0x203DE0FF, 0x403DE0FF, 0x803DE0FF, 0xFF3DE0FF, 0xFFFFFFFF};
    private static final float[] RAYO_MULT = {3.2f, 2.2f, 1.5f, 1f, 0.4f};
    private static final String[] NUM = new String[61];
    private static final String[] NUM_HP = new String[61];
    static {
        for (int i = 0; i < NUM.length; i++) {
            NUM[i] = "-" + i + "%";
            NUM_HP[i] = "-" + i + " HP";
        }
    }

    public interface Oyente {
        void progreso(int segundos, int jefePct, int vidaPropia);
        void misilDestruido();
        void impacto();
        void disparo(boolean critico);
        void fin(int puntaje, boolean gano);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final Particulas part = new Particulas();
    private final Random azar = new Random();

    // misiles
    private final boolean[] mViv = new boolean[MAX_MISILES];
    private final float[] mX = new float[MAX_MISILES], mY = new float[MAX_MISILES], mAng = new float[MAX_MISILES];
    private final float[] mVel = new float[MAX_MISILES], mEdad = new float[MAX_MISILES], mHumo = new float[MAX_MISILES];
    // explosiones
    private final float[] xX = new float[MAX_EXPL], xY = new float[MAX_EXPL], xT = new float[MAX_EXPL];
    private final float[] xTam = new float[MAX_EXPL];
    // escena
    private final float[] edX = new float[MAX_EDIF], edW = new float[MAX_EDIF], edH = new float[MAX_EDIF];
    private final int[] fuego = new int[MAX_FUEGOS];
    private LinearGradient cielo, suelo;

    private Apariencia apariencia;
    private Oyente oyente;
    private boolean jugando;
    private long ultimoNanos;
    private float w, h, horizonte, tam, kaijuX, kaijuSuelo, bocaX, bocaY, objX, objY;
    private float bossX, bossY, bossBaseY, u;

    // estado de juego
    private float tiempo, bossVida, vidaPropia, carga, enfriamiento, proxMisil, proxLaser;
    private float hitStop, temblor, destelloRojo, golpeBoss, rayoT, rayoCarga, escalaTiempo = 1f, finT, chispas;
    private float laserT; // <0 inactivo; 0..AVISO aviso; AVISO..AVISO+DUR disparo
    private boolean laserDano, furia, cargando, critico, ganando, finalizando, finEnviado, rayoAliento;
    private int punteroCarga = -1, ultimoSeg = -1, ultimoPct = -1, ultimaVida = -1;
    private float ultimoProg;

    public BatallaView(Context c) { this(c, null); }

    public BatallaView(Context c, AttributeSet a) {
        super(c, a);
        setContentDescription("Área de juego: toca los misiles y mantén presionado para cargar el aliento");
    }

    public void setApariencia(Apariencia a) {
        apariencia = a.conAnimo(EstadoAnimo.NORMAL);
        recalcular();
        invalidate();
    }

    public void setOyente(Oyente o) { oyente = o; }
    public boolean isJugando() { return jugando && !finalizando; }

    public void iniciar() {
        for (int i = 0; i < MAX_MISILES; i++) mViv[i] = false;
        for (int i = 0; i < MAX_EXPL; i++) xT[i] = 9f;
        part.limpiar();
        tiempo = 0;
        bossVida = 100f;
        vidaPropia = 100f;
        carga = 0;
        enfriamiento = 0;
        proxMisil = 1.2f;
        proxLaser = 9f;
        hitStop = temblor = destelloRojo = golpeBoss = chispas = 0;
        rayoT = RAYO_DUR;
        escalaTiempo = 1f;
        finT = 0;
        laserT = -1f;
        furia = cargando = ganando = finalizando = finEnviado = rayoAliento = false;
        punteroCarga = -1;
        ultimoSeg = ultimoPct = ultimaVida = -1;
        ultimoProg = 0;
        ultimoNanos = 0;
        jugando = true;
        notificar(true);
        invalidate();
    }

    /** Detiene la ronda y devuelve el puntaje parcial. */
    public int detener() {
        jugando = false;
        return puntaje(false);
    }

    /** Aliento Atómico de reserva: disparo instantáneo de daño máximo (una vez). */
    public boolean alientoMaximo() {
        if (!isJugando()) return false;
        dispararAliento(DANO_ALIENTO_MAX, true, true);
        return true;
    }

    private int puntaje(boolean gano) {
        int dano = Math.round(100f - bossVida);
        int bonus = gano ? Math.round((DURACION_S - tiempo) * 2f) : 0;
        return Math.max(0, dano + Math.round(vidaPropia) + bonus);
    }

    // ---------------------------------------------------------------- geometría

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        super.onSizeChanged(nw, nh, ow, oh);
        w = nw;
        h = nh;
        horizonte = h * 0.56f;
        cielo = new LinearGradient(0, 0, 0, horizonte, new int[]{0xFF14082B, 0xFF4A1B50, 0xFFC2452D, 0xFFFF9A4A},
                new float[]{0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP);
        suelo = new LinearGradient(0, horizonte, 0, h, 0xFF2A1220, 0xFF07040C, Shader.TileMode.CLAMP);
        Random r = new Random(7);
        float x = -w * 0.02f;
        for (int i = 0; i < MAX_EDIF; i++) {
            edX[i] = x;
            edW[i] = w * (0.04f + r.nextFloat() * 0.07f);
            edH[i] = h * (0.05f + r.nextFloat() * 0.13f);
            x += edW[i] * 0.92f;
        }
        for (int i = 0; i < MAX_FUEGOS; i++) fuego[i] = 1 + i * 3 % MAX_EDIF;
        bossX = w * 0.73f;
        bossBaseY = h * 0.27f;
        u = Math.min(h * 0.32f, w * 0.62f) / 140f;
        recalcular();
    }

    private void recalcular() {
        if (w == 0) return;
        kaijuX = w * 0.17f;
        kaijuSuelo = h * 1.0f;
        if (apariencia == null) {
            tam = h * 0.42f;
            bocaX = w * 0.3f;
            bocaY = h * 0.7f;
        } else {
            tam = h * 0.27f / apariencia.escalaEtapa();
            PointF b = KaijuPainter.boca(kaijuX, kaijuSuelo, tam, apariencia);
            bocaX = b.x;
            bocaY = b.y;
        }
        objX = kaijuX + tam * 0.1f;
        objY = bocaY + tam * 0.28f;
    }

    // ---------------------------------------------------------------- entrada

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isJugando()) return true;
        int a = e.getActionMasked();
        int idx = e.getActionIndex();
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            float x = e.getX(idx), y = e.getY(idx);
            if (golpearMisil(x, y)) return true;
            if (golpearLaser(x, y)) return true;
            if (!cargando && enfriamiento <= 0) {
                cargando = true;
                punteroCarga = e.getPointerId(idx);
                carga = 0;
            }
        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_POINTER_UP || a == MotionEvent.ACTION_CANCEL) {
            if (cargando && (a == MotionEvent.ACTION_CANCEL || e.getPointerId(idx) == punteroCarga)) {
                soltar(a != MotionEvent.ACTION_CANCEL);
            }
        }
        return true;
    }

    private boolean golpearMisil(float x, float y) {
        float radio = w * 0.09f;
        for (int i = 0; i < MAX_MISILES; i++) {
            if (!mViv[i]) continue;
            float dx = x - mX[i], dy = y - mY[i];
            if (dx * dx + dy * dy < radio * radio) {
                destruirMisil(i);
                if (oyente != null) oyente.misilDestruido();
                return true;
            }
        }
        return false;
    }

    /** Un toque cerca de la línea de aviso interrumpe el láser del jefe. */
    private boolean golpearLaser(float x, float y) {
        if (laserT < 0 || laserT > LASER_AVISO) return false;
        float x0 = bossX - 40 * u, y0 = bossY - 55 * u;
        float vx = objX - x0, vy = objY - y0, l2 = vx * vx + vy * vy;
        float s = Math.max(0f, Math.min(1f, ((x - x0) * vx + (y - y0) * vy) / l2));
        float dx = x - (x0 + vx * s), dy = y - (y0 + vy * s);
        if (dx * dx + dy * dy > w * 0.1f * w * 0.1f) return false;
        interrumpirLaser();
        return true;
    }

    private void interrumpirLaser() {
        laserT = -1f;
        part.emitir(Particulas.Tipo.CHISPA, bossX - 40 * u, bossY - 55 * u, 14, u * 5);
        part.texto("¡INTERRUMPIDO!", bossX, bossY - 85 * u, 0xFFFFE066, w * 0.05f);
        if (oyente != null) oyente.misilDestruido();
    }

    private void soltar(boolean disparar) {
        cargando = false;
        punteroCarga = -1;
        float c = carga;
        carga = 0;
        kaiju.setCargaPlacas(0);
        if (!disparar || c < CARGA_MIN || finalizando) return;
        dispararAliento(DANO_CARGA_COMPLETA * c, c >= 0.98f, false);
        enfriamiento = 0.25f;
    }

    private void dispararAliento(float dano, boolean crit, boolean reserva) {
        rayoT = 0;
        rayoCarga = Math.min(1f, dano / DANO_CARGA_COMPLETA);
        critico = crit;
        rayoAliento = reserva;
        if (laserT >= 0 && laserT < LASER_AVISO) laserT = -1f; // aturde al jefe: cancela el aviso
        bossVida = Math.max(0f, bossVida - dano);
        golpeBoss = 0.25f;
        float gx = bossX - 10 * u, gy = bossY - 10 * u;
        part.emitir(Particulas.Tipo.ATOMICO, gx, gy, 10 + (int) (rayoCarga * 14), u * 6);
        part.emitir(Particulas.Tipo.CHISPA, gx, gy, 10, u * 5);
        part.texto(NUM[Math.min(60, Math.round(dano))], gx, gy - 30 * u, CAL, w * (0.05f + 0.04f * rayoCarga));
        if (crit) part.texto("¡CRÍTICO!", gx, gy - 52 * u, 0xFFFFE066, w * 0.075f);
        temblor = Math.max(temblor, 0.4f + 0.5f * rayoCarga);
        if (dano >= DANO_CARGA_COMPLETA * 0.45f) hitStop = HITSTOP_S;
        if (oyente != null) oyente.disparo(crit);
    }

    // ---------------------------------------------------------------- simulación

    private void actualizar(float dt) {
        if (finalizando) {
            actualizarFin(dt);
            return;
        }
        tiempo += dt;
        enfriamiento = Math.max(0f, enfriamiento - dt);
        rayoT += dt;
        golpeBoss = Math.max(0f, golpeBoss - dt);
        destelloRojo = Math.max(0f, destelloRojo - dt * 2.2f);
        if (cargando) {
            carga = Math.min(1f, carga + dt / CARGA_S);
            kaiju.setCargaPlacas(carga);
            if (azar.nextFloat() < 0.5f) {
                part.emitir(Particulas.Tipo.ATOMICO, bocaX + (azar.nextFloat() - 0.5f) * tam * 0.4f,
                        bocaY + (azar.nextFloat() - 0.5f) * tam * 0.3f, 1, tam * 0.012f);
            }
        }
        if (!furia && bossVida <= 50f) activarFuria();
        if (furia) {
            chispas -= dt;
            if (chispas <= 0) {
                chispas = 0.14f;
                part.emitir(Particulas.Tipo.CHISPA, bossX + (azar.nextFloat() - 0.5f) * 60 * u,
                        bossY + (azar.nextFloat() - 0.3f) * 70 * u, 2, u * 4);
            }
        }
        proxMisil -= dt;
        if (proxMisil <= 0 && laserT < 0) {
            lanzarMisil();
            proxMisil = (furia ? 0.8f : 1.4f) + azar.nextFloat() * 0.5f;
        }
        actualizarMisiles(dt);
        actualizarLaser(dt);
        for (int i = 0; i < MAX_EXPL; i++) xT[i] += dt;
        if (bossVida <= 0f) iniciarFin(true);
        else if (vidaPropia <= 0f) iniciarFin(false);
        else if (tiempo >= DURACION_S) iniciarFin(false);
        notificar(false);
    }

    private void activarFuria() {
        furia = true;
        temblor = 1f;
        part.texto("¡ENFURECIDO!", bossX, bossY - 85 * u, 0xFFFF5A3C, w * 0.07f);
        part.emitir(Particulas.Tipo.CHISPA, bossX, bossY, 22, u * 6);
    }

    private void lanzarMisil() {
        for (int i = 0; i < MAX_MISILES; i++) {
            if (mViv[i]) continue;
            mViv[i] = true;
            mX[i] = bossX - 10 * u;
            mY[i] = bossY - 50 * u;
            mAng[i] = (float) (-Math.PI * 0.55 + (azar.nextFloat() - 0.5f) * 0.9f); // sale hacia arriba
            mVel[i] = w * (furia ? 0.5f : 0.36f) * (0.9f + azar.nextFloat() * 0.25f);
            mEdad[i] = 0;
            mHumo[i] = 0;
            part.emitir(Particulas.Tipo.HUMO, mX[i], mY[i], 3, u * 4);
            return;
        }
    }

    private void actualizarMisiles(float dt) {
        float giro = (furia ? 2.4f : 1.8f) * dt;
        for (int i = 0; i < MAX_MISILES; i++) {
            if (!mViv[i]) continue;
            mEdad[i] += dt;
            float deseado = (float) Math.atan2(objY - mY[i], objX - mX[i]);
            float d = deseado - mAng[i];
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            if (mEdad[i] > 0.25f) mAng[i] += Math.max(-giro, Math.min(giro, d));
            mX[i] += (float) Math.cos(mAng[i]) * mVel[i] * dt;
            mY[i] += (float) Math.sin(mAng[i]) * mVel[i] * dt;
            mHumo[i] -= dt;
            if (mHumo[i] <= 0) {
                mHumo[i] = 0.05f;
                part.emitir(Particulas.Tipo.HUMO, mX[i], mY[i], 1, u * 2.5f);
            }
            float dx = objX - mX[i], dy = objY - mY[i];
            if (dx * dx + dy * dy < (tam * 0.28f) * (tam * 0.28f)) impactoEnJugador(i);
        }
    }

    private void actualizarLaser(float dt) {
        if (laserT < 0) {
            proxLaser -= dt;
            if (proxLaser <= 0) {
                laserT = 0;
                laserDano = false;
                proxLaser = (furia ? 8f : 12f) + azar.nextFloat() * 3f;
            }
            return;
        }
        laserT += dt;
        if (laserT >= LASER_AVISO && !laserDano) {
            laserDano = true;
            recibirDano(DANO_LASER);
            part.emitir(Particulas.Tipo.CHISPA, objX, objY, 16, tam * 0.02f);
        }
        if (laserT >= LASER_AVISO + LASER_DUR) laserT = -1f;
    }

    private void destruirMisil(int i) {
        mViv[i] = false;
        explosion(mX[i], mY[i], w * 0.09f);
        part.emitir(Particulas.Tipo.ESCOMBRO, mX[i], mY[i], 8, u * 5);
        part.emitir(Particulas.Tipo.CHISPA, mX[i], mY[i], 8, u * 5);
        part.emitir(Particulas.Tipo.HUMO, mX[i], mY[i], 4, u * 6);
    }

    private void impactoEnJugador(int i) {
        mViv[i] = false;
        explosion(mX[i], mY[i], w * 0.13f);
        part.emitir(Particulas.Tipo.ESCOMBRO, mX[i], mY[i], 10, u * 6);
        recibirDano(furia ? DANO_MISIL_FURIA : DANO_MISIL);
    }

    private void recibirDano(float d) {
        vidaPropia = Math.max(0f, vidaPropia - d);
        temblor = Math.max(temblor, 0.8f);
        destelloRojo = 1f;
        hitStop = Math.max(hitStop, HITSTOP_S);
        part.texto(NUM_HP[Math.min(60, Math.round(d))], objX, objY - tam * 0.3f,
                0xFFFF5A5A, w * 0.055f);
        if (oyente != null) oyente.impacto();
    }

    private void explosion(float x, float y, float tamPx) {
        int k = 0;
        for (int i = 0; i < MAX_EXPL; i++) {
            if (xT[i] > 0.7f) { k = i; break; }
            if (xT[i] > xT[k]) k = i;
        }
        xX[k] = x;
        xY[k] = y;
        xT[k] = 0;
        xTam[k] = tamPx;
    }

    private void iniciarFin(boolean gano) {
        finalizando = true;
        ganando = gano;
        finT = 0;
        cargando = false;
        kaiju.setCargaPlacas(0);
        for (int i = 0; i < MAX_MISILES; i++) mViv[i] = false;
        laserT = -1f;
        notificar(true);
        if (gano) {
            temblor = 1f;
            explosion(bossX, bossY, w * 0.5f);
            part.emitir(Particulas.Tipo.ESCOMBRO, bossX, bossY, 30, u * 8);
            part.emitir(Particulas.Tipo.CHISPA, bossX, bossY, 30, u * 8);
        }
    }

    private void actualizarFin(float dtReal) {
        finT += dtReal;
        if (ganando) {
            if (azar.nextFloat() < 0.35f) {
                explosion(bossX + (azar.nextFloat() - 0.5f) * 90 * u, bossY + (azar.nextFloat() - 0.5f) * 110 * u,
                        w * (0.12f + 0.2f * azar.nextFloat()));
                part.emitir(Particulas.Tipo.ESCOMBRO, bossX, bossY, 4, u * 7);
                part.emitir(Particulas.Tipo.HUMO, bossX, bossY, 3, u * 8);
            }
            bossY += dtReal * h * 0.05f;
        }
        for (int i = 0; i < MAX_EXPL; i++) xT[i] += dtReal;
        if (finT >= FIN_DUR && !finEnviado) {
            finEnviado = true;
            jugando = false;
            if (oyente != null) oyente.fin(puntaje(ganando), ganando);
        }
    }

    private void notificar(boolean forzar) {
        if (oyente == null) return;
        int seg = Math.max(0, DURACION_S - (int) tiempo);
        int pct = Math.round(bossVida);
        int vida = Math.round(vidaPropia);
        if (!forzar && seg == ultimoSeg && pct == ultimoPct && vida == ultimaVida) return;
        ultimoSeg = seg;
        ultimoPct = pct;
        ultimaVida = vida;
        oyente.progreso(seg, pct, vida);
    }

    // ---------------------------------------------------------------- dibujo

    @Override
    protected void onDraw(Canvas c) {
        if (w == 0) return;
        long ahora = System.nanoTime();
        float dtReal = ultimoNanos == 0 ? 0f : Math.min(0.05f, (ahora - ultimoNanos) / 1e9f);
        ultimoNanos = ahora;
        float tAnim = ahora / 1e9f;
        float dt = 0f;
        if (hitStop > 0) hitStop = Math.max(0f, hitStop - dtReal);
        else {
            escalaTiempo = finalizando && ganando && finT < FIN_LENTA ? 0.3f : 1f;
            dt = dtReal * escalaTiempo;
        }
        if (jugando) actualizar(finalizando ? dtReal : dt);
        temblor = Math.max(0f, temblor - dtReal * 2.2f);

        c.save();
        if (temblor > 0) {
            float a = temblor * temblor * w * 0.025f;
            c.translate((float) Math.sin(tAnim * 83) * a, (float) Math.cos(tAnim * 97) * a);
        }
        dibujarEscena(c, tAnim);
        if (!(finalizando && ganando && finT > 0.5f)) dibujarJefe(c, tAnim);
        if (laserT >= 0) dibujarLaser(c, tAnim);
        dibujarKaiju(c, tAnim);
        if (rayoT < RAYO_DUR) dibujarRayo(c);
        for (int i = 0; i < MAX_MISILES; i++) if (mViv[i]) dibujarMisil(c, i, tAnim);
        for (int i = 0; i < MAX_EXPL; i++) if (xT[i] < 0.7f) dibujarExplosion(c, i);
        part.dibujar(c, hitStop > 0 ? 0f : (finalizando ? dtReal * escalaTiempo : dt));
        c.restore();
        dibujarInterfaz(c);
        if (jugando) postInvalidateOnAnimation();
    }

    private void dibujarEscena(Canvas c, float t) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(cielo);
        c.drawRect(-w * 0.05f, -h * 0.05f, w * 1.05f, horizonte + 1, p);
        p.setShader(null);
        // sol rojizo entre el humo
        p.setColor(0x55FFC070);
        c.drawCircle(w * 0.3f, horizonte - h * 0.05f, w * 0.22f, p);
        p.setColor(0x88FFD890);
        c.drawCircle(w * 0.3f, horizonte - h * 0.05f, w * 0.1f, p);
        // columnas de humo
        for (int i = 0; i < MAX_FUEGOS; i++) {
            int e = fuego[i];
            float bx = edX[e] + edW[e] * 0.5f, by = horizonte - edH[e];
            for (int k = 0; k < 4; k++) {
                float f = (t * 0.25f + k * 0.25f + i * 0.13f) % 1f;
                p.setColor(((int) (70 * (1f - f)) << 24) | 0x1A0E16);
                c.drawCircle(bx + (float) Math.sin(t + i + k) * w * 0.02f + f * w * 0.05f,
                        by - f * h * 0.2f, w * (0.025f + 0.05f * f), p);
            }
        }
        p.setColor(0xFF1B0A1E);
        for (int i = 0; i < MAX_EDIF; i++) {
            c.drawRect(edX[i], horizonte - edH[i], edX[i] + edW[i], horizonte + 1, p);
        }
        // ventanas encendidas y fuegos
        p.setColor(0xCCFFB44A);
        for (int i = 0; i < MAX_EDIF; i += 2) {
            for (int k = 1; k < 4; k++) {
                c.drawRect(edX[i] + edW[i] * 0.25f, horizonte - edH[i] * (k / 4f) - h * 0.008f,
                        edX[i] + edW[i] * 0.4f, horizonte - edH[i] * (k / 4f), p);
            }
        }
        for (int i = 0; i < MAX_FUEGOS; i++) {
            int e = fuego[i];
            float bx = edX[e] + edW[e] * 0.5f, by = horizonte - edH[e];
            float fl = 0.8f + 0.25f * (float) Math.sin(t * 11 + i * 2);
            p.setColor(0x66FF5A1A);
            c.drawCircle(bx, by, w * 0.035f * fl, p);
            p.setColor(0xCCFF9A2A);
            c.drawCircle(bx, by - w * 0.008f, w * 0.022f * fl, p);
            p.setColor(0xFFFFE27A);
            c.drawCircle(bx, by - w * 0.012f, w * 0.01f * fl, p);
        }
        p.setShader(suelo);
        c.drawRect(-w * 0.05f, horizonte, w * 1.05f, h + 2, p);
        p.setShader(null);
        p.setColor(0x33FF7A3A); // reflejo del incendio en el asfalto
        c.drawRect(0, horizonte, w, horizonte + h * 0.015f, p);
    }

    private void dibujarKaiju(Canvas c, float t) {
        if (apariencia == null) return;
        float mand = rayoT < RAYO_DUR ? 0.9f : (cargando ? 0.3f + 0.4f * carga : 0f);
        kaiju.dibujar(c, kaijuX, kaijuSuelo, tam, apariencia, t, mand, false);
        if (cargando && carga > 0.05f) {
            float r = tam * (0.08f + 0.22f * carga) * (0.9f + 0.1f * (float) Math.sin(t * 25));
            p.setStyle(Paint.Style.FILL);
            for (int k = 3; k >= 0; k--) {
                p.setColor(((int) ((30 + 40 * carga) * (4 - k) / 4f) << 24) | 0x3DE0FF);
                c.drawCircle(bocaX, bocaY, r * (1f + k * 0.55f), p);
            }
            p.setColor(0xFFFFFFFF);
            c.drawCircle(bocaX, bocaY, r * 0.35f, p);
        }
    }

    private void dibujarRayo(Canvas c) {
        float f = rayoT / RAYO_DUR;
        float env = (float) Math.sin(Math.min(1f, f * 1.15f) * Math.PI);
        float base = w * (0.035f + 0.06f * rayoCarga) * env;
        float x1 = bossX - 5 * u, y1 = bossY - 5 * u;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 5; i++) {
            p.setColor(RAYO_COLS[i]);
            p.setStrokeWidth(base * RAYO_MULT[i]);
            c.drawLine(bocaX, bocaY, x1, y1, p);
        }
        p.setStyle(Paint.Style.FILL);
        for (int k = 3; k >= 0; k--) { // bloom en el punto de impacto
            p.setColor(((int) (60 * env) << 24) | (k == 0 ? 0xFFFFFF : 0x3DE0FF));
            c.drawCircle(x1, y1, base * (1.2f + k * 1.3f), p);
        }
        if (rayoAliento) {
            p.setColor(((int) (120 * env) << 24) | 0xFFFFFF);
            c.drawRect(0, 0, w, h, p);
        }
    }

    private void dibujarMisil(Canvas c, int i, float t) {
        float l = w * 0.065f, g = l * 0.22f;
        c.save();
        c.translate(mX[i], mY[i]);
        c.rotate((float) Math.toDegrees(mAng[i]));
        p.setStyle(Paint.Style.FILL);
        float fl = 0.7f + 0.3f * (float) Math.sin(t * 40 + i);
        p.setColor(0x66FF8A2A);
        c.drawCircle(-l * 0.7f, 0, g * 2.2f * fl, p);
        p.setColor(0xFFFFD25A);
        c.drawCircle(-l * 0.55f, 0, g * 1.1f * fl, p);
        p.setColor(0xFFC9D0DA);
        rf.set(-l * 0.5f, -g, l * 0.4f, g);
        c.drawRoundRect(rf, g, g, p);
        p.setColor(0xFFE0392B);
        path.reset();
        path.moveTo(l * 0.4f, -g);
        path.lineTo(l * 0.75f, 0);
        path.lineTo(l * 0.4f, g);
        path.close();
        c.drawPath(path, p);
        p.setColor(0xFF5B6472);
        c.drawRect(-l * 0.5f, -g * 1.9f, -l * 0.3f, g * 1.9f, p);
        c.restore();
        p.setStyle(Paint.Style.STROKE); // aro de objetivo para indicar que se puede tocar
        p.setStrokeWidth(Math.max(2f, w * 0.004f));
        p.setColor(0x66FF6A5A);
        c.drawCircle(mX[i], mY[i], w * 0.075f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void dibujarExplosion(Canvas c, int i) {
        float f = xT[i] / 0.7f;
        float r = xTam[i] * (0.35f + f);
        float a = 1f - f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(((int) (140 * a) << 24) | 0xFF4A1A);
        c.drawCircle(xX[i], xY[i], r, p);
        p.setColor(((int) (200 * a) << 24) | 0xFF9A2A);
        c.drawCircle(xX[i], xY[i], r * 0.7f, p);
        p.setColor(((int) (255 * a) << 24) | 0xFFE98A);
        c.drawCircle(xX[i], xY[i], r * 0.4f, p);
        p.setColor(((int) (255 * Math.max(0f, 1f - f * 2.5f)) << 24) | 0xFFFFFF);
        c.drawCircle(xX[i], xY[i], r * 0.25f, p);
    }

    private void dibujarLaser(Canvas c, float t) {
        float x0 = bossX - 40 * u, y0 = bossY - 55 * u;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        if (laserT < LASER_AVISO) { // línea de aviso parpadeante
            float parp = 0.5f + 0.5f * (float) Math.sin(t * 28);
            p.setStrokeWidth(Math.max(2f, w * 0.006f));
            p.setColor(((int) (90 + 140 * parp) << 24) | 0xFF2A2A);
            c.drawLine(x0, y0, objX, objY, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (60 + 100 * laserT) << 24) | 0xFF2A2A);
            c.drawCircle(x0, y0, w * 0.03f * (0.6f + laserT), p);
            return;
        }
        float f = (laserT - LASER_AVISO) / LASER_DUR;
        float ancho = w * 0.05f * (1f - f * 0.5f);
        p.setColor(0x44FF2A2A);
        p.setStrokeWidth(ancho * 2.5f);
        c.drawLine(x0, y0, objX, objY, p);
        p.setColor(0xCCFF4A4A);
        p.setStrokeWidth(ancho);
        c.drawLine(x0, y0, objX, objY, p);
        p.setColor(0xFFFFFFFF);
        p.setStrokeWidth(ancho * 0.3f);
        c.drawLine(x0, y0, objX, objY, p);
        p.setStyle(Paint.Style.FILL);
    }

    // ---------------------------------------------------------------- el Mecha-Kaiju (diseño original)

    private void r(Canvas c, float x0, float y0, float x1, float y1, float rad, int col) {
        rf.set(x0, y0, x1, y1);
        p.setColor(col);
        c.drawRoundRect(rf, rad, rad, p);
    }

    private void dibujarJefe(Canvas c, float t) {
        if (!(finalizando && ganando)) bossY = bossBaseY + (float) Math.sin(t * 2f) * h * 0.012f;
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * (furia ? 9f : 4f));
        float flama = 0.75f + 0.25f * (float) Math.sin(t * 38);
        c.save();
        c.translate(bossX, bossY);
        c.scale(-u, u); // mira hacia la izquierda (hacia el kaiju); +x local = hacia delante
        p.setStyle(Paint.Style.FILL);
        // propulsores traseros y llama bajo los pies
        for (int k = 3; k >= 1; k--) {
            p.setColor(((54 * (4 - k)) << 24) | (furia ? 0xFF3A1A : 0x4AB4FF));
            c.drawCircle(-58, -8, 8 * k * flama, p);
            c.drawCircle(-10, 82, 6 * k * flama, p);
            c.drawCircle(12, 82, 6 * k * flama, p);
        }
        p.setColor(0xFFFFFFFF);
        c.drawCircle(-58, -8, 5 * flama, p);
        // alas / mochila de reactores
        p.setColor(0xFF4C5563);
        path.reset();
        path.moveTo(-26, -36);
        path.lineTo(-62, -62);
        path.lineTo(-66, -4);
        path.lineTo(-26, 12);
        path.close();
        c.drawPath(path, p);
        r(c, -66, -16, -50, 2, 5, 0xFF2F3541);
        // piernas
        r(c, -20, 24, -4, 70, 5, 0xFF59626F);
        r(c, 4, 24, 20, 70, 5, 0xFF59626F);
        r(c, -24, 66, 0, 78, 4, 0xFF39414D);
        r(c, 0, 66, 24, 78, 4, 0xFF39414D);
        // torso
        r(c, -30, -42, 30, 32, 10, 0xFF8C96A4);
        r(c, -22, -34, 22, 8, 6, 0xFFB7BFCB);
        r(c, -22, 12, 22, 28, 4, 0xFF4A5260);
        // núcleo del pecho
        int nuc = furia ? 0xFFFF4A2A : 0xFFFF8A3A;
        p.setColor((((int) (60 + 70 * pulso)) << 24) | (nuc & 0xFFFFFF));
        c.drawCircle(0, -14, 17 + 4 * pulso, p);
        p.setColor(nuc);
        c.drawCircle(0, -14, 8, p);
        p.setColor(0xFFFFF0B0);
        c.drawCircle(0, -14, 3.5f, p);
        // hombrera con lanzamisiles
        r(c, -8, -58, 26, -40, 5, 0xFF59626F);
        p.setColor(0xFF1C2028);
        for (int k = 0; k < 3; k++) c.drawCircle(-2 + k * 10, -49, 3.4f, p);
        // brazo-cañón frontal
        r(c, 20, -34, 40, -8, 6, 0xFF6F7886);
        r(c, 34, -22, 56, -12, 3, 0xFF2F3541);
        // cabeza
        r(c, 6, -86, 40, -52, 9, 0xFF7C8695);
        path.reset();
        path.moveTo(8, -86);
        path.lineTo(-6, -104);
        path.lineTo(20, -86);
        path.close();
        p.setColor(0xFFB7BFCB);
        c.drawPath(path, p); // cresta
        // visor rojo con brillo
        p.setColor((((int) (50 + 60 * pulso)) << 24) | 0xFF1A1A);
        c.drawRoundRect(14, -76, 48, -58, 8, 8, p);
        r(c, 20, -72, 40, -62, 4, furia ? 0xFFFF3A2A : 0xFFE0242A);
        r(c, 24, -70, 36, -66, 2, 0xFFFFC2B0);
        // grietas de blindaje (furia)
        if (furia) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.4f);
            p.setColor(0xFF1A1114);
            path.reset();
            path.moveTo(-18, -38); path.lineTo(-8, -22); path.lineTo(-16, -10); path.lineTo(-4, 4);
            path.moveTo(14, -30); path.lineTo(6, -16); path.lineTo(18, -2);
            path.moveTo(24, -80); path.lineTo(16, -66);
            c.drawPath(path, p);
            p.setColor(0x99FF7A2A);
            p.setStrokeWidth(1.2f);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
        }
        // destello blanco al recibir daño
        if (golpeBoss > 0) {
            p.setColor(((int) (190 * golpeBoss / 0.25f) << 24) | 0xFFFFFF);
            c.drawRoundRect(-30, -42, 30, 32, 10, 10, p);
            c.drawRoundRect(6, -86, 40, -52, 9, 9, p);
        }
        c.restore();
    }

    // ---------------------------------------------------------------- interfaz dentro del lienzo

    private void barra(Canvas c, float x, float y, float ancho, float alto, float frac, int col, boolean derecha) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAA000000);
        rf.set(x - 3, y - 3, x + ancho + 3, y + alto + 3);
        c.drawRoundRect(rf, alto, alto, p);
        p.setColor(col);
        float f = Math.max(0f, Math.min(1f, frac));
        if (f > 0) {
            if (derecha) rf.set(x + ancho * (1f - f), y, x + ancho, y + alto);
            else rf.set(x, y, x + ancho * f, y + alto);
            c.drawRoundRect(rf, alto / 2, alto / 2, p);
        }
    }

    private void dibujarInterfaz(Canvas c) {
        float m = w * 0.04f, bh = h * 0.016f;
        barra(c, m, m + bh, w - 2 * m, bh, bossVida / 100f, furia ? 0xFFFF3A2A : 0xFFE0A02A, true);
        p.setTextSize(w * 0.034f);
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.LEFT);
        p.setFakeBoldText(true);
        c.drawText("MECHA-KAIJU", m, m + bh * 0.2f, p);
        barra(c, m, h - m - bh * 1.4f, w * 0.5f, bh, vidaPropia / 100f, 0xFF4BD36A, false);
        if (cargando) {
            barra(c, m, h - m - bh * 3.6f, w * 0.5f, bh * 0.8f, carga, CAL, false);
        }
        p.setFakeBoldText(false);
        if (destelloRojo > 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (120 * destelloRojo) << 24) | 0xFF0000);
            c.drawRect(0, 0, w, h, p);
        }
        if (finalizando && ganando && finT > 0.5f) {
            p.setColor(((int) (200 * Math.max(0f, 1f - (finT - 0.5f) * 2f)) << 24) | 0xFFFFFF);
            c.drawRect(0, 0, w, h, p);
        }
    }
}
