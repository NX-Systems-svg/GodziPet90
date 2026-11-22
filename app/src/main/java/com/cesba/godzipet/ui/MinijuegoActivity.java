package com.cesba.godzipet.ui;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Record;
import com.cesba.godzipet.modelo.Resultado;

/**
 * Flujo común de los minijuegos: instrucciones → cobro de energía → partida →
 * puntuación → victoria/derrota → premio proporcional → repetir o salir.
 */
public abstract class MinijuegoActivity extends AppCompatActivity {
    protected Juego juego;
    private boolean enCurso;

    protected abstract Minijuego minijuego();

    protected abstract void comenzarRonda();

    /** Detiene la ronda actual y devuelve el puntaje parcial (si el jugador sale a mitad). */
    protected abstract int abortarRonda();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        juego = Juego.get(this);
    }

    protected Partida partida() {
        return juego.partida();
    }

    protected void listoParaEmpezar() {
        juego.asegurar(this::mostrarInstrucciones, this::finish);
    }

    private void mostrarInstrucciones() {
        Minijuego m = minijuego();
        Record r = partida().getRecord(m.id);
        new AlertDialog.Builder(this)
                .setTitle(m.icono + "  " + m.nombre)
                .setMessage(m.instrucciones + "\n\n⚡ Costo: " + m.costoEnergia + " de energía\n🏆 Tu récord: "
                        + r.mejorPuntaje + " · Victorias: " + r.victorias)
                .setCancelable(false)
                .setPositiveButton("¡EMPEZAR!", (d, w) -> intentarIniciar())
                .setNegativeButton("Salir", (d, w) -> finish())
                .show();
    }

    private void intentarIniciar() {
        juego.tick();
        Resultado r = partida().iniciarMinijuego(minijuego());
        if (!r.ok) {
            Sonidos.error(this);
            new AlertDialog.Builder(this).setTitle("No puede jugar ahora").setMessage(r.mensaje)
                    .setCancelable(false).setPositiveButton("Volver", (d, w) -> finish()).show();
            return;
        }
        juego.guardar();
        enCurso = true;
        Sonidos.ok(this);
        comenzarRonda();
    }

    /** Llamar al terminar la ronda. */
    protected void terminar(int puntaje, boolean gano) {
        if (!enCurso) return;
        enCurso = false;
        Resultado r = partida().registrarMinijuego(minijuego(), puntaje, gano);
        juego.guardar();
        if (gano) Sonidos.rugido(this);
        else Sonidos.error(this);
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle(gano ? "🏆 ¡VICTORIA!" : "💥 Derrota")
                .setMessage("Puntaje: " + puntaje + "\n" + r.mensaje)
                .setCancelable(false)
                .setPositiveButton("🔁 Repetir", (d, w) -> intentarIniciar())
                .setNegativeButton("Salir", (d, w) -> finish())
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (enCurso) {
            // salir a mitad de la ronda cuenta como derrota con el puntaje parcial
            int parcial = abortarRonda();
            enCurso = false;
            partida().registrarMinijuego(minijuego(), parcial, false);
            juego.guardar();
            finish();
        }
    }
}
