package com.cesba.godzipet.modelo;

/** Compras: descuenta monedas, registra en inventario y bloquea operaciones inválidas. */
public final class Tienda {
    private Tienda() { }

    public static Resultado comprar(Partida p, String articuloId) {
        Articulo a = Catalogo.buscar(articuloId);
        if (a == null) return Resultado.error("Ese artículo no existe.");
        if (p.getNivel() < a.nivelRequerido) return Resultado.error("Disponible desde el nivel " + a.nivelRequerido + ".");
        if (!a.esConsumible() && p.getInventario().posee(a.id)) return Resultado.error("Ya tienes esta decoración.");
        if (!p.getInventario().puedeAgregar(a.id, 1)) return Resultado.error("Inventario lleno para este artículo (máx. 99).");
        if (!p.gastarMonedas(a.precio)) {
            return Resultado.error("Saldo insuficiente: te faltan " + (a.precio - p.getMonedas()) + " monedas.");
        }
        p.getInventario().agregar(a.id, 1);
        return Resultado.exito("Compraste " + a.nombre + " por " + a.precio + " monedas.", Reaccion.MONEDAS);
    }
}
