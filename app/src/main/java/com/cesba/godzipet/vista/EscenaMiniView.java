package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.cesba.godzipet.modelo.Escenario;

/** Vista previa pequeña de un escenario (mapa de viaje y Kaiju-pedia). */
public class EscenaMiniView extends View {
    private final EscenaPainter escena = new EscenaPainter();
    private final Paint velo = new Paint();
    private final long inicio = System.currentTimeMillis();
    private Escenario escenario = Escenario.ISLA_ODO;
    private boolean bloqueado;

    public EscenaMiniView(Context c) { this(c, null); }

    public EscenaMiniView(Context c, AttributeSet a) {
        super(c, a);
        velo.setColor(0xB3070A16);
    }

    public void setEscenario(Escenario e, boolean bloqueado) {
        this.escenario = e;
        this.bloqueado = bloqueado;
        setContentDescription(e.nombre + (bloqueado ? ", bloqueado" : ""));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w == 0) return;
        float t = (System.currentTimeMillis() - inicio) / 1000f;
        c.save();
        c.clipRect(0, 0, w, h);
        escena.dibujarEscenario(c, w, h, escenario, t, EscenaPainter.momento(), false);
        c.restore();
        if (bloqueado) {
            c.drawRect(0, 0, w, h, velo);
        } else if (isShown()) {
            postInvalidateDelayed(80);
        }
    }
}
