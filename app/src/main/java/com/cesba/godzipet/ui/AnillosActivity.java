package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.vista.AnillosView;
import com.cesba.godzipet.vista.Apariencia;

/** Minijuego 3: Anillos de Humo (homenaje a Minilla, 1967). */
public class AnillosActivity extends MinijuegoActivity implements AnillosView.Oyente {
    private AnillosView vista;
    private TextView tvTiempo, tvAciertos, tvFallos;

    @Override
    protected Minijuego minijuego() {
        return Minijuego.ANILLOS;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_defensa);
        ((TextView) findViewById(R.id.tvTituloJuego)).setText("💨 Anillos de Humo");
        findViewById(R.id.btnAliento).setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        android.view.View viejo = findViewById(R.id.juego);
        LinearLayout padre = (LinearLayout) viejo.getParent();
        int indice = padre.indexOfChild(viejo);
        padre.removeView(viejo);
        vista = new AnillosView(this);
        vista.setContentDescription("Área de juego: toca para soplar anillos de humo a los insectos");
        padre.addView(vista, indice, lp);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvAciertos = findViewById(R.id.tvAciertos);
        tvFallos = findViewById(R.id.tvEscapes);
        vista.setOyente(this);
        findViewById(R.id.btnSalir).setOnClickListener(v -> finish());
        progreso(0, 0, AnillosView.DURACION_MS / 1000);
        juego.asegurar(() -> {
            vista.setApariencia(Apariencia.de(partida()));
            listoParaEmpezar();
        }, this::finish);
    }

    @Override
    protected void comenzarRonda() {
        vista.iniciar();
    }

    @Override
    protected int abortarRonda() {
        vista.detener();
        return 0;
    }

    @Override
    public void progreso(int aciertos, int fallos, int seg) {
        tvTiempo.setText("⏱ " + seg);
        tvAciertos.setText("💨 " + aciertos + " / " + AnillosView.META_ACIERTOS);
        tvFallos.setText("🦗 Escaparon: " + fallos);
    }

    @Override
    public void acierto() {
        Sonidos.toque(this);
    }

    @Override
    public void fin(int aciertos, int puntaje) {
        tvTiempo.setText("⏱ 0");
        terminar(puntaje, aciertos >= AnillosView.META_ACIERTOS);
    }
}
