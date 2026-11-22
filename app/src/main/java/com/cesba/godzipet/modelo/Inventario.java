package com.cesba.godzipet.modelo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cantidades de artículos poseídos. Los consumibles se descuentan al usarse. */
public final class Inventario {
    public static final int MAX_POR_ARTICULO = 99;

    private final Map<String, Integer> cantidades = new LinkedHashMap<>();

    public int cantidad(String id) {
        Integer c = cantidades.get(id);
        return c == null ? 0 : c;
    }

    public boolean posee(String id) {
        return cantidad(id) > 0;
    }

    public boolean puedeAgregar(String id, int n) {
        return cantidad(id) + n <= MAX_POR_ARTICULO;
    }

    public void agregar(String id, int n) {
        if (n <= 0) throw new IllegalArgumentException("Cantidad inválida");
        if (Catalogo.buscar(id) == null) throw new IllegalArgumentException("Artículo desconocido: " + id);
        cantidades.put(id, Math.min(MAX_POR_ARTICULO, cantidad(id) + n));
    }

    /** Consume una unidad. Devuelve false si no había. */
    public boolean consumir(String id) {
        int c = cantidad(id);
        if (c <= 0) return false;
        if (c == 1) cantidades.remove(id);
        else cantidades.put(id, c - 1);
        return true;
    }

    public Map<String, Integer> comoMapa() {
        return Collections.unmodifiableMap(cantidades);
    }
}
