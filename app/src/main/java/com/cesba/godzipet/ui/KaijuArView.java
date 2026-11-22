package com.cesba.godzipet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuPainter;

/** Capa de realidad aumentada casera: dibuja al kaiju y deja moverlo, escalarlo y girarlo. */
final class KaijuArView extends View {
    private static final float ESCALA_MIN = 0.3f, ESCALA_MAX = 3f;

    private final KaijuPainter pintor = new KaijuPainter();
    private final Paint sombra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector detectorEscala;
    private final long inicio = SystemClock.uptimeMillis();
    private Apariencia apariencia;

    private float x = -1, y = -1;   // posición de los pies (en px de la vista)
    private float escala = 1f;
    private float giro = 0f;        // grados
    private boolean espejo = false;

    private float ultimoX, ultimoY, anguloPrevio;
    private int puntero = -1;

    KaijuArView(Context c) {
        super(c);
        detectorEscala = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                escala = Math.max(ESCALA_MIN, Math.min(ESCALA_MAX, escala * d.getScaleFactor()));
                invalidate();
                return true;
            }
        });
        setContentDescription("Tu kaiju en la cámara. Arrastra para moverlo y pellizca para cambiar su tamaño.");
    }

    void setApariencia(Apariencia a) {
        apariencia = a;
        invalidate();
    }

    void alternarEspejo() {
        espejo = !espejo;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (apariencia == null) return;
        if (x < 0) {
            x = getWidth() / 2f;
            y = getHeight() * 0.72f;
        }
        float t = (SystemClock.uptimeMillis() - inicio) / 1000f;
        dibujar(c, getWidth(), getHeight(), 1f, t);
        postInvalidateOnAnimation();
    }

    /** Dibuja sombra y kaiju; {@code factor} reescala posición para componer sobre otra resolución. */
    void dibujar(Canvas c, float ancho, float alto, float factor, float t) {
        if (apariencia == null) return;
        float tam = Math.min(ancho, alto) * 0.55f / Math.max(0.62f, apariencia.escalaEtapa());
        c.save();
        c.translate(x * factor, y * factor);
        c.rotate(giro);
        c.scale(factor * escala * (espejo ? -1 : 1), factor * escala);
        float rx = tam * 0.38f, ry = tam * 0.07f;
        sombra.setShader(new RadialGradient(0, 0, rx, 0x88000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, ry / rx);
        c.drawCircle(0, 0, rx, sombra);
        c.restore();
        pintor.dibujar(c, 0, 0, tam, apariencia, t, 0, false);
        c.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        detectorEscala.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                puntero = e.getPointerId(0);
                ultimoX = e.getX();
                ultimoY = e.getY();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() == 2) anguloPrevio = angulo(e);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() >= 2) {
                    float a = angulo(e);
                    giro += a - anguloPrevio;
                    anguloPrevio = a;
                } else if (!detectorEscala.isInProgress()) {
                    int i = e.findPointerIndex(puntero);
                    if (i >= 0) {
                        x += e.getX(i) - ultimoX;
                        y += e.getY(i) - ultimoY;
                        ultimoX = e.getX(i);
                        ultimoY = e.getY(i);
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                // Al soltar un dedo, el que queda pasa a ser el de arrastre sin saltos.
                int sube = e.getActionIndex();
                int resto = sube == 0 ? 1 : 0;
                puntero = e.getPointerId(resto);
                ultimoX = e.getX(resto);
                ultimoY = e.getY(resto);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private static float angulo(MotionEvent e) {
        return (float) Math.toDegrees(Math.atan2(e.getY(1) - e.getY(0), e.getX(1) - e.getX(0)));
    }
}
