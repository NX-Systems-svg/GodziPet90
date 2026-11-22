package com.cesba.godzipet.modelo;

/** Identidad, etapa, ruta y estado de sueño del kaiju. */
public final class Personaje {
    public static final int TOQUES_PARA_ECLOSIONAR = 5;

    private String nombre;
    private final OrigenHuevo origen;
    private Etapa etapa;
    private RutaEvolucion ruta;
    private boolean dormido;
    private long despertarEn;
    private int toquesHuevo;

    public Personaje(String nombre, OrigenHuevo origen, Etapa etapa, RutaEvolucion ruta,
                     boolean dormido, long despertarEn, int toquesHuevo) {
        this.nombre = validarNombre(nombre);
        this.origen = origen;
        this.etapa = etapa;
        this.ruta = ruta;
        this.dormido = dormido;
        this.despertarEn = despertarEn;
        this.toquesHuevo = toquesHuevo;
    }

    public static Personaje nuevo(String nombre, OrigenHuevo origen) {
        return new Personaje(nombre, origen, Etapa.HUEVO, RutaEvolucion.NINGUNA, false, 0, 0);
    }

    /** Nombre de 2 a 14 caracteres, sin espacios sobrantes. */
    public static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 2 || limpio.length() > 14) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 14 caracteres");
        }
        return limpio;
    }

    public String getNombre() { return nombre; }
    public OrigenHuevo getOrigen() { return origen; }
    public Etapa getEtapa() { return etapa; }
    public RutaEvolucion getRuta() { return ruta; }
    public boolean isDormido() { return dormido; }
    public long getDespertarEn() { return despertarEn; }
    public int getToquesHuevo() { return toquesHuevo; }

    void setEtapa(Etapa etapa) { this.etapa = etapa; }
    void setRuta(RutaEvolucion ruta) { this.ruta = ruta; }
    void dormir(long hasta) { dormido = true; despertarEn = hasta; }
    void despertar() { dormido = false; despertarEn = 0; }
    int tocarHuevo() { return ++toquesHuevo; }

    /** Nombre de la forma visible (p. ej. "Cría" o "Godzilla Atómico"). */
    public String nombreForma() {
        return etapa == Etapa.FINAL ? ruta.nombre : etapa.nombre;
    }
}
