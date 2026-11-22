package com.cesba.godzipet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Personaje;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuMiniView;

import java.util.ArrayList;
import java.util.List;

/** Selección de personaje: origen del huevo (imagen, descripción, ventaja) y nombre. */
public class SeleccionActivity extends AppCompatActivity {
    private OrigenHuevo elegido = OrigenHuevo.ISLA_ODO;
    private final List<View> tarjetas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_seleccion);
        LinearLayout cont = findViewById(R.id.contenedorHuevos);
        LayoutInflater inf = LayoutInflater.from(this);
        for (OrigenHuevo o : OrigenHuevo.values()) {
            View v = inf.inflate(R.layout.item_huevo, cont, false);
            ((KaijuMiniView) v.findViewById(R.id.vistaHuevo)).setApariencia(
                    new Apariencia(Etapa.HUEVO, RutaEvolucion.NINGUNA, o, EstadoAnimo.NORMAL, 0));
            ((TextView) v.findViewById(R.id.tvNombre)).setText(o.nombre);
            ((TextView) v.findViewById(R.id.tvDescripcion)).setText(o.descripcion);
            ((TextView) v.findViewById(R.id.tvBono)).setText("★ " + o.bono);
            v.setTag(o);
            v.setOnClickListener(x -> {
                elegido = o;
                Sonidos.toque(this);
                marcar();
            });
            tarjetas.add(v);
            cont.addView(v);
        }
        marcar();
        findViewById(R.id.btnIncubar).setOnClickListener(v -> incubar());
    }

    private void marcar() {
        for (View v : tarjetas) {
            boolean si = v.getTag() == elegido;
            ((TextView) v.findViewById(R.id.tvCheck)).setText(si ? "✅" : "⚪");
            v.setAlpha(si ? 1f : 0.6f);
            v.setContentDescription(((OrigenHuevo) v.getTag()).nombre + (si ? ", seleccionado" : ""));
        }
    }

    private void incubar() {
        EditText et = findViewById(R.id.etNombre);
        TextView err = findViewById(R.id.tvError);
        String nombre;
        try {
            nombre = Personaje.validarNombre(et.getText().toString());
        } catch (IllegalArgumentException e) {
            err.setText(e.getMessage());
            err.setVisibility(View.VISIBLE);
            Sonidos.error(this);
            return;
        }
        findViewById(R.id.btnIncubar).setEnabled(false);
        Juego juego = Juego.get(this);
        long uid = juego.prefs().usuarioId();
        Partida p = Partida.nueva(nombre, elegido, System.currentTimeMillis());
        juego.repo().borrarPartida(uid, () -> {
            juego.establecer(uid, p);
            juego.guardar();
            Sonidos.ok(this);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(new Intent(this, InicioActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            startActivity(i);
            finish();
        });
    }
}
