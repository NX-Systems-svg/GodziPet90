package com.cesba.godzipet.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 3 misiones diarias renovables + 5 logros permanentes. Evita reclamos duplicados. */
public final class GestorMisiones {
    private static final List<Mision> MISIONES = new ArrayList<>();

    static {
        MISIONES.add(new Mision("d_comer", "Hora del atún", "Alimenta a tu kaiju 3 veces hoy",
                true, 3, 30, 20, p -> p.getContadores().hoyAlimentar));
        MISIONES.add(new Mision("d_juegos", "Reflejos de kaiju", "Juega 2 minijuegos hoy",
                true, 2, 40, 25, p -> p.getContadores().hoyMinijuegos));
        MISIONES.add(new Mision("d_entreno", "Rutina de combate", "Entrena 2 veces hoy",
                true, 2, 30, 25, p -> p.getContadores().hoyEntrenar));

        MISIONES.add(new Mision("l_nivel", "Rey en ascenso", "Alcanza el nivel 4",
                false, 4, 100, 0, Partida::getNivel));
        MISIONES.add(new Mision("l_victorias", "Campeón de la bahía", "Gana 5 minijuegos",
                false, 5, 120, 50, p -> p.getContadores().victorias));
        MISIONES.add(new Mision("l_guarida", "Decorador de guaridas", "Equipa 3 decoraciones al mismo tiempo",
                false, 3, 80, 30, p -> p.getDecoraciones().size()));
        MISIONES.add(new Mision("l_defensas", "Protector de Nueva York", "Gana 3 Alertas Kaiju",
                false, 3, 150, 60, p -> p.getContadores().defensas));
        MISIONES.add(new Mision("l_evolucion", "Forma definitiva", "Alcanza una evolución final",
                false, 1, 200, 0, p -> p.getPersonaje().getEtapa() == Etapa.FINAL ? 1 : 0));
    }

    private GestorMisiones() { }

    public static List<Mision> todas() {
        return Collections.unmodifiableList(MISIONES);
    }

    public static Mision buscar(String id) {
        for (Mision m : MISIONES) if (m.id.equals(id)) return m;
        return null;
    }

    /** Las diarias se consideran reclamadas sólo si se reclamaron en el día actual. */
    public static boolean reclamada(Partida p, Mision m) {
        String marca = p.getMisionesReclamadas().get(m.id);
        if (marca == null) return false;
        return !m.diaria || marca.equals(p.getContadores().diaClave);
    }

    public static int pendientesDeReclamar(Partida p) {
        int n = 0;
        for (Mision m : MISIONES) if (m.completada(p) && !reclamada(p, m)) n++;
        return n;
    }
}
