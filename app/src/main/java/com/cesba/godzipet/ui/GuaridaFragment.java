package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.modelo.Articulo;
import com.cesba.godzipet.modelo.Catalogo;
import com.cesba.godzipet.modelo.Guarida;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Slot;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.GuaridaView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Habitación personalizable: 4 posiciones, sólo se equipa lo que se posee. */
public class GuaridaFragment extends Fragment implements Refrescable {
    private static final String[] ICONOS_SLOT = {"🪟", "◀", "▶", "⬇"};
    private GuaridaView guarida;
    private final Map<Slot, TextView> etiquetas = new EnumMap<>(Slot.class);

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_lista, cont, false);
        LinearLayout lista = v.findViewById(R.id.lista);
        lista.addView(Ui.titulo(requireContext(), "La guarida"));

        LinearLayout marco = Ui.panel(requireContext());
        marco.setPadding(Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 8), Ui.dp(requireContext(), 8));
        guarida = new GuaridaView(requireContext());
        marco.addView(guarida, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(requireContext(), 300)));
        lista.addView(marco);
        guarida.setAlTocar(this::elegir);

        lista.addView(Ui.texto(requireContext(), "Toca un espacio de la guarida o usa los botones para colocar, cambiar o retirar decoraciones. Se compran en la tienda.", R.style.Cuerpo));
        for (Slot s : Slot.values()) {
            LinearLayout fila = Ui.panel(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ((LinearLayout.LayoutParams) fila.getLayoutParams()).topMargin = Ui.dp(requireContext(), 10);
            LinearLayout textos = new LinearLayout(requireContext());
            textos.setOrientation(LinearLayout.VERTICAL);
            textos.addView(Ui.texto(requireContext(), ICONOS_SLOT[s.ordinal()] + "  " + s.nombre, R.style.Subtitulo));
            TextView actual = Ui.texto(requireContext(), "", R.style.Cuerpo);
            textos.addView(actual);
            etiquetas.put(s, actual);
            fila.addView(textos, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView btn = Ui.boton(requireContext(), "CAMBIAR", R.drawable.bg_boton_turquesa);
            btn.setTextSize(13);
            btn.setOnClickListener(x -> elegir(s));
            fila.addView(btn);
            lista.addView(fila);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
    }

    private void elegir(Slot slot) {
        Partida p = Juego.get(requireContext()).partida();
        guarida.setResaltado(slot);
        List<Articulo> propias = new ArrayList<>();
        for (Articulo a : Catalogo.decoracionesDe(slot)) if (p.getInventario().posee(a.id)) propias.add(a);
        String actual = p.getDecoraciones().get(slot);
        List<String> opciones = new ArrayList<>();
        for (Articulo a : propias) opciones.add(a.icono + "  " + a.nombre + (a.id.equals(actual) ? "  (en uso)" : ""));
        AlertDialog.Builder d = new AlertDialog.Builder(requireContext())
                .setTitle(slot.nombre + " · " + slot.descripcion)
                .setOnDismissListener(x -> guarida.setResaltado(null));
        if (propias.isEmpty()) {
            d.setMessage("No tienes decoraciones para esta posición. Disponibles en la tienda: "
                    + nombres(Catalogo.decoracionesDe(slot)) + ".")
                    .setPositiveButton("Ir a la tienda", (x, w) -> ((MainActivity) requireActivity()).mostrarPestana(2, false));
        } else {
            d.setItems(opciones.toArray(new String[0]), (x, w) ->
                    ((MainActivity) requireActivity()).despuesDeAccion(Guarida.equipar(p, slot, propias.get(w).id)));
        }
        if (actual != null) {
            d.setNeutralButton("Retirar", (x, w) -> ((MainActivity) requireActivity()).despuesDeAccion(Guarida.retirar(p, slot)));
        }
        d.setNegativeButton("Cerrar", null).show();
    }

    private static String nombres(List<Articulo> l) {
        StringBuilder sb = new StringBuilder();
        for (Articulo a : l) sb.append(sb.length() > 0 ? ", " : "").append(a.nombre).append(" (").append(a.precio).append(")");
        return sb.toString();
    }

    @Override
    public void refrescar() {
        Partida p = Juego.get(requireContext()).partida();
        if (p == null || guarida == null) return;
        guarida.setDatos(p.getDecoraciones(), Apariencia.de(p));
        for (Slot s : Slot.values()) {
            Articulo a = Catalogo.buscar(p.getDecoraciones().get(s));
            etiquetas.get(s).setText(a == null ? "Vacío" : a.icono + " " + a.nombre);
        }
    }
}
