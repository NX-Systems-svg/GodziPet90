package com.cesba.godzipet.juego;

import android.content.Context;

import com.cesba.godzipet.datos.Preferencias;
import com.cesba.godzipet.datos.Repositorio;
import com.cesba.godzipet.modelo.Partida;

/**
 * Punto único de acceso a la partida en memoria. Las pantallas leen de aquí y,
 * tras cualquier cambio, llaman a {@link #guardar()} para persistir.
 */
public final class Juego {
    private static Juego instancia;

    private final Context app;
    private final Preferencias prefs;
    private final Repositorio repo;
    private Partida partida;
    private long uid = -1;

    private Juego(Context ctx) {
        app = ctx.getApplicationContext();
        prefs = new Preferencias(app);
        repo = Repositorio.get(app);
    }

    public static synchronized Juego get(Context ctx) {
        if (instancia == null) instancia = new Juego(ctx);
        return instancia;
    }

    public Preferencias prefs() { return prefs; }
    public Repositorio repo() { return repo; }
    public Partida partida() { return partida; }
    public long uid() { return uid; }
    public boolean cargada() { return partida != null && uid == prefs.usuarioId(); }

    public void establecer(long uid, Partida p) {
        this.uid = uid;
        this.partida = p;
    }

    public double velocidad() { return prefs.velocidad(); }

    /** Aplica el paso del tiempo real. */
    public void tick() {
        if (partida != null) partida.actualizar(System.currentTimeMillis(), velocidad());
    }

    public void guardar() {
        if (partida != null && uid > 0) repo.guardar(uid, partida);
    }

    /** Garantiza que la partida esté en memoria (p. ej. tras cerrar la app). */
    public void asegurar(Runnable listo, Runnable sinPartida) {
        if (cargada()) {
            listo.run();
            return;
        }
        long id = prefs.usuarioId();
        if (id <= 0) {
            sinPartida.run();
            return;
        }
        repo.cargar(id, p -> {
            if (p == null) {
                sinPartida.run();
            } else {
                establecer(id, p);
                tick();
                listo.run();
            }
        });
    }

    // ------------------------------------------------------------ modo profesor

    /**
     * Respalda la partida real y activa el modo profesor SÓLO si el respaldo se verificó.
     * El callback recibe true si el modo quedó activo.
     */
    public void activarProfesor(Repositorio.Callback<Boolean> listo) {
        if (prefs.profesor()) {
            listo.listo(true);
            return;
        }
        if (partida == null || uid <= 0) {
            listo.listo(false);
            return;
        }
        repo.respaldar(uid, partida, ok -> {
            if (ok) prefs.setProfesor(true);
            listo.listo(ok);
        });
    }

    /**
     * Restaura la partida real y sale del modo profesor. Si no se encuentra el respaldo, NO se
     * sobrescribe nada y el modo sigue activo (callback con false).
     */
    public void desactivarProfesor(Repositorio.Callback<Boolean> listo) {
        if (!prefs.profesor() || uid <= 0) {
            prefs.setProfesor(false);
            listo.listo(true);
            return;
        }
        repo.restaurarRespaldo(uid, p -> {
            if (p == null) {
                listo.listo(false);
                return;
            }
            partida = p;
            prefs.setProfesor(false);
            listo.listo(true);
        });
    }

    public void olvidar() {
        partida = null;
        uid = -1;
    }
}
