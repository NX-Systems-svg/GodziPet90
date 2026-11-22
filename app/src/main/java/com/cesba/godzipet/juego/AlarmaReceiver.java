package com.cesba.godzipet.juego;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Recibe la alarma programada y publica la notificación local. */
public class AlarmaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String titulo = intent.getStringExtra(Alertas.EXTRA_TITULO);
        String texto = intent.getStringExtra(Alertas.EXTRA_TEXTO);
        if (titulo == null || texto == null) return;
        Alertas.mostrar(context, intent.getIntExtra(Alertas.EXTRA_ID, 1000), titulo, texto);
    }
}
