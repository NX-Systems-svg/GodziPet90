package com.cesba.godzipet.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/** Mecánica característica: la radiación se equilibra y su exceso provoca la fusión. */
public class RadiacionTest {
    private static final long T0 = 1_800_000_000_000L;
    private static final long MIN = 60_000L;

    private Partida junior() {
        Partida p = Partida.nueva("Zilla", OrigenHuevo.ISLA_ODO, T0);
        p.setAzar(new Random(3));
        ModoProfesor.fijarNivel(p, Evolucion.NIVEL_JUNIOR, null);
        p.consumirEventos();
        return p;
    }

    @Test
    public void laRadiacionTieneZonaIdealNoMasEsMejor() {
        Necesidades n = Necesidades.iniciales();
        n.establecer(Stat.RADIACION, 55);
        assertEquals(100, n.calidad(Stat.RADIACION), 0.001);
        n.establecer(Stat.RADIACION, 100);
        assertTrue(n.calidad(Stat.RADIACION) < 50);
        assertTrue(n.esCritico(Stat.RADIACION));
        n.establecer(Stat.RADIACION, 10);
        assertTrue(n.esCritico(Stat.RADIACION));
    }

    @Test
    public void laRadiacionBajaTresPorHora() {
        Partida p = junior();
        p.getNecesidades().establecer(Stat.RADIACION, 60);
        p.actualizar(T0 + 120 * MIN, 1.0);
        assertEquals(54, p.getNecesidades().valor(Stat.RADIACION));
    }

    @Test
    public void laCentralSubeYNadarEnfria() {
        Partida p = junior();
        p.getNecesidades().establecer(Stat.RADIACION, 40);
        assertTrue(Cuidados.central(p, T0).ok);
        assertEquals(65, p.getNecesidades().valor(Stat.RADIACION));
        assertFalse(Cuidados.central(p, T0 + 1000).ok); // tiempo de espera
        p.getNecesidades().establecer(Stat.HIGIENE, 50);
        assertTrue(Cuidados.banar(p, T0).ok);
        assertEquals(53, p.getNecesidades().valor(Stat.RADIACION));
    }

    @Test
    public void elMisilDeCadmioEnfriaYEsSecreto() {
        Partida p = junior();
        p.getNecesidades().establecer(Stat.RADIACION, 90);
        assertTrue(Cuidados.usarArticulo(p, Catalogo.CADMIO, T0).ok);
        assertEquals(55, p.getNecesidades().valor(Stat.RADIACION));
        assertTrue(p.getSecretos().contains(Secreto.CADMIO));
    }

    @Test
    public void laFusionHaceRenacerAJuniorConLegado() {
        Partida p = junior();
        ModoProfesor.fijarForma(p, Etapa.FINAL, RutaEvolucion.HEISEI);
        p.getNecesidades().establecer(Stat.RADIACION, 100);
        p.getNecesidades().establecer(Stat.ALIMENTACION, 100);
        p.actualizar(T0 + 31 * MIN, 1.0);
        // con 3/h de descenso, 100 tarda más de 30 min en bajar de 95 → se funde
        assertEquals(Etapa.JUNIOR, p.getPersonaje().getEtapa());
        assertEquals(2, p.getGeneracion());
        assertTrue(p.multLegado() > 1.0);
        assertTrue(p.getSecretos().contains(Secreto.FUSION));
    }

    @Test
    public void antesDeJuniorNoPuedeFundirse() {
        Partida p = Partida.nueva("Mini", OrigenHuevo.ISLA_ODO, T0);
        for (int i = 0; i < 5; i++) p.tocarHuevo();
        p.getNecesidades().establecer(Stat.RADIACION, 100);
        p.actualizar(T0 + 60 * MIN, 1.0);
        assertEquals(1, p.getGeneracion());
        assertTrue(p.getNecesidades().get(Stat.RADIACION) <= Partida.RAD_MAX_PEQUENOS);
    }

    @Test
    public void deCriaElAlientoEsUnAnilloDeHumo() {
        Partida p = Partida.nueva("Mini", OrigenHuevo.ISLA_ODO, T0);
        for (int i = 0; i < 5; i++) p.tocarHuevo();
        ModoProfesor.cargarAliento(p);
        Resultado r = Cuidados.alientoAtomico(p);
        assertEquals(Reaccion.ANILLO_HUMO, r.reaccion);
        assertTrue(p.getSecretos().contains(Secreto.ANILLO_HUMO));
    }

    @Test
    public void elDestructorDeOxigenoNoSeVendePeroEsSecreto() {
        Partida p = junior();
        int monedas = p.getMonedas();
        assertFalse(Tienda.comprar(p, Catalogo.DESTRUCTOR).ok);
        assertEquals(monedas, p.getMonedas());
        assertTrue(p.getSecretos().contains(Secreto.DESTRUCTOR_OXIGENO));
    }

    @Test
    public void viajarRequiereNivelYRegistraVisitas() {
        Partida p = junior();
        assertFalse(p.viajar(Escenario.ISLA_MONSTRUOS).ok);
        assertTrue(p.viajar(Escenario.GINZA).ok);
        assertEquals(Escenario.GINZA, p.getEscenario());
        assertTrue(p.getVisitados().contains(Escenario.GINZA));
    }

    @Test
    public void modoProfesorPuedeMostrarCualquierForma() {
        Partida p = junior();
        ModoProfesor.fijarNivel(p, 6, RutaEvolucion.BURNING);
        assertEquals(Etapa.FINAL, p.getPersonaje().getEtapa());
        assertEquals(RutaEvolucion.BURNING, p.getPersonaje().getRuta());
        ModoProfesor.darTodo(p);
        assertEquals(9999, p.getMonedas());
        assertTrue(ModoProfesor.codigoValido(" gojira1954 "));
        assertFalse(ModoProfesor.codigoValido("godzilla"));
    }
}
