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
import com.cesba.godzipet.modelo.ModoProfesor;
import com.cesba.godzipet.modelo.Secreto;
import com.cesba.godzipet.vista.Filtro;
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
        panel.addView(interruptor("🎵 Música japonesa", "Koto y taiko sintetizados, distintos en cada escenario",
                pr.musica(), (v, on) -> pr.setMusica(on)));
        panel.addView(interruptor("📳 Vibración", "Pisadas, rugidos y ronroneo", pr.vibracion(), (v, on) -> pr.setVibracion(on)));
        panel.addView(interruptor("✨ Efectos de GPU", "Calor, ondas, rayos de luz y resplandor (Android 13+)",
                pr.efectosGpu(), (v, on) -> pr.setEfectosGpu(on)));
        panel.addView(interruptor("🌀 Profundidad 3D", "Los fondos se mueven al inclinar el teléfono (giroscopio)",
                pr.inclinacion(), (v, on) -> pr.setInclinacion(on)));
        panel.addView(interruptor("📳 Agitar = terremoto", "Agita el teléfono para que Godzilla dé un pisotón",
                pr.agitar(), (v, on) -> pr.setAgitar(on)));
        panel.addView(interruptor("🎤 Micrófono", "Rugirle al teléfono (sólo mide el volumen, no graba nada)",
                pr.microfono(), (v, on) -> pr.setMicrofono(on)));
        lista.addView(panel);

        LinearLayout filtros = Ui.panel(this);
        filtros.addView(Ui.texto(this, "🎞 Filtro visual", R.style.Subtitulo));
        android.widget.RadioGroup grupo = new android.widget.RadioGroup(this);
        for (Filtro f : Filtro.values()) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(f.nombre);
            rb.setTextColor(getColor(R.color.texto));
            rb.setMinHeight(Ui.dp(this, 48));
            rb.setId(android.view.View.generateViewId());
            rb.setChecked(f == Filtro.desde(pr.filtro()));
            rb.setOnClickListener(v -> elegirFiltro(f));
            grupo.addView(rb);
        }
        filtros.addView(grupo);
        lista.addView(filtros);

        LinearLayout prof = Ui.panel(this);
        prof.addView(Ui.texto(this, "🎓 Modo evaluador", R.style.Subtitulo));
        prof.addView(Ui.texto(this, pr.profesor() ? "Activo: tu partida real está respaldada."
                : "Para el profesor: desbloquea todo para demostrar el juego. Requiere código.", R.style.Cuerpo));
        TextView abrir = Ui.boton(this, pr.profesor() ? "ABRIR PANEL" : "INGRESAR CÓDIGO", R.drawable.bg_boton_amarillo);
        abrir.setLayoutParams(Ui.anchoCompleto(this, 10));
        abrir.setOnClickListener(v -> codigoProfesor());
        prof.addView(abrir);
        lista.addView(prof);

        LinearLayout reglas = Ui.panel(this);
        reglas.addView(Ui.texto(this, "⏱ Reglas de tiempo (por hora de juego)", R.style.Subtitulo));
        reglas.addView(Ui.texto(this, "Alimentación −8 (dormido −4) · Higiene −5 (dormido −2) · Energía −4 (dormido +120) · "
                + "Felicidad −4 (−8 si tiene hambre o está sucio) · Salud −6 si algo es crítico, −5 si la radiación ≥ 85, "
                + "+2 si el promedio ≥ 60 · Radiación −3 (dormido −1.5; Burning +1).\n"
                + "Radiación ideal: 40-70. Con 95 o más durante 30 min de juego el núcleo se funde.\n"
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

    private void elegirFiltro(Filtro f) {
        Juego juego = Juego.get(this);
        juego.prefs().setFiltro(f.name());
        if (f == Filtro.B_N_1954 && juego.partida() != null && juego.partida().descubrir(Secreto.MODO_1954)) {
            juego.partida().consumirEventos();
            juego.guardar();
            Ui.mensaje(this, "🔓 Secreto: " + Secreto.MODO_1954.dato, 0xFF6A3DB8);
        }
    }

    private void codigoProfesor() {
        Juego juego = Juego.get(this);
        if (juego.prefs().profesor()) {
            startActivity(new Intent(this, ProfesorActivity.class));
            return;
        }
        EditText campo = new EditText(this);
        campo.setHint("Código de evaluador");
        campo.setSingleLine(true);
        int m = Ui.dp(this, 20);
        LinearLayout cont = new LinearLayout(this);
        cont.setPadding(m, 0, m, 0);
        cont.addView(campo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle("🎓 Modo evaluador")
                .setView(cont)
                .setPositiveButton("Activar", (d, w) -> {
                    if (!ModoProfesor.codigoValido(campo.getText().toString())) {
                        Ui.mensaje(this, "Código incorrecto.", 0xFFB3122E);
                        return;
                    }
                    if (juego.prefs().usuarioId() <= 0) {
                        Ui.mensaje(this, "Inicia sesión y crea una partida primero.", 0xFFB3122E);
                        return;
                    }
                    startActivity(new Intent(this, ProfesorActivity.class));
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Pantalla "終" (Fin) como en las películas clásicas, tras reiniciar. */
    private void mostrarFin() {
        TextView fin = new TextView(this);
        fin.setText("終");
        fin.setTextSize(120);
        fin.setTextColor(0xFFF2E6C9);
        fin.setGravity(android.view.Gravity.CENTER);
        fin.setBackgroundColor(0xFF000000);
        fin.setAlpha(0f);
        setContentView(fin);
        fin.animate().alpha(1f).setDuration(900).start();
        fin.postDelayed(() -> {
            Intent i = new Intent(this, InicioActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(InicioActivity.EXTRA_FIN, true);
            startActivity(i);
            finish();
        }, 2600);
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
                        juego.prefs().setProfesor(false);
                        mostrarFin();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
