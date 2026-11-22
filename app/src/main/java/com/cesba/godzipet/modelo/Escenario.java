package com.cesba.godzipet.modelo;

/** Escenarios japoneses de las películas. Se desbloquean por nivel y se visitan desde el mapa. */
public enum Escenario {
    ISLA_ODO("Isla Odo", "大戸島", "Godzilla (1954)", "La aldea pesquera donde todo comenzó.", 1),
    BAHIA_TOKIO("Bahía de Tokio", "東京湾", "Godzilla (1954)", "Por aquí emergió para llegar a Shinagawa. Nadar aquí enfría su radiación el doble.", 1),
    GINZA("Ginza 1954", "銀座", "Godzilla (1954)", "La torre del reloj Wako marca la hora real de tu teléfono.", 2),
    MONTE_FUJI("Monte Fuji", "富士山", "King Kong vs. Godzilla (1962)", "El paisaje más clásico de Japón.", 2),
    TORRE_TOKIO("Torre de Tokio", "東京タワー", "Mothra vs. Godzilla (1964)", "Toca la punta de la torre… alguien podría visitarte.", 3),
    OSAKA("Castillo de Osaka", "大阪城", "Godzilla Raids Again (1955)", "Donde luchó contra Anguirus.", 4),
    KAMAKURA("Kamakura", "鎌倉", "Shin Godzilla (2016)", "El Gran Buda vigila la costa.", 5),
    ISLA_MONSTRUOS("Isla de los Monstruos", "怪獣島", "Destroy All Monsters (1968)", "El paraíso de los kaiju: aquí la felicidad no baja.", 6);

    public final String nombre;
    public final String kanji;
    public final String pelicula;
    public final String descripcion;
    public final int nivelRequerido;

    Escenario(String nombre, String kanji, String pelicula, String descripcion, int nivelRequerido) {
        this.nombre = nombre;
        this.kanji = kanji;
        this.pelicula = pelicula;
        this.descripcion = descripcion;
        this.nivelRequerido = nivelRequerido;
    }

    public static Escenario desde(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            return ISLA_ODO;
        }
    }
}
