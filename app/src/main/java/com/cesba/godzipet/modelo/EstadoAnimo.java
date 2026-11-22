package com.cesba.godzipet.modelo;

/** Estados visuales del kaiju (10). Se calculan por reglas, nunca con un botón. */
public enum EstadoAnimo {
    DORMIDO("Dormido", "Zzz… está recuperando energía."),
    FUSION("¡En fusión!", "¡Su núcleo se sobrecalienta! Enfríalo en el mar o con un misil de cadmio."),
    ENFERMO("Enfermo", "Su salud está baja. Usa un botiquín de la Fuerza G."),
    DEBIL("Sin radiación", "Sus placas no brillan. Llévalo a una central nuclear o dale un isótopo."),
    HAMBRIENTO("Hambriento", "Le ruge el estómago más que él. Dale de comer."),
    SUCIO("Sucio", "Está cubierto de escombros. Necesita nadar en el mar."),
    CANSADO("Cansado", "Apenas puede mantener la cola en alto. Mándalo a dormir."),
    TRISTE("Triste", "Se siente solo. Juega con él o llévalo a un minijuego."),
    FELIZ("Feliz", "¡Está de excelente humor! Ruge de alegría."),
    NORMAL("Normal", "Vigila la costa de Japón tranquilamente.");

    public final String nombre;
    public final String mensaje;

    EstadoAnimo(String nombre, String mensaje) {
        this.nombre = nombre;
        this.mensaje = mensaje;
    }

    /**
     * Reglas (por prioridad): dormido → radiación ≥ 85 en fusión → salud &lt; 30 enfermo →
     * radiación &lt; 20 débil → alim &lt; 25 hambriento → higiene &lt; 25 sucio → energía &lt; 20 cansado →
     * felicidad &lt; 35 triste → felicidad ≥ 75 y promedio ≥ 65 feliz → normal.
     */
    public static EstadoAnimo calcular(Necesidades n, boolean dormido) {
        if (dormido) return DORMIDO;
        if (n.get(Stat.RADIACION) >= Necesidades.RAD_ALTA) return FUSION;
        if (n.get(Stat.SALUD) < 30) return ENFERMO;
        if (n.get(Stat.RADIACION) < Necesidades.RAD_BAJA) return DEBIL;
        if (n.get(Stat.ALIMENTACION) < 25) return HAMBRIENTO;
        if (n.get(Stat.HIGIENE) < 25) return SUCIO;
        if (n.get(Stat.ENERGIA) < 20) return CANSADO;
        if (n.get(Stat.FELICIDAD) < 35) return TRISTE;
        if (n.get(Stat.FELICIDAD) >= 75 && n.promedio() >= 65) return FELIZ;
        return NORMAL;
    }
}
