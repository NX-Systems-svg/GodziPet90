package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Escenario;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Resultado;
import com.cesba.godzipet.vista.EscenaMiniView;

/** Mapa de Japón: viajar entre los escenarios de las películas (se desbloquean por nivel). */
public class MapaActivity extends AppCompatActivity {
    private LinearLayout lista;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pantalla);
        ((TextView) findViewById(R.id.tvTitulo)).setText("🗾 Viajar por Japón");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        lista = findViewById(R.id.lista);
        Juego.get(this).asegurar(this::construir, this::finish);
    }

    private void construir() {
        Partida p = Juego.get(this).partida();
        lista.removeAllViews();
        lista.addView(Ui.texto(this, "Cada escenario aparece en una película de Godzilla. Viajar a uno nuevo da +10 XP.", R.style.Cuerpo));
        for (Escenario e : Escenario.values()) {
            boolean bloqueado = p.getNivel() < e.nivelRequerido;
            boolean actual = p.getEscenario() == e;
            LinearLayout panel = Ui.panel(this);
            ((LinearLayout.LayoutParams) panel.getLayoutParams()).topMargin = Ui.dp(this, 12);
            FrameLayout marco = new FrameLayout(this);
            EscenaMiniView mini = new EscenaMiniView(this);
            mini.setEscenario(e, bloqueado);
            marco.addView(mini, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 120)));
            if (bloqueado) {
                TextView candado = Ui.texto(this, "🔒 Nivel " + e.nivelRequerido, R.style.Subtitulo);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER);
                marco.addView(candado, lp);
            }
            panel.addView(marco);
            TextView nombre = Ui.texto(this, (actual ? "📍 " : p.getVisitados().contains(e) ? "✔ " : "")
                    + e.nombre + "  " + e.kanji, R.style.Subtitulo);
            nombre.setPadding(0, Ui.dp(this, 8), 0, 0);
            panel.addView(nombre);
            TextView peli = Ui.texto(this, "🎬 " + e.pelicula, R.style.Cuerpo);
            peli.setTextColor(getColor(R.color.oro));
            panel.addView(peli);
            panel.addView(Ui.texto(this, e.descripcion, R.style.Cuerpo));
            TextView boton = Ui.boton(this, actual ? "AQUÍ ESTÁS" : bloqueado ? "🔒 BLOQUEADO" : "✈ VIAJAR",
                    actual || bloqueado ? R.drawable.bg_boton_gris : R.drawable.bg_boton_verde);
            boton.setLayoutParams(Ui.anchoCompleto(this, 10));
            boton.setEnabled(!actual);
            boton.setOnClickListener(v -> viajar(e));
            panel.addView(boton);
            lista.addView(panel);
        }
    }

    private void viajar(Escenario e) {
        Juego j = Juego.get(this);
        Resultado r = j.partida().viajar(e);
        Ui.aviso(this, r);
        if (r.ok) {
            j.guardar();
            Sonidos.rugido(this);
            finish();
        }
    }
}
