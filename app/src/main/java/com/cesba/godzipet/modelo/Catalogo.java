package com.cesba.godzipet.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo fijo de la tienda: 3 comidas, 3 cuidados y 10 decoraciones. */
public final class Catalogo {
    public static final String ATUN = "atun";
    public static final String PESCADO_RAD = "pescado_rad";
    public static final String SARDINAS = "sardinas";
    public static final String JABON = "jabon";
    public static final String BOTIQUIN = "botiquin";
    public static final String ISOTOPO = "isotopo";

    private static final Map<String, Articulo> ARTICULOS = new LinkedHashMap<>();

    static {
        TipoArticulo C = TipoArticulo.COMIDA, K = TipoArticulo.CUIDADO;
        add(Articulo.consumible(SARDINAS, "Lata de sardinas", "Un bocadillo rápido del muelle.",
                "🥫", 8, C, 1, 12, 2, 0, 0, 0, 0));
        add(Articulo.consumible(ATUN, "Atún gigante", "Su comida favorita: ¡un atún entero!",
                "🐟", 15, C, 1, 25, 6, 0, 0, 0, 0));
        add(Articulo.consumible(PESCADO_RAD, "Pescado radioactivo", "Brilla en la oscuridad. Carga el Aliento Atómico.",
                "🐠", 30, C, 2, 15, 4, 10, 0, -2, 25));
        add(Articulo.consumible(JABON, "Jabón marino", "Burbujas de agua salada para escamas brillantes.",
                "🧼", 12, K, 1, 0, 5, 0, 40, 0, 0));
        add(Articulo.consumible(BOTIQUIN, "Botiquín H.E.A.T.", "Equipo médico del laboratorio del Dr. Tatopoulos.",
                "⛑", 35, K, 1, 0, -4, 5, 0, 35, 0));
        add(Articulo.consumible(ISOTOPO, "Isótopo energético", "Una barra brillante que recarga energía al instante.",
                "🔋", 40, K, 3, 0, 3, 40, 0, 0, 10));

        add(Articulo.decoracion("v_skyline", "Skyline de Manhattan", "Las torres de NY iluminadas de noche.", "🌃", 60, Slot.VENTANA, 1));
        add(Articulo.decoracion("v_arrecife", "Arrecife submarino", "Peces y corales por la ventana.", "🪸", 80, Slot.VENTANA, 2));
        add(Articulo.decoracion("v_isla", "Isla Monstruo al atardecer", "Un volcán humeante en el horizonte.", "🌋", 110, Slot.VENTANA, 3));
        add(Articulo.decoracion("i_helicoptero", "Helicóptero de juguete", "Réplica del helicóptero militar que lo persigue.", "🚁", 50, Slot.IZQUIERDA, 1));
        add(Articulo.decoracion("i_barco", "Barco pesquero", "Un barquito blanco como el de la bahía.", "🚢", 70, Slot.IZQUIERDA, 2));
        add(Articulo.decoracion("d_antena", "Antena de radio torcida", "Doblada tras una pelea con un kaiju.", "📡", 45, Slot.DERECHA, 1));
        add(Articulo.decoracion("d_estatua", "Mini Estatua de la Libertad", "Un recuerdo de su primera victoria.", "🗽", 120, Slot.DERECHA, 4));
        add(Articulo.decoracion("s_algas", "Alfombra de algas", "Suave y húmeda, como le gusta.", "🌿", 35, Slot.SUELO, 1));
        add(Articulo.decoracion("s_conos", "Conos de tráfico aplastados", "Trofeos de sus paseos por la ciudad.", "🚧", 25, Slot.SUELO, 1));
        add(Articulo.decoracion("s_charco", "Charco radioactivo", "Brilla verde por las noches.", "☢", 55, Slot.SUELO, 3));
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
        for (Articulo a : ARTICULOS.values()) if (a.tipo == tipo) r.add(a);
        return r;
    }

    public static List<Articulo> decoracionesDe(Slot slot) {
        List<Articulo> r = new ArrayList<>();
        for (Articulo a : ARTICULOS.values()) if (a.slot == slot) r.add(a);
        return r;
    }
}
