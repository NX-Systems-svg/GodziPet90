package com.cesba.godzipet.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class PartidaTest {
    private static final long T0 = 1_800_000_000_000L;
    private static final long HORA = 3_600_000L;

    private Partida nacida() {
        Partida p = Partida.nueva("Zilla", OrigenHuevo.MANHATTAN, T0);
        for (int i = 0; i < Personaje.TOQUES_PARA_ECLOSIONAR; i++) p.tocarHuevo();
        p.setAzar(new Random(1));
        return p;
    }

    @Test
    public void casoObligatorio_80DeAlimentacionBajaA64TrasDosHoras() {
        Partida p = nacida();
        p.getNecesidades().establecer(Stat.ALIMENTACION, 80);
        p.actualizar(T0 + 2 * HORA, 1.0);
        assertEquals(64, p.getNecesidades().valor(Stat.ALIMENTACION));
    }

    @Test
    public void elModoDemoAceleraElTiempo() {
        Partida p = nacida();
        p.getNecesidades().establecer(Stat.ALIMENTACION, 80);
        p.actualizar(T0 + 6 * 60_000L, 20.0); // 6 min reales x20 = 2 h de juego
        assertEquals(64, p.getNecesidades().valor(Stat.ALIMENTACION));
    }

    @Test
    public void elHuevoEclosionaConCincoToques() {
        Partida p = Partida.nueva("Zilla", OrigenHuevo.POLINESIA, T0);
        assertEquals(Etapa.HUEVO, p.getPersonaje().getEtapa());
        for (int i = 0; i < 4; i++) p.tocarHuevo();
        assertEquals(Etapa.HUEVO, p.getPersonaje().getEtapa());
        p.tocarHuevo();
        assertEquals(Etapa.CRIA, p.getPersonaje().getEtapa());
    }

    @Test
    public void alimentarConsumeInventarioYValidaExistencias() {
        Partida p = nacida();
        p.getNecesidades().establecer(Stat.ALIMENTACION, 30);
        int antes = p.getInventario().cantidad(Catalogo.ATUN);
        assertTrue(Cuidados.alimentar(p, Catalogo.ATUN).ok);
        assertEquals(antes - 1, p.getInventario().cantidad(Catalogo.ATUN));
        assertEquals(55, p.getNecesidades().valor(Stat.ALIMENTACION));
        assertFalse(Cuidados.alimentar(p, Catalogo.PESCADO_RAD).ok); // no lo tiene
    }

    @Test
    public void dormirRecuperaEnergiaYDespiertaSolo() {
        Partida p = nacida();
        p.getNecesidades().establecer(Stat.ENERGIA, 30);
        assertTrue(Cuidados.dormir(p, T0, 1.0).ok);
        assertTrue(p.getPersonaje().isDormido());
        p.actualizar(T0 + 25 * 60_000L, 1.0);
        assertFalse(p.getPersonaje().isDormido());
        assertTrue(p.getNecesidades().get(Stat.ENERGIA) > 65);
    }

    @Test
    public void lasAccionesRespetanEsperasYEnergia() {
        Partida p = nacida();
        assertTrue(Cuidados.jugar(p, T0).ok);
        assertFalse(Cuidados.jugar(p, T0 + 1000).ok);
        p.getNecesidades().establecer(Stat.ENERGIA, 5);
        assertFalse(Cuidados.entrenar(p, T0).ok);
    }

    @Test
    public void tiendaBloqueaCompraSinSaldoYDecoracionDuplicada() {
        Partida p = nacida();
        assertTrue(Tienda.comprar(p, "s_conos").ok);
        assertFalse(Tienda.comprar(p, "s_conos").ok);
        while (p.getMonedas() >= 8) Tienda.comprar(p, Catalogo.SARDINAS);
        int monedas = p.getMonedas();
        assertFalse(Tienda.comprar(p, Catalogo.ATUN).ok);
        assertEquals(monedas, p.getMonedas());
    }

    @Test
    public void soloSePuedeEquiparLoQueSePosee() {
        Partida p = nacida();
        assertFalse(Guarida.equipar(p, Slot.SUELO, "s_algas").ok);
        Tienda.comprar(p, "s_algas");
        assertFalse(Guarida.equipar(p, Slot.VENTANA, "s_algas").ok);
        assertTrue(Guarida.equipar(p, Slot.SUELO, "s_algas").ok);
        assertTrue(Guarida.retirar(p, Slot.SUELO).ok);
    }

    @Test
    public void misionNoSePuedeReclamarDosVeces() {
        Partida p = nacida();
        p.getContadores().hoyAlimentar = 3;
        assertTrue(p.reclamarMision("d_comer").ok);
        assertFalse(p.reclamarMision("d_comer").ok);
    }

    @Test
    public void misionDiariaSeRenuevaAlCambiarElDia() {
        Partida p = nacida();
        p.getContadores().hoyAlimentar = 3;
        assertTrue(p.reclamarMision("d_comer").ok);
        p.actualizar(T0 + 26 * HORA, 1.0);
        assertEquals(0, p.getContadores().hoyAlimentar);
        p.getContadores().hoyAlimentar = 3;
        assertTrue(p.reclamarMision("d_comer").ok);
    }

    @Test
    public void subirDeNivelHaceEvolucionarAJuvenilYLuegoAFinal() {
        Partida p = nacida();
        p.sumarXp(Niveles.UMBRALES[2]);
        assertEquals(Etapa.JUVENIL, p.getPersonaje().getEtapa());
        p.sumarXp(Niveles.UMBRALES[4]);
        assertEquals(Etapa.FINAL, p.getPersonaje().getEtapa());
        assertTrue(p.getPersonaje().getRuta() != RutaEvolucion.NINGUNA);
    }
}
