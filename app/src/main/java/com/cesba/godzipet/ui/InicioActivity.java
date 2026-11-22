package com.cesba.godzipet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Alertas;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuMiniView;

/** Pantalla de partida: continuar, nueva partida, configuración y cierre de sesión. */
public class InicioActivity extends AppCompatActivity {
    /** Viene de reiniciar la partida: el próximo juego registra el secreto 終. */
    public static final String EXTRA_FIN = "fin";
    private boolean vieneDeFin;
    private Juego juego;
    private Partida guardada;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        juego = Juego.get(this);
        if (juego.prefs().usuarioId() <= 0) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_inicio);
        vieneDeFin = getIntent().getBooleanExtra(EXTRA_FIN, false);
        findViewById(R.id.btnContinuar).setOnClickListener(v -> continuar());
        findViewById(R.id.btnNueva).setOnClickListener(v -> nueva());
        findViewById(R.id.btnAjustes).setOnClickListener(v -> startActivity(new Intent(this, AjustesActivity.class)));
        findViewById(R.id.btnSalir).setOnClickListener(v -> salir());
        juego.repo().nombreUsuario(juego.prefs().usuarioId(), n ->
                ((TextView) findViewById(R.id.tvHola)).setText("Hola, " + (n == null ? "jugador" : n) + " 👋"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (juego.prefs().usuarioId() <= 0) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        findViewById(R.id.btnContinuar).setEnabled(false);
        long uid = juego.prefs().usuarioId();
        juego.repo().cargar(uid, p -> {
            guardada = p;
            mostrar();
        });
    }

    private void mostrar() {
        KaijuMiniView vista = findViewById(R.id.vistaKaiju);
        TextView resumen = findViewById(R.id.tvResumen);
        TextView detalle = findViewById(R.id.tvDetalle);
        View continuar = findViewById(R.id.btnContinuar);
        if (guardada == null) {
            vista.setApariencia(new Apariencia(Etapa.HUEVO, RutaEvolucion.NINGUNA, OrigenHuevo.ISLA_ODO, EstadoAnimo.NORMAL, 0));
            vista.setSilueta(false);
            resumen.setText("Aún no tienes un kaiju");
            detalle.setText("Inicia una nueva partida y elige tu huevo.");
            continuar.setEnabled(false);
            return;
        }
        guardada.actualizar(System.currentTimeMillis(), juego.velocidad());
        vista.setApariencia(Apariencia.de(guardada));
        resumen.setText(guardada.getPersonaje().getNombre() + " · " + guardada.getPersonaje().nombreForma());
        detalle.setText("Nivel " + guardada.getNivel() + " · " + guardada.getMonedas() + " monedas · "
                + guardada.getEstado().nombre);
        continuar.setEnabled(true);
    }

    private void continuar() {
        if (guardada == null) return;
        juego.establecer(juego.prefs().usuarioId(), guardada);
        startActivity(new Intent(this, MainActivity.class));
    }

    private void nueva() {
        if (guardada == null) {
            startActivity(new Intent(this, SeleccionActivity.class).putExtra(EXTRA_FIN, vieneDeFin));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("¿Empezar de nuevo?")
                .setMessage("Se borrará a " + guardada.getPersonaje().getNombre()
                        + " con todo su progreso, monedas e inventario. Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, nueva partida", (d, w) -> startActivity(new Intent(this, SeleccionActivity.class).putExtra(EXTRA_FIN, true)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salir() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("Tu partida queda guardada en este teléfono. ¿Cerrar sesión?")
                .setPositiveButton("Cerrar sesión", (d, w) -> {
                    Alertas.cancelar(this);
                    juego.prefs().cerrarSesion();
                    juego.olvidar();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
