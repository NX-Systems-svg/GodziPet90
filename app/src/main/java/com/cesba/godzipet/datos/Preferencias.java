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

    /** Filtro visual: NINGUNO, B_N_1954, POSTER, CRT. */
    public String filtro() { return sp.getString("filtro", "NINGUNO"); }
    public void setFiltro(String f) { sp.edit().putString("filtro", f).apply(); }

    public boolean microfono() { return sp.getBoolean("microfono", true); }
    public void setMicrofono(boolean v) { sp.edit().putBoolean("microfono", v).apply(); }

    public boolean agitar() { return sp.getBoolean("agitar", true); }
    public void setAgitar(boolean v) { sp.edit().putBoolean("agitar", v).apply(); }

    /** Modo profesor activo (la partida real está respaldada). */
    public boolean profesor() { return sp.getBoolean("profesor", false); }
    public void setProfesor(boolean v) { sp.edit().putBoolean("profesor", v).apply(); }

    /** Se ingresó el código en la pantalla de título: activar al cargar la partida. */
    public boolean profesorSolicitado() { return sp.getBoolean("profesor_solicitado", false); }
    public void setProfesorSolicitado(boolean v) { sp.edit().putBoolean("profesor_solicitado", v).apply(); }

    /** Minutos de juego por minuto real. */
    public double velocidad() { return modoDemo() ? VELOCIDAD_DEMO : 1.0; }
}
