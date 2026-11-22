package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Escenario;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Secreto;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuMiniView;

/** Álbum coleccionable: formas descubiertas, escenarios visitados y secretos (easter eggs). */
public class KaijupediaActivity extends AppCompatActivity {
    private static final Object[][] FORMAS = {
            {Etapa.HUEVO, RutaEvolucion.NINGUNA, "Huevo", "El comienzo de todo."},
            {Etapa.BEBE, RutaEvolucion.NINGUNA, "Baby Godzilla", "Godzilla vs. Mechagodzilla II (1993)"},
            {Etapa.PEQUENO, RutaEvolucion.NINGUNA, "Little Godzilla", "Godzilla vs. SpaceGodzilla (1994)"},
            {Etapa.JUNIOR, RutaEvolucion.NINGUNA, "Godzilla Junior", "Godzilla vs. Destoroyah (1995)"},
            {Etapa.FINAL, RutaEvolucion.SHOWA, "Godzilla Showa", "Era Showa (1954-1975): el héroe"},
            {Etapa.FINAL, RutaEvolucion.HEISEI, "Godzilla Heisei", "Era Heisei (1984-1995): el guerrero"},
            {Etapa.FINAL, RutaEvolucion.BURNING, "Burning Godzilla", "Su núcleo al borde de la fusión (1995)"},
            {Etapa.FINAL, RutaEvolucion.SHIN, "Shin Godzilla", "Shin Godzilla (2016): forma secreta"}};

    private LinearLayout lista;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pantalla);
        ((TextView) findViewById(R.id.tvTitulo)).setText("📖 Kaiju-pedia");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        lista = findViewById(R.id.lista);
        Juego.get(this).asegurar(this::construir, this::finish);
    }

    private void construir() {
        Partida p = Juego.get(this).partida();
        lista.removeAllViews();
        TextView kanji = Ui.texto(this, "ゴジラ", R.style.Titulo);
        kanji.setTextSize(44);
        kanji.setTextColor(getColor(R.color.poster_rojo));
        kanji.setGravity(android.view.Gravity.CENTER);
        kanji.setContentDescription("Gojira. Toca para descubrir el origen del nombre");
        kanji.setOnClickListener(v -> secreto(Secreto.NOMBRE));
        lista.addView(kanji, Ui.anchoCompleto(this, 0));
        int total = FORMAS.length + Escenario.values().length + Secreto.values().length;
        int hallados = p.getFormasVistas().size() + p.getVisitados().size() + p.getSecretos().size();
        lista.addView(Ui.texto(this, "Colección: " + Math.min(hallados, total) + " / " + total, R.style.Subtitulo));
        formas(p);
        escenarios(p);
        rugido();
        secretos(p);
    }

    private void formas(Partida p) {
        lista.addView(Ui.titulo(this, "Formas"));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        for (int i = 0; i < FORMAS.length; i++) {
            Etapa e = (Etapa) FORMAS[i][0];
            RutaEvolucion r = (RutaEvolucion) FORMAS[i][1];
            boolean vista = p.getFormasVistas().contains(Partida.claveForma(e, r));
            LinearLayout celda = Ui.panel(this);
            celda.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            KaijuMiniView mini = new KaijuMiniView(this);
            mini.setApariencia(new Apariencia(e, r, p.getPersonaje().getOrigen(), EstadoAnimo.NORMAL, 0, 55));
            mini.setSilueta(!vista);
            mini.setContentDescription(vista ? (String) FORMAS[i][2] : "Forma sin descubrir");
            celda.addView(mini, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 96)));
            TextView n = Ui.texto(this, vista ? (String) FORMAS[i][2] : "???", R.style.Subtitulo);
            n.setTextSize(14);
            n.setGravity(android.view.Gravity.CENTER);
            celda.addView(n);
            TextView d = Ui.texto(this, vista ? (String) FORMAS[i][3] : "Sigue cuidándolo para descubrirla", R.style.Cuerpo);
            d.setTextSize(11);
            d.setGravity(android.view.Gravity.CENTER);
            celda.addView(d);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(i / 2), GridLayout.spec(i % 2, 1f));
            lp.width = 0;
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
            grid.addView(celda, lp);
        }
        lista.addView(grid);
    }

    private void escenarios(Partida p) {
        lista.addView(Ui.titulo(this, "Escenarios"));
        LinearLayout panel = Ui.panel(this);
        for (Escenario e : Escenario.values()) {
            boolean v = p.getVisitados().contains(e);
            TextView t = Ui.texto(this, (v ? "✔ " + e.nombre + " " + e.kanji + " — " + e.pelicula : "✖ ??? (nivel " + e.nivelRequerido + ")"),
                    R.style.Cuerpo);
            t.setTextColor(getColor(v ? R.color.texto : R.color.texto_sec));
            panel.addView(t);
        }
        lista.addView(panel);
    }

    private void rugido() {
        LinearLayout panel = Ui.panel(this);
        ((LinearLayout.LayoutParams) panel.getLayoutParams()).topMargin = Ui.dp(this, 12);
        panel.addView(Ui.texto(this, "🎻 ¿Cómo se hizo el rugido?", R.style.Subtitulo));
        panel.addView(Ui.texto(this, "En 1954 el compositor Akira Ifukube frotó una cuerda de contrabajo con un guante "
                + "cubierto de resina y ralentizó la grabación. En este juego el rugido es nuestro: se sintetiza "
                + "con una onda de sierra que baja de tono mezclada con ruido.", R.style.Cuerpo));
        TextView b = Ui.boton(this, "🔊 Escuchar el rugido", R.drawable.bg_boton_amarillo);
        b.setLayoutParams(Ui.anchoCompleto(this, 10));
        b.setOnClickListener(v -> {
            Sonidos.rugido(this);
            secreto(Secreto.RUGIDO);
        });
        panel.addView(b);
        lista.addView(panel);
    }

    private void secretos(Partida p) {
        lista.addView(Ui.titulo(this, "Secretos " + p.getSecretos().size() + "/" + Secreto.values().length));
        for (Secreto s : Secreto.values()) {
            boolean ok = p.getSecretos().contains(s);
            LinearLayout panel = Ui.panel(this);
            ((LinearLayout.LayoutParams) panel.getLayoutParams()).topMargin = Ui.dp(this, 8);
            panel.addView(Ui.texto(this, ok ? "🔓 " + s.titulo : "🔒 ???", R.style.Subtitulo));
            TextView d = Ui.texto(this, ok ? s.dato : "Pista: " + s.pista, R.style.Cuerpo);
            if (!ok) d.setTextColor(getColor(R.color.oro));
            panel.addView(d);
            lista.addView(panel);
        }
    }

    private void secreto(Secreto s) {
        Juego j = Juego.get(this);
        if (j.partida().descubrir(s)) {
            j.partida().consumirEventos();
            j.guardar();
            Sonidos.moneda(this);
            Ui.mensaje(this, "🔓 " + s.titulo + ": " + s.dato, 0xFF6A3DB8);
            construir();
        } else if (s == Secreto.NOMBRE) {
            Ui.mensaje(this, s.dato, 0xFF3A4670);
        }
    }
}
