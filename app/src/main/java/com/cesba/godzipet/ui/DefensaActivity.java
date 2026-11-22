package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.DefensaView;

/** Minijuego 1 (reflejos con temporizador). */
public class DefensaActivity extends MinijuegoActivity implements DefensaView.Oyente {
    private DefensaView vista;
    private TextView tvTiempo, tvAciertos, tvEscapes, btnAliento;

    @Override
    protected Minijuego minijuego() {
        return Minijuego.DEFENSA;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_defensa);
        vista = findViewById(R.id.juego);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvAciertos = findViewById(R.id.tvAciertos);
        tvEscapes = findViewById(R.id.tvEscapes);
        btnAliento = findViewById(R.id.btnAliento);
        vista.setOyente(this);
        findViewById(R.id.btnSalir).setOnClickListener(v -> finish());
        btnAliento.setOnClickListener(v -> usarAliento());
        progreso(0, 0, DefensaView.DURACION_MS / 1000);
        juego.asegurar(() -> {
            vista.setApariencia(Apariencia.de(partida()));
            vista.invalidate();
            actualizarAliento();
            listoParaEmpezar();
        }, this::finish);
    }

    @Override
    protected void comenzarRonda() {
        actualizarAliento();
        vista.iniciar();
    }

    @Override
    protected int abortarRonda() {
        vista.detener();
        return 0;
    }

    private void usarAliento() {
        if (!vista.isJugando()) return;
        if (!partida().usarAlientoEnMinijuego()) {
            Sonidos.error(this);
            return;
        }
        int n = vista.alientoAtomico();
        Sonidos.rugido(this);
        Ui.mensaje(this, "☢ ¡Aliento Atómico! Destruyó " + n + " monstruos.", 0xFF2E8B57);
        actualizarAliento();
    }

    private void actualizarAliento() {
        boolean listo = partida() != null && partida().alientoDesbloqueado() && partida().getCargaAliento() >= 100;
        btnAliento.setVisibility(listo ? View.VISIBLE : View.GONE);
    }

    @Override
    public void progreso(int aciertos, int escapes, int seg) {
        tvTiempo.setText("⏱ " + seg);
        tvAciertos.setText("💥 " + aciertos + " / " + DefensaView.META_ACIERTOS);
        tvEscapes.setText("🏃 Escaparon: " + escapes);
    }

    @Override
    public void acierto() {
        Sonidos.toque(this);
    }

    @Override
    public void fin(int aciertos, int puntaje) {
        tvTiempo.setText("⏱ 0");
        terminar(puntaje, aciertos >= DefensaView.META_ACIERTOS);
    }
}
