package com.cesba.godzipet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.datos.Preferencias;
import com.cesba.godzipet.juego.Alertas;
import com.cesba.godzipet.juego.Juego;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Configuración: sonido, notificaciones, modo demostración y reinicio con confirmación. */
public class AjustesActivity extends AppCompatActivity {
    private static final String PALABRA_CONFIRMACION = "REINICIAR";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pantalla);
        ((TextView) findViewById(R.id.tvTitulo)).setText("Configuración");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        LinearLayout lista = findViewById(R.id.lista);
        Juego juego = Juego.get(this);
        Preferencias pr = juego.prefs();

        LinearLayout panel = Ui.panel(this);
        panel.addView(interruptor("🔊 Sonido", "Efectos y rugidos", pr.sonido(), (v, on) -> pr.setSonido(on)));
        panel.addView(interruptor("🔔 Notificaciones", "Necesidad crítica y fin del descanso", pr.notificaciones(), (v, on) -> {
            pr.setNotificaciones(on);
            if (!on) Alertas.cancelar(this);
        }));
        panel.addView(interruptor("⏩ Modo demostración", "El tiempo avanza ×20 (1 min real = 20 min de juego). Útil para exponer.",
                pr.modoDemo(), (v, on) -> {
                    juego.tick(); // aplica el tiempo con la velocidad anterior antes de cambiarla
                    pr.setModoDemo(on);
                    juego.guardar();
                }));
        lista.addView(panel);

        LinearLayout reglas = Ui.panel(this);
        reglas.addView(Ui.texto(this, "⏱ Reglas de tiempo (por hora de juego)", R.style.Subtitulo));
        reglas.addView(Ui.texto(this, "Alimentación −8 (dormido −4) · Higiene −5 (dormido −2) · Energía −4 (dormido +120) · "
                + "Felicidad −4 (−8 si tiene hambre o está sucio) · Salud −6 si algo es crítico, +2 si el promedio ≥ 60.\n"
                + "Ejemplo: 80 de alimentación tras 2 horas → 64.", R.style.Cuerpo));
        lista.addView(reglas);

        TextView reiniciar = Ui.boton(this, "🗑  REINICIAR PARTIDA", R.drawable.bg_boton_rojo);
        reiniciar.setLayoutParams(Ui.anchoCompleto(this, 8));
        reiniciar.setOnClickListener(v -> confirmarReinicio());
        lista.addView(reiniciar);
    }

    private android.view.View interruptor(String titulo, String detalle, boolean valor,
                                          CompoundButton.OnCheckedChangeListener l) {
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(titulo + "\n" + detalle);
        s.setTextColor(getColor(R.color.texto));
        s.setTextSize(15);
        s.setChecked(valor);
        s.setMinHeight(Ui.dp(this, 64));
        s.setOnCheckedChangeListener(l);
        return s;
    }

    /** Doble confirmación para evitar reinicios accidentales. */
    private void confirmarReinicio() {
        Juego juego = Juego.get(this);
        if (juego.prefs().usuarioId() <= 0) return;
        EditText campo = new EditText(this);
        campo.setHint("Escribe " + PALABRA_CONFIRMACION);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int m = Ui.dp(this, 20);
        LinearLayout cont = new LinearLayout(this);
        cont.setPadding(m, 0, m, 0);
        cont.addView(campo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle("⚠ Reiniciar partida")
                .setMessage("Se borrará tu kaiju, monedas, inventario, misiones y récords. No se puede deshacer.\n\nEscribe "
                        + PALABRA_CONFIRMACION + " para confirmar:")
                .setView(cont)
                .setPositiveButton("Borrar todo", (d, w) -> {
                    if (!PALABRA_CONFIRMACION.equals(campo.getText().toString().trim().toUpperCase())) {
                        Ui.mensaje(this, "Confirmación incorrecta. No se borró nada.", 0xFFB3122E);
                        return;
                    }
                    Alertas.cancelar(this);
                    juego.repo().borrarPartida(juego.prefs().usuarioId(), () -> {
                        juego.olvidar();
                        Intent i = new Intent(this, InicioActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
