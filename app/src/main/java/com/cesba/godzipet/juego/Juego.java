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

    /** Respalda la partida real y activa el modo profesor. */
    public void activarProfesor(Runnable listo) {
        if (partida == null || uid <= 0 || prefs.profesor()) {
            listo.run();
            return;
        }
        repo.respaldar(uid, partida, () -> {
            prefs.setProfesor(true);
            listo.run();
        });
    }

    /** Restaura la partida real y sale del modo profesor. */
    public void desactivarProfesor(Runnable listo) {
        if (!prefs.profesor() || uid <= 0) {
            prefs.setProfesor(false);
            listo.run();
            return;
        }
        repo.restaurarRespaldo(uid, p -> {
            if (p != null) partida = p;
            prefs.setProfesor(false);
            listo.run();
        });
    }

    public void olvidar() {
        partida = null;
        uid = -1;
    }
}
