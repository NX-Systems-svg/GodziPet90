package com.cesba.godzipet.modelo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EvolucionTest {
    @Test
    public void descuidoLlevaAShinGodzilla() {
        assertEquals(RutaEvolucion.SHIN, Evolucion.decidir(40, 55, 20, 5));
    }

    @Test
    public void radiacionAltaSostenidaLlevaABurning() {
        assertEquals(RutaEvolucion.BURNING, Evolucion.decidir(70, 80, 2, 30));
    }

    @Test
    public void habitoDeEntrenarLlevaAHeisei() {
        assertEquals(RutaEvolucion.HEISEI, Evolucion.decidir(70, 55, 10, 15));
    }

    @Test
    public void cuidadoEquilibradoLlevaAShowa() {
        assertEquals(RutaEvolucion.SHOWA, Evolucion.decidir(80, 55, 3, 30));
        assertEquals(RutaEvolucion.SHOWA, Evolucion.decidir(80, 60, 10, 30));
    }

    @Test
    public void nivelesSeCalculanPorExperiencia() {
        assertEquals(1, Niveles.nivelPara(0));
        assertEquals(2, Niveles.nivelPara(60));
        assertEquals(6, Niveles.nivelPara(10_000));
    }

    @Test
    public void compatibilidadConNombresDeLaVersion1() {
        assertEquals(RutaEvolucion.SHOWA, RutaEvolucion.desde("GUARDIAN"));
        assertEquals(RutaEvolucion.HEISEI, RutaEvolucion.desde("ATOMICO"));
        assertEquals(Etapa.BEBE, Etapa.desde("CRIA"));
        assertEquals(Etapa.JUNIOR, Etapa.desde("JUVENIL"));
        assertEquals(OrigenHuevo.NUEVA_YORK, OrigenHuevo.desde("MANHATTAN"));
    }
}
