package com.cesba.godzipet.modelo;

/**
 * Herramientas del modo profesor/evaluador: permiten demostrar cualquier parte del juego
 * en vivo (nivel, forma, artículos, tiempo, eventos). La interfaz respalda la partida real
 * antes de activarlo y la restaura al salir.
 */
public final class ModoProfesor {
    public static final String CODIGO = "GOJIRA1954";

    private ModoProfesor() { }

    public static boolean codigoValido(String s) {
        return s != null && CODIGO.equalsIgnoreCase(s.trim().replace(" ", ""));
    }

    /** Fija el nivel y ajusta la etapa (y la forma final si llega a 5). */
    public static void fijarNivel(Partida p, int nivel, RutaEvolucion rutaFinal) {
        int n = Math.max(1, Math.min(Niveles.MAXIMO, nivel));
        p.setXp(Niveles.UMBRALES[n - 1]);
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) eclosionar(p);
        if (n >= Evolucion.NIVEL_FINAL) fijarForma(p, Etapa.FINAL, rutaFinal == null ? RutaEvolucion.SHOWA : rutaFinal);
        else fijarForma(p, Evolucion.etapaPorNivel(n), RutaEvolucion.NINGUNA);
    }

    /** Cambia directamente la forma visible y lanza la animación de evolución. */
    public static void fijarForma(Partida p, Etapa etapa, RutaEvolucion ruta) {
        if (etapa == Etapa.HUEVO) {
            p.getPersonaje().setEtapa(Etapa.HUEVO);
            p.getPersonaje().setRuta(RutaEvolucion.NINGUNA);
            return;
        }
        p.getPersonaje().setEtapa(etapa);
        p.getPersonaje().setRuta(etapa == Etapa.FINAL ? (ruta == RutaEvolucion.NINGUNA ? RutaEvolucion.SHOWA : ruta)
                : RutaEvolucion.NINGUNA);
        p.registrarForma();
        p.setEvolucionPendiente();
    }

    private static void eclosionar(Partida p) {
        while (p.getPersonaje().getEtapa() == Etapa.HUEVO) p.tocarHuevo();
    }

    /** Todos los artículos (5 de cada consumible y todas las decoraciones) y 9999 monedas. */
    public static void darTodo(Partida p) {
        for (Articulo a : Catalogo.todos()) {
            if (a.prohibido) continue;
            int falta = (a.esConsumible() ? 5 : 1) - p.getInventario().cantidad(a.id);
            if (falta > 0) p.getInventario().agregar(a.id, falta);
        }
        p.setMonedas(9999);
        for (Escenario e : Escenario.values()) p.getVisitados().add(e);
    }

    public static void fijarStat(Partida p, Stat s, int valor) {
        p.getNecesidades().establecer(s, valor);
    }

    /** Simula que pasaron {@code minutosJuego} minutos (con la app cerrada). */
    public static void adelantarTiempo(Partida p, double minutosJuego, double velocidad, long ahora) {
        long ms = (long) (minutosJuego * 60000 / velocidad);
        p.setUltimaActualizacion(p.getUltimaActualizacion() - ms);
        p.actualizar(ahora, velocidad);
    }

    public static void cargarAliento(Partida p) {
        p.setCargaAliento(Partida.ALIENTO_MAX);
    }

    public static void provocarAlerta(Partida p, long ahora, double velocidad) {
        if (p.getAlerta() == null) p.forzarAlerta(ahora, velocidad);
    }

    /** Deja al kaiju a 1 minuto de la fusión (requiere Junior o forma final). */
    public static Resultado provocarFusion(Partida p) {
        if (p.getPersonaje().getEtapa().ordinal() < Etapa.JUNIOR.ordinal()) {
            fijarNivel(p, Evolucion.NIVEL_JUNIOR, null);
        }
        p.getNecesidades().establecer(Stat.RADIACION, 99);
        p.setFusion(Partida.MINUTOS_FUSION - 1);
        return Resultado.exito("Radiación al 99%: la fusión ocurrirá en 1 minuto de juego. ¡Enfríalo o mira el renacer de Junior!",
                Reaccion.FUSION);
    }

    public static void viajarA(Partida p, Escenario e) {
        p.setEscenario(e);
    }

    public static void descubrirTodo(Partida p) {
        for (Secreto s : Secreto.values()) p.descubrir(s);
        for (Etapa e : Etapa.values()) {
            if (e == Etapa.FINAL) {
                for (RutaEvolucion r : RutaEvolucion.values()) {
                    if (r != RutaEvolucion.NINGUNA) p.getFormasVistas().add(Partida.claveForma(e, r));
                }
            } else {
                p.getFormasVistas().add(Partida.claveForma(e, RutaEvolucion.NINGUNA));
            }
        }
    }
}
