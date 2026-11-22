package com.cesba.godzipet.vista;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.SparseArray;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.RutaEvolucion;

/**
 * Dibuja al kaiju con trazos vectoriales (estilo caricatura, contorno grueso) siguiendo el linaje
 * japonés de Toho: Baby (1993), Little (1994), Junior (1995), Showa, Heisei, Burning y Shin.
 * Sistema local: 100 unidades = tam px, origen en el suelo entre los pies, mirando a la derecha.
 *
 * Nota de tamaño: el archivo supera las 800 líneas (~1150) porque concentra, en un solo painter sin
 * dependencias nuevas, siete formas distintas, diez estados de ánimo y la secuencia de radiación;
 * se partió en métodos pequeños y no se reserva ningún objeto dentro de {@link #dibujar}.
 */
public final class KaijuPainter {
    private static final int CONTORNO = 0xFF0B0F14;
    private static final int DIENTE = 0xFFF4EEDC;
    private static final int CIAN = 0xFF3DE0FF;
    private static final int ROJO_CALIENTE = 0xFFFF3B1F;
    private static final float ANCHO_TRAZO = 2.0f;

    // formas (índices de las tablas)
    private static final int F_BEBE = 0, F_PEQ = 1, F_JUN = 2, F_SHOWA = 3, F_HEISEI = 4, F_BURN = 5, F_SHIN = 6;
    private static final float[] CAB_ESC = {1.5f, 1.28f, 1.1f, 1.05f, 1f, 1f, 0.95f};
    private static final float[] HOCICO = {0.45f, 0.6f, 0.9f, 0.82f, 0.88f, 0.95f, 1.08f};

    // cola y placas
    private static final int COLA_N = 14;
    private static final int[] COLA_PLACAS = {2, 4, 6, 8, 10, 12};
    private static final float[][] ANCLAS_CUERPO = {
            {3, -84, -0.2f, -1f}, {-4, -76, -0.6f, -0.9f}, {-12, -66, -0.85f, -0.7f},
            {-18, -54, -1f, -0.45f}, {-22, -42, -1f, -0.35f}};
    private static final float[] ALTO_PLACA = {12, 16, 18, 17, 14, 13, 11, 9, 7.5f, 6, 4.5f};
    private static final int N_PLACAS = ANCLAS_CUERPO.length + COLA_PLACAS.length;

    // grietas y parches (pares x,y)
    private static final float[] CR_TORSO1 = {-15, -60, -9, -54, -13, -48, -6, -42, -10, -35, -5, -29};
    private static final float[] CR_TORSO2 = {7, -64, 3, -57, 8, -51, 4, -45, 9, -39};
    private static final float[] CR_PIERNA = {2, -34, 6, -28, 3, -22, 7, -16};
    private static final float[] CR_CABEZA = {16, -93, 20, -89, 18, -86};
    private static final float[][] PARCHES = {{-10, -55, 7}, {4, -40, 6}, {-16, -34, 5}, {8, -62, 4}, {12, -26, 4}};
    private static final float[][] VAPOR = {{-2, -86}, {8, -82}, {-10, -76}, {-18, -64}, {14, -92}};
    private static final float[][] MANCHAS = {{-10, -40, 4}, {4, -28, 3}, {10, -60, 2.5f}, {-16, -58, 3}};
    private static final float[][] RAYAS = {{-15, -52, -6, -49}, {-18, -42, -6, -38}, {-15, -31, -5, -28}};
    private static final float[][] MOTAS = {{-8, -46, 4}, {9, -36, 5}, {-12, -22, 3.5f}, {6, -14, 3}, {12, -52, 2.5f},
            {-3, -58, 2.2f}, {14, -24, 2.2f}, {-15, -36, 2.6f}};

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aux = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final SparseArray<RadialGradient> halos = new SparseArray<>();
    private final Shader sombraH, sombraCola, sombraCabeza, sombraHuevo, parcheCaliente;

    /** Rasgos propios de cada forma (se reutiliza una sola instancia). */
    private static final class Perfil {
        int forma, cuerpo, vientre, placa, placaBorde, glow, ojo, pupila;
        float gord, cabEsc, hocico, craneoAlto, placaEsc, colaLargo, colaGrosor, brazoEsc, piernaG;
        float ojoRx, ojoRy, barriga;
        int placaForma, dientes, ojoTipo;

        void configurar(int f) {
            forma = f;
            gord = 1f; craneoAlto = 0f; placaEsc = 1f; placaForma = 1; colaLargo = 1f; colaGrosor = 1f;
            brazoEsc = 1f; piernaG = 0f; dientes = 1; ojoTipo = 0; ojoRx = 4.4f; ojoRy = 3.4f; barriga = 0f;
            glow = CIAN; pupila = 0xFF1A0E05;
            cabEsc = CAB_ESC[f];
            hocico = HOCICO[f];
            switch (f) {
                case F_BEBE:
                    colores(0xFF7FA36B, 0xFFD3E4A8, 0xFFD9E8C0, 0xFF8FA877, 0xFF6B4423);
                    gord = 1.18f; craneoAlto = -5f; placaEsc = 0.45f; placaForma = 0; colaLargo = 0.7f;
                    colaGrosor = 1.1f; brazoEsc = 0.8f; dientes = 0; ojoRx = 6.3f; ojoRy = 6.1f; ojoTipo = 1;
                    barriga = 1f; pupila = 0xFF140A04;
                    break;
                case F_PEQ:
                    colores(0xFF8DB87A, 0xFFDCEBB4, 0xFFE4EFCB, 0xFF94B07C, 0xFF7A5226);
                    gord = 1.22f; craneoAlto = -3f; placaEsc = 0.6f; placaForma = 0; colaLargo = 0.8f;
                    colaGrosor = 1.15f; brazoEsc = 0.85f; dientes = 0; ojoRx = 5.6f; ojoRy = 5.4f; ojoTipo = 1;
                    barriga = 1f;
                    break;
                case F_JUN:
                    colores(0xFF4E5D4A, 0xFF7D8C76, 0xFFE6E2CF, 0xFF9C977F, 0xFFFFB300);
                    gord = 0.9f; placaEsc = 0.85f; placaForma = 2;
                    break;
                case F_SHOWA:
                    colores(0xFF3E4A3C, 0xFF8A9684, 0xFFE6E2CF, 0xFF9E9A84, 0xFFFFD66B);
                    gord = 1.12f; craneoAlto = -2f; placaEsc = 1.05f; placaForma = 1; piernaG = 2f;
                    ojoRx = 5.1f; ojoRy = 4.5f; ojoTipo = 2;
                    break;
                case F_HEISEI:
                    colores(0xFF2C2F33, 0xFF4C5259, 0xFFD9D4C0, 0xFF8F8A78, 0xFFFFE45C);
                    placaEsc = 1.3f; placaForma = 3; dientes = 2; ojoRx = 4.6f; ojoRy = 2.7f; ojoTipo = 3;
                    break;
                case F_BURN:
                    colores(0xFF2A201E, 0xFF4A3832, 0xFFFF8A2A, 0xFF8A2E0C, 0xFFFFE040);
                    glow = 0xFFFF7A1A; gord = 1.05f; placaEsc = 1.3f; placaForma = 3; dientes = 2;
                    ojoRx = 4.6f; ojoRy = 2.9f; ojoTipo = 3;
                    break;
                default: // SHIN
                    colores(0xFF3A1F1C, 0xFF6B3B33, 0xFF4A3558, 0xFF1E1226, 0xFFF2F2F2);
                    glow = 0xFF7A3DFF; gord = 0.95f; placaEsc = 1.15f; placaForma = 4; colaLargo = 1.3f;
                    colaGrosor = 1.25f; brazoEsc = 0.5f; piernaG = 2f; dientes = 3; ojoRx = 2.3f; ojoRy = 2.3f;
                    ojoTipo = 4;
                    break;
            }
        }

