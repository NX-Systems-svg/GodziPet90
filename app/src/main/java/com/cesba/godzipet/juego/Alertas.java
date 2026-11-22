package com.cesba.godzipet.juego;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.cesba.godzipet.R;
import com.cesba.godzipet.modelo.Deterioro;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Necesidades;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Stat;
import com.cesba.godzipet.ui.InicioActivity;

/**
 * Alertas locales: (1) necesidad crítica (hambre/higiene) y (2) fin del descanso.
 * Se programan con AlarmManager inexacto al salir de la app (no requiere permiso de alarmas exactas).
 */
public final class Alertas {
    public static final String CANAL = "kaiju_alertas";
    static final String EXTRA_TITULO = "titulo";
    static final String EXTRA_TEXTO = "texto";
    static final String EXTRA_ID = "id";
    private static final int ID_CRITICA = 1001;
    private static final int ID_DESPERTAR = 1002;

    private Alertas() { }

    public static void crearCanal(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CANAL, "Alertas del kaiju",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Necesidades críticas y fin del descanso");
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static boolean permisoConcedido(Context c) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Programa las dos alertas a partir del estado actual de la partida. */
    public static void programar(Context c, Partida p, double velocidad) {
        cancelar(c);
        if (p == null || !Juego.get(c).prefs().notificaciones() || p.getPersonaje().getEtapa() == Etapa.HUEVO) return;
        long ahora = System.currentTimeMillis();
        String nombre = p.getPersonaje().getNombre();

        if (p.getPersonaje().isDormido()) {
            alarma(c, ID_DESPERTAR, p.getPersonaje().getDespertarEn(), "☀ ¡" + nombre + " despertó!",
                    "Terminó su descanso. Ya puede jugar y entrenar.");
        }
        Necesidades n = p.getNecesidades();
        double mult = p.getPersonaje().getRuta().multDeterioro;
        double minAlim = minutosHastaCritico(n.get(Stat.ALIMENTACION), Deterioro.ALIM_HORA * mult);
        double minHig = minutosHastaCritico(n.get(Stat.HIGIENE), Deterioro.HIGIENE_HORA * mult);
        boolean hambre = minAlim <= minHig;
        double minutos = Math.max(1, Math.min(minAlim, minHig));
        long cuando = ahora + (long) (minutos * 60000 / velocidad);
        alarma(c, ID_CRITICA, cuando, hambre ? "🐟 ¡" + nombre + " tiene mucha hambre!" : "🫧 ¡" + nombre + " está muy sucio!",
                hambre ? "Su alimentación llegó a nivel crítico. Abre la app y dale de comer."
                        : "Su higiene llegó a nivel crítico. Dale un baño antes de que enferme.");
    }

    static double minutosHastaCritico(double valor, double porHora) {
        if (valor <= Necesidades.CRITICO) return 0;
        return (valor - Necesidades.CRITICO) / porHora * 60.0;
    }

    private static void alarma(Context c, int id, long cuando, String titulo, String texto) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, intent(c, id, titulo, texto));
    }

    private static PendingIntent intent(Context c, int id, String titulo, String texto) {
        Intent i = new Intent(c, AlarmaReceiver.class)
                .putExtra(EXTRA_ID, id).putExtra(EXTRA_TITULO, titulo).putExtra(EXTRA_TEXTO, texto);
        return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancelar(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        am.cancel(intent(c, ID_CRITICA, "", ""));
        am.cancel(intent(c, ID_DESPERTAR, "", ""));
    }

    static void mostrar(Context c, int id, String titulo, String texto) {
        if (!permisoConcedido(c) || !Juego.get(c).prefs().notificaciones()) return;
        crearCanal(c);
        PendingIntent abrir = PendingIntent.getActivity(c, 0, new Intent(c, InicioActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(abrir)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException e) {
            android.util.Log.w("Alertas", "Permiso de notificaciones denegado", e);
        }
    }
}
