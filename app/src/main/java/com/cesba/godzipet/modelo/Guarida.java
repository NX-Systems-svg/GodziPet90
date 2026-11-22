package com.cesba.godzipet.modelo;

/** Habitación personalizable: equipar / retirar decoraciones poseídas en posiciones fijas. */
public final class Guarida {
    private Guarida() { }

    public static Resultado equipar(Partida p, Slot slot, String articuloId) {
        Articulo a = Catalogo.buscar(articuloId);
        if (a == null || a.tipo != TipoArticulo.DECORACION) return Resultado.error("No es una decoración.");
        if (a.slot != slot) return Resultado.error(a.nombre + " va en: " + a.slot.nombre + ".");
        if (!p.getInventario().posee(a.id)) return Resultado.error("No puedes equipar algo que no tienes.");
        p.getDecoraciones().put(slot, a.id);
        return Resultado.exito(a.nombre + " colocado en " + slot.nombre + ".", Reaccion.JUGAR);
    }

    public static Resultado retirar(Partida p, Slot slot) {
        if (p.getDecoraciones().remove(slot) == null) return Resultado.error("No hay nada en " + slot.nombre + ".");
        return Resultado.exito("Retiraste la decoración de " + slot.nombre + ".", Reaccion.NINGUNA);
    }
}
