package com.cesba.godzipet.modelo;

/**
 * Las seis acciones de cuidado + el Aliento Atómico (mecánica exclusiva #1).
 * Cada acción: valida restricciones, modifica ≥2 estadísticas, devuelve retroalimentación.
 */
public final class Cuidados {
    public static final long ESPERA_JUGAR_MS = 20_000;
    public static final long ESPERA_ENTRENAR_MS = 30_000;
    public static final long ESPERA_BANO_GRATIS_MS = 60_000;
    public static final int ENERGIA_ENTRENAR = 20;

    public enum Accion { ALIMENTAR, JUGAR, DORMIR, BANAR, CURAR, ENTRENAR }

    private Cuidados() { }

    private static Resultado validarBase(Partida p) {
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) {
            return Resultado.error("Aún es un huevo. Tócalo para ayudarlo a eclosionar.");
        }
        return null;
    }

    private static String espera(long restanteMs) {
        return "Espera " + (restanteMs / 1000 + 1) + " s antes de repetirlo.";
    }

    public static Resultado alimentar(Partida p, String articuloId) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        Articulo a = Catalogo.buscar(articuloId);
        if (a == null || a.tipo != TipoArticulo.COMIDA) return Resultado.error("Elige una comida válida.");
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido. Déjalo descansar.");
        if (!p.getInventario().posee(a.id)) return Resultado.error("No tienes " + a.nombre + ". Cómprala en la tienda.");
        if (p.getNecesidades().get(Stat.ALIMENTACION) >= 95) return Resultado.error("Está lleno, no quiere comer más.");
        p.getInventario().consumir(a.id);
        aplicarEfectos(p, a);
        p.getContadores().alimentar++;
        p.getContadores().hoyAlimentar++;
        p.sumarXp(5);
        return Resultado.exito("¡Ñam! Devoró " + a.nombre + ". " + a.resumenEfectos(), Reaccion.COMER);
    }

    public static Resultado jugar(Partida p, long ahora) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        Necesidades n = p.getNecesidades();
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        if (n.get(Stat.ENERGIA) < 15) return Resultado.error("Está muy cansado para jugar (mín. 15 ⚡).");
        long resta = p.ultimoJugar + ESPERA_JUGAR_MS - ahora;
        if (resta > 0) return Resultado.error(espera(resta));
        p.ultimoJugar = ahora;
        n.cambiar(Stat.FELICIDAD, 15);
        n.cambiar(Stat.ENERGIA, -10);
        n.cambiar(Stat.ALIMENTACION, -5);
        n.cambiar(Stat.HIGIENE, -5);
        p.getContadores().jugar++;
        p.sumarXp(5);
        return Resultado.exito("¡Persiguió un helicóptero de juguete! +15 😊  -10 ⚡", Reaccion.JUGAR);
    }

    /** Dormir o, si ya duerme, despertarlo antes de tiempo. */
    public static Resultado dormir(Partida p, long ahora, double velocidad) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        Personaje k = p.getPersonaje();
        Necesidades n = p.getNecesidades();
        if (k.isDormido()) {
            k.despertar();
            n.cambiar(Stat.FELICIDAD, -5);
            return Resultado.exito("Lo despertaste antes de tiempo. Está gruñón (-5 😊).", Reaccion.ERROR);
        }
        if (n.get(Stat.ENERGIA) >= 85) return Resultado.error("No tiene sueño (energía ≥ 85).");
        k.dormir(ahora + (long) (Partida.MINUTOS_SUENO * 60000 / velocidad));
        n.cambiar(Stat.ENERGIA, 5);
        n.cambiar(Stat.SALUD, 3);
        p.getContadores().dormir++;
        p.sumarXp(3);
        return Resultado.exito("Se acurrucó en el muelle… Zzz. Recupera energía mientras duerme.", Reaccion.DORMIR);
    }

    public static Resultado banar(Partida p, long ahora) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        Necesidades n = p.getNecesidades();
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        if (n.get(Stat.HIGIENE) >= 95) return Resultado.error("Ya está reluciente.");
        String msg;
        if (p.getInventario().consumir(Catalogo.JABON)) {
            Articulo jabon = Catalogo.buscar(Catalogo.JABON);
            aplicarEfectos(p, jabon);
            msg = "¡Baño con jabón marino! " + jabon.resumenEfectos();
        } else {
            long resta = p.ultimoBanoGratis + ESPERA_BANO_GRATIS_MS - ahora;
            if (resta > 0) return Resultado.error("Sin jabón. Chapuzón gratis en " + (resta / 1000 + 1) + " s, o compra jabón.");
            p.ultimoBanoGratis = ahora;
            n.cambiar(Stat.HIGIENE, 15);
            n.cambiar(Stat.ENERGIA, -3);
            msg = "Chapuzón en la bahía (sin jabón): +15 🫧  -3 ⚡";
        }
        p.getContadores().banar++;
        p.sumarXp(4);
        return Resultado.exito(msg, Reaccion.BANAR);
    }

    public static Resultado curar(Partida p) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        if (p.getNecesidades().get(Stat.SALUD) >= 95) return Resultado.error("Está completamente sano.");
        if (!p.getInventario().posee(Catalogo.BOTIQUIN)) return Resultado.error("No tienes botiquines H.E.A.T. Cómpralos en la tienda.");
        p.getInventario().consumir(Catalogo.BOTIQUIN);
        Articulo b = Catalogo.buscar(Catalogo.BOTIQUIN);
        aplicarEfectos(p, b);
        p.getContadores().curar++;
        p.sumarXp(5);
        return Resultado.exito("El Dr. Tatopoulos lo atendió. " + b.resumenEfectos(), Reaccion.CURAR);
    }

    public static Resultado entrenar(Partida p, long ahora) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        Necesidades n = p.getNecesidades();
        int costo = costoEntrenar(p);
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        if (n.get(Stat.SALUD) < 30) return Resultado.error("Está enfermo, no puede entrenar.");
        if (n.get(Stat.ENERGIA) < costo + 5) return Resultado.error("Necesita " + (costo + 5) + " de energía para entrenar.");
        if (n.get(Stat.ALIMENTACION) < 20) return Resultado.error("Tiene demasiada hambre para entrenar.");
        long resta = p.ultimoEntrenar + ESPERA_ENTRENAR_MS - ahora;
        if (resta > 0) return Resultado.error(espera(resta));
        p.ultimoEntrenar = ahora;
        n.cambiar(Stat.ENERGIA, -costo);
        n.cambiar(Stat.ALIMENTACION, -10);
        n.cambiar(Stat.HIGIENE, -10);
        n.cambiar(Stat.FELICIDAD, 5);
        p.cargarAliento(20);
        p.getContadores().entrenar++;
        p.getContadores().hoyEntrenar++;
        p.sumarXp(15);
        return Resultado.exito("¡Derribó una torre de contenedores! +15 XP, +☢ aliento, -" + costo + " ⚡", Reaccion.ENTRENAR);
    }

    public static int costoEntrenar(Partida p) {
        return (int) Math.round(ENERGIA_ENTRENAR * p.getPersonaje().getRuta().multCostoEntreno);
    }

    /** Mecánica exclusiva #1: descargar el Aliento Atómico cuando la carga llega a 100. */
    public static Resultado alientoAtomico(Partida p) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        if (!p.alientoDesbloqueado()) return Resultado.error("El Aliento Atómico se desbloquea en el nivel 3.");
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        if (p.getCargaAliento() < Partida.ALIENTO_MAX) {
            return Resultado.error("Carga insuficiente (" + (int) p.getCargaAliento() + "/100). Entrena o dale pescado radioactivo.");
        }
        if (p.getNecesidades().get(Stat.ENERGIA) < 10) return Resultado.error("Necesita 10 de energía.");
        p.consumirAliento();
        p.getNecesidades().cambiar(Stat.FELICIDAD, 25);
        p.getNecesidades().cambiar(Stat.ENERGIA, -10);
        p.sumarXp(20);
        return Resultado.exito("¡ALIENTO ATÓMICO! Un rayo verde ilumina toda la bahía. +25 😊 +20 XP", Reaccion.ALIENTO);
    }

    /** Usar un consumible desde el inventario. */
    public static Resultado usarArticulo(Partida p, String id, long ahora) {
        Articulo a = Catalogo.buscar(id);
        if (a == null || !a.esConsumible()) return Resultado.error("Este artículo no se puede usar así.");
        if (a.tipo == TipoArticulo.COMIDA) return alimentar(p, id);
        if (Catalogo.JABON.equals(id)) return banar(p, ahora);
        if (Catalogo.BOTIQUIN.equals(id)) return curar(p);
        Resultado base = validarBase(p);
        if (base != null) return base;
        if (!p.getInventario().consumir(id)) return Resultado.error("No tienes " + a.nombre + ".");
        aplicarEfectos(p, a);
        p.sumarXp(3);
        return Resultado.exito("Usaste " + a.nombre + ". " + a.resumenEfectos(), Reaccion.CURAR);
    }

    private static void aplicarEfectos(Partida p, Articulo a) {
        Necesidades n = p.getNecesidades();
        n.cambiar(Stat.ALIMENTACION, a.alimentacion);
        n.cambiar(Stat.FELICIDAD, a.felicidad);
        n.cambiar(Stat.ENERGIA, a.energia);
        n.cambiar(Stat.HIGIENE, a.higiene);
        n.cambiar(Stat.SALUD, a.salud);
        if (a.aliento > 0) p.cargarAliento(a.aliento);
    }
}
