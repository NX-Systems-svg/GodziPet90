package com.cesba.godzipet.modelo;

/** Mecánica exclusiva #2: un monstruo mutante ataca la ciudad durante un tiempo limitado. */
public final class AlertaKaiju {
    public static final String[] MONSTRUOS = {
            "una mantis gigante", "un pteranodón colosal", "una langosta mutante",
            "una polilla oscura", "un robot de guerra", "un dragón de tres cabezas"
    };
    public static final String[] LUGARES = {
            "Shinjuku", "Shibuya", "el puerto de Yokohama", "Shinagawa",
            "el aeropuerto de Haneda", "la bahía de Osaka"
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
        return "¡" + Character.toUpperCase(monstruo.charAt(0)) + monstruo.substring(1) + " ataca " + lugar + "!";
    }
}
