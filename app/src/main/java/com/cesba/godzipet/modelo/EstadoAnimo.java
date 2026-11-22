package com.cesba.godzipet.modelo;

/** Estados visuales del kaiju. Se calculan por reglas, nunca con un botón. */
public enum EstadoAnimo {
    DORMIDO("Dormido", "Zzz… está recuperando energía."),
    ENFERMO("Enfermo", "Su salud está baja. ¡Usa un botiquín H.E.A.T.!"),
    HAMBRIENTO("Hambriento", "Le ruge el estómago más fuerte que él. Dale pescado."),
    SUCIO("Sucio", "Huele a alcantarilla de Nueva York. Necesita un baño."),
    CANSADO("Cansado", "Apenas puede mantener la cola en alto. Mándalo a dormir."),
    TRISTE("Triste", "Se siente solo. Juega con él o llévalo a un minijuego."),
    FELIZ("Feliz", "¡Está de excelente humor! Ruge de alegría."),
    NORMAL("Normal", "Vigila la bahía tranquilamente.");

    public final String nombre;
    public final String mensaje;

    EstadoAnimo(String nombre, String mensaje) {
        this.nombre = nombre;
        this.mensaje = mensaje;
    }

    /**
     * Reglas de transición (por prioridad):
     * dormido → salud&lt;30 enfermo → alim&lt;25 hambriento → higiene&lt;25 sucio →
     * energía&lt;20 cansado → felicidad&lt;35 triste → felicidad≥75 y promedio≥65 feliz → normal.
     */
    public static EstadoAnimo calcular(Necesidades n, boolean dormido) {
        if (dormido) return DORMIDO;
        if (n.get(Stat.SALUD) < 30) return ENFERMO;
        if (n.get(Stat.ALIMENTACION) < 25) return HAMBRIENTO;
        if (n.get(Stat.HIGIENE) < 25) return SUCIO;
        if (n.get(Stat.ENERGIA) < 20) return CANSADO;
        if (n.get(Stat.FELICIDAD) < 35) return TRISTE;
        if (n.get(Stat.FELICIDAD) >= 75 && n.promedio() >= 65) return FELIZ;
        return NORMAL;
    }
}
