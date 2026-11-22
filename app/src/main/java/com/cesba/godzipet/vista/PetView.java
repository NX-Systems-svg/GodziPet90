package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Escenario;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Reaccion;
import com.cesba.godzipet.juego.Inclinacion;

import java.util.Calendar;
import java.util.Random;

/**
 * Escena principal animada: escenario japonés, clima, el kaiju, su estado de ánimo,
 * partículas, filtros y las reacciones a cada acción. También detecta los toques secretos
 * (7 toques en la cabeza = ¡Shie!; punta de la Torre de Tokio = Mothra).
 */
public class PetView extends View {
    public interface Oyente {
        void tocado();
        void toqueCabeza(int seguidos);
        void toqueTorre();
        /** El jugador frotó al kaiju (caricia). */
        void caricia();
        /** Un paso pesado mientras camina. */
        void pisada();
    }

    private static final int HUELLAS = 10;
    private static final float VELOCIDAD_PASEO = 0.04f; // fracción del ancho por segundo

    private static final long VENTANA_TOQUES_MS = 1300;

    private final KaijuPainter kaiju = new KaijuPainter();
    private final EscenaPainter escena = new EscenaPainter();
    private final Particulas particulas = new Particulas();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long inicio = System.currentTimeMillis();
    private final Clima clima = Clima.actual();

    private Apariencia apariencia;
    private Escenario escenario = Escenario.ISLA_ODO;
    private Filtro filtro = Filtro.NINGUNO;
    private Reaccion reaccion = Reaccion.NINGUNA;
    private long reaccionInicio;
    private Apariencia evolucionAntes;
    private long evolucionInicio;
    private Oyente oyente;
    private float toqueX = -1, toqueY;
    private long toqueEn, ultimoToqueCabeza, ultimoCuadro, vaporEn;
    private int toquesCabeza;
    private float miradaX, miradaY, objetivoX, objetivoY;
    private long miradaHasta;
    private float trauma;
    private boolean movimientoReducido;

    // ---- motor 2.5D
    private final Parallax parallax;
    private final Efectos3D efectos = new Efectos3D();
    private final Efectos3D.Params fx = new Efectos3D.Params();
    private final Render25D render = new Render25D();
    private final Render25D.Pose pose = new Render25D.Pose();
    private final Random azar = new Random();
    private Inclinacion inclinacion;
    private float noche, atardecer, luzX;
    private long choqueEn = -1, ondaEn = -1, relampagoEn, proximoRelampago;
    private float choqueX, choqueY, ondaX, ondaY;
    // paseo por el escenario
    private float desplaz, objetivoDesplaz;
    private boolean caminando, mirandoIzq;
    private long proximoPaseo = System.currentTimeMillis() + 6000, ultimaPisada;
    private final float[] huellaX = new float[HUELLAS], huellaY = new float[HUELLAS];
    private final long[] huellaT = new long[HUELLAS];
    private int huellaSig;
    private boolean pieIzq;
    // comida para arrastrar
    private String comidaIcono;
    private Runnable alComer;
    private float comidaX, comidaY;
    private boolean arrastrando;
    private long comidaHasta, comidaArrastradaEn;
    // caricias
    private float recorridoCaricia, ultX, ultY;
    private long inicioCaricia, ultimaCaricia;

    public PetView(Context c) { this(c, null); }

    public PetView(Context c, AttributeSet a) {
        super(c, a);
        texto.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        texto.setTextAlign(Paint.Align.CENTER);
        setContentDescription("Tu kaiju en Japón");
        setHapticFeedbackEnabled(true);
        parallax = new Parallax(c);
    }

    public void setInclinacion(Inclinacion i) { inclinacion = i; }

    private boolean efectosGpu = true;
    private float ultCalor = -1, ultRayos = -1, ultBloom = -1, ultVineta = -1;

    /** Activa o desactiva los shaders de GPU (ajuste del jugador). */
    public void setEfectosGpu(boolean activos) {
        efectosGpu = activos;
        if (!activos) efectos.quitar(this);
    }

    /** Los emuladores de Android Studio pierden el contexto gráfico con RenderEffect animado. */
    private static final boolean EMULADOR = android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.FINGERPRINT.contains("generic");

    /** Ofrece una comida que el jugador debe arrastrar hasta la boca del kaiju. */
    public void ofrecerComida(String icono, Runnable alComer) {
        this.comidaIcono = icono;
        this.alComer = alComer;
        this.arrastrando = false;
        this.comidaHasta = System.currentTimeMillis() + 15000;
        comidaX = getWidth() * 0.15f;
        comidaY = getHeight() * 0.8f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        prepararFondo();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        parallax.liberar();
        efectos.quitar(this);
    }

    private boolean necesitaDia, necesitaNoche;

    /** Carga las capas 3D del escenario (sólo día, sólo noche o ambas en la transición). */
    private void prepararFondo() {
        if (getWidth() == 0) return;
        calcularLuz(apariencia != null && apariencia.animo == EstadoAnimo.DORMIDO);
        necesitaDia = noche < 1f;
        necesitaNoche = noche > 0f;
        parallax.preparar(escenario, getWidth(), getHeight(), necesitaDia, necesitaNoche);
    }

    public void setApariencia(Apariencia a) {
        apariencia = a;
        if (a != null) setContentDescription("Tu kaiju: " + a.etapa.nombre + ", " + a.animo.nombre
                + ", radiación " + Math.round(a.radiacion) + ", en " + escenario.nombre);
    }

    public void setEscenario(Escenario e) {
        if (e == null || e == escenario) return;
        escenario = e;
        desplaz = 0;
        objetivoDesplaz = 0;
        caminando = false;
        prepararFondo();
    }
    public void setFiltro(Filtro f) { filtro = f == null ? Filtro.NINGUNO : f; }
    public void setOyente(Oyente o) { oyente = o; }
    public void setMovimientoReducido(boolean r) { movimientoReducido = r; }