        private void colores(int c, int v, int p, int pb, int o) {
            cuerpo = c; vientre = v; placa = p; placaBorde = pb; ojo = o;
        }
    }

    private final Perfil pf = new Perfil();

    // estado de la pasada actual
    private float miradaX, miradaY, cargaPlacas;
    private float t, brilloRad, calor, hocD;
    private boolean sil, apagado, rayasSerieActiva;
    private EstadoAnimo animo = EstadoAnimo.NORMAL;
    private int cu, vi, os, pl, pb, glowC, ojoC, pupC;
    private float tmpX, tmpY;

    private final float[] colaX = new float[COLA_N + 1], colaY = new float[COLA_N + 1];
    private final float[] colaNx = new float[COLA_N + 1], colaNy = new float[COLA_N + 1];
    private final float[] colaW = new float[COLA_N + 1];
    private final float[] ax = new float[N_PLACAS], ay = new float[N_PLACAS];
    private final float[] anx = new float[N_PLACAS], any = new float[N_PLACAS];

    /** Dirección de la mirada / cabeza hacia el dedo del jugador, cada eje en [-1, 1]. */
    public void setMirada(float dx, float dy) {
        miradaX = Math.max(-1, Math.min(1, dx));
        miradaY = Math.max(-1, Math.min(1, dy));
    }

    /** Secuencia de carga del aliento: 0 = apagado, 1 = todas las placas encendidas (de la cola a la cabeza). */
    public void setCargaPlacas(float p) {
        cargaPlacas = Math.max(0, Math.min(1, p));
    }

