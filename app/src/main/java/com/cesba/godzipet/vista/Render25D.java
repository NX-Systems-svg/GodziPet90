package com.cesba.godzipet.vista;

import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * Da volumen 2.5D al kaiju dibujado en Canvas:
 * <ul>
 *   <li>sombra proyectada en el suelo según la posición real del sol (o la luna);</li>
 *   <li>contraluz (rim light) del color de la luz ambiente;</li>
 *   <li>inclinación 3D con perspectiva real ({@link Camera}) que sigue al giroscopio;</li>
 *   <li>reflejo en suelo mojado.</li>
 * </ul>
 */
public final class Render25D {
    private final Camera camara = new Camera();
    private final Matrix matriz = new Matrix();
    private final Paint sombra = new Paint();
    private final Paint contraluz = new Paint();
    private final Paint reflejo = new Paint();
    private int colorContraluz;

    public Render25D() {
        sombra.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
        reflejo.setAlpha(46);
        setColorContraluz(0xFFFFF2C0);
    }

    public void setColorContraluz(int color) {
        if (color == colorContraluz) return;
        colorContraluz = color;
        contraluz.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    /** Parámetros de dibujo del cuadro actual. */
    public static final class Pose {
        public float cx, suelo, tam, t, mandibula;
        /** Dirección de la luz en [-1, 1]: -1 viene de la izquierda (mañana), 1 de la derecha (tarde). */
        public float luzX;
        /** Intensidad 0..1 de la sombra y del contraluz. */
        public float fuerzaSombra, fuerzaContraluz;
        /** Inclinación del teléfono en [-1, 1]. */
        public float inclX, inclY;
        public boolean espejo, sueloMojado;
    }

    public void dibujar(Canvas c, KaijuPainter k, Apariencia a, Pose p) {
        float u = p.tam / 100f * a.escalaEtapa();
        if (p.fuerzaSombra > 0.01f) dibujarSombra(c, k, a, p, u);
        if (p.sueloMojado) dibujarReflejo(c, k, a, p, u);

        c.save();
        aplicarPerspectiva(c, p, u);
        if (p.espejo) c.scale(-1, 1, p.cx, 0);
        if (p.fuerzaContraluz > 0.01f) {
            contraluz.setAlpha((int) (200 * p.fuerzaContraluz));
            int capa = c.saveLayer(null, contraluz);
            float dx = (p.espejo ? p.luzX : -p.luzX) * 2.6f * u;
            c.translate(-dx, -1.6f * u);
            k.dibujar(c, p.cx, p.suelo, p.tam, a, p.t, p.mandibula, false);
            c.restoreToCount(capa);
        }
        k.dibujar(c, p.cx, p.suelo, p.tam, a, p.t, p.mandibula, false);
        c.restore();
    }

    /** Sombra aplanada sobre el suelo y sesgada en dirección contraria a la luz. */
    private void dibujarSombra(Canvas c, KaijuPainter k, Apariencia a, Pose p, float u) {
        sombra.setAlpha((int) (110 * p.fuerzaSombra));
        int capa = c.saveLayer(null, sombra);
        matriz.reset();
        matriz.postScale(p.espejo ? -1 : 1, -0.22f, p.cx, p.suelo);
        matriz.postSkew(p.luzX * 1.1f, 0, p.cx, p.suelo);
        c.concat(matriz);
        k.dibujar(c, p.cx, p.suelo, p.tam, a, p.t, p.mandibula, false);
        c.restoreToCount(capa);
    }

    /** Reflejo invertido y tenue sobre el pavimento mojado. */
    private void dibujarReflejo(Canvas c, KaijuPainter k, Apariencia a, Pose p, float u) {
        int capa = c.saveLayer(null, reflejo);
        c.clipRect(0, p.suelo, c.getWidth(), p.suelo + 60 * u);
        c.scale(p.espejo ? -1 : 1, -0.55f, p.cx, p.suelo);
        k.dibujar(c, p.cx, p.suelo, p.tam, a, p.t, p.mandibula, false);
        c.restoreToCount(capa);
    }

    /** Rotación 3D alrededor del centro del cuerpo con proyección en perspectiva. */
    private void aplicarPerspectiva(Canvas c, Pose p, float u) {
        if (Math.abs(p.inclX) < 0.01f && Math.abs(p.inclY) < 0.01f) return;
        float px = p.cx, py = p.suelo - 45 * u;
        camara.save();
        camara.rotateY(p.inclX * 16f);
        camara.rotateX(-p.inclY * 7f);
        camara.getMatrix(matriz);
        camara.restore();
        matriz.preTranslate(-px, -py);
        matriz.postTranslate(px, py);
        c.concat(matriz);
    }
}