    public void reaccionar(Reaccion r) {
        reaccion = r;
        reaccionInicio = System.currentTimeMillis();
        caminando = false;
        mirandoIzq = false; // se gira hacia el jugador para reaccionar
        emitirInicio(r);
        iniciarOndas(r);
        switch (r) {
            case TERREMOTO: sacudir(1f); break;
            case ENTRENAR: case FUSION: sacudir(0.5f); break;
            case ERROR: performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); break;
            default: performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    public void mostrarEvolucion(Apariencia antes) {
        evolucionAntes = antes;
        evolucionInicio = System.currentTimeMillis();
    }

    public boolean evolucionando() { return evolucionAntes != null; }

    /** Texto flotante (p. ej. "+10 🪙") sobre el kaiju. */
    public void textoFlotante(String s, int color) {
        if (getWidth() == 0) return;
        particulas.texto(s, getWidth() * 0.55f, getHeight() * 0.35f, color, getHeight() * 0.07f);
    }

    private void sacudir(float cantidad) {
        if (!movimientoReducido) trauma = Math.min(1f, trauma + cantidad);
    }

    /** Captura la escena actual (modo foto). */
    public Bitmap capturar() {
        Bitmap b = Bitmap.createBitmap(Math.max(1, getWidth()), Math.max(1, getHeight()), Bitmap.Config.ARGB_8888);
        draw(new Canvas(b));
        return b;
    }

    private static long duracion(Reaccion r) {
        switch (r) {
            case ALIENTO: return 2800;
            case ANILLO_HUMO: return 2400;
            case MOTHRA: return 4500;
            case ECLOSION: return 2000;
            case TERREMOTO: return 1600;
            case SHIE: return 1800;
            case ERROR: return 600;
            default: return 1500;
        }
    }

    // ------------------------------------------------------------ toques

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (comidaIcono != null && Math.hypot(x - comidaX, y - comidaY) < getHeight() * 0.12f) {
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    arrastrando = true;
                    comidaHasta = System.currentTimeMillis() + 15000;
                    return true;
                }
                // sobre el kaiju la escena se queda con el gesto (caricias); fuera de él se puede hacer scroll
                if (sobreKaiju(x, y) && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                recorridoCaricia = 0;
                inicioCaricia = System.currentTimeMillis();
                ultX = x;
                ultY = y;
                apuntar(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (arrastrando) {
                    comidaX = x;
                    comidaY = y;
                    apuntar(x, y);
                    return true;
                }
                acumularCaricia(x, y);
                apuntar(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                if (arrastrando) {
                    soltarComida();
                    return true;
                }
                apuntar(x, y);
                toqueX = x;
                toqueY = y;
                toqueEn = System.currentTimeMillis();
                if (recorridoCaricia < getWidth() * 0.15f) procesarToque(x, y);
                performClick();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private boolean sobreKaiju(float x, float y) {
        if (apariencia == null || apariencia.etapa == Etapa.HUEVO) return false;
        float u = tam() / 100f * apariencia.escalaEtapa();
        float cx = centroX(), suelo = pies();
        return x > cx - 40 * u && x < cx + 40 * u && y > suelo - 95 * u && y < suelo;
    }

    /** Frotar el cuerpo del kaiju (recorrido largo en poco tiempo) cuenta como caricia. */
    private void acumularCaricia(float x, float y) {
        if (apariencia == null || apariencia.etapa == Etapa.HUEVO) return;
        float u = tam() / 100f * apariencia.escalaEtapa();
        float cx = centroX(), suelo = pies();
        boolean sobreCuerpo = x > cx - 40 * u && x < cx + 40 * u && y > suelo - 95 * u && y < suelo;
        if (sobreCuerpo) recorridoCaricia += (float) Math.hypot(x - ultX, y - ultY);
        ultX = x;
        ultY = y;
        long ahora = System.currentTimeMillis();
        if (recorridoCaricia > getWidth() * 0.9f && ahora - inicioCaricia < 2500 && ahora - ultimaCaricia > 1500) {
            ultimaCaricia = ahora;
            recorridoCaricia = 0;
            inicioCaricia = ahora;
            if (oyente != null) oyente.caricia();
        }
    }

    private final RectF torre3D = new RectF();

    /** Punta de la Torre de Tokio en el render 3D (≈30% del ancho, 12% del alto de la imagen). */
    private RectF puntaTorre3D() {
        if (escenario != Escenario.TORRE_TOKIO) return null;
        float h = getHeight(), anchoImagen = Math.max(getWidth() * 1.25f, h * 1.5f);
        float x = getWidth() / 2f + (0.30f - 0.5f) * anchoImagen;
        float y = h * 0.12f;
        float r = getWidth() * 0.1f;
        torre3D.set(x - r, y - r, x + r, y + r * 1.4f);
        return torre3D;
    }

    private void soltarComida() {
        arrastrando = false;
        if (apariencia == null) return;
        PointF boca = KaijuPainter.boca(centroX(), pies(), tam(), apariencia);
        if (Math.hypot(comidaX - boca.x, comidaY - boca.y) < tam() * apariencia.escalaEtapa() * 0.3f) {
            Runnable r = alComer;
            comidaArrastradaEn = System.currentTimeMillis();
            comidaIcono = null;
            alComer = null;
            if (r != null) r.run();
        } else {
            comidaX = getWidth() * 0.15f;  // vuelve a su lugar
            comidaY = getHeight() * 0.8f;
        }
    }

    private void apuntar(float x, float y) {
        if (apariencia == null || getWidth() == 0) return;
        PointF cab = KaijuPainter.cabeza(centroX(), pies(), tam(), apariencia);
        objetivoX = Math.max(-1, Math.min(1, (x - cab.x) / (getWidth() * 0.4f)));
        objetivoY = Math.max(-1, Math.min(1, (y - cab.y) / (getHeight() * 0.4f)));
        miradaHasta = System.currentTimeMillis() + 2000;
    }

    private void procesarToque(float x, float y) {
        if (oyente == null || apariencia == null) return;
        RectF torre = parallax.disponible(escenario) && parallax.listo()
                ? puntaTorre3D() : escena.puntaTorre(getWidth(), getHeight(), escenario);
        if (torre != null && torre.contains(x, y)) {
            oyente.toqueTorre();
            return;
        }
        PointF cab = KaijuPainter.cabeza(centroX(), pies(), tam(), apariencia);
        float radio = tam() * apariencia.escalaEtapa() * 0.28f;
        long ahora = System.currentTimeMillis();
        if (apariencia.etapa != Etapa.HUEVO && Math.hypot(x - cab.x, y - cab.y) < radio) {
            toquesCabeza = ahora - ultimoToqueCabeza < VENTANA_TOQUES_MS ? toquesCabeza + 1 : 1;
            ultimoToqueCabeza = ahora;
            oyente.toqueCabeza(toquesCabeza);
            if (toquesCabeza >= 7) toquesCabeza = 0;
        }
        oyente.tocado();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // ------------------------------------------------------------ geometría

    private float tam() { return Math.min(getHeight() * 0.72f, getWidth() * 0.7f); }
    private float pies() { return getHeight() * Escenas.PIES; }

    private float centroX() {
        float u = apariencia == null ? 0 : tam() / 100f * apariencia.escalaEtapa();
        return getWidth() * (0.5f + desplaz) + (apariencia != null && apariencia.etapa == Etapa.HUEVO ? 0 : 8 * u);
    }

    // ------------------------------------------------------------ dibujo

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        long ahora = System.currentTimeMillis();
        float dt = ultimoCuadro == 0 ? 0.016f : Math.min(0.05f, (ahora - ultimoCuadro) / 1000f);
        ultimoCuadro = ahora;
        float t = (ahora - inicio) / 1000f;
        float w = getWidth(), h = getHeight();
        if (w == 0 || apariencia == null) {
            postInvalidateOnAnimation();
            return;
        }
        boolean dormido = apariencia.animo == EstadoAnimo.DORMIDO;
        int momento = EscenaPainter.momento();
        calcularLuz(dormido);
        if ((noche < 1f) != necesitaDia || (noche > 0f) != necesitaNoche) prepararFondo();

        float pr = reaccion == Reaccion.NINGUNA ? 1f : (ahora - reaccionInicio) / (float) duracion(reaccion);
        if (pr >= 1f) reaccion = Reaccion.NINGUNA;
        actualizarMirada(ahora, dt);
        actualizarPaseo(ahora, dt);
        float ix = 0, iy = 0;
        if (inclinacion != null && !movimientoReducido) {
            ix = inclinacion.x();
            iy = inclinacion.y();
        }

        int capa = Filtros.inicio(c, w, h, filtro);
        c.save();
        trauma = Math.max(0, trauma - dt * 1.4f);
        float sac = trauma * trauma;
        c.translate((float) Math.sin(t * 61) * 14 * sac, (float) Math.cos(t * 53) * 10 * sac);
        aplicarCamaraCine(c, pr);

        boolean capas3D = parallax.disponible(escenario) && parallax.listo();
        float tFondo = movimientoReducido ? 0f : t; // sin deriva del fondo si se redujo el movimiento
        if (capas3D) parallax.dibujarFondo(c, w, h, ix, iy, tFondo, noche, atardecer);
        else escena.dibujarEscenario(c, w, h, escenario, t, momento, dormido);

        float suelo = pies(), tam = tam(), cx = centroX();
        dibujarHuellas(c, ahora);
        if (evolucionAntes != null) {
            dibujarEvolucion(c, w, h, cx, suelo, tam, t, ahora);
        } else {
            dibujarKaiju(c, cx, suelo, tam, t, pr, ix, iy);
            if (apariencia.etapa != Etapa.HUEVO) dibujarAnimo(c, cx, suelo, tam, t, ahora);
            dibujarReaccion(c, w, h, cx, suelo, tam, pr);
        }
        if (capas3D) parallax.dibujarFrente(c, w, h, ix, iy, tFondo, noche);
        if (!movimientoReducido) escena.dibujarClima(c, w, h, clima, t);
        dibujarRelampago(c, w, h, ahora);
        particulas.dibujar(c, dt);
        if (dormido && evolucionAntes == null) {
            p.setColor(0x44000022);
            c.drawRect(0, 0, w, h, p);
        }
        dibujarSobrecalentamiento(c, w, h, t);
        dibujarComida(c, t, ahora);
        dibujarToque(c, ahora);
        c.restore();
        Filtros.fin(c, capa, w, h, filtro, t);
        aplicarEfectosGpu(ahora, t, pr);
        postInvalidateOnAnimation();
    }

    // ------------------------------------------------------------ luz del día real

    /** noche 0..1 y atardecer 0..1 según el minuto real; también la dirección del sol. */
    private void calcularLuz(boolean dormido) {
        Calendar cal = Calendar.getInstance();
        float hora = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f;
        float n;
        if (hora >= 7f && hora <= 17.5f) n = 0f;
        else if (hora > 17.5f && hora < 20f) n = (hora - 17.5f) / 2.5f;
        else if (hora >= 5f && hora < 7f) n = 1f - (hora - 5f) / 2f;
        else n = 1f;
        noche = dormido ? 1f : n;
        float tarde = 1f - Math.min(1f, Math.abs(hora - 18.5f) / 1.6f);
        float alba = 0.6f * (1f - Math.min(1f, Math.abs(hora - 6f) / 1.2f));
        atardecer = dormido ? 0f : Math.max(tarde, alba);
        luzX = hora >= 6f && hora <= 19f ? Math.max(-1f, Math.min(1f, (hora - 12.5f) / 6f)) : 0.45f;
    }

    // ------------------------------------------------------------ paseo y huellas

    /** El kaiju pasea solo por el escenario: camina, se detiene y vuelve a pasear. */
    private void actualizarPaseo(long ahora, float dt) {
        boolean puede = !movimientoReducido && reaccion == Reaccion.NINGUNA && evolucionAntes == null
                && apariencia.etapa != Etapa.HUEVO && apariencia.animo != EstadoAnimo.DORMIDO
                && apariencia.animo != EstadoAnimo.FUSION && apariencia.animo != EstadoAnimo.ENFERMO
                && comidaIcono == null;
        if (!puede) {
            caminando = false;
            return;
        }
        if (!caminando && ahora > proximoPaseo) {
            objetivoDesplaz = (azar.nextFloat() * 2 - 1) * 0.16f;
            caminando = Math.abs(objetivoDesplaz - desplaz) > 0.03f;
            mirandoIzq = objetivoDesplaz < desplaz;
            proximoPaseo = ahora + 7000 + azar.nextInt(8000);
        }
        if (!caminando) {
            if (ahora > proximoPaseo - 5000) mirandoIzq = false; // vuelve a mirar al jugador
            return;
        }
        float paso = VELOCIDAD_PASEO * dt * (mirandoIzq ? -1 : 1);
        desplaz += paso;
        if ((mirandoIzq && desplaz <= objetivoDesplaz) || (!mirandoIzq && desplaz >= objetivoDesplaz)) {
            desplaz = objetivoDesplaz;
            caminando = false;
        }
        if (ahora - ultimaPisada > 520) {
            ultimaPisada = ahora;
            pieIzq = !pieIzq;
            float u = tam() / 100f * apariencia.escalaEtapa();
            huellaX[huellaSig] = centroX() + (pieIzq ? -10 : 8) * u;
            huellaY[huellaSig] = pies() - 1 * u;
            huellaT[huellaSig] = ahora;
            huellaSig = (huellaSig + 1) % HUELLAS;
            sacudir(apariencia.etapa == Etapa.FINAL ? 0.18f : 0.08f);
            particulas.emitir(Particulas.Tipo.HUMO, centroX(), pies(), 1, u);
            if (oyente != null && apariencia.etapa.ordinal() >= Etapa.JUNIOR.ordinal()) oyente.pisada();
        }
    }

    private void dibujarHuellas(Canvas c, long ahora) {
        if (apariencia == null) return;
        float u = tam() / 100f * apariencia.escalaEtapa();
        for (int i = 0; i < HUELLAS; i++) {
            long edad = ahora - huellaT[i];
            if (huellaT[i] == 0 || edad > 9000) continue;
            p.setColor(Color.argb((int) (70 * (1 - edad / 9000f)), 20, 14, 10));
            c.drawOval(huellaX[i] - 6 * u, huellaY[i] - 1.6f * u, huellaX[i] + 6 * u, huellaY[i] + 1.6f * u, p);
        }
    }

    // ------------------------------------------------------------ cámara de cine

    /** Acercamiento suave durante el aliento atómico y la evolución. */
    private void aplicarCamaraCine(Canvas c, float pr) {
        if (movimientoReducido) return;
        float zoom = 1f;
        if (reaccion == Reaccion.ALIENTO) {
            zoom = pr < 0.45f ? 1f + 0.14f * (pr / 0.45f) : 1.14f - 0.14f * Math.min(1f, (pr - 0.45f) / 0.55f);
        } else if (evolucionAntes != null) {
            zoom = 1.06f;
        }
        if (zoom > 1.001f && apariencia != null) {
            PointF cab = KaijuPainter.cabeza(centroX(), pies(), tam(), apariencia);
            c.scale(zoom, zoom, cab.x, cab.y + tam() * 0.2f);
        }
    }

    private void dibujarRelampago(Canvas c, float w, float h, long ahora) {
        if (clima != Clima.LLUVIA || movimientoReducido) return;
        if (proximoRelampago == 0) proximoRelampago = ahora + 6000 + azar.nextInt(9000);
        if (ahora > proximoRelampago) {
            relampagoEn = ahora;
            proximoRelampago = ahora + 7000 + azar.nextInt(12000);
            sacudir(0.12f);
        }
        float f = (ahora - relampagoEn) / 450f;
        if (f < 0 || f > 1) return;
        boolean doble = f > 0.35f && f < 0.5f;
        p.setColor(Color.argb((int) ((doble ? 140 : 190) * (1 - f)), 225, 235, 255));
        c.drawRect(0, 0, w, h, p);
    }

    private void dibujarComida(Canvas c, float t, long ahora) {
        if (comidaIcono == null) return;
        if (!arrastrando && ahora > comidaHasta) {
            comidaIcono = null;
            alComer = null;
            return;
        }
        float s = tam() / 100f;
        float bob = arrastrando ? 0 : (float) Math.sin(t * 4) * s * 2;
        p.setColor(0x55000000);
        c.drawOval(comidaX - s * 9, comidaY + s * 7, comidaX + s * 9, comidaY + s * 10, p);
        texto.setTextSize(s * 20);
        textoContorno(c, comidaIcono, comidaX, comidaY + bob + s * 6, Color.WHITE, 255);
        if (!arrastrando) {
            texto.setTextSize(s * 6.5f);
            textoContorno(c, "¡Arrástralo a su boca!", comidaX + s * 14, comidaY - s * 14, 0xFFFFD23F,
                    (int) (180 + 75 * Math.sin(t * 5)));
        }
    }

    // ------------------------------------------------------------ efectos de GPU (AGSL)

    private void iniciarOndas(Reaccion r) {
        if (apariencia == null || getWidth() == 0) return;
        long ahora = System.currentTimeMillis();
        PointF boca = KaijuPainter.boca(centroX(), pies(), tam(), apariencia);
        switch (r) {
            case TERREMOTO: case SHIE:
                choqueEn = ahora; choqueX = centroX(); choqueY = pies(); break;
            case RUGIDO: case EVOLUCION:
                choqueEn = ahora; choqueX = boca.x; choqueY = boca.y; break;
            case ALIENTO:
                choqueEn = ahora + 1260; choqueX = getWidth() * 0.92f; choqueY = boca.y; break;
            case BANAR: case ENFRIAR:
                ondaEn = ahora; ondaX = centroX(); ondaY = pies(); break;
            default: break;
        }
    }

    private void aplicarEfectosGpu(long ahora, float t, float pr) {
        if (!Efectos3D.soportado() || movimientoReducido || !efectosGpu || EMULADOR) {
            if (ultVineta >= 0) efectos.quitar(this);
            ultVineta = -1;
            return;
        }
        float rad = apariencia.radiacion;
        fx.calor = (apariencia.etapa == Etapa.FINAL && apariencia.ruta == com.cesba.godzipet.modelo.RutaEvolucion.BURNING ? 0.3f : 0f)
                + (rad >= 85 ? (rad - 85) / 15f * 0.7f : 0f);
        fx.choqueX = choqueX;
        fx.choqueY = choqueY;
        fx.choqueProgreso = choqueEn < 0 || ahora < choqueEn ? -1 : (ahora - choqueEn) / 750f;
        if (fx.choqueProgreso > 1) { fx.choqueProgreso = -1; choqueEn = -1; }
        fx.ondaX = ondaX;
        fx.ondaY = ondaY;
        fx.ondaProgreso = ondaEn < 0 ? -1 : (ahora - ondaEn) / 1300f;
        if (fx.ondaProgreso > 1) { fx.ondaProgreso = -1; ondaEn = -1; }
        fx.rayos = noche < 0.6f && clima != Clima.LLUVIA ? 0.28f * (1 - noche) : 0f;
        fx.solX = getWidth() * (0.5f + luzX * 0.4f);
        fx.solY = getHeight() * 0.1f;
        boolean disparo = reaccion == Reaccion.ALIENTO && pr > 0.45f && pr < 0.9f;
        fx.bloom = disparo ? 0.85f : reaccion == Reaccion.CENTRAL ? 0.45f : rad >= 85 ? 0.3f : noche * 0.15f;
        fx.vineta = evolucionAntes != null ? 0.7f : 0.3f;
        // sólo se recrea el efecto si algo cambia o hay una animación que depende del tiempo
        boolean animado = fx.calor > 0 || fx.choqueProgreso >= 0 || fx.ondaProgreso >= 0;
        boolean cambio = Math.abs(fx.calor - ultCalor) > 0.01f || Math.abs(fx.rayos - ultRayos) > 0.01f
                || Math.abs(fx.bloom - ultBloom) > 0.01f || Math.abs(fx.vineta - ultVineta) > 0.01f;
        if (!animado && !cambio) return;
        ultCalor = fx.calor;
        ultRayos = fx.rayos;
        ultBloom = fx.bloom;
        ultVineta = fx.vineta;
        efectos.aplicar(this, t, fx);
    }

    private void actualizarMirada(long ahora, float dt) {
        if (ahora > miradaHasta) {
            objetivoX = 0;
            objetivoY = 0;
        }
        float k = Math.min(1f, dt * 6);
        miradaX += (objetivoX - miradaX) * k;
        miradaY += (objetivoY - miradaY) * k;
        kaiju.setMirada(miradaX, miradaY);
    }

    private void dibujarKaiju(Canvas c, float cx, float suelo, float tam, float t, float pr, float ix, float iy) {
        float saltoY = 0, jaw = 0, sx = 0, giro = 0, carga = 0;
        float s = tam / 100f;
        switch (reaccion) {
            case JUGAR: saltoY = -(float) Math.abs(Math.sin(pr * Math.PI * 2)) * 18; break;
            case COMER: jaw = (float) Math.abs(Math.sin(pr * Math.PI * 3)); break;
            case ALIENTO:
            case ANILLO_HUMO:
                carga = Math.min(1f, pr / 0.45f);
                jaw = pr < 0.4f ? 0 : 1f;
                if (reaccion == Reaccion.ANILLO_HUMO) carga *= 0.5f;
                break;
            case ENTRENAR: saltoY = -(float) Math.abs(Math.sin(pr * Math.PI * 3)) * 10; jaw = 0.6f; break;
            case TERREMOTO: saltoY = pr < 0.25f ? -pr * 4 * 30 * s : 0; jaw = 0.5f; break;
            case RUGIDO: jaw = pr < 0.85f ? 1f : 0; break;
            case CARICIA: saltoY = -(float) Math.abs(Math.sin(pr * Math.PI * 4)) * 4; break;
            case SHIE:
                saltoY = -(float) Math.sin(Math.min(1, pr * 1.6f) * Math.PI) * 28 * s;
                giro = (float) Math.sin(Math.min(1, pr * 1.6f) * Math.PI) * -14;
                jaw = 0.7f;
                break;
            case ERROR: sx = (float) Math.sin(pr * 40) * 6 * (1 - pr); break;
            default: break;
        }
        if (caminando) saltoY -= (float) Math.abs(Math.sin((System.currentTimeMillis() - ultimaPisada) / 520f * Math.PI)) * 2.5f * s;
        kaiju.setCargaPlacas(carga);

        pose.cx = cx + sx;
        pose.suelo = suelo + saltoY;
        pose.tam = tam;
        pose.t = t;
        pose.mandibula = jaw;
        pose.luzX = luzX;
        pose.fuerzaSombra = (1 - noche * 0.6f) * (saltoY < -2 ? 0.7f : 1f);
        pose.fuerzaContraluz = apariencia.etapa == Etapa.HUEVO ? 0.35f : 0.75f;
        pose.inclX = ix;
        pose.inclY = iy;
        pose.espejo = mirandoIzq;
        pose.sueloMojado = clima == Clima.LLUVIA || (noche > 0.5f && (escenario == Escenario.GINZA
                || escenario == Escenario.BAHIA_TOKIO));
        int colorLuz = reaccion == Reaccion.ALIENTO || apariencia.radiacion >= 85 ? 0xFF7FEBFF
                : noche > 0.6f ? 0xFF9FC8FF : atardecer > 0.4f ? 0xFFFFB070 : 0xFFFFF2C0;
        render.setColorContraluz(colorLuz);

        c.save();
        if (giro != 0) c.rotate(giro, cx, suelo);
        render.dibujar(c, kaiju, apariencia, pose);
        c.restore();
    }

    private void dibujarSobrecalentamiento(Canvas c, float w, float h, float t) {
        if (apariencia.radiacion < 85 || evolucionAntes != null) return;
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * (apariencia.radiacion >= 95 ? 9 : 4));
        int alfa = (int) ((apariencia.radiacion >= 95 ? 110 : 50) * pulso);
        p.setShader(new RadialGradient(w / 2, h / 2, Math.max(w, h) * 0.75f,
                Color.argb(0, 255, 59, 31), Color.argb(alfa, 255, 59, 31), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        long ahora = System.currentTimeMillis();
        if (!movimientoReducido && ahora - vaporEn > 450) {
            vaporEn = ahora;
            PointF cab = KaijuPainter.cabeza(centroX(), pies(), tam(), apariencia);
            particulas.emitir(Particulas.Tipo.VAPOR, cab.x - tam() * 0.15f, cab.y + tam() * 0.1f, 2, tam() / 100f);
        }
    }

    // ------------------------------------------------------------ ánimo

    private void dibujarAnimo(Canvas c, float cx, float suelo, float tam, float t, long ahora) {
        PointF cab = KaijuPainter.cabeza(cx, suelo, tam, apariencia);
        float s = tam / 100f;
        switch (apariencia.animo) {
            case DORMIDO:
                for (int i = 0; i < 3; i++) {
                    float f = ((t * 0.5f) + i / 3f) % 1f;
                    texto.setTextSize(s * (6 + f * 8));
                    textoContorno(c, "Z", cab.x + s * (8 + f * 20), cab.y - s * (4 + f * 26), 0xFFE8F0FF, (int) (255 * (1 - f)));
                }
                break;
            case TRISTE: {
                float f = (t * 0.8f) % 1f;
                p.setColor(0xFF7FC8FF);
                c.drawCircle(cab.x + s * 3, cab.y + s * (4 + f * 30), s * 1.6f, p);
                break;
            }
            case HAMBRIENTO:
                burbujaPensamiento(c, cab.x + s * 16, cab.y - s * 20, s, "🍙");
                break;
            case DEBIL:
                burbujaPensamiento(c, cab.x + s * 16, cab.y - s * 20, s, "☢");
                break;
            case ENFERMO:
                burbujaPensamiento(c, cab.x + s * 16, cab.y - s * 20, s, "🤒");
                break;
            case FUSION:
                texto.setTextSize(s * 9);
                textoContorno(c, "⚠", cab.x + s * 14, cab.y - s * 16 - (float) Math.abs(Math.sin(t * 6)) * s * 4,
                        0xFFFF3B1F, 255);
                break;
            case FELIZ:
                for (int i = 0; i < 3; i++) {
                    float f = ((t * 0.6f) + i / 3f) % 1f;
                    texto.setTextSize(s * 9);
                    textoContorno(c, i % 2 == 0 ? "♪" : "★", cx + s * (-30 + i * 25), suelo - s * (60 + f * 40), 0xFFFFE066, (int) (255 * (1 - f)));
                }
                break;
            default:
                break;
        }
    }

    private void burbujaPensamiento(Canvas c, float x, float y, float s, String emoji) {
        p.setColor(Color.WHITE);
        c.drawCircle(x - s * 10, y + s * 12, s * 1.6f, p);
        c.drawCircle(x - s * 6, y + s * 7, s * 2.6f, p);
        c.drawOval(x - s * 9, y - s * 9, x + s * 11, y + s * 5, p);
        texto.setTextSize(s * 11);
        texto.setColor(Color.BLACK);
        texto.setAlpha(255);
        texto.setStyle(Paint.Style.FILL);
        c.drawText(emoji, x + s, y + s * 1.5f, texto);
    }

    // ------------------------------------------------------------ reacciones

    private void emitirInicio(Reaccion r) {
        if (apariencia == null || getWidth() == 0) return;
        float cx = centroX(), suelo = pies(), s = tam() / 100f;
        PointF boca = KaijuPainter.boca(cx, suelo, tam(), apariencia);
        int n = movimientoReducido ? 4 : 1;
        switch (r) {
            case JUGAR: particulas.emitir(Particulas.Tipo.CORAZON, cx, suelo - s * 60, 8 / n, s); break;
            case CARICIA: particulas.emitir(Particulas.Tipo.CORAZON, cx, suelo - s * 70, 5 / n, s); break;
            case BANAR: particulas.emitir(Particulas.Tipo.BURBUJA, cx, suelo - s * 30, 18 / n, s); break;
            case CURAR: particulas.emitir(Particulas.Tipo.CORAZON, cx, suelo - s * 50, 6 / n, s); break;
            case ENTRENAR: case TERREMOTO:
                particulas.emitir(Particulas.Tipo.ESCOMBRO, cx, suelo, (r == Reaccion.TERREMOTO ? 30 : 14) / n, s);
                particulas.emitir(Particulas.Tipo.HUMO, cx, suelo, 6 / n, s);
                break;
            case CENTRAL: particulas.emitir(Particulas.Tipo.ATOMICO, cx - s * 10, suelo - s * 50, 26 / n, s); break;
            case ENFRIAR: particulas.emitir(Particulas.Tipo.VAPOR, cx, suelo - s * 60, 16 / n, s); break;
            case MONEDAS: particulas.emitir(Particulas.Tipo.MONEDA, cx, suelo - s * 40, 12 / n, s); break;
            case ECLOSION: particulas.emitir(Particulas.Tipo.ESCOMBRO, cx, suelo - s * 30, 20 / n, s); break;
            case SHIE: particulas.emitir(Particulas.Tipo.CHISPA, cx, suelo - s * 60, 16 / n, s); break;
            case RUGIDO: particulas.emitir(Particulas.Tipo.CHISPA, boca.x, boca.y, 10 / n, s); break;
            case FUSION: particulas.emitir(Particulas.Tipo.VAPOR, cx, suelo - s * 70, 20 / n, s); break;
            case MOTHRA: particulas.emitir(Particulas.Tipo.PETALO, getWidth() * 0.5f, getHeight() * 0.2f, 20 / n, s); break;
            default: break;
        }
        String rotulo = rotulo(r);
        if (rotulo != null) particulas.texto(rotulo, cx, suelo - s * 100, colorRotulo(r), s * 16);
    }

    private static String rotulo(Reaccion r) {
        switch (r) {
            case COMER: return "¡ÑAM!";
            case BANAR: return "¡SPLASH!";
            case ENTRENAR: return "¡POW!";
            case CENTRAL: return "+☢";
            case ENFRIAR: return "¡FSSS!";
            case TERREMOTO: return "¡PISOTÓN!";
            case RUGIDO: return "¡GRAOOOR!";
            case SHIE: return "¡SHIE!";
            case ECLOSION: return "¡CRACK!";
            case FUSION: return "¡FUSIÓN!";
            case DORMIR: return "Zzz…";
            default: return null;
        }
    }

    private static int colorRotulo(Reaccion r) {
        switch (r) {
            case CENTRAL: case ENFRIAR: case BANAR: return 0xFF3DE0FF;
            case FUSION: return 0xFFFF3B1F;
            case ENTRENAR: case TERREMOTO: return 0xFFFF8A1F;
            default: return 0xFFFFD23F;
        }
    }

    private void dibujarReaccion(Canvas c, float w, float h, float cx, float suelo, float tam, float pr) {
        if (reaccion == Reaccion.NINGUNA) return;
        float s = tam / 100f;
        PointF boca = KaijuPainter.boca(cx, suelo, tam, apariencia);
        switch (reaccion) {
            case COMER: {
                float f = Math.min(1f, pr * 2.2f);
                float fx = w * 0.95f + (boca.x - w * 0.95f) * f;
                float fy = h * 0.3f + (boca.y - h * 0.3f) * f - (float) Math.sin(f * Math.PI) * h * 0.12f;
                if (f < 1f && System.currentTimeMillis() - comidaArrastradaEn > 2500) {
                    texto.setTextSize(s * 14);
                    textoContorno(c, "🍙", fx, fy, Color.WHITE, 255);
                }
                break;
            }
            case ALIENTO: aliento(c, w, boca, s, pr); break;
            case ANILLO_HUMO: anillos(c, w, boca, s, pr); break;
            case RUGIDO: ondas(c, boca, s, pr); break;
            case MOTHRA: mothra(c, w, h, s, pr); break;
            case ECLOSION:
                p.setColor(Color.argb((int) (200 * (1 - pr)), 255, 255, 255));
                c.drawRect(0, 0, w, h, p);
                break;
            case ERROR:
                texto.setTextSize(s * 14);
                textoContorno(c, "✖", cx + s * 30, suelo - s * 95, 0xFFFF4D5E, (int) (255 * (1 - pr)));
                break;
            default:
                break;
        }
    }

    /** Aliento atómico azul: tras encenderse las placas (cola → cabeza), el rayo cruza la escena. */
    private void aliento(Canvas c, float w, PointF boca, float s, float pr) {
        if (pr < 0.45f) {
            float f = pr / 0.45f;
            p.setShader(new RadialGradient(boca.x, boca.y, s * 18 * f + 1, 0xFFE6FBFF, 0x003DE0FF, Shader.TileMode.CLAMP));
            c.drawCircle(boca.x, boca.y, s * 18 * f + 1, p);
            p.setShader(null);
            return;
        }
        if (pr < 0.5f) {
            sacudir(0.12f);
            if (!movimientoReducido) particulas.emitir(Particulas.Tipo.ATOMICO, w - 10, boca.y, 3, s);
        }
        float alfa = pr > 0.85f ? (1 - pr) / 0.15f : 1f;
        float grosor = s * (9 + 3 * (float) Math.sin(pr * 90));
        p.setColor(Color.argb((int) (90 * alfa), 61, 224, 255));
        c.drawRect(boca.x, boca.y - grosor * 1.9f, w + 40, boca.y + grosor * 1.9f, p);
        p.setColor(Color.argb((int) (200 * alfa), 120, 235, 255));
        c.drawRect(boca.x, boca.y - grosor, w + 40, boca.y + grosor, p);
        p.setColor(Color.argb((int) (255 * alfa), 240, 252, 255));
        c.drawRect(boca.x, boca.y - grosor * 0.4f, w + 40, boca.y + grosor * 0.4f, p);
        texto.setTextSize(s * 15);
        textoContorno(c, "¡GRAAAOOOR!", w * 0.5f, boca.y - s * 30, 0xFF3DE0FF, (int) (255 * alfa));
    }

    /** Homenaje a Minilla: anillos de humo en lugar de rayo. */
    private void anillos(Canvas c, float w, PointF boca, float s, float pr) {
        if (pr < 0.4f) return;
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            float f = (pr - 0.4f) / 0.6f - i * 0.18f;
            if (f <= 0 || f >= 1) continue;
            float x = boca.x + f * (w * 0.55f);
            float y = boca.y - f * s * 25 + (float) Math.sin(f * 12) * s * 2;
            float r = s * (4 + f * 12);
            p.setStrokeWidth(s * (3.5f - f * 2));
            p.setColor(Color.argb((int) (230 * (1 - f)), 225, 225, 230));
            c.drawOval(x - r * 0.55f, y - r, x + r * 0.55f, y + r, p);
        }
        p.setStyle(Paint.Style.FILL);
        texto.setTextSize(s * 12);
        textoContorno(c, "¿…puf?", boca.x + s * 20, boca.y - s * 28, 0xFFE8E8F0, (int) (255 * Math.min(1, (1 - pr) * 3)));
    }

    private void ondas(Canvas c, PointF boca, float s, float pr) {
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            float f = (pr * 1.5f - i * 0.2f);
            if (f <= 0 || f >= 1) continue;
            p.setStrokeWidth(s * 2);
            p.setColor(Color.argb((int) (200 * (1 - f)), 255, 235, 150));
            c.drawArc(boca.x - s * 40 * f, boca.y - s * 40 * f, boca.x + s * 40 * f, boca.y + s * 40 * f,
                    -50, 100, false, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    /** Visita de Mothra: una polilla gigante cruza el cielo con dos hadas diminutas. */
    private void mothra(Canvas c, float w, float h, float s, float pr) {
        float x = -w * 0.2f + pr * w * 1.4f;
        float y = h * 0.18f + (float) Math.sin(pr * 9) * h * 0.04f;
        float aleteo = (float) Math.abs(Math.sin(pr * 40));
        float ala = s * 22;
        for (int lado = -1; lado <= 1; lado += 2) {
            path.reset();
            path.moveTo(x, y);
            path.quadTo(x - ala * 0.6f, y + lado * ala * (0.4f + aleteo * 0.6f), x - ala * 1.1f, y + lado * ala * 0.2f);
            path.quadTo(x - ala * 0.3f, y + lado * ala * 0.1f, x + ala * 0.2f, y);
            path.close();
            p.setColor(0xFFF2A33A);
            c.drawPath(path, p);
            p.setColor(0xFFB3412E);
            c.drawCircle(x - ala * 0.55f, y + lado * ala * 0.25f * (0.6f + aleteo), ala * 0.12f, p);
        }
        p.setColor(0xFF4A3426);
        c.drawOval(x - ala * 0.35f, y - ala * 0.08f, x + ala * 0.3f, y + ala * 0.08f, p);
        p.setColor(0xFFFFF6C8);
        c.drawCircle(x - ala * 1.4f, y + s * 14, s * 1.4f, p);
        c.drawCircle(x - ala * 1.25f, y + s * 15, s * 1.4f, p);
        texto.setTextSize(s * 9);
        textoContorno(c, "♪ ♫", x - ala * 1.3f, y + s * 9, 0xFFFFE066, (int) (255 * Math.min(1, (1 - pr) * 4)));
    }

    private void textoContorno(Canvas c, String s, float x, float y, int color, int alfa) {
        alfa = Math.max(0, Math.min(255, alfa));
        texto.setStyle(Paint.Style.STROKE);
        texto.setStrokeWidth(texto.getTextSize() * 0.14f);
        texto.setColor(0xFF0B0F14);
        texto.setAlpha(alfa);
        c.drawText(s, x, y, texto);
        texto.setStyle(Paint.Style.FILL);
        texto.setColor(color);
        texto.setAlpha(alfa);
        c.drawText(s, x, y, texto);
        texto.setAlpha(255);
    }

    private void dibujarToque(Canvas c, long ahora) {
        if (toqueX < 0) return;
        float f = (ahora - toqueEn) / 500f;
        if (f >= 1) {
            toqueX = -1;
            return;
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setColor(Color.argb((int) (200 * (1 - f)), 61, 224, 255));
        c.drawCircle(toqueX, toqueY, 20 + f * 40, p);
        p.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------------ evolución

    private void dibujarEvolucion(Canvas c, float w, float h, float cx, float suelo, float tam, float t, long ahora) {
        float pr = (ahora - evolucionInicio) / 4200f;
        if (pr >= 1f) {
            evolucionAntes = null;
            kaiju.dibujar(c, cx, suelo, tam, apariencia, t, 0, false);
            return;
        }
        p.setColor(Color.argb((int) (200 * Math.min(1, pr * 4)), 17, 17, 17));
        c.drawRect(0, 0, w, h, p);
        // sol rojo naciente con rayos de póster
        float sx = cx - 8 * tam / 100f, sy = suelo - tam * 0.45f;
        c.save();
        c.rotate(pr * 90, sx, sy);
        p.setColor(Color.argb(120, 200, 16, 46));
        for (int i = 0; i < 16; i++) {
            c.rotate(22.5f, sx, sy);
            path.reset();
            path.moveTo(sx, sy);
            path.lineTo(sx + w * 1.2f, sy - 22);
            path.lineTo(sx + w * 1.2f, sy + 22);
            path.close();
            c.drawPath(path, p);
        }
        c.restore();
        p.setColor(0xFFC8102E);
        c.drawCircle(sx, sy, tam * 0.32f, p);
        if (pr < 0.7f) {
            float ritmo = 3 + pr * 18;
            boolean nueva = Math.sin(pr * ritmo * Math.PI) > 0;
            kaiju.setCargaPlacas(pr / 0.7f);
            kaiju.dibujar(c, cx, suelo, tam, nueva ? apariencia : evolucionAntes, t, 0, true);
        } else {
            kaiju.setCargaPlacas(1f);
            kaiju.dibujar(c, cx, suelo, tam, apariencia, t, 0.8f, false);
            p.setColor(Color.argb((int) (255 * (1 - (pr - 0.7f) / 0.3f)), 255, 255, 255));
            c.drawRect(0, 0, w, h, p);
        }
        float s = tam / 100f;
        texto.setTextSize(s * 13);
        textoContorno(c, "¡EVOLUCIÓN! 進化", w / 2, h * 0.2f, 0xFFF2E6C9, 255);
        if (pr > 0.7f) {
            String nombre = apariencia.etapa == Etapa.FINAL ? apariencia.ruta.nombre + "  " + apariencia.ruta.kanji
                    : apariencia.etapa.nombre;
            texto.setTextSize(s * 9);
            textoContorno(c, nombre.toUpperCase(), w / 2, h * 0.3f, 0xFFFFD23F, 255);
        }
    }
}
