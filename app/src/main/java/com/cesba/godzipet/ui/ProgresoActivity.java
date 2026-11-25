package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.modelo.Contadores;
import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Evolucion;
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.modelo.Niveles;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Record;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.BarraView;
import com.cesba.godzipet.vista.KaijuMiniView;

import java.util.Locale;

/** Progreso y evolución: nivel, experiencia, ruta, regla medible y récords. */
public class ProgresoActivity extends AppCompatActivity {
    private LinearLayout lista;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_pantalla);
        ((TextView) findViewById(R.id.tvTitulo)).setText("Progreso y evolución");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        lista = findViewById(R.id.lista);
        Juego.get(this).asegurar(this::construir, this::finish);
    }

    private void construir() {
        Partida p = Juego.get(this).partida();
        Juego.get(this).tick();
        lista.removeAllViews();
        nivel(p);
        etapas(p);
        evolucion(p);
        records(p);
    }

    private void nivel(Partida p) {
        LinearLayout panel = Ui.panel(this);
        int nivel = p.getNivel();
        TextView t = Ui.texto(this, "⭐ Nivel " + nivel + " de " + Niveles.MAXIMO, R.style.Titulo);
        panel.addView(t);
        int base = Niveles.xpBase(nivel), sig = Niveles.xpSiguiente(nivel);
        BarraView barra = new BarraView(this);
        barra.configurar(getColor(R.color.atomico), 0);
        if (sig < 0) barra.setValor(1, 1);
        else barra.setValor(p.getXp() - base, sig - base);
        panel.addView(barra, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));
        panel.addView(Ui.texto(this, sig < 0 ? p.getXp() + " XP · ¡Nivel máximo!"
                : p.getXp() + " / " + sig + " XP para el nivel " + (nivel + 1), R.style.Cuerpo));
        panel.addView(Ui.texto(this, "\nDesbloqueos:", R.style.Subtitulo));
        for (int n = 1; n <= Niveles.MAXIMO; n++) {
            TextView d = Ui.texto(this, (n <= nivel ? "✅ " : "🔒 ") + "Nv " + n + ": " + Niveles.desbloqueo(n), R.style.Cuerpo);
            d.setTextColor(getColor(n <= nivel ? R.color.texto : R.color.texto_sec));
            panel.addView(d);
        }
        lista.addView(panel);
    }

    private void etapas(Partida p) {
        LinearLayout panel = Ui.panel(this);
        panel.addView(Ui.texto(this, "Línea evolutiva", R.style.Subtitulo));
        LinearLayout fila = new LinearLayout(this);
        Etapa[] etapas = {Etapa.HUEVO, Etapa.BEBE, Etapa.PEQUENO, Etapa.JUNIOR, Etapa.FINAL};
        Etapa actual = p.getPersonaje().getEtapa();
        for (Etapa e : etapas) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            KaijuMiniView mini = new KaijuMiniView(this);
            RutaEvolucion r = e == Etapa.FINAL ? (actual == Etapa.FINAL ? p.getPersonaje().getRuta() : RutaEvolucion.SHOWA)
                    : RutaEvolucion.NINGUNA;
            mini.setApariencia(new Apariencia(e, r, p.getPersonaje().getOrigen(), EstadoAnimo.NORMAL, 0));
            mini.setSilueta(e.ordinal() > actual.ordinal());
            col.addView(mini, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 80)));
            TextView n = Ui.texto(this, e == Etapa.FINAL && actual == Etapa.FINAL ? r.nombre : e.nombre, R.style.Cuerpo);
            n.setGravity(android.view.Gravity.CENTER);
            n.setTextSize(12);
            if (e == actual) n.setTextColor(getColor(R.color.atomico));
            col.addView(n);
            fila.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        panel.addView(fila);
        panel.addView(Ui.texto(this, "Baby → Little en el nivel " + Evolucion.NIVEL_PEQUENO + " · Junior en el nivel " + Evolucion.NIVEL_JUNIOR
                + " · Forma final en el nivel " + Evolucion.NIVEL_FINAL, R.style.Cuerpo));
        lista.addView(panel);
    }

    private void evolucion(Partida p) {
        LinearLayout panel = Ui.panel(this);
        Contadores c = p.getContadores();
        double prom = p.getPromedioBienestar();
        RutaEvolucion actual = p.getPersonaje().getRuta();
        RutaEvolucion prevista = Evolucion.decidir(prom, p.getPromedioRadiacion(), c.entrenosGeneracion(), c.cuidadosGeneracion());
        panel.addView(Ui.texto(this, "🧬 Evolución por decisiones", R.style.Subtitulo));
        if (actual != RutaEvolucion.NINGUNA) {
            TextView t = Ui.texto(this, "Ruta alcanzada: " + actual.nombre, R.style.Titulo);
            t.setTextSize(20);
            panel.addView(t);
            panel.addView(Ui.texto(this, "Ventaja: " + actual.ventaja, R.style.Cuerpo));
        } else {
            TextView t = Ui.texto(this, "Ruta prevista hoy: " + prevista.nombre, R.style.Subtitulo);
            t.setTextColor(getColor(R.color.amarillo));
            panel.addView(t);
            panel.addView(Ui.texto(this, "Ventaja: " + prevista.ventaja, R.style.Cuerpo));
        }
        panel.addView(Ui.texto(this, "\nTu historial:", R.style.Subtitulo));
        panel.addView(Ui.texto(this, String.format(Locale.US,
                "• Bienestar promedio: %.0f / 100\n• Radiación promedio: %.0f\n• Entrenamientos: %d\n• Cuidados (comer, jugar, dormir, nadar, curar): %d\n• Alertas Kaiju ganadas: %d\n• Alientos atómicos: %d",
                prom, p.getPromedioRadiacion(), c.entrenosGeneracion(), c.cuidadosGeneracion(), c.defensas, c.alientos), R.style.Cuerpo));
        panel.addView(Ui.texto(this, "\nRegla (se evalúa al llegar al nivel 5):", R.style.Subtitulo));
        panel.addView(Ui.texto(this,
                "1. Bienestar promedio < 45 → Shin Godzilla (forma secreta por descuido)\n"
                        + "2. Radiación promedio ≥ 72 → Burning Godzilla\n"
                        + "3. Entrenamientos ≥ 6 y entrenamientos × 2 ≥ cuidados → Godzilla Heisei\n"
                        + "4. En otro caso → Godzilla Showa (cuidado equilibrado)", R.style.Cuerpo));
        lista.addView(panel);
    }

    private void records(Partida p) {
        LinearLayout panel = Ui.panel(this);
        panel.addView(Ui.texto(this, "🏆 Récords", R.style.Subtitulo));
        for (Minijuego m : Minijuego.values()) {
            Record r = p.getRecord(m.id);
            panel.addView(Ui.texto(this, m.icono + " " + m.nombre + ": mejor " + r.mejorPuntaje
                    + " pts · " + r.victorias + " victorias de " + r.jugadas, R.style.Cuerpo));
        }
        lista.addView(panel);
    }
}
