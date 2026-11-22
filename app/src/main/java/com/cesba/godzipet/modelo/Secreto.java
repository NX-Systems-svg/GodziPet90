package com.cesba.godzipet.modelo;

/** Easter eggs para fans. Al descubrirlos se registran en la Kaiju-pedia. */
public enum Secreto {
    SHIE("¡Shie!", "Toca su cabeza 7 veces seguidas.",
            "Godzilla hizo este baile de la victoria en Invasion of Astro-Monster (1965)."),
    ANILLO_HUMO("Anillos de humo", "Usa el aliento siendo Baby o Little Godzilla.",
            "Minilla, el hijo de Godzilla (1967), soplaba anillos de humo en vez de rayos."),
    DESTRUCTOR_OXIGENO("Destructor de Oxígeno", "Intenta comprarlo en la tienda.",
            "El arma del Dr. Serizawa que derrotó al primer Godzilla en 1954."),
    MODO_1954("Modo 1954", "Activa el filtro en blanco y negro.",
            "La película original de Ishirō Honda se filmó en blanco y negro."),
    MOTHRA("Visita de Mothra", "Toca la punta de la Torre de Tokio.",
            "Mothra tejió su capullo en la Torre de Tokio (Mothra, 1961)."),
    NOMBRE("Gorira + Kujira", "Toca el título ゴジラ en la Kaiju-pedia.",
            "'Gojira' combina gorira (gorila) y kujira (ballena)."),
    RUGIDO("El rugido", "Ruge al micrófono o escucha el rugido en la Kaiju-pedia.",
            "Akira Ifukube creó el rugido frotando una cuerda de contrabajo con un guante con resina."),
    FUSION("Meltdown", "Deja que la radiación llegue a la fusión.",
            "En Godzilla vs. Destoroyah (1995) su núcleo se fundió y Junior heredó su poder."),
    TERREMOTO("¡Terremoto!", "Agita el teléfono.",
            "Cada paso de Godzilla hacía temblar Tokio."),
    CADMIO("Misil de cadmio", "Usa un misil de cadmio.",
            "En The Return of Godzilla (1984) usaron cadmio para frenar su reactor."),
    NUEVA_YORK("Nueva York", "Elige el huevo de Nueva York.",
            "Homenaje a la serie animada de 1998 (Godzilla: The Series)."),
    FIN("終", "Reinicia una partida.",
            "Las películas japonesas clásicas terminaban con el kanji 終 (Fin).");

    public final String titulo;
    public final String pista;
    public final String dato;

    Secreto(String titulo, String pista, String dato) {
        this.titulo = titulo;
        this.pista = pista;
        this.dato = dato;
    }
}
