package com.cesba.godzipet.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NecesidadesTest {
    @Test
    public void losValoresNuncaSalenDelRango() {
        Necesidades n = new Necesidades(150, -20, 50, 50, 50, 55);
        assertEquals(100, n.valor(Stat.ALIMENTACION));
        assertEquals(0, n.valor(Stat.FELICIDAD));
        n.cambiar(Stat.ENERGIA, 999);
        n.cambiar(Stat.HIGIENE, -999);
        assertEquals(100, n.valor(Stat.ENERGIA));
        assertEquals(0, n.valor(Stat.HIGIENE));
    }

    @Test
    public void detectaElPeorIndicadorCritico() {
        Necesidades n = new Necesidades(10, 15, 80, 80, 80, 55);
        assertEquals(Stat.ALIMENTACION, n.peorCritico());
        assertNull(Necesidades.iniciales().peorCritico());
    }

    @Test
    public void estadoDependeDeReglasNoDeBotones() {
        assertEquals(EstadoAnimo.DORMIDO, EstadoAnimo.calcular(Necesidades.iniciales(), true));
        assertEquals(EstadoAnimo.ENFERMO, EstadoAnimo.calcular(new Necesidades(80, 80, 80, 80, 20, 55), false));
        assertEquals(EstadoAnimo.HAMBRIENTO, EstadoAnimo.calcular(new Necesidades(10, 80, 80, 80, 80, 55), false));
        assertEquals(EstadoAnimo.FELIZ, EstadoAnimo.calcular(new Necesidades(90, 90, 90, 90, 90, 55), false));
        assertEquals(EstadoAnimo.FUSION, EstadoAnimo.calcular(new Necesidades(80, 80, 80, 80, 80, 90), false));
        assertEquals(EstadoAnimo.DEBIL, EstadoAnimo.calcular(new Necesidades(80, 80, 80, 80, 80, 10), false));
        assertEquals(EstadoAnimo.TRISTE, EstadoAnimo.calcular(new Necesidades(80, 20, 80, 80, 80, 55), false));
    }
}
