package com.cesba.godzipet.datos;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias simples: sesión, sonido, notificaciones y modo demostración. */
public final class Preferencias {
    public static final double VELOCIDAD_DEMO = 20.0;
    private static final String ARCHIVO = "godzipet_prefs";
    private final SharedPreferences sp;

    public Preferencias(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public long usuarioId() { return sp.getLong("usuario_id", -1); }
    public void setUsuarioId(long id) { sp.edit().putLong("usuario_id", id).apply(); }
    public void cerrarSesion() { sp.edit().remove("usuario_id").apply(); }

    public boolean sonido() { return sp.getBoolean("sonido", true); }
    public void setSonido(boolean v) { sp.edit().putBoolean("sonido", v).apply(); }

    public boolean notificaciones() { return sp.getBoolean("notificaciones", true); }
    public void setNotificaciones(boolean v) { sp.edit().putBoolean("notificaciones", v).apply(); }

    public boolean modoDemo() { return sp.getBoolean("modo_demo", false); }
    public void setModoDemo(boolean v) { sp.edit().putBoolean("modo_demo", v).apply(); }

    /** Minutos de juego por minuto real. */
    public double velocidad() { return modoDemo() ? VELOCIDAD_DEMO : 1.0; }
}
