package com.cesba.godzipet.modelo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EvolucionTest {
    @Test
    public void descuidoLlevaASalvaje() {
        assertEquals(RutaEvolucion.SALVAJE, Evolucion.decidir(40, 20, 5));
    }

    @Test
    public void habitoDeEntrenarLlevaAAtomico() {
        assertEquals(RutaEvolucion.ATOMICO, Evolucion.decidir(70, 10, 15));
    }

    @Test
    public void buenCuidadoLlevaAGuardian() {
        assertEquals(RutaEvolucion.GUARDIAN, Evolucion.decidir(80, 3, 30));
        assertEquals(RutaEvolucion.GUARDIAN, Evolucion.decidir(80, 10, 30));
    }

    @Test
    public void nivelesSeCalculanPorExperiencia() {
        assertEquals(1, Niveles.nivelPara(0));
        assertEquals(2, Niveles.nivelPara(60));
        assertEquals(6, Niveles.nivelPara(10_000));
    }
}
