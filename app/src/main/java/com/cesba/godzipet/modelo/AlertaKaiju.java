package com.cesba.godzipet.modelo;

/** Mecánica exclusiva #2: un monstruo mutante ataca la ciudad durante un tiempo limitado. */
public final class AlertaKaiju {
    public static final String[] MONSTRUOS = {
            "Cangrejo mutante", "Murciélago colosal", "Lagarto de las alcantarillas",
            "Calamar radioactivo", "Mantis gigante", "Cucaracha titánica"
    };
    public static final String[] LUGARES = {
            "el Puente de Brooklyn", "Times Square", "el muelle 17", "Central Park",
            "el túnel Lincoln", "la Isla de la Libertad"
    };

    public final String monstruo;
    public final String lugar;
    public final long expira;

    public AlertaKaiju(String monstruo, String lugar, long expira) {
        this.monstruo = monstruo;
        this.lugar = lugar;
        this.expira = expira;
    }

    public String descripcion() {
        return "¡Un " + monstruo + " ataca " + lugar + "!";
    }
}
