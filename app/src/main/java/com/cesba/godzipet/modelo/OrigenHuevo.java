package com.cesba.godzipet.modelo;

/** Selección de personaje: origen del huevo con configuración inicial distinta. */
public enum OrigenHuevo {
    MANHATTAN("Huevo de Manhattan",
            "Encontrado en el Madison Square Garden. Llega con ahorros del equipo H.E.A.T.",
            "+60 monedas iniciales", 0xFF6F8F7A, 0xFF3E5A48, 60, 0, 0),
    POLINESIA("Huevo de la Polinesia",
            "Expuesto a la radiación de las pruebas nucleares. Nace con el aliento cargado.",
            "Aliento Atómico inicia al 50%", 0xFF8BC34A, 0xFF4E7D23, 0, 50, 0),
    ISLA_MONSTRUO("Huevo de Isla Monstruo",
            "Viene de una isla de kaijus. Es resistente y empieza con más salud y energía.",
            "+2 botiquines y energía al 100", 0xFF8E6C5A, 0xFF5B3E33, 0, 0, 2);

    public final String nombre;
    public final String descripcion;
    public final String bono;
    public final int colorClaro;
    public final int colorOscuro;
    public final int monedasExtra;
    public final int alientoInicial;
    public final int botiquinesExtra;

    OrigenHuevo(String nombre, String descripcion, String bono, int colorClaro, int colorOscuro,
                int monedasExtra, int alientoInicial, int botiquinesExtra) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.bono = bono;
        this.colorClaro = colorClaro;
        this.colorOscuro = colorOscuro;
        this.monedasExtra = monedasExtra;
        this.alientoInicial = alientoInicial;
        this.botiquinesExtra = botiquinesExtra;
    }

    public static OrigenHuevo desde(String nombre) {
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return MANHATTAN;
        }
    }
}
