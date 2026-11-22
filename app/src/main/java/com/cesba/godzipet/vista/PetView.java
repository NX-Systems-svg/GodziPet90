package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Reaccion;

/** Escena principal animada: la bahía, el kaiju, su estado de ánimo y las reacciones a cada acción. */
public class PetView extends View {
    public interface AlTocar {
        void tocado();
    }

    private final KaijuPainter kaiju = new KaijuPainter();
    private final EscenaPainter escena = new EscenaPainter();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long inicio = System.currentTimeMillis();

    private Apariencia apariencia;
    private Reaccion reaccion = Reaccion.NINGUNA;
    private long reaccionInicio;
    private Apariencia evolucionAntes;
    private long evolucionInicio;
    private AlTocar alTocar;
    private float toqueX = -1, toqueY;
    private long toqueEn;

    public PetView(Context c) { this(c, null); }

    public PetView(Context c, AttributeSet a) {
        super(c, a);
        texto.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        texto.setTextAlign(Paint.Align.CENTER);
        setContentDescription("Tu kaiju en la bahía de Nueva York");
    }

    public void setApariencia(Apariencia a) {
        apariencia = a;
        if (a != null) setContentDescription("Tu kaiju: " + a.etapa.nombre + ", estado " + a.animo.nombre);
    }

    public void setAlTocar(AlTocar l) { alTocar = l; }

    public void reaccionar(Reaccion r) {
        reaccion = r;
        reaccionInicio = System.currentTimeMillis();
    }

    public void mostrarEvolucion(Apariencia antes) {
        evolucionAntes = antes;
        evolucionInicio = System.currentTimeMillis();
    }

    public boolean evolucionando() {
        return evolucionAntes != null;
    }

