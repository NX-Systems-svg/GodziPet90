package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Minijuego;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minijuego 2 (memoria por parejas con temporizador). */
public class MemoriaActivity extends MinijuegoActivity {
    private static final String[] SIMBOLOS = {"🐟", "🚁", "🥚", "⚡", "🦀", "🗽", "⚓", "☢"};
    private static final int SEGUNDOS = 60;
    private static final int PARES = SIMBOLOS.length;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> cartas = new ArrayList<>();
    private final TextView[] vistas = new TextView[PARES * 2];
    private final boolean[] encontrada = new boolean[PARES * 2];
    private TextView tvTiempo, tvPares, tvIntentos;
    private CountDownTimer temporizador;
    private int primera = -1, segunda = -1, pares, intentos, restantes;
    private boolean jugando;

    @Override
    protected Minijuego minijuego() {
        return Minijuego.MEMORIA;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_memoria);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvPares = findViewById(R.id.tvPares);
        tvIntentos = findViewById(R.id.tvIntentos);
        findViewById(R.id.btnSalir).setOnClickListener(v -> finish());
        construirTablero();
        for (int i = 0; i < vistas.length; i++) tapar(i);
        restantes = SEGUNDOS;
        actualizarHud();
        listoParaEmpezar();
    }

    private void construirTablero() {
        GridLayout grid = findViewById(R.id.grid);
        for (int i = 0; i < vistas.length; i++) {
            TextView t = new TextView(this);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(30);
            t.setBackgroundResource(R.drawable.bg_boton_gris);
            t.setClickable(true);
            t.setFocusable(true);
            final int idx = i;
            t.setOnClickListener(v -> voltear(idx));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(i / 4, 1f), GridLayout.spec(i % 4, 1f));
            lp.width = 0;
            lp.height = 0;
            int m = Ui.dp(this, 4);
            lp.setMargins(m, m, m, m);
            grid.addView(t, lp);
            vistas[i] = t;
        }
    }

    @Override
    protected void comenzarRonda() {
        cartas.clear();
        for (String s : SIMBOLOS) {
            cartas.add(s);
            cartas.add(s);
        }
        Collections.shuffle(cartas);
        for (int i = 0; i < vistas.length; i++) {
            encontrada[i] = false;
            tapar(i);
        }
        primera = segunda = -1;
        pares = intentos = 0;
        restantes = SEGUNDOS;
        jugando = true;
        actualizarHud();
        if (temporizador != null) temporizador.cancel();
        temporizador = new CountDownTimer(SEGUNDOS * 1000L, 1000) {
            @Override
            public void onTick(long ms) {
                restantes = (int) (ms / 1000);
                actualizarHud();
            }

            @Override
            public void onFinish() {
                restantes = 0;
                finRonda(false);
            }
        }.start();
    }

    private void voltear(int i) {
        if (!jugando || encontrada[i] || i == primera || segunda != -1) return;
        Sonidos.toque(this);
        mostrar(i);
        if (primera == -1) {
            primera = i;
            return;
        }
        segunda = i;
        intentos++;
        if (cartas.get(primera).equals(cartas.get(segunda))) {
            encontrada[primera] = encontrada[segunda] = true;
            vistas[primera].setBackgroundResource(R.drawable.bg_boton_verde);
            vistas[segunda].setBackgroundResource(R.drawable.bg_boton_verde);
            pares++;
            primera = segunda = -1;
            Sonidos.moneda(this);
            if (pares == PARES) finRonda(true);
        } else {
            final int a = primera, b = segunda;
            handler.postDelayed(() -> {
                tapar(a);
                tapar(b);
                primera = segunda = -1;
            }, 700);
        }
        actualizarHud();
    }

    private void mostrar(int i) {
        TextView t = vistas[i];
        t.animate().scaleX(0f).setDuration(90).withEndAction(() -> {
            t.setText(cartas.get(i));
            t.setBackgroundResource(R.drawable.bg_boton_amarillo);
            t.setContentDescription(cartas.get(i));
            t.animate().scaleX(1f).setDuration(90).start();
        }).start();
    }

    private void tapar(int i) {
        vistas[i].setText("?");
        vistas[i].setTextColor(getColor(R.color.texto));
        vistas[i].setBackgroundResource(R.drawable.bg_boton_gris);
        vistas[i].setContentDescription("Carta boca abajo");
    }

    private int puntaje() {
        return pares * 10 + (pares == PARES ? restantes : 0);
    }

    private void finRonda(boolean gano) {
        if (!jugando) return;
        jugando = false;
        if (temporizador != null) temporizador.cancel();
        terminar(puntaje(), gano);
    }

    @Override
    protected int abortarRonda() {
        jugando = false;
        if (temporizador != null) temporizador.cancel();
        return pares * 10;
    }

    private void actualizarHud() {
        tvTiempo.setText("⏱ " + restantes);
        tvPares.setText("✔ Parejas " + pares + "/" + PARES);
        tvIntentos.setText("🔁 Intentos " + intentos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (temporizador != null) temporizador.cancel();
    }
}
