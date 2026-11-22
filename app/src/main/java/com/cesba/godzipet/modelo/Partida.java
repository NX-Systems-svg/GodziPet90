package com.cesba.godzipet.modelo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Estado completo de una partida: personaje, necesidades, economía, progreso,
 * guarida, misiones y récords. Toda regla del juego que cambia el estado vive aquí
 * (o en {@link Cuidados}, {@link Tienda} y {@link Guarida}).
 */
public final class Partida {
    public static final int ALIENTO_MAX = 100;
    public static final double MINUTOS_SUENO = 20;            // minutos de juego
    public static final double MIN_ALERTA = 10, MAX_ALERTA = 25; // minutos entre alertas
    public static final double DURACION_ALERTA = 8;            // minutos de juego
    public static final double MAX_MINUTOS_OFFLINE = 7 * 24 * 60;

    private final Personaje personaje;
    private final Necesidades necesidades;
    private final Inventario inventario;
    private final Contadores contadores;
    private final Map<Slot, String> decoraciones = new EnumMap<>(Slot.class);
    private final Map<String, String> misionesReclamadas = new HashMap<>();
    private final Map<String, Record> records = new HashMap<>();
    private final List<Evento> eventos = new ArrayList<>();
    private Random azar = new Random();

    private int monedas;
    private int xp;
    private double cargaAliento;
    private long ultimaActualizacion;
    private AlertaKaiju alerta;
    private long proximaAlerta;
    long ultimoJugar, ultimoEntrenar, ultimoBanoGratis;
    private boolean evolucionPendiente;

    public Partida(Personaje personaje, Necesidades necesidades, Inventario inventario,
                   Contadores contadores, int monedas, int xp, double cargaAliento,
                   long ultimaActualizacion) {
        this.personaje = personaje;
        this.necesidades = necesidades;
        this.inventario = inventario;
        this.contadores = contadores;
        this.monedas = Math.max(0, monedas);
        this.xp = Math.max(0, xp);
        this.cargaAliento = Math.max(0, Math.min(ALIENTO_MAX, cargaAliento));
        this.ultimaActualizacion = ultimaActualizacion;
    }

    /** Nueva partida con la configuración inicial del origen elegido. */
    public static Partida nueva(String nombre, OrigenHuevo origen, long ahora) {
        Necesidades n = Necesidades.iniciales();
        if (origen == OrigenHuevo.ISLA_MONSTRUO) n.establecer(Stat.ENERGIA, 100);
        Inventario inv = new Inventario();
        inv.agregar(Catalogo.ATUN, 3);
        inv.agregar(Catalogo.JABON, 2);
        inv.agregar(Catalogo.BOTIQUIN, 1 + origen.botiquinesExtra);
        Contadores c = new Contadores();
        c.renovarDia(claveDia(ahora));
        return new Partida(Personaje.nuevo(nombre, origen), n, inv, c,
                100 + origen.monedasExtra, 0, origen.alientoInicial, ahora);
    }

