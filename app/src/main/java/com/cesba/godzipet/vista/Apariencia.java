package com.cesba.godzipet.vista;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Stat;

/** Datos visuales necesarios para dibujar al kaiju (inmutable). */
public final class Apariencia {
    public final Etapa etapa;
    public final RutaEvolucion ruta;
    public final OrigenHuevo origen;
    public final EstadoAnimo animo;
    public final int toquesHuevo;
    /** Radiación 0-100: controla el brillo de las placas dorsales (≥85 = sobrecalentado). */
    public final float radiacion;

    public Apariencia(Etapa etapa, RutaEvolucion ruta, OrigenHuevo origen, EstadoAnimo animo, int toquesHuevo) {
        this(etapa, ruta, origen, animo, toquesHuevo, 55f);
    }

    public Apariencia(Etapa etapa, RutaEvolucion ruta, OrigenHuevo origen, EstadoAnimo animo, int toquesHuevo,
                      float radiacion) {
        this.etapa = etapa;
        this.ruta = ruta;
        this.origen = origen;
        this.animo = animo;
        this.toquesHuevo = toquesHuevo;
        this.radiacion = Math.max(0, Math.min(100, radiacion));
    }

    public static Apariencia de(Partida p) {
        return new Apariencia(p.getPersonaje().getEtapa(), p.getPersonaje().getRuta(),
                p.getPersonaje().getOrigen(), p.getEstado(), p.getPersonaje().getToquesHuevo(),
                (float) p.getNecesidades().get(Stat.RADIACION));
    }

    public Apariencia conAnimo(EstadoAnimo a) {
        return new Apariencia(etapa, ruta, origen, a, toquesHuevo, radiacion);
    }

    /** Rayas granate de la serie animada de 1998 (sólo huevo de Nueva York). */
    public boolean rayasSerie() {
        return origen == OrigenHuevo.NUEVA_YORK;
    }

    /** Escala relativa del cuerpo según la etapa. */
    public float escalaEtapa() {
        switch (etapa) {
            case HUEVO: return 0.55f;
            case BEBE: return 0.52f;
            case PEQUENO: return 0.64f;
            case JUNIOR: return 0.82f;
            default: return 1f;
        }
    }
}