    public KaijuPainter() {
        trazo.setStyle(Paint.Style.STROKE);
        trazo.setStrokeJoin(Paint.Join.ROUND);
        trazo.setStrokeCap(Paint.Cap.ROUND);
        trazo.setColor(CONTORNO);
        relleno.setStyle(Paint.Style.FILL);
        sombraH = new LinearGradient(-30, 0, 20, 0, 0x88000000, 0x00000000, Shader.TileMode.CLAMP);
        sombraCola = new LinearGradient(0, -45, 0, 0, 0x00000000, 0x66000000, Shader.TileMode.CLAMP);
        sombraCabeza = new LinearGradient(0, -100, 0, -68, 0x00000000, 0x66000000, Shader.TileMode.CLAMP);
        sombraHuevo = new LinearGradient(-26, 0, 26, 0, 0x00000000, 0x77000000, Shader.TileMode.CLAMP);
        parcheCaliente = new RadialGradient(0, 0, 1, new int[]{0xFFFFB000, ROJO_CALIENTE, 0x00FF3B1F},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------------ utilidades de color y geometría

    static int mezclar(int a, int b, float t) {
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.argb(255, r, g, bl);
    }

    private static int gris(int c, float k) {
        int l = (Color.red(c) * 30 + Color.green(c) * 59 + Color.blue(c) * 11) / 100;
        return mezclar(c, Color.rgb(l, l, l), k);
    }

    private static float acotar(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static int formaDe(Apariencia a) {
        switch (a.etapa) {
            case BEBE: return F_BEBE;
            case PEQUENO: return F_PEQ;
            case JUNIOR: return F_JUN;
            case FINAL:
                if (a.ruta == RutaEvolucion.HEISEI) return F_HEISEI;
                if (a.ruta == RutaEvolucion.BURNING) return F_BURN;
                if (a.ruta == RutaEvolucion.SHIN) return F_SHIN;
                return F_SHOWA;
            default: return F_SHOWA;
        }
    }

    /** x de la cabeza ajustada al largo del hocico (sólo afecta de x=18 hacia la nariz). */
    private float hx(float x) {
        return x + hocD * acotar((x - 18f) / 29f, 0f, 1f);
    }

    private void rot(float x, float y, float px, float py, float grados) {
        double r = Math.toRadians(grados);
        float dx = x - px, dy = y - py;
        tmpX = (float) (px + dx * Math.cos(r) - dy * Math.sin(r));
        tmpY = (float) (py + dx * Math.sin(r) + dy * Math.cos(r));
    }

    // ------------------------------------------------------------------ preparación por cuadro

    private static float brilloDeRad(float r, float t) {
        if (r < 20f) return 0f;
        if (r < 40f) return (r - 20f) / 20f * 0.35f;
        if (r < 70f) return 0.4f + 0.15f * (float) Math.sin(t * 1.8f);
        if (r < 85f) {
            float k = (r - 70f) / 15f;
            return 0.4f + 0.35f * k + 0.15f * (float) Math.sin(t * (1.8f + k * 4f));
        }
        return 0.85f + 0.15f * (float) Math.sin(t * 9f);
    }

    private void preparar(Apariencia a, float tiempo, boolean silueta) {
        t = tiempo;
        sil = silueta;
        animo = a.animo;
        rayasSerieActiva = a.rayasSerie() && !silueta;
        pf.configurar(formaDe(a));
        hocD = (pf.hocico - 1f) * 28f;
        float r = a.radiacion;
        apagado = r < 20f || animo == EstadoAnimo.DEBIL;
        brilloRad = apagado ? 0f : brilloDeRad(r, tiempo);
        float pulso = 0.8f + 0.2f * (float) Math.sin(tiempo * 9f);
        calor = acotar((r - 80f) / 20f, 0f, 1f) * pulso;
        if (animo == EstadoAnimo.FUSION) calor = Math.max(calor, 0.55f);
        if (silueta) {
            cu = vi = os = pl = pb = glowC = ojoC = pupC = Color.WHITE;
            return;
        }
        int c1 = pf.cuerpo, v1 = pf.vientre, p1 = pf.placa, b1 = pf.placaBorde;
        if (apagado) {
            c1 = gris(c1, 0.4f); v1 = gris(v1, 0.4f);
            p1 = mezclar(gris(p1, 0.5f), 0xFF7D8288, 0.5f); b1 = mezclar(b1, 0xFF555A60, 0.5f);
        }
        if (animo == EstadoAnimo.ENFERMO) {
            c1 = mezclar(c1, 0xFFA7C77E, 0.4f); v1 = mezclar(v1, 0xFFD4E6A5, 0.45f);
        } else if (animo == EstadoAnimo.DORMIDO) {
            c1 = mezclar(c1, 0xFF101A33, 0.2f); v1 = mezclar(v1, 0xFF101A33, 0.15f);
        }
        float rojo = 0.38f * calor;
        if (rojo > 0.01f) {
            c1 = mezclar(c1, ROJO_CALIENTE, rojo); v1 = mezclar(v1, 0xFFFF6A3A, rojo);
        }
        cu = c1; vi = v1; pl = p1; pb = b1;
        os = mezclar(c1, CONTORNO, 0.35f);
        glowC = pf.glow;
        ojoC = animo == EstadoAnimo.FUSION ? 0xFFFF5A2A : pf.ojo;
        pupC = pf.pupila;
    }

    // ------------------------------------------------------------------ entrada principal

    /**
     * @param cx        centro horizontal en pantalla
     * @param suelo     coordenada Y del suelo
     * @param tam       tamaño en px de 100 unidades
     * @param t         tiempo en segundos (animación)
     * @param mandibula apertura extra de la mandíbula 0-1 (rugir/comer)
     * @param silueta   todo en blanco puro (destello de evolución)
     */
    public void dibujar(Canvas c, float cx, float suelo, float tam, Apariencia a, float t, float mandibula,
                        boolean silueta) {
        if (a.etapa == Etapa.HUEVO) {
            dibujarHuevo(c, cx, suelo, tam, a, t, silueta);
            return;
        }
        float u = tam / 100f * a.escalaEtapa();
        preparar(a, t, silueta);
        float vel = animo == EstadoAnimo.DORMIDO ? 1.4f : 2.2f;
        float respira = (float) Math.sin(t * vel);
        float rebote = animo == EstadoAnimo.FELIZ ? -Math.abs((float) Math.sin(t * 5f)) * 4f : 0f;
        trazo.setColor(silueta ? Color.WHITE : CONTORNO);
        trazo.setStrokeWidth(ANCHO_TRAZO);

        c.save();
        c.translate(cx, suelo);
        c.scale(u, u);
        c.translate(0, rebote);
        calcularCola(t);
        dibujarAura(c);
        dibujarPlacas(c);
        dibujarCola(c);
        dibujarPierna(c, true);
        dibujarTorso(c, respira);
        dibujarPierna(c, false);
        dibujarBrazo(c);
        dibujarCabeza(c, mandibula, respira);
        if (!silueta) dibujarVapor(c);
        c.restore();
        trazo.setColor(CONTORNO);
    }

    private void rellenar(Canvas c, Path p, int color, Shader sombra) {
        relleno.setShader(null);
        relleno.setColor(color);
        c.drawPath(p, relleno);
        if (sombra != null && !sil) {
            relleno.setShader(sombra);
            c.drawPath(p, relleno);
            relleno.setShader(null);
        }
        c.drawPath(p, trazo);
    }

    private Shader haloShader(int color) {
        RadialGradient g = halos.get(color);
        if (g == null) {
            int base = color & 0x00FFFFFF;
            g = new RadialGradient(0, 0, 1, new int[]{color | 0xFF000000, base | 0x70000000, base},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
            halos.put(color, g);
        }
        return g;
    }

    private void halo(Canvas c, float x, float y, float r, Shader s, float alfa) {
        if (alfa <= 0.01f || sil) return;
        aux.setStyle(Paint.Style.FILL);
        aux.setShader(s);
        aux.setAlpha((int) (255 * acotar(alfa, 0f, 1f)));
        c.save();
        c.translate(x, y);
        c.scale(r, r);
        c.drawCircle(0, 0, 1f, aux);
        c.restore();
        aux.setShader(null);
        aux.setAlpha(255);
    }

    // ------------------------------------------------------------------ cola

    private void calcularCola(float t) {
        float amp = 5f, caida = 0f, vel = 2.2f;
        if (animo == EstadoAnimo.DORMIDO) { amp = 1.5f; caida = 3f; }
        else if (animo == EstadoAnimo.DEBIL || animo == EstadoAnimo.CANSADO) { amp = 2f; caida = 6f; }
        else if (animo == EstadoAnimo.TRISTE) { amp = 2.5f; caida = 4f; }
        else if (animo == EstadoAnimo.FELIZ) { amp = 9f; vel = 4.2f; }
        for (int i = 0; i <= COLA_N; i++) {
            float s = i / (float) COLA_N;
            float fase = t * vel - s * 3.4f;
            colaX[i] = -12f - 52f * pf.colaLargo * s + (float) Math.cos(fase) * amp * 0.25f * s;
            colaY[i] = -31f + 25f * (0.5f * s + 0.5f * s * s) + caida * s
                    + (float) Math.sin(fase) * amp * s * (0.4f + 0.6f * s);
            colaW[i] = pf.colaGrosor * 9.5f * (1f - s) * (1f - 0.25f * s) + 0.8f;
        }
        for (int i = 0; i <= COLA_N; i++) {
            int i0 = Math.max(0, i - 1), i1 = Math.min(COLA_N, i + 1);
            float tx = colaX[i1] - colaX[i0], ty = colaY[i1] - colaY[i0];
            float len = (float) Math.sqrt(tx * tx + ty * ty) + 1e-4f;
            colaNx[i] = -ty / len;
            colaNy[i] = tx / len;
        }
    }

    private void dibujarCola(Canvas c) {
        path.reset();
        for (int i = 0; i <= COLA_N; i++) {
            float x = colaX[i] + colaNx[i] * colaW[i], y = colaY[i] + colaNy[i] * colaW[i];
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        for (int i = COLA_N; i >= 0; i--) {
            path.lineTo(colaX[i] - colaNx[i] * colaW[i], colaY[i] - colaNy[i] * colaW[i]);
        }
        path.close();
        rellenar(c, path, cu, sombraCola);
        if (sil) return;
        // franja clara del vientre de la cola
        path.reset();
        for (int i = 1; i < COLA_N - 1; i++) {
            float x = colaX[i] - colaNx[i] * colaW[i] * 0.55f, y = colaY[i] - colaNy[i] * colaW[i] * 0.55f;
            if (i == 1) path.moveTo(x, y); else path.lineTo(x, y);
        }
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(2f);
        aux.setColor(mezclar(vi, cu, 0.4f));
        c.drawPath(path, aux);
        decorarCola(c);
        aux.setStyle(Paint.Style.FILL);
    }

    /** Rayas de la serie animada, grietas de Shin y parches de lava de Burning sobre la cola. */
    private void decorarCola(Canvas c) {
        if (rayasSerieActiva) {
            aux.setColor(0xFF7B2D3B);
            aux.setStrokeWidth(2.4f);
            for (int i = 2; i < COLA_N - 2; i += 2) {
                c.drawLine(colaX[i] + colaNx[i] * colaW[i] * 0.8f, colaY[i] + colaNy[i] * colaW[i] * 0.8f,
                        colaX[i] - colaNx[i] * colaW[i] * 0.3f, colaY[i] - colaNy[i] * colaW[i] * 0.3f, aux);
            }
        }
        float pulso = 0.65f + 0.35f * (float) Math.sin(t * 3f);
        if (pf.forma == F_SHIN) {
            aux.setStrokeWidth(1.2f);
            aux.setColor(0xFFFF4A2E);
            aux.setAlpha((int) (255 * pulso));
            for (int i = 2; i < COLA_N - 2; i += 2) {
                float k = (i % 4 == 0) ? 0.5f : -0.1f;
                c.drawLine(colaX[i] + colaNx[i] * colaW[i] * 0.5f, colaY[i] + colaNy[i] * colaW[i] * 0.5f,
                        colaX[i + 1] + colaNx[i + 1] * colaW[i + 1] * k, colaY[i + 1] + colaNy[i + 1] * colaW[i + 1] * k, aux);
            }
            aux.setAlpha(255);
        } else if (pf.forma == F_BURN) {
            for (int i = 3; i < COLA_N - 3; i += 3) parche(c, colaX[i], colaY[i], colaW[i] * 0.9f + 1.5f, pulso);
        }
    }

    // ------------------------------------------------------------------ placas dorsales

    private void construirAnclas() {
        for (int i = 0; i < ANCLAS_CUERPO.length; i++) {
            float[] a = ANCLAS_CUERPO[i];
            ax[i] = -2f + (a[0] + 2f) * pf.gord;
            ay[i] = a[1];
            float ny = a[3] - 0.35f, len = (float) Math.sqrt(a[2] * a[2] + ny * ny);
            anx[i] = a[2] / len;
            any[i] = ny / len;
        }
        for (int k = 0; k < COLA_PLACAS.length; k++) {
            int j = ANCLAS_CUERPO.length + k, i = COLA_PLACAS[k];
            ax[j] = colaX[i] + colaNx[i] * colaW[i];
            ay[j] = colaY[i] + colaNy[i] * colaW[i];
            float ny = colaNy[i] - 0.3f, len = (float) Math.sqrt(colaNx[i] * colaNx[i] + ny * ny);
            anx[j] = colaNx[i] / len;
            any[j] = ny / len;
        }
    }

    /** Aura difusa tras el cuerpo (radiación alta, carga del aliento y fusión). */
    private void dibujarAura(Canvas c) {
        if (sil) return;
        float g = Math.max(brilloRad, cargaPlacas);
        halo(c, -14, -56, 52f, haloShader(glowC), 0.22f * g);
        if (animo == EstadoAnimo.FUSION) halo(c, -6, -56, 60f, haloShader(ROJO_CALIENTE), 0.3f * calor);
    }

    private void dibujarPlacas(Canvas c) {
        construirAnclas();
        int claro = mezclar(glowC, Color.WHITE, 0.35f);
        for (int i = 0; i < N_PLACAS; i++) {
            // la placa 0 es la de la nuca; la última, la de la cola: se encienden de la cola a la cabeza
            float lit = acotar(cargaPlacas * (N_PLACAS + 1) - (N_PLACAS - 1 - i), 0f, 1f);
            float g = Math.max(brilloRad, lit);
            float h = ALTO_PLACA[i] * pf.placaEsc;
            float w = h * 0.62f + 2f;
            float mx = ax[i] + anx[i] * h * 0.5f, my = ay[i] + any[i] * h * 0.5f;
            halo(c, mx, my, h * 1.4f + 4f, haloShader(glowC), 0.55f * g);
            int fill = sil ? Color.WHITE : mezclar(pl, claro, g * 0.75f);
            int borde = sil ? Color.WHITE : mezclar(pb, glowC, g * 0.6f);
            dibujarPlaca(c, ax[i], ay[i], anx[i], any[i], h, w, fill, borde);
        }
    }

    private void trazarPlaca(float h, float w) {
        float a = w / 2f;
        path.reset();
        switch (pf.placaForma) {
            case 0: // redondeada (Baby / Little)
                path.moveTo(-a, 0);
                path.cubicTo(-a * 1.1f, -h * 0.95f, a * 1.1f, -h * 0.95f, a, 0);
                break;
            case 1: // hoja de arce (Showa)
                path.moveTo(-a, 0);
                path.lineTo(-a * 1.15f, -h * 0.38f); path.lineTo(-a * 0.55f, -h * 0.42f);
                path.lineTo(-a * 0.8f, -h * 0.78f); path.lineTo(-a * 0.25f, -h * 0.65f);
                path.lineTo(0, -h);
                path.lineTo(a * 0.25f, -h * 0.65f); path.lineTo(a * 0.8f, -h * 0.78f);
                path.lineTo(a * 0.55f, -h * 0.42f); path.lineTo(a * 1.15f, -h * 0.38f);
                path.lineTo(a, 0);
                break;
            case 2: // afilada (Junior)
                path.moveTo(-a, 0);
                path.quadTo(-a * 0.2f, -h * 0.5f, 0, -h);
                path.quadTo(a * 0.2f, -h * 0.5f, a, 0);
                break;
            case 3: // alta y dentada (Heisei / Burning)
                path.moveTo(-a, 0);
                path.lineTo(-a * 0.85f, -h * 0.5f); path.lineTo(-a * 0.35f, -h * 0.4f);
                path.lineTo(0, -h);
                path.lineTo(a * 0.3f, -h * 0.45f); path.lineTo(a * 0.8f, -h * 0.55f);
                path.lineTo(a, 0);
                break;
            default: // irregular (Shin)
                path.moveTo(-a, 0);
                path.lineTo(-a * 0.9f, -h * 0.55f); path.lineTo(-a * 0.5f, -h * 0.35f);
                path.lineTo(-a * 0.35f, -h); path.lineTo(0, -h * 0.5f);
                path.lineTo(a * 0.25f, -h * 0.8f); path.lineTo(a * 0.55f, -h * 0.3f);
                path.lineTo(a, 0);
                break;
        }
        path.close();
    }

    private void dibujarPlaca(Canvas c, float x, float y, float nx, float ny, float h, float w, int fill, int borde) {
        float ang = (float) Math.toDegrees(Math.atan2(nx, -ny));
        c.save();
        c.translate(x - nx * 1.5f, y - ny * 1.5f);
        c.rotate(ang);
        trazo.setStrokeWidth(ANCHO_TRAZO * 0.8f);
        trazarPlaca(h + 1.5f, w);
        relleno.setShader(null);
        relleno.setColor(pf.placaForma == 3 ? borde : fill);
        c.drawPath(path, relleno);
        c.drawPath(path, trazo);
        if (pf.placaForma == 3) { // segunda capa de la placa Heisei
            c.scale(0.68f, 0.78f);
            trazarPlaca(h + 1.5f, w);
            relleno.setColor(fill);
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        } else if (!sil && pf.placaForma != 0) {
            aux.setStyle(Paint.Style.STROKE);
            aux.setStrokeWidth(0.7f);
            aux.setColor(borde);
            c.drawLine(0, -h * 0.15f, 0, -h * 0.7f, aux);
            aux.setStyle(Paint.Style.FILL);
        }
        trazo.setStrokeWidth(ANCHO_TRAZO);
        c.restore();
    }

    // ------------------------------------------------------------------ torso y piernas

    private void dibujarTorso(Canvas c, float respira) {
        c.save();
        c.scale(pf.gord * (1f + 0.012f * respira), 1f + 0.02f * respira, -2f, -14f);
        path.reset();
        path.moveTo(-18, -22);
        path.cubicTo(-27, -46, -14, -68, 2, -70);
        path.cubicTo(16, -72, 22, -50, 18, -34);
        path.cubicTo(16, -21, 4, -14, -8, -14);
        path.cubicTo(-14, -14, -17, -17, -18, -22);
        path.close();
        rellenar(c, path, cu, sombraH);
        float b = pf.barriga * 3f;
        path.reset();
        path.moveTo(4, -66);
        path.cubicTo(15 + b, -60, 19 + b, -40, 13 + b * 0.6f, -20);
        path.cubicTo(10, -17, 5, -19, 3, -23);
        path.cubicTo(7, -40, 7, -54, 2, -64);
        path.close();
        rellenar(c, path, vi, null);
        if (!sil && pf.forma != F_SHIN) {
            aux.setStyle(Paint.Style.STROKE);
            aux.setStrokeWidth(0.9f);
            aux.setColor(mezclar(vi, CONTORNO, 0.45f));
            for (int i = 0; i < 6; i++) {
                float y = -58 + i * 6.5f;
                c.drawLine(6f + i * 0.3f, y, 14.5f - Math.abs(i - 3) * 0.6f, y + 1.2f, aux);
            }
            aux.setStyle(Paint.Style.FILL);
        }
        if (!sil) decorarTorso(c);
        c.restore();
    }

    private void decorarTorso(Canvas c) {
        float pulso = 0.65f + 0.35f * (float) Math.sin(t * 3f);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        if (rayasSerieActiva) {
            aux.setStrokeWidth(2.4f);
            aux.setColor(0xFF7B2D3B);
            for (float[] r : RAYAS) {
                path.reset();
                path.moveTo(r[0], r[1]);
                path.quadTo((r[0] + r[2]) / 2 + 1.5f, (r[1] + r[3]) / 2 - 2.5f, r[2], r[3]);
                c.drawPath(path, aux);
            }
        }
        if (pf.forma == F_SHIN) {
            grieta(c, CR_TORSO1, 0x55FF4A2E, 0xFFFF4A2E, pulso);
            grieta(c, CR_TORSO2, 0x55FF4A2E, 0xFFFF4A2E, pulso);
        } else if (pf.forma == F_BURN) {
            grieta(c, CR_TORSO2, 0x55FF3B1F, 0xFFFFB000, pulso);
            for (int i = 0; i < PARCHES.length; i++) {
                float p = 0.6f + 0.4f * (float) Math.sin(t * 3f + i * 1.7f);
                parche(c, PARCHES[i][0], PARCHES[i][1], PARCHES[i][2], p);
            }
        }
        aux.setStyle(Paint.Style.FILL);
        if (animo == EstadoAnimo.SUCIO) {
            aux.setColor(0xAA5A3D1E);
            for (float[] m : MANCHAS) c.drawCircle(m[0], m[1], m[2], aux);
        }
    }

    private void parche(Canvas c, float x, float y, float r, float pulso) {
        aux.setStyle(Paint.Style.FILL);
        aux.setShader(parcheCaliente);
        aux.setAlpha((int) (255 * acotar(pulso, 0f, 1f)));
        c.save();
        c.translate(x, y);
        c.scale(r, r);
        c.drawCircle(0, 0, 1f, aux);
        c.restore();
        aux.setShader(null);
        aux.setAlpha(255);
    }

    private void grieta(Canvas c, float[] pts, int ancho, int nucleo, float pulso) {
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeWidth(3.4f);
        aux.setColor(ancho);
        c.drawPath(path, aux);
        aux.setStrokeWidth(1.1f);
        aux.setColor(nucleo);
        aux.setAlpha((int) (255 * acotar(pulso, 0f, 1f)));
        c.drawPath(path, aux);
        aux.setAlpha(255);
    }

    private void dibujarPierna(Canvas c, boolean trasera) {
        c.save();
        c.scale(pf.gord, 1f, -2f, 0f);
        float g = pf.piernaG;
        if (trasera) {
            rect.set(-20 - g, -32, -2, -4);
            path.reset();
            path.addOval(rect, Path.Direction.CW);
            int col = sil ? Color.WHITE : mezclar(cu, CONTORNO, 0.25f);
            rellenar(c, path, col, null);
            pie(c, -22 - g, -6, 20 + g, col);
        } else {
            path.reset();
            path.moveTo(-4 - g, -30);
            path.cubicTo(-4 - g, -40, 16 + g, -40, 18 + g, -28);
            path.cubicTo(19 + g, -20, 14 + g, -10, 12 + g, -5);
            path.lineTo(2 - g * 0.3f, -5);
            path.cubicTo(2 - g, -12, -4 - g, -18, -4 - g, -30);
            path.close();
            rellenar(c, path, cu, sombraH);
            if (!sil && pf.forma == F_SHIN) {
                grieta(c, CR_PIERNA, 0x55FF4A2E, 0xFFFF4A2E, 0.5f + 0.5f * (float) Math.sin(t * 3f));
            }
            pie(c, -1 - g, -6, 24 + g, cu);
        }
        c.restore();
    }

    private void pie(Canvas c, float x, float y, float largo, int color) {
        rect.set(x, y, x + largo, y + 6);
        path.reset();
        path.addRoundRect(rect, 3, 3, Path.Direction.CW);
        rellenar(c, path, color, null);
        relleno.setShader(null);
        relleno.setColor(sil ? Color.WHITE : DIENTE);
        for (int i = 0; i < 3; i++) {
            float gx = x + largo - 1 + i * 0.2f, gy = y + 1 + i * 2f;
            path.reset();
            path.moveTo(gx, gy);
            path.lineTo(gx + 3.5f, gy + 1.2f);
            path.lineTo(gx, gy + 2f);
            path.close();
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        }
    }

    // ------------------------------------------------------------------ brazo

    private void dibujarBrazo(Canvas c) {
        float ang = (float) Math.sin(t * 1.8f + 0.5f) * 5f;
        if (animo == EstadoAnimo.FELIZ) ang = -30f * Math.abs((float) Math.sin(t * 5f));
        else if (animo == EstadoAnimo.DORMIDO || animo == EstadoAnimo.DEBIL) ang = 9f;
        else if (pf.forma == F_SHIN) ang = (float) Math.sin(t * 6f) * 6f;
        float es = pf.brazoEsc;
        c.save();
        c.translate(6f * pf.gord, -58f);
        c.rotate(ang);
        c.scale(es, es);
        trazo.setStrokeWidth(ANCHO_TRAZO / es);
        path.reset();
        path.moveTo(0, 0);
        path.cubicTo(9, 1, 16, 5, 19, 12);
        path.lineTo(15, 14);
        path.cubicTo(11, 9, 6, 7, -1, 7);
        path.close();
        rellenar(c, path, cu, null);
        relleno.setShader(null);
        relleno.setColor(sil ? Color.WHITE : DIENTE);
        int garras = pf.forma == F_SHIN ? 2 : 3;
        for (int i = 0; i < garras; i++) {
            float x = 17.5f + i * 1.3f, y = 12.5f + i * 0.9f;
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + 1.6f, y + 3.4f);
            path.lineTo(x - 0.8f, y + 1.4f);
            path.close();
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        }
        trazo.setStrokeWidth(ANCHO_TRAZO);
        c.restore();
    }

    // ------------------------------------------------------------------ cabeza

    private float aperturaPorAnimo() {
        if (animo == EstadoAnimo.FELIZ) return 0.45f;
        if (animo == EstadoAnimo.HAMBRIENTO) return 0.2f;
        if (pf.forma == F_SHIN) return 0.22f;
        return 0f;
    }

    private float inclinacionBase() {
        float base;
        switch (animo) {
            case DORMIDO: base = 16f; break;
            case DEBIL: base = 9f; break;
            case TRISTE: base = 7f; break;
            case CANSADO: base = 6f; break;
            case ENFERMO: base = 5f; break;
            case HAMBRIENTO: base = 3f; break;
            default: base = (float) Math.sin(t * 1.1f) * 2f;
        }
        return base + (pf.forma == F_SHIN ? 5f : 0f);
    }

    private void dibujarCabeza(Canvas c, float mandibula, float respira) {
        float apertura = Math.max(mandibula, aperturaPorAnimo());
        float angulo = inclinacionBase() + acotar(miradaY, -1f, 1f) * 12f;
        float cab = pf.cabEsc;
        c.save();
        c.translate(miradaX * 1.2f, -0.8f * respira);
        c.rotate(angulo, 6, -68);
        c.scale(cab, cab, 10, -70);
        dibujarCuello(c);
        dibujarMandibula(c, apertura);
        dibujarCraneo(c, apertura);
        if (!sil) dibujarRostro(c);
        c.restore();
    }

    private void dibujarCuello(Canvas c) {
        path.reset();
        path.moveTo(-4, -64);
        path.cubicTo(-4, -80, 2, -88, 13, -89);
        path.lineTo(20, -73);
        path.cubicTo(14, -68, 8, -62, 4, -58);
        path.close();
        rellenar(c, path, cu, sombraH);
    }

    private void interiorBoca(Canvas c, float ang) {
        relleno.setShader(null);
        relleno.setColor(0xFF5E1020);
        path.reset();
        path.moveTo(12, -75);
        path.lineTo(hx(42), -76);
        rot(hx(40), -73, 12, -74, ang);
        path.lineTo(tmpX, tmpY);
        path.close();
        c.drawPath(path, relleno);
        relleno.setColor(0xFFE0566E); // lengua
        rot(28, -72, 12, -74, ang * 0.7f);
        c.drawOval(tmpX - 7, tmpY - 2, tmpX + 5, tmpY + 2, relleno);
    }

    private void dibujarMandibula(Canvas c, float apertura) {
        float ang = apertura * 26f;
        if (ang > 1f && !sil) interiorBoca(c, ang);
        c.save();
        c.rotate(ang, 12, -74);
        path.reset();
        path.moveTo(10, -75);
        path.lineTo(hx(40), -76);
        path.cubicTo(hx(43), -75, hx(42), -71, hx(37), -70);
        path.cubicTo(hx(28), -68, 18, -67, 11, -69);
        path.close();
        rellenar(c, path, sil ? Color.WHITE : mezclar(vi, cu, 0.25f), null);
        if (!sil && pf.forma == F_SHIN) { // mandíbula partida
            aux.setStyle(Paint.Style.STROKE);
            aux.setStrokeWidth(1.1f);
            aux.setColor(CONTORNO);
            c.drawLine(hx(31), -75.5f, hx(33), -68.8f, aux);
            aux.setStyle(Paint.Style.FILL);
        }
        if (ang > 4f && !sil) {
            if (pf.dientes >= 2) fila(c, -75.2f, false, 3.4f, 3f, 0f, false);
            if (pf.dientes >= 1) fila(c, -75.2f, false, pf.dientes >= 2 ? 3.4f : 4.2f, 2.8f, 1.7f, pf.dientes == 3);
        }
        c.restore();
    }

    private void dibujarCraneo(Canvas c, float apertura) {
        float ry = pf.craneoAlto;
        path.reset();
        path.moveTo(4, -82);
        path.cubicTo(6, -95 + ry, 18, -99 + ry, hx(28), -95 + ry);
        path.cubicTo(hx(37), -92, hx(45), -86, hx(47), -80);
        path.cubicTo(hx(47), -77, hx(45), -76, hx(41), -76);
        path.lineTo(18, -74.5f);
        path.cubicTo(10, -73, 4, -76, 4, -82);
        path.close();
        rellenar(c, path, cu, sombraCabeza);
        if (sil) return;
        if (animo == EstadoAnimo.FUSION && apertura < 0.1f) {
            dientesApretados(c);
        } else if (apertura > 0.15f && pf.dientes >= 1) {
            fila(c, -75.5f, true, pf.dientes >= 2 ? 3.4f : 4.2f, 3f, 0f, pf.dientes == 3);
            if (pf.dientes >= 2) fila(c, -75.5f, true, 3.4f, 2f, 1.7f, false);
        } else if (pf.dientes >= 2) {
            fila(c, -75.6f, true, 5.2f, 1.8f, 0f, pf.dientes == 3); // colmillos asomando
        }
    }

    /** Fila de dientes triangulares sobre el labio; arriba=true cuelgan hacia abajo. */
    private void fila(Canvas c, float y, boolean arriba, float paso, float tam, float desfase, boolean irregular) {
        relleno.setShader(null);
        relleno.setColor(DIENTE);
        trazo.setStrokeWidth(0.5f);
        float xe = hx(40);
        int i = 0;
        for (float x = 18f + desfase; x < xe; x += paso, i++) {
            float h = tam * (irregular ? 0.6f + 0.5f * ((i * 7) % 5) / 4f : 1f);
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + paso * 0.55f, y);
            path.lineTo(x + paso * 0.27f, arriba ? y + h : y - h);
            path.close();
            c.drawPath(path, relleno);
            c.drawPath(path, trazo);
        }
        trazo.setStrokeWidth(ANCHO_TRAZO);
    }

    private void dientesApretados(Canvas c) {
        relleno.setShader(null);
        relleno.setColor(DIENTE);
        float xe = hx(40);
        rect.set(16, -77f, xe, -73.6f);
        c.drawRoundRect(rect, 1.2f, 1.2f, relleno);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeWidth(0.6f);
        aux.setColor(CONTORNO);
        c.drawRoundRect(rect, 1.2f, 1.2f, aux);
        for (float x = 20f; x < xe - 1f; x += 3.2f) c.drawLine(x, -77f, x, -73.6f, aux);
        c.drawLine(16, -75.3f, xe, -75.3f, aux);
        aux.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------------------ rostro: ojo, cejas, detalles

    private void dibujarRostro(Canvas c) {
        float ox = 4f + (hx(47) - 4f) * 0.55f;
        float oy = pf.forma <= F_PEQ ? -85f : -87f;
        detallesCabeza(c);
        dibujarOjo(c, ox, oy);
        dibujarCeja(c, ox, oy);
        boolean cria = pf.forma <= F_PEQ;
        if (animo == EstadoAnimo.FELIZ || cria) {
            relleno.setShader(null);
            relleno.setColor(animo == EstadoAnimo.FELIZ ? 0x88FF6F91 : 0x44FF6F91);
            c.drawCircle(ox + 5f, oy + (cria ? 8f : 6f), cria ? 3.4f : 3f, relleno);
        }
        if (animo == EstadoAnimo.HAMBRIENTO) dibujarBaba(c);
        if (animo == EstadoAnimo.TRISTE) {
            float f = (t * 0.7f) % 1f;
            relleno.setColor(Color.argb((int) (200 * (1f - f)), 150, 210, 255));
            c.drawCircle(ox + pf.ojoRx * 0.4f, oy + pf.ojoRy + 1f + f * 10f, 1.3f, relleno);
        }
    }

    private void detallesCabeza(Canvas c) {
        relleno.setShader(null);
        relleno.setColor(CONTORNO);
        c.drawCircle(hx(44.5f), -81, 0.9f, relleno); // fosa nasal
        if (pf.forma == F_SHOWA || pf.forma == F_HEISEI || pf.forma == F_BURN) {
            rect.set(8f, -86f, 11.5f, -83.2f); // oído
            c.drawOval(rect, relleno);
        }
        if (pf.forma == F_HEISEI || pf.forma == F_BURN || pf.forma == F_JUN) { // cresta de la ceja
            float k = pf.forma == F_JUN ? 0.7f : 1f;
            for (int i = 0; i < 2; i++) {
                float x = 15f + i * 6f, y = -96.5f + pf.craneoAlto - i * 1.2f;
                path.reset();
                path.moveTo(x, y + 1.5f);
                path.lineTo(x - 2.4f * k, y - 3.2f * k);
                path.lineTo(x + 2.6f, y + 0.6f);
                path.close();
                rellenar(c, path, cu, null);
            }
        }
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 3f);
        if (pf.forma == F_SHIN) grieta(c, CR_CABEZA, 0x55FF4A2E, 0xFFFF4A2E, pulso);
        if (pf.forma == F_BURN) parche(c, 14f, -90f, 4f, 0.5f + 0.5f * pulso);
        aux.setStyle(Paint.Style.FILL);
    }

    private float cierreParpado() {
        if (animo == EstadoAnimo.DORMIDO) return 1f;
        if (pf.forma != F_SHIN && (t % 4.2f) < 0.13f) return 1f;
        if (animo == EstadoAnimo.CANSADO) return 0.5f;
        if (animo == EstadoAnimo.ENFERMO) return 0.4f;
        return animo == EstadoAnimo.TRISTE ? 0.15f : 0f;
    }

    private void trazarOjo(float ox, float oy, float rx, float ry) {
        path.reset();
        if (pf.ojoTipo == 3) { // ojo rasgado de gato
            path.moveTo(ox - rx, oy + 0.5f);
            path.quadTo(ox, oy - ry * 1.9f, ox + rx, oy - 0.3f);
            path.quadTo(ox, oy + ry * 1.1f, ox - rx, oy + 0.5f);
            path.close();
        } else {
            rect.set(ox - rx, oy - ry, ox + rx, oy + ry);
            path.addOval(rect, Path.Direction.CW);
        }
    }

    private void dibujarOjo(Canvas c, float ox, float oy) {
        float rx = pf.ojoRx, ry = pf.ojoRy;
        float cierre = cierreParpado();
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(1.6f);
        aux.setColor(CONTORNO);
        if (animo == EstadoAnimo.FELIZ) { // ojo feliz ^
            path.reset();
            path.moveTo(ox - rx, oy + 1);
            path.quadTo(ox, oy - ry * 1.4f - 1.5f, ox + rx, oy + 1);
            c.drawPath(path, aux);
        } else if (cierre >= 0.95f) {
            path.reset();
            path.moveTo(ox - rx, oy);
            path.quadTo(ox, oy + 2.4f, ox + rx, oy);
            c.drawPath(path, aux);
        } else {
            ojoAbierto(c, ox, oy, rx, ry, cierre);
        }
        aux.setStyle(Paint.Style.FILL);
    }

    private void ojoAbierto(Canvas c, float ox, float oy, float rx, float ry, float cierre) {
        trazarOjo(ox, oy, rx, ry);
        c.save();
        c.clipPath(path);
        float px = ox + miradaX * rx * 0.35f;
        float py = oy + miradaY * ry * 0.35f + (animo == EstadoAnimo.TRISTE ? ry * 0.3f : 0f);
        relleno.setShader(null);
        relleno.setColor(pf.ojoTipo == 2 || pf.ojoTipo == 4 ? 0xFFF8F8F0 : ojoC);
        c.drawRect(ox - rx - 1, oy - ry * 2, ox + rx + 1, oy + ry * 2, relleno);
        pupilaYBrillo(c, ox, oy, px, py, ry);
        boolean enojo = pf.ojoTipo == 3 || animo == EstadoAnimo.HAMBRIENTO || animo == EstadoAnimo.FUSION;
        relleno.setColor(cu);
        if (enojo && pf.ojoTipo != 1 && pf.ojoTipo != 4) { // párpado oblicuo
            path.reset();
            path.moveTo(ox - rx - 1, oy - ry * 2);
            path.lineTo(ox + rx + 1, oy - ry * 2);
            path.lineTo(ox + rx + 1, oy - ry * 0.25f);
            path.close();
            c.drawPath(path, relleno);
        }
        if (cierre > 0f) {
            c.drawRect(ox - rx - 1, oy - ry - 1, ox + rx + 1, oy - ry + 2 * ry * cierre, relleno);
        }
        c.restore();
        trazarOjo(ox, oy, rx, ry);
        c.drawPath(path, aux);
    }

    private void pupilaYBrillo(Canvas c, float ox, float oy, float px, float py, float ry) {
        switch (pf.ojoTipo) {
            case 1: // enorme y tierno (Baby / Little)
                relleno.setColor(pupC);
                c.drawCircle(px, py, ry * 0.68f, relleno);
                relleno.setColor(Color.WHITE);
                c.drawCircle(px + ry * 0.28f, py - ry * 0.3f, ry * 0.26f, relleno);
                c.drawCircle(px - ry * 0.25f, py + ry * 0.28f, ry * 0.12f, relleno);
                break;
            case 2: // Showa: iris grande y expresivo
                relleno.setColor(ojoC);
                c.drawCircle(px, py, ry * 0.95f, relleno);
                relleno.setColor(pupC);
                c.drawCircle(px + 0.2f, py, ry * 0.5f, relleno);
                relleno.setColor(Color.WHITE);
                c.drawCircle(px + ry * 0.3f, py - ry * 0.3f, ry * 0.25f, relleno);
                break;
            case 3: // gato: pupila vertical
                relleno.setColor(pupC);
                rect.set(px - 0.8f, oy - ry * 1.6f, px + 0.8f, oy + ry * 1.2f);
                c.drawOval(rect, relleno);
                break;
            case 4: // Shin: ojo muerto, pupila diminuta
                relleno.setColor(pupC);
                c.drawCircle(px, py, 0.7f, relleno);
                break;
            default:
                relleno.setColor(pupC);
                rect.set(px - 1.1f, py - ry + 0.6f, px + 1.1f, py + ry - 0.6f);
                c.drawOval(rect, relleno);
                relleno.setColor(Color.WHITE);
                c.drawCircle(ox + 1.6f, oy - 1.3f, 0.8f, relleno);
        }
    }

    private void dibujarCeja(Canvas c, float ox, float oy) {
        if (animo == EstadoAnimo.FELIZ || animo == EstadoAnimo.DORMIDO) return;
        float rx = pf.ojoRx, ry = pf.ojoRy;
        float base = oy - ry;
        boolean cria = pf.forma <= F_PEQ;
        boolean enojo = (pf.ojoTipo == 3 || animo == EstadoAnimo.HAMBRIENTO || animo == EstadoAnimo.SUCIO
                || animo == EstadoAnimo.FUSION) && !cria;
        float yi, yd; // izquierda (nuca) y derecha (hocico)
        if (animo == EstadoAnimo.TRISTE) { yi = base - 1.2f; yd = base - 4.2f; }
        else if (enojo) { yi = base - 4.4f; yd = base - (pf.ojoTipo == 3 ? 0.6f : 1.4f); }
        else if (pf.forma == F_SHIN) { yi = base - 2.5f; yd = base - 1.5f; }
        else if (cria) { yi = base - 2.4f; yd = base - 2.6f; }
        else { yi = base - 2.6f; yd = base - 2.9f; }
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(pf.ojoTipo == 3 ? 2.8f : cria ? 1.5f : 2.2f);
        aux.setColor(CONTORNO);
        path.reset();
        path.moveTo(ox - rx - 1, yi);
        path.quadTo(ox, (yi + yd) / 2f - 1f, ox + rx + 1, yd);
        c.drawPath(path, aux);
        aux.setStyle(Paint.Style.FILL);
    }

    private void dibujarBaba(Canvas c) {
        float f = (t * 0.9f) % 1f;
        float x = hx(34);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeWidth(1.2f);
        aux.setColor(0xCCBFE8FF);
        c.drawLine(x, -69, x, -69 + 5f + 9f * f, aux);
        aux.setStyle(Paint.Style.FILL);
        c.drawCircle(x, -64f + 9f * f, 1.5f, aux);
    }

    // ------------------------------------------------------------------ vapor

    private void dibujarVapor(Canvas c) {
        boolean fusion = animo == EstadoAnimo.FUSION;
        if (pf.forma != F_BURN && !fusion) return;
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(2.6f);
        int g = fusion ? 0xB0 : 0xD0, b = fusion ? 0xA0 : 0xA0;
        for (int k = 0; k < VAPOR.length; k++) {
            float fase = (t * 0.45f + k * 0.21f) % 1f;
            float yS = VAPOR[k][1] - 34f * fase;
            float x = VAPOR[k][0];
            float ph = t * 2f + k + fase * 5f;
            aux.setColor(Color.argb((int) (140 * Math.sin(fase * Math.PI)), 255, g, b));
            path.reset();
            path.moveTo(x + 5f * (float) Math.sin(ph), yS);
            path.quadTo(x + 9f * (float) Math.sin(ph + 1f), yS - 7f, x + 4f * (float) Math.sin(ph + 2f), yS - 14f);
            c.drawPath(path, aux);
        }
        aux.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------------------ huevo

    private void dibujarHuevo(Canvas c, float cx, float suelo, float tam, Apariencia a, float t, boolean silueta) {
        float u = tam / 100f * a.escalaEtapa();
        float toques = a.toquesHuevo;
        float tambaleo = (float) Math.sin(t * (2 + toques)) * (2 + toques * 2.5f);
        int claro = silueta ? Color.WHITE : a.origen.colorClaro;
        int oscuro = silueta ? Color.WHITE : a.origen.colorOscuro;
        sil = silueta;
        this.t = t;
        trazo.setColor(silueta ? Color.WHITE : CONTORNO);
        c.save();
        c.translate(cx, suelo);
        c.scale(u, u);
        c.rotate(tambaleo, 0, 0);
        trazo.setStrokeWidth(ANCHO_TRAZO);
        path.reset();
        path.moveTo(0, -66);
        path.cubicTo(18, -66, 28, -34, 26, -16);
        path.cubicTo(24, -4, 12, 0, 0, 0);
        path.cubicTo(-12, 0, -24, -4, -26, -16);
        path.cubicTo(-28, -34, -18, -66, 0, -66);
        path.close();
        rellenar(c, path, claro, sombraHuevo);
        if (!silueta) {
            c.save();
            c.clipPath(path);
            // resplandor interior que late
            halo(c, 0, -30, 26f, haloShader(mezclar(claro, Color.WHITE, 0.7f)), 0.2f + 0.15f * (float) Math.sin(t * 2f));
            for (int i = 0; i < MOTAS.length; i++) { // moteado en los colores del origen
                relleno.setShader(null);
                relleno.setColor(i % 2 == 0 ? oscuro : mezclar(oscuro, Color.WHITE, 0.35f));
                c.drawCircle(MOTAS[i][0], MOTAS[i][1], MOTAS[i][2], relleno);
            }
            relleno.setColor(0x55FFFFFF);
            c.drawOval(-17, -58, -9, -40, relleno);
            c.restore();
            dibujarGrietas(c, a.toquesHuevo, claro);
        }
        c.restore();
        trazo.setColor(CONTORNO);
    }

    private void dibujarGrietas(Canvas c, int toques, int claro) {
        trazo.setStrokeWidth(1.3f);
        if (toques >= 1) grietaHuevo(c, claro, -6, -60, -2, -54, -7, -49);
        if (toques >= 2) grietaHuevo(c, claro, 10, -46, 4, -42, 9, -36);
        if (toques >= 3) grietaHuevo(c, claro, -18, -30, -11, -32, -14, -26);
        if (toques >= 4) grietaHuevo(c, claro, -2, -54, 3, -50, 1, -44);
        trazo.setStrokeWidth(ANCHO_TRAZO);
    }

    private void grietaHuevo(Canvas c, int claro, float x0, float y0, float x1, float y1, float x2, float y2) {
        path.reset();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        aux.setStyle(Paint.Style.STROKE);
        aux.setStrokeCap(Paint.Cap.ROUND);
        aux.setStrokeWidth(3.2f);
        aux.setColor(mezclar(claro, Color.WHITE, 0.8f));
        aux.setAlpha((int) (170 + 80 * Math.sin(t * 3f)));
        c.drawPath(path, aux);
        aux.setAlpha(255);
        aux.setStyle(Paint.Style.FILL);
        c.drawPath(path, trazo);
    }

    // ------------------------------------------------------------------ puntos de referencia en pantalla

    /** Punto aproximado de la boca en pantalla (para efectos de comer / aliento). */
    public static PointF boca(float cx, float suelo, float tam, Apariencia a) {
        float u = tam / 100f * a.escalaEtapa();
        if (a.etapa == Etapa.HUEVO) return new PointF(cx, suelo - 30 * u);
        int f = formaDe(a);
        float cab = CAB_ESC[f], d = (HOCICO[f] - 1f) * 28f;
        float bx = 10 + (44 + d - 10) * cab, by = -70 + (-76 + 70) * cab;
        return new PointF(cx + bx * u, suelo + by * u);
    }

    /** Punto aproximado de la cabeza (para burbujas de pensamiento / Zzz). */
    public static PointF cabeza(float cx, float suelo, float tam, Apariencia a) {
        float u = tam / 100f * a.escalaEtapa();
        if (a.etapa == Etapa.HUEVO) return new PointF(cx, suelo - 64 * tam / 100f);
        float cab = CAB_ESC[formaDe(a)];
        return new PointF(cx + (10 + 16 * cab) * u, suelo + (-70 - 26 * cab) * u);
    }
}
