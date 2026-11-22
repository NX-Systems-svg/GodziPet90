package com.cesba.godzipet.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo fijo de la tienda: comida japonesa, cuidados, control de radiación y decoraciones. */
public final class Catalogo {
    public static final String ONIGIRI = "onigiri";
    public static final String RAMEN = "ramen";
    public static final String ATUN = "atun";
    public static final String PESCADO_RAD = "pescado_rad";
    public static final String JABON = "jabon";
    public static final String BOTIQUIN = "botiquin";
    public static final String ISOTOPO = "isotopo";
    public static final String CADMIO = "cadmio";
    public static final String DESTRUCTOR = "destructor_oxigeno";

    private static final Map<String, Articulo> ARTICULOS = new LinkedHashMap<>();

    static {
        TipoArticulo C = TipoArticulo.COMIDA, K = TipoArticulo.CUIDADO;
        //                                                                     nv alim fel ene hig sal alie rad
        add(Articulo.consumible(ONIGIRI, "Onigiri", "Bola de arroz con alga nori. Un bocadillo rápido.",
                "🍙", 8, C, 1, 12, 3, 0, 0, 0, 0, 0));
        add(Articulo.consumible(ATUN, "Atún gigante", "Un atún entero del mercado de Tsukiji.",
                "🐟", 15, C, 1, 25, 6, 0, 0, 0, 0, 0));
        add(Articulo.consumible(RAMEN, "Ramen humeante", "Un tazón enorme, su favorito de los días fríos.",
                "🍜", 18, C, 1, 22, 10, 3, 0, 0, 0, 0));
        add(Articulo.consumible(PESCADO_RAD, "Pescado radioactivo", "Brilla en la oscuridad. Sube la radiación y carga el aliento.",
                "🐠", 30, C, 2, 15, 4, 10, 0, -2, 25, 20));
        add(Articulo.consumible(JABON, "Jabón marino", "Para que el baño en el mar deje las escamas brillantes.",
                "🧼", 12, K, 1, 0, 5, 0, 40, 0, 0, 0));
        add(Articulo.consumible(BOTIQUIN, "Botiquín Fuerza G", "Equipo médico del cuartel de la Fuerza G.",
                "⛑", 35, K, 1, 0, -4, 5, 0, 35, 0, 0));
        add(Articulo.consumible(ISOTOPO, "Isótopo energético", "Una barra de uranio brillante: energía y radiación al instante.",
                "🔋", 40, K, 2, 0, 3, 40, 0, 0, 10, 25));
        add(Articulo.consumible(CADMIO, "Misil de cadmio", "Enfría su núcleo de golpe, como en The Return of Godzilla (1984).",
                "🚀", 25, K, 1, 0, -5, 0, 0, -5, 0, -35));
        add(Articulo.prohibido(DESTRUCTOR, "Destructor de Oxígeno", "El arma del Dr. Serizawa. Solo de exhibición… ¿verdad?",
                "🧪", 99999));

        add(Articulo.decoracion("v_fuji", "Vista al Monte Fuji", "El volcán nevado por la ventana shoji.", "🗻", 60, Slot.VENTANA, 1));
        add(Articulo.decoracion("v_odo", "Isla Odo de noche", "La aldea pesquera bajo la luna.", "🌕", 80, Slot.VENTANA, 2));
        add(Articulo.decoracion("v_monstruos", "Isla de los Monstruos", "Un volcán humeante y palmeras.", "🌋", 110, Slot.VENTANA, 3));
        add(Articulo.decoracion("i_toro", "Linterna de piedra", "Un tōrō de jardín japonés con luz cálida.", "🏮", 50, Slot.IZQUIERDA, 1));
        add(Articulo.decoracion("i_torre", "Torre de Tokio miniatura", "Roja y blanca, como la que visitó Mothra.", "🗼", 120, Slot.IZQUIERDA, 4));
        add(Articulo.decoracion("d_daruma", "Muñeco Daruma", "Pinta su segundo ojo cuando cumples una meta.", "🎎", 45, Slot.DERECHA, 1));
        add(Articulo.decoracion("d_bonsai", "Bonsái de pino", "Paciencia de 100 años en una maceta.", "🌲", 70, Slot.DERECHA, 2));
        add(Articulo.decoracion("d_reloj", "Reloj de la torre Wako", "Marca la hora real, como en Ginza 1954.", "🕰", 90, Slot.DERECHA, 3));
        add(Articulo.decoracion("s_tatami", "Tatami", "Esteras de paja de arroz.", "🟫", 30, Slot.SUELO, 1));
        add(Articulo.decoracion("s_ola", "Alfombra La Gran Ola", "Inspirada en el grabado ukiyo-e de Hokusai.", "🌊", 65, Slot.SUELO, 2));
        add(Articulo.decoracion("s_koi", "Estanque koi", "Peces koi nadando en círculos.", "🎏", 85, Slot.SUELO, 3));
    }

    private Catalogo() { }

    private static void add(Articulo a) {
        ARTICULOS.put(a.id, a);
    }

    public static Articulo buscar(String id) {
        return id == null ? null : ARTICULOS.get(id);
    }

    public static List<Articulo> todos() {
        return Collections.unmodifiableList(new ArrayList<>(ARTICULOS.values()));
    }

    public static List<Articulo> porTipo(TipoArticulo tipo) {
        List<Articulo> r = new ArrayList<>();
        for (Articulo a : ARTICULOS.values()) if (a.tipo == tipo && !a.prohibido) r.add(a);
        return r;
    }

    public static List<Articulo> decoracionesDe(Slot slot) {
        List<Articulo> r = new ArrayList<>();
        for (Articulo a : ARTICULOS.values()) if (a.slot == slot) r.add(a);
        return r;
    }
}
