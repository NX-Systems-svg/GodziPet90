package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Escenario;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.ModoProfesor;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Stat;

/**
 * Panel del modo profesor/evaluador: permite demostrar en vivo cualquier parte del juego.
 * La partida real se respalda al activarlo y se restaura al salir.
 */
public class ProfesorActivity extends AppCompatActivity {
    private LinearLayout lista;
    private Juego juego;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pantalla);
        ((TextView) findViewById(R.id.tvTitulo)).setText("🎓 Modo evaluador");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        lista = findViewById(R.id.lista);
        juego = Juego.get(this);
        juego.asegurar(() -> {
            if (juego.prefs().profesor()) construir();
            else juego.activarProfesor(this::construir);
        }, this::finish);
    }

    private Partida p() {
        return juego.partida();
    }

    private void construir() {
        lista.removeAllViews();
        TextView aviso = Ui.texto(this, "Tu partida real está respaldada. Todo lo que hagas aquí se descarta al pulsar "
                + "«Salir del modo evaluador». Código de acceso: " + ModoProfesor.CODIGO, R.style.Cuerpo);
        aviso.setTextColor(getColor(R.color.oro));
        lista.addView(aviso);

        seccion("⭐ Nivel (ajusta la etapa automáticamente)");
        GridLayout niveles = grilla(6);
        for (int n = 1; n <= 6; n++) {
            final int nivel = n;
            boton(niveles, "NV " + n, R.drawable.bg_boton_verde, () -> {
                ModoProfesor.fijarNivel(p(), nivel, RutaEvolucion.SHOWA);
                hecho("Nivel " + nivel + " · " + p().getPersonaje().nombreForma());
            });
        }
        lista.addView(niveles);

        seccion("🧬 Forma (lanza la animación de evolución)");
        GridLayout formas = grilla(2);
        Object[][] f = {{Etapa.HUEVO, RutaEvolucion.NINGUNA, "🥚 Huevo"}, {Etapa.BEBE, RutaEvolucion.NINGUNA, "Baby"},
                {Etapa.PEQUENO, RutaEvolucion.NINGUNA, "Little"}, {Etapa.JUNIOR, RutaEvolucion.NINGUNA, "Junior"},
                {Etapa.FINAL, RutaEvolucion.SHOWA, "Showa 昭和"}, {Etapa.FINAL, RutaEvolucion.HEISEI, "Heisei 平成"},
                {Etapa.FINAL, RutaEvolucion.BURNING, "Burning 🔥"}, {Etapa.FINAL, RutaEvolucion.SHIN, "Shin シン"}};
        for (Object[] o : f) {
            boton(formas, (String) o[2], R.drawable.bg_boton_amarillo, () -> {
                ModoProfesor.fijarForma(p(), (Etapa) o[0], (RutaEvolucion) o[1]);
                hecho("Forma: " + p().getPersonaje().nombreForma());
            });
        }
        lista.addView(formas);

        seccion("🗾 Escenario");
        GridLayout lugares = grilla(2);
        for (Escenario e : Escenario.values()) {
            boton(lugares, e.nombre, R.drawable.bg_boton_azul, () -> {
                ModoProfesor.viajarA(p(), e);
                hecho("Ahora en " + e.nombre);
            });
        }
        lista.addView(lugares);

        seccion("📊 Necesidades (desliza)");
        for (Stat s : Stat.values()) deslizador(s);

        seccion("⚡ Eventos y tiempo");
        GridLayout eventos = grilla(2);
        boton(eventos, "🪙 Todo + 9999", R.drawable.bg_boton_amarillo, () -> {
            ModoProfesor.darTodo(p());
            hecho("Todos los artículos, escenarios y 9999 monedas");
        });
        boton(eventos, "🔥 Aliento 100%", R.drawable.bg_boton_naranja, () -> {
            ModoProfesor.cargarAliento(p());
            hecho("Aliento cargado");
        });
        boton(eventos, "🚨 Alerta Kaiju", R.drawable.bg_boton_rojo, () -> {
            if (p().getNivel() < 3) ModoProfesor.fijarNivel(p(), 3, null);
            ModoProfesor.provocarAlerta(p(), System.currentTimeMillis(), juego.velocidad());
            hecho("Alerta activa: ve a Hogar y pulsa DEFENDER");
        });
        boton(eventos, "☢ Provocar fusión", R.drawable.bg_boton_rojo, () ->
                hecho(ModoProfesor.provocarFusion(p()).mensaje));
        boton(eventos, "⏩ +1 hora", R.drawable.bg_boton_gris, () -> {
            ModoProfesor.adelantarTiempo(p(), 60, juego.velocidad(), System.currentTimeMillis());
            hecho("Pasó 1 hora de juego");
        });
        boton(eventos, "⏩ +1 día", R.drawable.bg_boton_gris, () -> {
            ModoProfesor.adelantarTiempo(p(), 24 * 60, juego.velocidad(), System.currentTimeMillis());
            hecho("Pasó 1 día de juego");
        });
        boton(eventos, "🔓 Todos los secretos", R.drawable.bg_boton_rosa, () -> {
            ModoProfesor.descubrirTodo(p());
            hecho("Kaiju-pedia completa");
        });
        boton(eventos, "⏱ Modo demo ×20", R.drawable.bg_boton_turquesa, () -> {
            juego.tick();
            juego.prefs().setModoDemo(!juego.prefs().modoDemo());
            hecho("Modo demostración " + (juego.prefs().modoDemo() ? "activado" : "desactivado"));
        });
        lista.addView(eventos);

        TextView salir = Ui.boton(this, "↩ SALIR DEL MODO EVALUADOR (restaurar partida)", R.drawable.bg_boton_rojo);
        salir.setLayoutParams(Ui.anchoCompleto(this, 18));
        salir.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Salir del modo evaluador")
                .setMessage("Se descartarán los cambios de prueba y volverá tu partida real.")
                .setPositiveButton("Restaurar", (d, w) -> juego.desactivarProfesor(() -> {
                    Sonidos.ok(this);
                    finish();
                }))
                .setNegativeButton("Cancelar", null)
                .show());
        lista.addView(salir);
    }

    private void seccion(String titulo) {
        lista.addView(Ui.titulo(this, titulo));
    }

    private GridLayout grilla(int columnas) {
        GridLayout g = new GridLayout(this);
        g.setColumnCount(columnas);
        return g;
    }

    private void boton(GridLayout g, String texto, int fondo, Runnable accion) {
        int i = g.getChildCount();
        TextView b = Ui.boton(this, texto, fondo);
        b.setTextSize(12);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(i / g.getColumnCount()),
                GridLayout.spec(i % g.getColumnCount(), 1f));
        lp.width = 0;
        int m = Ui.dp(this, 3);
        lp.setMargins(m, m, m, m);
        b.setOnClickListener(v -> {
            Ui.pulsar(v);
            accion.run();
        });
        g.addView(b, lp);
    }

    private void deslizador(Stat s) {
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView etiqueta = Ui.texto(this, s.icono + " " + s.nombre + ": " + p().getNecesidades().valor(s), R.style.Cuerpo);
        etiqueta.setMinWidth(Ui.dp(this, 150));
        SeekBar barra = new SeekBar(this);
        barra.setMax(100);
        barra.setProgress(p().getNecesidades().valor(s));
        barra.setContentDescription(s.nombre);
        barra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int v, boolean usuario) {
                if (!usuario) return;
                ModoProfesor.fijarStat(p(), s, v);
                etiqueta.setText(s.icono + " " + s.nombre + ": " + v);
            }

            @Override public void onStartTrackingTouch(SeekBar sb) { }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                juego.guardar();
            }
        });
        fila.addView(etiqueta);
        fila.addView(barra, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
        lista.addView(fila);
    }

    private void hecho(String mensaje) {
        juego.guardar();
        Sonidos.ok(this);
        Ui.mensaje(this, "✔ " + mensaje, 0xFF2E8B57);
        construir();
    }
}
