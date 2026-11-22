package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.modelo.GestorMisiones;
import com.cesba.godzipet.modelo.Mision;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.vista.BarraView;

/** Misiones diarias (se renuevan cada día) y logros permanentes, con reclamo único. */
public class MisionesFragment extends Fragment implements Refrescable {
    private LinearLayout lista;
    private String huella = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_lista, cont, false);
        lista = v.findViewById(R.id.lista);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        huella = "";
        refrescar();
    }

    @Override
    public void refrescar() {
        Partida p = Juego.get(requireContext()).partida();
        if (p == null || lista == null) return;
        StringBuilder h = new StringBuilder(p.getContadores().diaClave);
        for (Mision m : GestorMisiones.todas()) h.append(m.progreso(p)).append(GestorMisiones.reclamada(p, m));
        if (h.toString().equals(huella)) return;
        huella = h.toString();
        lista.removeAllViews();
        lista.addView(Ui.titulo(requireContext(), "📅 Misiones diarias"));
        lista.addView(Ui.texto(requireContext(), "Se renuevan cada día a medianoche.", R.style.Cuerpo));
        for (Mision m : GestorMisiones.todas()) if (m.diaria) lista.addView(fila(p, m));
        lista.addView(Ui.titulo(requireContext(), "🏆 Logros"));
        lista.addView(Ui.texto(requireContext(), "Se cumplen una sola vez y no se reinician.", R.style.Cuerpo));
        for (Mision m : GestorMisiones.todas()) if (!m.diaria) lista.addView(fila(p, m));
    }

    private View fila(Partida p, Mision m) {
        LinearLayout panel = Ui.panel(requireContext());
        ((LinearLayout.LayoutParams) panel.getLayoutParams()).topMargin = Ui.dp(requireContext(), 10);
        boolean completa = m.completada(p);
        boolean reclamada = GestorMisiones.reclamada(p, m);
        panel.addView(Ui.texto(requireContext(), (reclamada ? "✅ " : completa ? "⭐ " : "") + m.titulo, R.style.Subtitulo));
        panel.addView(Ui.texto(requireContext(), m.descripcion, R.style.Cuerpo));

        LinearLayout progreso = new LinearLayout(requireContext());
        progreso.setGravity(android.view.Gravity.CENTER_VERTICAL);
        progreso.setPadding(0, Ui.dp(requireContext(), 8), 0, Ui.dp(requireContext(), 4));
        BarraView barra = new BarraView(requireContext());
        barra.configurar(requireContext().getColor(completa ? R.color.atomico : R.color.heat), 0);
        barra.setValor(m.progreso(p), m.meta);
        progreso.addView(barra, new LinearLayout.LayoutParams(0, Ui.dp(requireContext(), 18), 1));
        TextView num = Ui.texto(requireContext(), "  " + m.progreso(p) + "/" + m.meta, R.style.Cuerpo);
        num.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        progreso.addView(num);
        panel.addView(progreso);

        String premio = "Recompensa: 🪙 " + m.recompensaMonedas + (m.recompensaXp > 0 ? "  ⭐ " + m.recompensaXp + " XP" : "");
        TextView tvPremio = Ui.texto(requireContext(), premio, R.style.Cuerpo);
        tvPremio.setTextColor(requireContext().getColor(R.color.amarillo));
        panel.addView(tvPremio);

        String txt = reclamada ? "RECLAMADA" : completa ? "🎁  RECLAMAR" : "EN PROGRESO";
        TextView boton = Ui.boton(requireContext(), txt, completa && !reclamada ? R.drawable.bg_boton_verde : R.drawable.bg_boton_gris);
        boton.setLayoutParams(Ui.anchoCompleto(requireContext(), 10));
        boton.setEnabled(completa && !reclamada);
        boton.setOnClickListener(x -> ((MainActivity) requireActivity()).despuesDeAccion(p.reclamarMision(m.id)));
        panel.addView(boton);
        return panel;
    }
}
