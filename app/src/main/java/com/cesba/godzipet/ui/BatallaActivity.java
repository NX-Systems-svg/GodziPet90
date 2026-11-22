package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.BatallaView;

/** Minijuego 3: batalla cinematográfica contra el Mecha-Kaiju. */
public class BatallaActivity extends MinijuegoActivity implements BatallaView.Oyente {
    private BatallaView vista;
    private TextView tvTiempo, tvJefe, tvVida, btnAliento;

    @Override
    protected Minijuego minijuego() {
        return Minijuego.BATALLA;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_batalla);
        vista = findViewById(R.id.juego);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvJefe = findViewById(R.id.tvJefe);
        tvVida = findViewById(R.id.tvVida);
        btnAliento = findViewById(R.id.btnAliento);
        vista.setOyente(this);
        findViewById(R.id.btnSalir).setOnClickListener(v -> finish());
        btnAliento.setOnClickListener(v -> usarAliento());
        progreso(BatallaView.DURACION_S, 100, 100);
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
        return vista.detener();
    }

    private void usarAliento() {
        if (!vista.isJugando()) return;
        if (!partida().usarAlientoEnMinijuego()) {
            Sonidos.error(this);
            return;
        }
        vista.alientoMaximo();
        Ui.mensaje(this, "☢ ¡Aliento Atómico al 100%!", 0xFF2E8B57);
        actualizarAliento();
    }

    private void actualizarAliento() {
        boolean listo = partida() != null && partida().alientoDesbloqueado() && partida().getCargaAliento() >= 100;
        btnAliento.setVisibility(listo ? View.VISIBLE : View.GONE);
    }

    @Override
    public void progreso(int segundos, int jefePct, int vidaPropia) {
        tvTiempo.setText("⏱ " + segundos);
        tvJefe.setText("🤖 Jefe: " + jefePct + "%");
        tvVida.setText("❤ Tu vida: " + vidaPropia);
    }

    @Override
    public void misilDestruido() {
        Sonidos.toque(this);
    }

    @Override
    public void impacto() {
        Sonidos.error(this);
    }

    @Override
    public void disparo(boolean critico) {
        if (critico) Sonidos.rugido(this);
        else Sonidos.ok(this);
    }

    @Override
    public void fin(int puntaje, boolean gano) {
        terminar(puntaje, gano);
    }
}
