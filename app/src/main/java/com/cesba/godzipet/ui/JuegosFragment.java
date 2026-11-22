package com.cesba.godzipet.ui;

import android.content.Intent;
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
import com.cesba.godzipet.modelo.Minijuego;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Record;
import com.cesba.godzipet.modelo.Resultado;

/** Selección de minijuegos con requisitos, costo de energía y récords. */
public class JuegosFragment extends Fragment implements Refrescable {
    private LinearLayout lista;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_lista, cont, false);
        lista = v.findViewById(R.id.lista);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        construir();
    }

    private void construir() {
        Partida p = Juego.get(requireContext()).partida();
        if (p == null || lista == null) return;
        lista.removeAllViews();
        lista.addView(Ui.titulo(requireContext(), "Minijuegos"));
        lista.addView(Ui.texto(requireContext(), "Gana monedas y experiencia. Cada partida cuesta energía y el premio depende de tu puntaje.", R.style.Cuerpo));
        for (Minijuego m : Minijuego.values()) {
            LinearLayout panel = Ui.panel(requireContext());
            ((LinearLayout.LayoutParams) panel.getLayoutParams()).topMargin = Ui.dp(requireContext(), 12);
            TextView nombre = Ui.texto(requireContext(), m.icono + "  " + m.nombre, R.style.Subtitulo);
            nombre.setTextSize(19);
            panel.addView(nombre);
            panel.addView(Ui.texto(requireContext(), m.instrucciones, R.style.Cuerpo));
            Record r = p.getRecord(m.id);
            TextView datos = Ui.texto(requireContext(), "⚡ Cuesta " + m.costoEnergia + " de energía   🏆 Récord: "
                    + r.mejorPuntaje + "   ✔ Victorias: " + r.victorias + "/" + r.jugadas, R.style.Cuerpo);
            datos.setTextColor(requireContext().getColor(R.color.amarillo));
            datos.setPadding(0, Ui.dp(requireContext(), 8), 0, 0);
            panel.addView(datos);
            Resultado acceso = p.puedeJugar(m);
            TextView boton = Ui.boton(requireContext(), acceso.ok ? "▶  JUGAR" : "🔒  " + acceso.mensaje,
                    acceso.ok ? R.drawable.bg_boton_verde : R.drawable.bg_boton_gris);
            boton.setLayoutParams(Ui.anchoCompleto(requireContext(), 12));
            boton.setOnClickListener(x -> abrir(m));
            panel.addView(boton);
            lista.addView(panel);
        }
    }

    private void abrir(Minijuego m) {
        Partida p = Juego.get(requireContext()).partida();
        Resultado acceso = p.puedeJugar(m);
        if (!acceso.ok) {
            Ui.aviso(requireActivity(), acceso);
            return;
        }
        Class<?> destino = m == Minijuego.DEFENSA ? DefensaActivity.class : MemoriaActivity.class;
        startActivity(new Intent(requireContext(), destino));
    }

    @Override
    public void refrescar() {
        // los requisitos cambian lentamente; se reconstruye al volver a la pestaña
    }
}