    public static String claveDia(long ms) {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(ms));
    }

    // ---------------------------------------------------------------- tiempo real

    /**
     * Aplica el paso del tiempo desde la última actualización (funciona igual con la app
     * abierta o al reabrirla). {@code velocidad} = minutos de juego por minuto real (1 normal,
     * 20 en modo demostración).
     */
    public void actualizar(long ahora, double velocidad) {
        if (contadores.renovarDia(claveDia(ahora))) {
            eventos.add(new Evento(Evento.Tipo.DIA_NUEVO, "¡Nuevo día! Las misiones diarias se renovaron."));
        }
        long dt = ahora - ultimaActualizacion;
        if (dt <= 0) return;
        if (personaje.getEtapa() == Etapa.HUEVO) { // el huevo no se deteriora
            ultimaActualizacion = ahora;
            return;
        }
        long cursor = ultimaActualizacion;
        while (cursor < ahora) {
            long fin = ahora;
            if (personaje.isDormido() && personaje.getDespertarEn() > cursor) {
                fin = Math.min(fin, personaje.getDespertarEn());
            }
            double minutos = Math.min((fin - cursor) / 60000.0 * velocidad, MAX_MINUTOS_OFFLINE);
            avanzar(minutos);
            cursor = fin;
            if (personaje.isDormido() && cursor >= personaje.getDespertarEn()) {
                personaje.despertar();
                eventos.add(new Evento(Evento.Tipo.DESPERTO,
                        personaje.getNombre() + " despertó con energía renovada."));
            }
        }
        ultimaActualizacion = ahora;
        revisarAlertas(ahora, velocidad);
    }

    private void avanzar(double minutos) {
        // pasos de 1 minuto para que los umbrales críticos se evalúen con precisión
        double restante = minutos;
        while (restante > 0) {
            double paso = Math.min(1.0, restante);
            Deterioro.aplicar(necesidades, paso, personaje.isDormido(), personaje.getRuta());
            contadores.muestrearBienestar(necesidades.promedio(), paso);
            restante -= paso;
        }
    }

    private void revisarAlertas(long ahora, double velocidad) {
        if (getNivel() < Evolucion.NIVEL_JUVENIL) return;
        if (alerta != null && ahora >= alerta.expira) {
            necesidades.cambiar(Stat.FELICIDAD, -15);
            necesidades.cambiar(Stat.SALUD, -5);
            eventos.add(new Evento(Evento.Tipo.ALERTA_FALLIDA, "Nadie detuvo al " + alerta.monstruo
                    + ". La ciudad sufrió daños (-15 😊, -5 ❤)."));
            alerta = null;
            proximaAlerta = 0;
        }
        if (alerta == null && proximaAlerta == 0) {
            programarSiguienteAlerta(ahora, velocidad);
        } else if (alerta == null && ahora >= proximaAlerta) {
            long duracion = (long) (DURACION_ALERTA * 60000 / velocidad);
            alerta = new AlertaKaiju(AlertaKaiju.MONSTRUOS[azar.nextInt(AlertaKaiju.MONSTRUOS.length)],
                    AlertaKaiju.LUGARES[azar.nextInt(AlertaKaiju.LUGARES.length)], ahora + duracion);
            eventos.add(new Evento(Evento.Tipo.ALERTA_NUEVA, alerta.descripcion()));
        }
    }

    private void programarSiguienteAlerta(long ahora, double velocidad) {
        double min = MIN_ALERTA + azar.nextDouble() * (MAX_ALERTA - MIN_ALERTA);
        proximaAlerta = ahora + (long) (min * 60000 / velocidad);
    }

    /** Mecánica exclusiva #2: responder a la Alerta Kaiju. */
    public Resultado defenderCiudad(long ahora, double velocidad) {
        if (alerta == null) return Resultado.error("No hay ninguna alerta activa.");
        if (personaje.isDormido()) return Resultado.error("Está dormido. ¡Despiértalo con un baño o espera!");
        if (necesidades.get(Stat.ENERGIA) < 20) return Resultado.error("Necesita 20 de energía para pelear.");
        if (necesidades.get(Stat.SALUD) < 30) return Resultado.error("Está demasiado enfermo para pelear.");
        AlertaKaiju a = alerta;
        alerta = null;
        programarSiguienteAlerta(ahora, velocidad);
        necesidades.cambiar(Stat.ENERGIA, -20);
        boolean usoAliento = cargaAliento >= ALIENTO_MAX;
        double prob = 0.35 + 0.5 * (necesidades.get(Stat.ENERGIA) + necesidades.get(Stat.SALUD)) / 200.0;
        boolean gano = usoAliento || personaje.getRuta() == RutaEvolucion.SALVAJE || azar.nextDouble() < prob;
        if (usoAliento) {
            cargaAliento = 0;
            contadores.alientos++;
        }
        if (gano) {
            contadores.defensas++;
            int xpGanada = personaje.getRuta() == RutaEvolucion.SALVAJE ? 45 : 30;
            necesidades.cambiar(Stat.FELICIDAD, 10);
            int m = ganar(40, xpGanada);
            cargarAliento(10);
            return Resultado.exito((usoAliento ? "¡ALIENTO ATÓMICO! " : "") + "Derrotó al " + a.monstruo
                    + ". +" + m + " monedas, +" + xpGanada + " XP", usoAliento ? Reaccion.ALIENTO : Reaccion.ENTRENAR);
        }
        necesidades.cambiar(Stat.FELICIDAD, -10);
        necesidades.cambiar(Stat.SALUD, -10);
        ganar(0, 10);
        return Resultado.error("El " + a.monstruo + " escapó y lo lastimó (-10 ❤). Entrénalo más.");
    }

    // ---------------------------------------------------------------- economía y progreso

    /** Suma monedas (con multiplicador de ruta) y experiencia. Devuelve monedas reales ganadas. */
    public int ganar(int monedasBase, int xpGanada) {
        int m = (int) Math.round(monedasBase * personaje.getRuta().multMonedas);
        monedas = Math.min(999_999, monedas + m);
        sumarXp(xpGanada);
        return m;
    }

    public void sumarXp(int cantidad) {
        if (cantidad <= 0) return;
        int antes = getNivel();
        xp += cantidad;
        int despues = getNivel();
        for (int n = antes + 1; n <= despues; n++) {
            eventos.add(new Evento(Evento.Tipo.SUBIO_NIVEL,
                    "¡Nivel " + n + "! Desbloqueado: " + Niveles.desbloqueo(n)));
        }
        revisarEvolucion();
    }

    private void revisarEvolucion() {
        int nivel = getNivel();
        if (personaje.getEtapa() == Etapa.CRIA && nivel >= Evolucion.NIVEL_JUVENIL) {
            personaje.setEtapa(Etapa.JUVENIL);
            evolucionPendiente = true;
            eventos.add(new Evento(Evento.Tipo.EVOLUCION,
                    personaje.getNombre() + " evolucionó a Juvenil. ¡Ya puede usar el Aliento Atómico!"));
        }
        if (personaje.getEtapa() == Etapa.JUVENIL && nivel >= Evolucion.NIVEL_FINAL) {
            RutaEvolucion ruta = Evolucion.decidir(getPromedioBienestar(), contadores.entrenar,
                    contadores.cuidadosTotales());
            personaje.setRuta(ruta);
            personaje.setEtapa(Etapa.FINAL);
            evolucionPendiente = true;
            eventos.add(new Evento(Evento.Tipo.EVOLUCION,
                    "¡" + personaje.getNombre() + " se convirtió en " + ruta.nombre + "! " + ruta.ventaja));
        }
    }

    public void cargarAliento(double cantidad) {
        if (getNivel() < Evolucion.NIVEL_JUVENIL && personaje.getEtapa() != Etapa.HUEVO) {
            cantidad = cantidad / 2; // de cría carga más lento
        }
        cargaAliento = Math.max(0, Math.min(ALIENTO_MAX,
                cargaAliento + cantidad * personaje.getRuta().multCargaAliento));
    }

    boolean gastarMonedas(int cantidad) {
        if (cantidad < 0 || monedas < cantidad) return false;
        monedas -= cantidad;
        return true;
    }

    // ---------------------------------------------------------------- huevo

    public Resultado tocarHuevo() {
        if (personaje.getEtapa() != Etapa.HUEVO) return Resultado.error("Ya eclosionó.");
        int toques = personaje.tocarHuevo();
        if (toques >= Personaje.TOQUES_PARA_ECLOSIONAR) {
            personaje.setEtapa(Etapa.CRIA);
            eventos.add(new Evento(Evento.Tipo.ECLOSION, "¡El huevo eclosionó! Nació " + personaje.getNombre() + "."));
            sumarXp(10);
            return Resultado.exito("¡Crack! Nació " + personaje.getNombre() + ".", Reaccion.ECLOSION);
        }
        return Resultado.exito("El huevo se mueve… (" + toques + "/" + Personaje.TOQUES_PARA_ECLOSIONAR + ")",
                Reaccion.JUGAR);
    }

    // ---------------------------------------------------------------- minijuegos

    public Resultado puedeJugar(Minijuego juego) {
        if (personaje.getEtapa() == Etapa.HUEVO) return Resultado.error("Primero haz eclosionar el huevo.");
        if (getNivel() < juego.nivelRequerido) return Resultado.error("Se desbloquea en el nivel " + juego.nivelRequerido + ".");
        if (personaje.isDormido()) return Resultado.error("Está dormido.");
        if (necesidades.get(Stat.SALUD) < 30) return Resultado.error("Está enfermo. Cúralo primero.");
        if (necesidades.get(Stat.ENERGIA) < juego.costoEnergia) {
            return Resultado.error("Necesita " + juego.costoEnergia + " de energía.");
        }
        return Resultado.exito("¡A jugar!", Reaccion.JUGAR);
    }

    /** Cobra la energía de entrada. Debe llamarse al iniciar la ronda. */
    public Resultado iniciarMinijuego(Minijuego juego) {
        Resultado r = puedeJugar(juego);
        if (!r.ok) return r;
        necesidades.cambiar(Stat.ENERGIA, -juego.costoEnergia);
        necesidades.cambiar(Stat.HIGIENE, -3);
        return r;
    }

    /** Registra el resultado y entrega el premio proporcional. */
    public Resultado registrarMinijuego(Minijuego juego, int puntaje, boolean gano) {
        puntaje = Math.max(0, puntaje);
        Record r = getRecord(juego.id);
        r.jugadas++;
        if (gano) r.victorias++;
        boolean nuevoRecord = puntaje > r.mejorPuntaje;
        if (nuevoRecord) r.mejorPuntaje = puntaje;
        contadores.minijuegos++;
        contadores.hoyMinijuegos++;
        if (gano) contadores.victorias++;
        necesidades.cambiar(Stat.FELICIDAD, gano ? 15 : 5);
        int xpGanada = juego.experiencia(puntaje, gano);
        int m = ganar(juego.monedas(puntaje, gano), xpGanada);
        String txt = (gano ? "¡Victoria! " : "Derrota. ") + "+" + m + " monedas, +" + xpGanada + " XP"
                + (nuevoRecord ? " · ¡Nuevo récord!" : "");
        return gano ? Resultado.exito(txt, Reaccion.MONEDAS) : Resultado.error(txt);
    }

    // ---------------------------------------------------------------- misiones

    public Resultado reclamarMision(String id) {
        Mision m = GestorMisiones.buscar(id);
        if (m == null) return Resultado.error("Misión desconocida.");
        if (!m.completada(this)) return Resultado.error("Aún no completas esta misión.");
        if (GestorMisiones.reclamada(this, m)) return Resultado.error("Esta recompensa ya fue reclamada.");
        misionesReclamadas.put(m.id, m.diaria ? contadores.diaClave : "LOGRO");
        int extra = getNivel() >= Niveles.MAXIMO ? 10 : 0;
        int ganadas = ganar(m.recompensaMonedas + extra, m.recompensaXp);
        return Resultado.exito("Misión cumplida: +" + ganadas + " monedas, +" + m.recompensaXp + " XP",
                Reaccion.MONEDAS);
    }

    // ---------------------------------------------------------------- consultas

    public List<Evento> consumirEventos() {
        List<Evento> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    public boolean consumirEvolucionPendiente() {
        boolean p = evolucionPendiente;
        evolucionPendiente = false;
        return p;
    }

    public EstadoAnimo getEstado() {
        return EstadoAnimo.calcular(necesidades, personaje.isDormido());
    }

    public double getPromedioBienestar() {
        return contadores.minutosBienestar > 0
                ? contadores.sumaBienestar / contadores.minutosBienestar
                : necesidades.promedio();
    }

    public Record getRecord(String juegoId) {
        Record r = records.get(juegoId);
        if (r == null) {
            r = new Record(0, 0, 0);
            records.put(juegoId, r);
        }
        return r;
    }

    public boolean alientoDesbloqueado() { return getNivel() >= Evolucion.NIVEL_JUVENIL; }
    public int getNivel() { return Niveles.nivelPara(xp); }
    public Personaje getPersonaje() { return personaje; }
    public Necesidades getNecesidades() { return necesidades; }
    public Inventario getInventario() { return inventario; }
    public Contadores getContadores() { return contadores; }
    public Map<Slot, String> getDecoraciones() { return decoraciones; }
    public Map<String, String> getMisionesReclamadas() { return misionesReclamadas; }
    public Map<String, Record> getRecords() { return records; }
    public int getMonedas() { return monedas; }
    public int getXp() { return xp; }
    public double getCargaAliento() { return cargaAliento; }
    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public AlertaKaiju getAlerta() { return alerta; }
    public long getProximaAlerta() { return proximaAlerta; }
    public long getUltimoJugar() { return ultimoJugar; }
    public long getUltimoEntrenar() { return ultimoEntrenar; }
    public long getUltimoBanoGratis() { return ultimoBanoGratis; }
    public boolean isEvolucionPendiente() { return evolucionPendiente; }

    /** Restauración desde la base de datos. */
    public void restaurar(AlertaKaiju alerta, long proximaAlerta, long ultimoJugar, long ultimoEntrenar,
                          long ultimoBanoGratis, boolean evolucionPendiente) {
        this.alerta = alerta;
        this.proximaAlerta = proximaAlerta;
        this.ultimoJugar = ultimoJugar;
        this.ultimoEntrenar = ultimoEntrenar;
        this.ultimoBanoGratis = ultimoBanoGratis;
        this.evolucionPendiente = evolucionPendiente;
    }

    /** Sólo para pruebas: azar determinista. */
    public void setAzar(Random azar) { this.azar = azar; }

    /** Usa el Aliento Atómico dentro del minijuego de defensa. */
    public boolean usarAlientoEnMinijuego() {
        if (!alientoDesbloqueado() || cargaAliento < ALIENTO_MAX) return false;
        consumirAliento();
        return true;
    }

    void consumirAliento() {
        cargaAliento = 0;
        contadores.alientos++;
    }
}
