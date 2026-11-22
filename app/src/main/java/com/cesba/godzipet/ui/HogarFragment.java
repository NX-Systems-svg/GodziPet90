package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Articulo;
import com.cesba.godzipet.modelo.Catalogo;
import com.cesba.godzipet.modelo.Cuidados;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Reaccion;
import com.cesba.godzipet.modelo.Resultado;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Stat;
import com.cesba.godzipet.modelo.TipoArticulo;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.BarraView;
import com.cesba.godzipet.vista.PetView;

import java.util.ArrayList;
import java.util.List;

/** Hogar: el kaiju, sus 5 indicadores, el mensaje contextual y las acciones de cuidado. */
public class HogarFragment extends Fragment implements Refrescable {
    private static final long DURACION_MENSAJE_MS = 4500;
    private static final int[] COLORES_STAT = {R.color.stat_alim, R.color.stat_feliz, R.color.stat_energia,
            R.color.stat_higiene, R.color.stat_salud};

    private PetView petView;
    private TextView tvEstado, tvMensaje, btnAliento;
    private final BarraView[] barras = new BarraView[Stat.values().length + 1];
    private final TextView[] valores = new TextView[Stat.values().length + 1];
    private final List<TextView> botones = new ArrayList<>();
    private String ultimoMensaje;
    private long mensajeHasta;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_hogar, cont, false);
        petView = v.findViewById(R.id.petView);
        tvEstado = v.findViewById(R.id.tvEstado);
        tvMensaje = v.findViewById(R.id.tvMensaje);
        btnAliento = v.findViewById(R.id.btnAliento);
        construirStats(inf, v.findViewById(R.id.panelStats));
        construirAcciones(v.findViewById(R.id.gridAcciones));
        petView.setAlTocar(this::tocarKaiju);
        btnAliento.setOnClickListener(x -> ejecutar(Cuidados.alientoAtomico(partida())));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
    }

    private Partida partida() {
        return Juego.get(requireContext()).partida();
    }

    private void construirStats(LayoutInflater inf, LinearLayout panel) {
        TextView titulo = Ui.texto(requireContext(), "Bienestar", R.style.Subtitulo);
        panel.addView(titulo);
        for (Stat s : Stat.values()) {
            View fila = inf.inflate(R.layout.item_stat, panel, false);
            ((TextView) fila.findViewById(R.id.tvEtiqueta)).setText(s.icono + " " + s.nombre);
            barras[s.ordinal()] = fila.findViewById(R.id.barra);
            barras[s.ordinal()].configurar(requireContext().getColor(COLORES_STAT[s.ordinal()]), 20);
            valores[s.ordinal()] = fila.findViewById(R.id.tvValor);
            panel.addView(fila);
        }
        View fila = inf.inflate(R.layout.item_stat, panel, false);
        ((TextView) fila.findViewById(R.id.tvEtiqueta)).setText("☢ Aliento");
        int i = Stat.values().length;
        barras[i] = fila.findViewById(R.id.barra);
        barras[i].configurar(requireContext().getColor(R.color.atomico), 0);
        valores[i] = fila.findViewById(R.id.tvValor);
        fila.setTag("aliento");
        panel.addView(fila);
    }

    private void construirAcciones(GridLayout grid) {
        Object[][] acciones = {
                {"🐟", "Alimentar", R.drawable.bg_boton_naranja},
                {"🎾", "Jugar", R.drawable.bg_boton_amarillo},
                {"💤", "Dormir", R.drawable.bg_boton_azul},
                {"🫧", "Bañar", R.drawable.bg_boton_turquesa},
                {"⛑", "Curar", R.drawable.bg_boton_rosa},
                {"💪", "Entrenar", R.drawable.bg_boton_rojo}};
        for (int i = 0; i < acciones.length; i++) {
            TextView b = Ui.boton(requireContext(), acciones[i][0] + "\n" + acciones[i][1], (Integer) acciones[i][2]);
            b.setTextSize(14);
            b.setLineSpacing(0, 1.05f);
            b.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 3), GridLayout.spec(i % 3, 1f));
            lp.width = 0;
            lp.setMargins(Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4));
            final int indice = i;
            b.setOnClickListener(x -> accion(indice));
            grid.addView(b, lp);
            botones.add(b);
        }
    }

    private void accion(int i) {
        Partida p = partida();
        if (p == null) return;
        long ahora = System.currentTimeMillis();
        Juego j = Juego.get(requireContext());
        switch (i) {
            case 0: elegirComida(p); return;
            case 1: ejecutar(Cuidados.jugar(p, ahora)); return;
            case 2: ejecutar(Cuidados.dormir(p, ahora, j.velocidad())); return;
            case 3: ejecutar(Cuidados.banar(p, ahora)); return;
            case 4: ejecutar(Cuidados.curar(p)); return;
            default: ejecutar(Cuidados.entrenar(p, ahora));
        }
    }

    private void elegirComida(Partida p) {
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) {
            ejecutar(Cuidados.alimentar(p, Catalogo.ATUN));
            return;
        }
        List<Articulo> comidas = new ArrayList<>();
        for (Articulo a : Catalogo.porTipo(TipoArticulo.COMIDA)) if (p.getInventario().posee(a.id)) comidas.add(a);
        if (comidas.isEmpty()) {
            Sonidos.error(requireContext());
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin comida 🐟")
                    .setMessage("No tienes alimentos en el inventario. Cómpralos en la tienda con tus monedas.")
                    .setPositiveButton("Ir a la tienda", (d, w) -> ((MainActivity) requireActivity()).mostrarPestana(2, false))
                    .setNegativeButton("Cerrar", null)
                    .show();
            return;
        }
        String[] opciones = new String[comidas.size()];
        for (int k = 0; k < comidas.size(); k++) {
            Articulo a = comidas.get(k);
            opciones[k] = a.icono + "  " + a.nombre + "  ×" + p.getInventario().cantidad(a.id) + "\n      " + a.resumenEfectos();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Qué le das de comer?")
                .setItems(opciones, (d, w) -> ejecutar(Cuidados.alimentar(p, comidas.get(w).id)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void tocarKaiju() {
        Partida p = partida();
        if (p == null) return;
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) {
            ejecutar(p.tocarHuevo());
        } else {
            Sonidos.toque(requireContext());
            mostrarMensaje(p.getPersonaje().getNombre() + ": " + p.getEstado().mensaje);
        }
    }

    private void ejecutar(Resultado r) {
        if (r == null || !isAdded()) return;
        reaccionar(r);
        if (r.reaccion == Reaccion.ALIENTO || r.reaccion == Reaccion.ECLOSION) Sonidos.rugido(requireContext());
        ((MainActivity) requireActivity()).despuesDeAccion(r);
    }

    /** Animación + mensaje contextual que explica la reacción. */
    public void reaccionar(Resultado r) {
        if (petView == null) return;
        petView.reaccionar(r.reaccion);
        mostrarMensaje((r.ok ? "✔ " : "✖ ") + r.mensaje);
    }

    private void mostrarMensaje(String m) {
        ultimoMensaje = m;
        mensajeHasta = System.currentTimeMillis() + DURACION_MENSAJE_MS;
        if (tvMensaje != null) tvMensaje.setText(m);
    }

    @Override
    public void refrescar() {
        Partida p = partida();
        if (p == null || petView == null) return;
        Apariencia ap = Apariencia.de(p);
        if (p.consumirEvolucionPendiente()) {
            petView.mostrarEvolucion(aparienciaAnterior(ap));
            if (getView() instanceof android.widget.ScrollView) ((android.widget.ScrollView) getView()).smoothScrollTo(0, 0);
            Sonidos.rugido(requireContext());
            Juego.get(requireContext()).guardar();
        }
        petView.setApariencia(ap);
        boolean huevo = p.getPersonaje().getEtapa() == Etapa.HUEVO;
        tvEstado.setText(huevo ? "🥚 Huevo" : p.getPersonaje().nombreForma() + " · " + p.getEstado().nombre);

        if (System.currentTimeMillis() > mensajeHasta) {
            tvMensaje.setText(huevo
                    ? "🥚 ¡Toca el huevo para ayudarlo a eclosionar! (" + p.getPersonaje().getToquesHuevo() + "/5)"
                    : "💬 " + p.getEstado().mensaje + pista(p));
        } else if (ultimoMensaje != null) {
            tvMensaje.setText(ultimoMensaje);
        }
        for (Stat s : Stat.values()) {
            int v = p.getNecesidades().valor(s);
            barras[s.ordinal()].setValor(v, 100);
            valores[s.ordinal()].setText(String.valueOf(v));
            valores[s.ordinal()].setTextColor(requireContext().getColor(v < 20 ? R.color.rojo : R.color.texto));
        }
        int i = Stat.values().length;
        int carga = (int) p.getCargaAliento();
        barras[i].setValor(carga, 100);
        valores[i].setText(p.alientoDesbloqueado() ? String.valueOf(carga) : "🔒");
        boolean alientoListo = p.alientoDesbloqueado() && carga >= 100 && !p.getPersonaje().isDormido();
        btnAliento.setVisibility(alientoListo ? View.VISIBLE : View.GONE);

        botones.get(0).setText("🐟\nAlimentar (" + contarComida(p) + ")");
        botones.get(2).setText(p.getPersonaje().isDormido() ? "☀\nDespertar" : "💤\nDormir");
        botones.get(3).setText("🫧\nBañar" + (p.getInventario().posee(Catalogo.JABON) ? " (" + p.getInventario().cantidad(Catalogo.JABON) + ")" : ""));
        botones.get(4).setText("⛑\nCurar (" + p.getInventario().cantidad(Catalogo.BOTIQUIN) + ")");
        for (TextView b : botones) b.setAlpha(huevo ? 0.45f : 1f);
    }

    private String pista(Partida p) {
        Stat critico = p.getNecesidades().peorCritico();
        if (critico != null) return "\n⚠ ¡" + critico.nombre + " en nivel crítico!";
        if (p.getPersonaje().isDormido()) {
            long seg = Math.max(0, (p.getPersonaje().getDespertarEn() - System.currentTimeMillis()) / 1000);
            return String.format(java.util.Locale.US, "\n⏰ Despierta en %d:%02d", seg / 60, seg % 60);
        }
        return "";
    }

    private int contarComida(Partida p) {
        int n = 0;
        for (Articulo a : Catalogo.porTipo(TipoArticulo.COMIDA)) n += p.getInventario().cantidad(a.id);
        return n;
    }

    private static Apariencia aparienciaAnterior(Apariencia a) {
        Etapa previa = a.etapa == Etapa.FINAL ? Etapa.JUVENIL : a.etapa == Etapa.JUVENIL ? Etapa.CRIA : Etapa.HUEVO;
        return new Apariencia(previa, RutaEvolucion.NINGUNA, a.origen, a.animo, 4);
    }
}
