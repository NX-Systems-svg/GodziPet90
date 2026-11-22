package com.cesba.godzipet.vista;

import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.RutaEvolucion;

/** Datos visuales necesarios para dibujar al kaiju (inmutable). */
public final class Apariencia {
    public final Etapa etapa;
    public final RutaEvolucion ruta;
    public final OrigenHuevo origen;
    public final EstadoAnimo animo;
    public final int toquesHuevo;

    public Apariencia(Etapa etapa, RutaEvolucion ruta, OrigenHuevo origen, EstadoAnimo animo, int toquesHuevo) {
        this.etapa = etapa;
        this.ruta = ruta;
        this.origen = origen;
        this.animo = animo;
        this.toquesHuevo = toquesHuevo;
    }

    public static Apariencia de(Partida p) {
        return new Apariencia(p.getPersonaje().getEtapa(), p.getPersonaje().getRuta(),
                p.getPersonaje().getOrigen(), p.getEstado(), p.getPersonaje().getToquesHuevo());
    }

    public Apariencia conAnimo(EstadoAnimo a) {
        return new Apariencia(etapa, ruta, origen, a, toquesHuevo);
    }

    /** Escala relativa del cuerpo según la etapa. */
    public float escalaEtapa() {
        switch (etapa) {
            case HUEVO: return 0.55f;
            case CRIA: return 0.62f;
            case JUVENIL: return 0.8f;
            default: return 1f;
        }
    }
}
