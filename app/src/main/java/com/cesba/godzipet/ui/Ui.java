package com.cesba.godzipet.ui;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Reaccion;
import com.cesba.godzipet.modelo.Resultado;
import com.google.android.material.snackbar.Snackbar;

/** Utilidades de interfaz compartidas: avisos de éxito/error y componentes estilo cómic. */
public final class Ui {
    private Ui() { }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Aviso visual + sonoro de un resultado del juego. */
    public static void aviso(Activity a, Resultado r) {
        if (r == null) return;
        sonido(a, r);
        mensaje(a, r.mensaje, r.ok ? 0xFF2E8B57 : 0xFFB3122E);
    }

    public static void sonido(Activity a, Resultado r) {
        if (r.ok) {
            if (r.reaccion == Reaccion.MONEDAS) Sonidos.moneda(a);
            else Sonidos.ok(a);
        } else {
            Sonidos.error(a);
        }
    }

    public static void mensaje(Activity a, String texto, int color) {
        View raiz = a.findViewById(R.id.raiz);
        if (raiz == null) raiz = a.findViewById(android.R.id.content);
        Snackbar s = Snackbar.make(raiz, texto, Snackbar.LENGTH_SHORT);
        s.setBackgroundTint(color);
        s.setTextColor(0xFFFFFFFF);
        View barra = a.findViewById(R.id.barraNav);
        if (barra != null && barra.isShown()) s.setAnchorView(barra);
        TextView tv = s.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) tv.setMaxLines(4);
        s.show();
    }

    /** true si el usuario desactivó las animaciones del sistema (accesibilidad). */
    public static boolean movimientoReducido(Context c) {
        try {
            return android.provider.Settings.Global.getFloat(c.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    /** "Aplasta" un botón al pulsarlo (squash) y da respuesta háptica. */
    public static void pulsar(View v) {
        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        if (movimientoReducido(v.getContext())) return;
        v.animate().cancel();
        v.setScaleX(0.9f);
        v.setScaleY(0.86f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(260)
                .setInterpolator(new android.view.animation.OvershootInterpolator(3f)).start();
    }

    public static TextView boton(Context c, String texto, int fondo) {
        TextView b = new TextView(c, null, 0, R.style.Boton);
        b.setText(texto);
        b.setBackgroundResource(fondo);
        return b;
    }

    public static LinearLayout panel(Context c) {
        LinearLayout l = new LinearLayout(c, null, 0, R.style.Panel);
        l.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView texto(Context c, String s, int estilo) {
        TextView t = new TextView(c, null, 0, estilo);
        t.setText(s);
        return t;
    }

    public static TextView titulo(Context c, String s) {
        TextView t = texto(c, s, R.style.Titulo);
        t.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 6);
        lp.bottomMargin = dp(c, 8);
        t.setLayoutParams(lp);
        t.setGravity(Gravity.START);
        return t;
    }

    public static LinearLayout.LayoutParams anchoCompleto(Context c, int margenSuperior) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, margenSuperior);
        return lp;
    }
}
