package com.cesba.godzipet.vista;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * CONTRATO (lo implementa el agente de UI): pantalla de título animada (sol naciente con rayos,
 * relámpagos, silueta de Godzilla con ojos brillantes, título ゴジラ en pincel).
 */
public class TituloView extends View {
    public interface AlTocarLogo { void tocado(); }

    public TituloView(Context c) { this(c, null); }

    public TituloView(Context c, AttributeSet a) { super(c, a); }

    /** Se llama cada vez que el jugador toca el título ゴジラ (7 toques → modo profesor). */
    public void setAlTocarLogo(AlTocarLogo l) { }
}
