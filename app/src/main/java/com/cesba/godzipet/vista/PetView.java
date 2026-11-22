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
    }

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

    public PetView(Context c) { this(c, null); }

    public PetView(Context c, AttributeSet a) {
        super(c, a);
        texto.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        texto.setTextAlign(Paint.Align.CENTER);
        setContentDescription("Tu kaiju en Japón");
        setHapticFeedbackEnabled(true);
    }

    public void setApariencia(Apariencia a) {
        apariencia = a;
        if (a != null) setContentDescription("Tu kaiju: " + a.etapa.nombre + ", " + a.animo.nombre
                + ", radiación " + Math.round(a.radiacion) + ", en " + escenario.nombre);
    }

    public void setEscenario(Escenario e) { if (e != null) escenario = e; }
    public void setFiltro(Filtro f) { filtro = f == null ? Filtro.NINGUNO : f; }
    public void setOyente(Oyente o) { oyente = o; }
    public void setMovimientoReducido(boolean r) { movimientoReducido = r; }

    public void reaccionar(Reaccion r) {
        reaccion = r;
        reaccionInicio = System.currentTimeMillis();
        emitirInicio(r);
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
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                apuntar(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_UP:
                apuntar(e.getX(), e.getY());
                toqueX = e.getX();
                toqueY = e.getY();
                toqueEn = System.currentTimeMillis();
                procesarToque(e.getX(), e.getY());
                performClick();
                return true;
            default:
                return super.onTouchEvent(e);
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
        RectF torre = escena.puntaTorre(getWidth(), getHeight(), escenario);
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

    private float tam() { return Math.min(getHeight() * 0.82f, getWidth() * 0.78f); }
    private float pies() { return getHeight() * Escenas.PIES; }

    private float centroX() {
        float u = apariencia == null ? 0 : tam() / 100f * apariencia.escalaEtapa();
        return getWidth() * 0.5f + (apariencia != null && apariencia.etapa == Etapa.HUEVO ? 0 : 8 * u);
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

        float pr = reaccion == Reaccion.NINGUNA ? 1f : (ahora - reaccionInicio) / (float) duracion(reaccion);
        if (pr >= 1f) reaccion = Reaccion.NINGUNA;
        actualizarMirada(ahora, dt);

        int capa = Filtros.inicio(c, w, h, filtro);
        c.save();
        trauma = Math.max(0, trauma - dt * 1.4f);
        float sac = trauma * trauma;
        c.translate((float) Math.sin(t * 61) * 14 * sac, (float) Math.cos(t * 53) * 10 * sac);

        escena.dibujarEscenario(c, w, h, escenario, t, momento, dormido);

        float suelo = pies(), tam = tam(), cx = centroX();
        float u = tam / 100f * apariencia.escalaEtapa();
        p.setColor(0x55000000);
        c.drawOval(cx - 45 * u, suelo - 4 * u, cx + 35 * u, suelo + 3 * u, p);

        if (evolucionAntes != null) {
            dibujarEvolucion(c, w, h, cx, suelo, tam, t, ahora);
        } else {
            dibujarKaiju(c, cx, suelo, tam, t, pr);
            if (apariencia.etapa != Etapa.HUEVO) dibujarAnimo(c, cx, suelo, tam, t, ahora);
            dibujarReaccion(c, w, h, cx, suelo, tam, pr);
        }
        if (!movimientoReducido) escena.dibujarClima(c, w, h, clima, t);
        particulas.dibujar(c, dt);
        if (dormido && evolucionAntes == null) {
            p.setColor(0x44000022);
            c.drawRect(0, 0, w, h, p);
        }
        dibujarSobrecalentamiento(c, w, h, t);
        dibujarToque(c, ahora);
        c.restore();
        Filtros.fin(c, capa, w, h, filtro, t);
        postInvalidateOnAnimation();
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

    private void dibujarKaiju(Canvas c, float cx, float suelo, float tam, float t, float pr) {
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
            case SHIE:
                saltoY = -(float) Math.sin(Math.min(1, pr * 1.6f) * Math.PI) * 28 * s;
                giro = (float) Math.sin(Math.min(1, pr * 1.6f) * Math.PI) * -14;
                jaw = 0.7f;
                break;
            case ERROR: sx = (float) Math.sin(pr * 40) * 6 * (1 - pr); break;
            default: break;
        }
        kaiju.setCargaPlacas(carga);
        c.save();
        if (giro != 0) c.rotate(giro, cx, suelo);
        kaiju.dibujar(c, cx + sx, suelo + saltoY, tam, apariencia, t, jaw, false);
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
                if (f < 1f) {
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
