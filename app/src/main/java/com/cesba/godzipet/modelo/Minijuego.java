package com.cesba.godzipet.modelo;

/** Minijuegos disponibles, condiciones de acceso y premio proporcional al resultado. */
public enum Minijuego {
    DEFENSA("defensa", "Defensa de Tokio", "🏙",
            "Kaijus enemigos emergen de la bahía de Tokio. Tócalos para lanzarles el aliento antes de que escapen. "
                    + "Tienes 30 segundos: destruye 15 o más para ganar. Si tu Aliento Atómico está al 100% "
                    + "puedes usarlo una vez para arrasar con todos.",
            1, 15),
    ANILLOS("anillos", "Anillos de Humo", "💨",
            "Como Minilla, el hijo de Godzilla: toca la pantalla para soplar un anillo de humo hacia los insectos "
                    + "gigantes que vuelan sobre la isla. Atrapa 10 en 30 segundos para ganar.",
            1, 10),
    MEMORIA("memoria", "Memoria Fuerza G", "🧠",
            "Encuentra las 8 parejas de cartas de la Fuerza G antes de que se acaben los 60 segundos. "
                    + "Mientras más rápido termines, mayor el premio.",
            2, 10);

    public final String id;
    public final String nombre;
    public final String icono;
    public final String instrucciones;
    public final int nivelRequerido;
    public final int costoEnergia;

    Minijuego(String id, String nombre, String icono, String instrucciones, int nivel, int energia) {
        this.id = id;
        this.nombre = nombre;
        this.icono = icono;
        this.instrucciones = instrucciones;
        this.nivelRequerido = nivel;
        this.costoEnergia = energia;
    }

    /** Monedas base (antes del multiplicador de ruta). */
    public int monedas(int puntaje, boolean gano) {
        if (this == DEFENSA) return puntaje / 5 + (gano ? 20 : 0);
        if (this == ANILLOS) return puntaje / 5 + (gano ? 15 : 0);
        return puntaje / 4 + (gano ? 15 : 0);
    }

    public int experiencia(int puntaje, boolean gano) {
        return puntaje / 10 + (gano ? 15 : 3);
    }
}