    private static long duracion(Reaccion r) {
        switch (r) {
            case ALIENTO: return 2400;
            case ECLOSION: return 2000;
            case ERROR: return 600;
            default: return 1500;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP) {
            toqueX = e.getX();
            toqueY = e.getY();
            toqueEn = System.currentTimeMillis();
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (alTocar != null) alTocar.tocado();
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        long ahora = System.currentTimeMillis();
        float t = (ahora - inicio) / 1000f;
        float w = getWidth(), h = getHeight();
        if (w == 0 || apariencia == null) {
            postInvalidateOnAnimation();
            return;
        }
        boolean dormido = apariencia.animo == EstadoAnimo.DORMIDO;
        int momento = EscenaPainter.momento();
        boolean noche = dormido || momento == 2;

        float sacudida = 0;
        float pr = reaccion == Reaccion.NINGUNA ? 1f : (ahora - reaccionInicio) / (float) duracion(reaccion);
        if (pr >= 1f) reaccion = Reaccion.NINGUNA;
        if (reaccion == Reaccion.ALIENTO || reaccion == Reaccion.ENTRENAR) {
            sacudida = (float) Math.sin(pr * 60) * 6 * (1 - pr);
        }
        c.save();
        c.translate(sacudida, 0);

        float horizonte = h * 0.5f;
        float muelle = h * 0.74f;
        escena.cielo(c, w, horizonte, momento, t, dormido);
        escena.ciudad(c, w, horizonte, h * 0.22f, momento, dormido);
        escena.agua(c, w, horizonte, muelle, t, noche);
        escena.muelle(c, w, muelle, h);

        float suelo = h * 0.93f;
        float tam = Math.min(h * 0.82f, w * 0.78f);
        float u = tam / 100f * apariencia.escalaEtapa();
        float cx = w * 0.5f + 8 * u;

        // sombra
        p.setColor(0x55000000);
        c.drawOval(cx - 45 * u, suelo - 4 * u, cx + 35 * u, suelo + 3 * u, p);

        float saltoY = 0, jaw = 0, sacudidaX = 0;
        switch (reaccion) {
            case JUGAR: saltoY = -(float) Math.abs(Math.sin(pr * Math.PI * 2)) * 18; break;
            case COMER: jaw = (float) Math.abs(Math.sin(pr * Math.PI * 3)); break;
            case ALIENTO: jaw = pr < 0.15f ? pr / 0.15f : 1f; break;
            case ENTRENAR: saltoY = -(float) Math.abs(Math.sin(pr * Math.PI * 3)) * 10; jaw = 0.6f; break;
            case ERROR: sacudidaX = (float) Math.sin(pr * 40) * 6 * (1 - pr); break;
            default: break;
        }

        if (evolucionAntes != null) {
            dibujarEvolucion(c, w, h, cx, suelo, tam, t, ahora);
        } else {
            kaiju.dibujar(c, cx + sacudidaX, suelo + saltoY, tam, apariencia, t, jaw, false);
            if (apariencia.etapa != Etapa.HUEVO) dibujarAnimo(c, cx, suelo, tam, t);
            dibujarReaccion(c, w, h, cx, suelo + saltoY, tam, pr);
        }
        if (dormido && evolucionAntes == null) {
            p.setColor(0x33000022);
            c.drawRect(0, 0, w, h, p);
        }
        dibujarToque(c, ahora);
        c.restore();
        postInvalidateOnAnimation();
    }

    // ------------------------------------------------------------ ánimo

    private void dibujarAnimo(Canvas c, float cx, float suelo, float tam, float t) {
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
                nube(c, cab.x - s * 5, cab.y - s * 22, s, t);
                break;
            }
            case HAMBRIENTO:
                burbujaPensamiento(c, cab.x + s * 16, cab.y - s * 20, s, "🐟");
                break;
            case SUCIO:
                p.setColor(0xFF1B1B1B);
                for (int i = 0; i < 4; i++) {
                    double a = t * (3 + i) + i * 1.7;
                    float fx = cx - s * 5 + (float) Math.cos(a) * s * (22 + i * 4);
                    float fy = suelo - s * 45 + (float) Math.sin(a * 1.3) * s * 18;
                    c.drawCircle(fx, fy, s * 1.1f, p);
                    p.setColor(0x88FFFFFF);
                    c.drawOval(fx - s * 1.8f, fy - s * 2f, fx, fy - s * 0.6f, p);
                    p.setColor(0xFF1B1B1B);
                }
                break;
            case ENFERMO:
            case CANSADO: {
                float f = (t * 0.7f) % 1f;
                p.setColor(0xFF9FD8FF);
                c.drawCircle(cab.x - s * 6, cab.y + s * (2 + f * 8), s * 1.4f, p);
                if (apariencia.animo == EstadoAnimo.ENFERMO) burbujaPensamiento(c, cab.x + s * 16, cab.y - s * 20, s, "🤒");
                break;
            }
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

    private void nube(Canvas c, float x, float y, float s, float t) {
        p.setColor(0xCC6B7894);
        c.drawCircle(x, y, s * 6, p);
        c.drawCircle(x + s * 7, y - s * 3, s * 7, p);
        c.drawCircle(x + s * 14, y, s * 5.5f, p);
        p.setColor(0xFF7FC8FF);
        for (int i = 0; i < 3; i++) {
            float f = ((t * 1.5f) + i / 3f) % 1f;
            c.drawLine(x + s * (2 + i * 6), y + s * (5 + f * 10), x + s * (1 + i * 6), y + s * (7 + f * 10), p);
            c.drawCircle(x + s * (2 + i * 6), y + s * (5 + f * 10), s * 0.9f, p);
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

    private void dibujarReaccion(Canvas c, float w, float h, float cx, float suelo, float tam, float pr) {
        if (reaccion == Reaccion.NINGUNA) return;
        float s = tam / 100f;
        PointF boca = KaijuPainter.boca(cx, suelo, tam, apariencia);
        switch (reaccion) {
            case COMER: {
                float f = Math.min(1f, pr * 2.2f);
                float fx = w * 0.95f + (boca.x - w * 0.95f) * f;
                float fy = h * 0.3f + (boca.y - h * 0.3f) * f - (float) Math.sin(f * Math.PI) * h * 0.12f;
                if (f < 1f) pez(c, fx, fy, s * 1.2f);
                if (pr > 0.5f) rotulo(c, "¡ÑAM!", boca.x, boca.y - s * 30, s, pr, 0xFFFFD23F);
                break;
            }
            case JUGAR:
                for (int i = 0; i < 4; i++) {
                    float f = Math.min(1f, pr * 1.3f + i * 0.08f);
                    texto.setTextSize(s * 10);
                    textoContorno(c, "♥", cx + s * (-25 + i * 16), suelo - s * (50 + f * 50), 0xFFFF5C8A, (int) (255 * (1 - pr)));
                }
                break;
            case DORMIR:
                rotulo(c, "Zzz…", cx, suelo - s * 100, s, pr, 0xFFBFD7FF);
                break;
            case BANAR:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.8f);
                for (int i = 0; i < 12; i++) {
                    float f = (pr * 1.4f + i * 0.09f) % 1f;
                    float bx = cx + s * (-50 + (i * 37) % 90);
                    float by = suelo - s * (5 + f * 90);
                    p.setColor(Color.argb((int) (220 * (1 - f)), 200, 240, 255));
                    c.drawCircle(bx, by, s * (2 + (i % 3)), p);
                }
                p.setStyle(Paint.Style.FILL);
                rotulo(c, "¡SPLASH!", cx, suelo - s * 105, s, pr, 0xFF7FE3FF);
                break;
            case CURAR:
                for (int i = 0; i < 5; i++) {
                    float f = Math.min(1f, pr + i * 0.1f);
                    texto.setTextSize(s * 12);
                    textoContorno(c, "+", cx + s * (-40 + i * 20), suelo - s * (40 + f * 50), 0xFF5CFF8A, (int) (255 * (1 - pr)));
                }
                break;
            case ENTRENAR:
                rotulo(c, "¡POW!", cx + s * 40, suelo - s * 90, s, pr, 0xFFFF8A1F);
                break;
            case MONEDAS:
                for (int i = 0; i < 6; i++) {
                    float f = Math.min(1f, pr * 1.2f + i * 0.05f);
                    float mx = cx + s * (-45 + i * 18);
                    float my = suelo - s * (30 + f * 70);
                    p.setColor(Color.argb((int) (255 * (1 - pr)), 255, 210, 63));
                    c.drawCircle(mx, my, s * 4, p);
                }
                break;
            case ALIENTO:
                aliento(c, w, boca, s, pr);
                break;
            case ECLOSION:
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    float d = pr * s * 70;
                    p.setColor(Color.argb((int) (255 * (1 - pr)), 220, 230, 200));
                    float px = cx - 8 * s + (float) Math.cos(a) * d, py = suelo - s * 40 + (float) Math.sin(a) * d;
                    c.drawRect(px - s * 3, py - s * 2, px + s * 3, py + s * 2, p);
                }
                p.setColor(Color.argb((int) (200 * (1 - pr)), 255, 255, 255));
                c.drawRect(0, 0, w, h, p);
                rotulo(c, "¡CRACK!", cx, suelo - s * 90, s, pr, 0xFFFFD23F);
                break;
            case ERROR:
                texto.setTextSize(s * 14);
                textoContorno(c, "✖", cx + s * 30, suelo - s * 95, 0xFFFF4D5E, (int) (255 * (1 - pr)));
                break;
            default:
                break;
        }
    }

    private void aliento(Canvas c, float w, PointF boca, float s, float pr) {
        if (pr < 0.12f) {
            p.setShader(new RadialGradient(boca.x, boca.y, s * 20 * pr / 0.12f + 1, 0xFFE9FFE0, 0x0078FF6B, Shader.TileMode.CLAMP));
            c.drawCircle(boca.x, boca.y, s * 20 * pr / 0.12f + 1, p);
            p.setShader(null);
            return;
        }
        float alfa = pr > 0.8f ? (1 - pr) / 0.2f : 1f;
        float grosor = s * (10 + 3 * (float) Math.sin(pr * 80));
        p.setColor(Color.argb((int) (120 * alfa), 120, 255, 107));
        c.drawRect(boca.x, boca.y - grosor * 1.6f, w + 40, boca.y + grosor * 1.6f, p);
        p.setColor(Color.argb((int) (220 * alfa), 160, 255, 140));
        c.drawRect(boca.x, boca.y - grosor, w + 40, boca.y + grosor, p);
        p.setColor(Color.argb((int) (255 * alfa), 240, 255, 235));
        c.drawRect(boca.x, boca.y - grosor * 0.4f, w + 40, boca.y + grosor * 0.4f, p);
        rotulo(c, "¡GRAAOOR!", w * 0.5f, boca.y - s * 35, s, pr, 0xFF9BFF7A);
    }

    private void pez(Canvas c, float x, float y, float s) {
        p.setColor(0xFF9EC3D9);
        c.drawOval(x - 9 * s, y - 4 * s, x + 7 * s, y + 4 * s, p);
        path.reset();
        path.moveTo(x + 6 * s, y);
        path.lineTo(x + 13 * s, y - 5 * s);
        path.lineTo(x + 13 * s, y + 5 * s);
        path.close();
        c.drawPath(path, p);
        p.setColor(Color.BLACK);
        c.drawCircle(x - 5 * s, y - s, s, p);
    }

    /** Texto estilo cómic con contorno grueso. */
    private void rotulo(Canvas c, String s, float x, float y, float esc, float pr, int color) {
        float aparicion = Math.min(1f, pr * 5f);
        texto.setTextSize(esc * 16 * (0.6f + 0.4f * aparicion));
        textoContorno(c, s, x, y, color, (int) (255 * Math.min(1f, (1 - pr) * 3)));
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
        p.setColor(Color.argb((int) (200 * (1 - f)), 255, 210, 63));
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
        p.setColor(Color.argb((int) (190 * Math.min(1, pr * 4)), 5, 8, 20));
        c.drawRect(0, 0, w, h, p);
        // rayos giratorios
        c.save();
        c.rotate(pr * 120, cx - 8 * tam / 100f, suelo - tam * 0.45f);
        p.setColor(Color.argb(90, 160, 255, 140));
        for (int i = 0; i < 12; i++) {
            c.rotate(30, cx - 8 * tam / 100f, suelo - tam * 0.45f);
            path.reset();
            path.moveTo(cx - 8 * tam / 100f, suelo - tam * 0.45f);
            path.lineTo(cx + w, suelo - tam * 0.45f - 30);
            path.lineTo(cx + w, suelo - tam * 0.45f + 30);
            path.close();
            c.drawPath(path, p);
        }
        c.restore();
        if (pr < 0.7f) {
            float ritmo = 3 + pr * 18;
            boolean nueva = Math.sin(pr * ritmo * Math.PI) > 0;
            kaiju.dibujar(c, cx, suelo, tam, nueva ? apariencia : evolucionAntes, t, 0, true);
        } else {
            kaiju.dibujar(c, cx, suelo, tam, apariencia, t, 0.8f, false);
            p.setColor(Color.argb((int) (255 * (1 - (pr - 0.7f) / 0.3f)), 255, 255, 255));
            c.drawRect(0, 0, w, h, p);
        }
        float s = tam / 100f;
        texto.setTextSize(s * 13);
        textoContorno(c, "¡EVOLUCIÓN!", w / 2, h * 0.26f, 0xFF9BFF7A, 255);
        if (apariencia.etapa == Etapa.FINAL && pr > 0.7f) {
            texto.setTextSize(s * 9);
            textoContorno(c, apariencia.ruta.nombre.toUpperCase(), w / 2, h * 0.36f, 0xFFFFD23F, 255);
        }
    }
}
