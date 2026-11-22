package com.cesba.godzipet.modelo;

/**
 * Las seis acciones de cuidado + el Aliento Atómico (mecánica exclusiva #1).
 * Cada acción: valida restricciones, modifica ≥2 estadísticas, devuelve retroalimentación.
 */
public final class Cuidados {
    public static final long ESPERA_JUGAR_MS = 20_000;
    public static final long ESPERA_ENTRENAR_MS = 30_000;
    public static final long ESPERA_BANO_GRATIS_MS = 60_000;
    public static final long ESPERA_CENTRAL_MS = 45_000;
    public static final int ENERGIA_ENTRENAR = 20;
    public static final int RADIACION_CENTRAL = 25;
    public static final int ENFRIAR_MAR = 12;

    public enum Accion { ALIMENTAR, JUGAR, DORMIR, BANAR, CURAR, ENTRENAR, CENTRAL }

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
        return Resultado.exito("¡Persiguió un tren bala de juguete! +15 😊  -10 ⚡", Reaccion.JUGAR);
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
        return Resultado.exito("Se acurrucó en la playa… Zzz. Recupera energía mientras duerme.", Reaccion.DORMIR);
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
            msg = "¡Nadó en el mar con jabón marino! " + jabon.resumenEfectos();
        } else {
            long resta = p.ultimoBanoGratis + ESPERA_BANO_GRATIS_MS - ahora;
            if (resta > 0) return Resultado.error("Sin jabón. Chapuzón gratis en " + (resta / 1000 + 1) + " s, o compra jabón.");
            p.ultimoBanoGratis = ahora;
            n.cambiar(Stat.HIGIENE, 15);
            n.cambiar(Stat.ENERGIA, -3);
            msg = "Nadó en el mar (sin jabón): +15 🫧  -3 ⚡";
        }
        double enfriar = p.getEscenario() == Escenario.BAHIA_TOKIO ? ENFRIAR_MAR * 2 : ENFRIAR_MAR;
        n.cambiar(Stat.RADIACION, -enfriar);
        msg += "  -" + (int) enfriar + " ☢";
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
        return Resultado.exito("Los médicos de la Fuerza G lo atendieron. " + b.resumenEfectos(), Reaccion.CURAR);
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
        return Resultado.exito("¡Derribó una torre de alta tensión! +15 XP, +🔥 aliento, -" + costo + " ⚡", Reaccion.ENTRENAR);
    }

    public static int costoEntrenar(Partida p) {
        return (int) Math.round(ENERGIA_ENTRENAR * p.getPersonaje().getRuta().multCostoEntreno);
    }

    /** Visitar una central nuclear: sube la radiación (mecánica característica). */
    public static Resultado central(Partida p, long ahora) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        long resta = p.ultimaCentral + ESPERA_CENTRAL_MS - ahora;
        if (resta > 0) return Resultado.error("La central se está recargando. " + espera(resta));
        Necesidades n = p.getNecesidades();
        p.ultimaCentral = ahora;
        double rad = RADIACION_CENTRAL * p.getPersonaje().getRuta().multRadiacion;
        n.cambiar(Stat.RADIACION, rad);
        n.cambiar(Stat.ENERGIA, 10);
        n.cambiar(Stat.FELICIDAD, 5);
        n.cambiar(Stat.HIGIENE, -5);
        p.cargarAliento(10);
        p.getContadores().centrales++;
        p.sumarXp(5);
        String aviso = n.get(Stat.RADIACION) >= Necesidades.RAD_ALTA
                ? " ⚠ ¡Cuidado, está sobrecalentado!" : n.enZonaIdeal() ? " Zona ideal ✔" : "";
        return Resultado.exito("Absorbió energía de una central nuclear: +" + (int) rad + " ☢ +10 ⚡." + aviso,
                Reaccion.CENTRAL);
    }

    /**
     * Mecánica exclusiva: Aliento Atómico con carga 100. De Baby o Little Godzilla sale un
     * anillo de humo (como Minilla en 1967); desde Junior, el rayo atómico.
     */
    public static Resultado alientoAtomico(Partida p) {
        Resultado base = validarBase(p);
        if (base != null) return base;
        if (p.getPersonaje().isDormido()) return Resultado.error("Está dormido.");
        if (p.getCargaAliento() < Partida.ALIENTO_MAX) {
            return Resultado.error("Carga insuficiente (" + (int) p.getCargaAliento() + "/100). Entrena, visita una central o dale pescado radioactivo.");
        }
        if (p.getNecesidades().get(Stat.ENERGIA) < 10) return Resultado.error("Necesita 10 de energía.");
        p.consumirAliento();
        if (p.alientoEsAnilloDeHumo()) {
            p.descubrir(Secreto.ANILLO_HUMO);
            p.getNecesidades().cambiar(Stat.FELICIDAD, 15);
            p.getNecesidades().cambiar(Stat.ENERGIA, -5);
            p.sumarXp(10);
            return Resultado.exito("Intentó lanzar su aliento… ¡y le salió un anillo de humo! Como Minilla. +15 😊", Reaccion.ANILLO_HUMO);
        }
        p.getNecesidades().cambiar(Stat.FELICIDAD, 25);
        p.getNecesidades().cambiar(Stat.ENERGIA, -10);
        p.getNecesidades().cambiar(Stat.RADIACION, -15);
        p.sumarXp(20);
        return Resultado.exito("¡ALIENTO ATÓMICO! Sus placas brillaron y un rayo azul iluminó la costa. +25 😊 -15 ☢", Reaccion.ALIENTO);
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
        if (Catalogo.CADMIO.equals(id)) {
            p.descubrir(Secreto.CADMIO);
            return Resultado.exito("¡Misil de cadmio! Su núcleo se enfrió de golpe. " + a.resumenEfectos(), Reaccion.ENFRIAR);
        }
        return Resultado.exito("Usaste " + a.nombre + ". " + a.resumenEfectos(),
                a.radiacion > 0 ? Reaccion.CENTRAL : Reaccion.CURAR);
    }

    private static void aplicarEfectos(Partida p, Articulo a) {
        Necesidades n = p.getNecesidades();
        n.cambiar(Stat.ALIMENTACION, a.alimentacion);
        n.cambiar(Stat.FELICIDAD, a.felicidad);
        n.cambiar(Stat.ENERGIA, a.energia);
        n.cambiar(Stat.HIGIENE, a.higiene);
        n.cambiar(Stat.SALUD, a.salud);
        n.cambiar(Stat.RADIACION, a.radiacion > 0 ? a.radiacion * p.getPersonaje().getRuta().multRadiacion : a.radiacion);
        if (a.aliento > 0) p.cargarAliento(a.aliento);
    }
}
