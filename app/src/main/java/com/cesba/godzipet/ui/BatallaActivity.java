package com.cesba.godzipet.ui;

import com.cesba.godzipet.modelo.Minijuego;

/** CONTRATO (lo implementa el agente de batalla): batalla contra un jefe mecánico. */
public class BatallaActivity extends MinijuegoActivity {
    @Override protected Minijuego minijuego() { return Minijuego.BATALLA; }
    @Override protected void comenzarRonda() { }
    @Override protected int abortarRonda() { return 0; }
}
