package com.cesba.godzipet.modelo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Estado completo de una partida: personaje, necesidades (con radiación), economía, progreso,
 * escenario, guarida, misiones, secretos y récords. Las reglas que cambian el estado viven aquí
 * (o en {@link Cuidados}, {@link Tienda}, {@link Guarida} y {@link ModoProfesor}).
 */
public final class Partida {
    public static final int ALIENTO_MAX = 100;
    public static final double MINUTOS_SUENO = 20;              // minutos de juego
    public static final double MIN_ALERTA = 10, MAX_ALERTA = 25; // minutos entre alertas
    public static final double DURACION_ALERTA = 8;              // minutos de juego
    public static final double MINUTOS_FUSION = 30;              // minutos de juego con radiación ≥ 95
    public static final double MAX_MINUTOS_OFFLINE = 7 * 24 * 60;
    public static final double RAD_MAX_PEQUENOS = 90;            // antes de Junior no puede fundirse
    public static final long ESPERA_INTERACCION_MS = 20_000;

    private final Personaje personaje;
    private final Necesidades necesidades;
    private final Inventario inventario;
    private final Contadores contadores;
    private final Map<Slot, String> decoraciones = new EnumMap<>(Slot.class);
    private final Map<String, String> misionesReclamadas = new HashMap<>();
    private final Map<String, Record> records = new HashMap<>();
    private final Set<Secreto> secretos = EnumSet.noneOf(Secreto.class);
    private final Set<Escenario> visitados = EnumSet.noneOf(Escenario.class);
    private final Set<String> formasVistas = new LinkedHashSet<>();
    private final List<Evento> eventos = new ArrayList<>();
    private Random azar = new Random();

    private int monedas;
    private int xp;
    private double cargaAliento;
    private long ultimaActualizacion;
    private AlertaKaiju alerta;
    private long proximaAlerta;
    long ultimoJugar, ultimoEntrenar, ultimoBanoGratis, ultimaCentral, ultimaInteraccion;
    private boolean evolucionPendiente;
    private Escenario escenario = Escenario.ISLA_ODO;
    private double fusion;      // minutos acumulados en zona de fusión
    private int generacion = 1;
    private boolean avisoFusion;

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
        visitados.add(Escenario.ISLA_ODO);
    }

    /** Nueva partida con la configuración inicial del origen elegido. */
    public static Partida nueva(String nombre, OrigenHuevo origen, long ahora) {
        Necesidades n = Necesidades.iniciales();
        n.establecer(Stat.RADIACION, origen.radiacionInicial);
        if (origen == OrigenHuevo.ISLA_NACIMIENTO) n.establecer(Stat.ENERGIA, 100);
        Inventario inv = new Inventario();
        inv.agregar(Catalogo.ONIGIRI, 2);
        inv.agregar(Catalogo.ATUN, 2);
        inv.agregar(Catalogo.JABON, 2);
        inv.agregar(Catalogo.BOTIQUIN, 1 + origen.botiquinesExtra);
        inv.agregar(Catalogo.CADMIO, 1);
        Contadores c = new Contadores();
        c.renovarDia(claveDia(ahora));
        Partida p = new Partida(Personaje.nuevo(nombre, origen), n, inv, c,
                100 + origen.monedasExtra, 0, origen.alientoInicial, ahora);
        p.formasVistas.add(claveForma(Etapa.HUEVO, RutaEvolucion.NINGUNA));
        if (origen == OrigenHuevo.NUEVA_YORK) p.descubrir(Secreto.NUEVA_YORK);
        p.eventos.clear();
        return p;
    }

    public static String claveDia(long ms) {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(ms));
    }

    public static String claveForma(Etapa e, RutaEvolucion r) {
        return e == Etapa.FINAL ? r.name() : e.name();
    }

    // ---------------------------------------------------------------- tiempo real

    /**
     * Aplica el paso del tiempo desde la última actualización (igual con la app abierta o al
     * reabrirla). {@code velocidad} = minutos de juego por minuto real (1 normal, 20 en demo).
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
        double presupuesto = MAX_MINUTOS_OFFLINE;
        while (cursor < ahora && presupuesto > 0) {
            long fin = ahora;
            if (personaje.isDormido() && personaje.getDespertarEn() > cursor) {
                fin = Math.min(fin, personaje.getDespertarEn());
            }
            double minutos = Math.min((fin - cursor) / 60000.0 * velocidad, presupuesto);
            presupuesto -= minutos;
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
        double restante = minutos;
        while (restante > 0) {
            double paso = Math.min(1.0, restante);
            Deterioro.aplicar(necesidades, paso, personaje.isDormido(), personaje.getRuta(), escenario);
            limitarRadiacionPequenos();
            contadores.muestrear(necesidades.promedio(), necesidades.get(Stat.RADIACION), paso,
                    necesidades.enZonaIdeal());
            revisarFusion(paso);
            restante -= paso;
        }
    }

    private void limitarRadiacionPequenos() {
        if (personaje.getEtapa().ordinal() < Etapa.JUNIOR.ordinal()
                && necesidades.get(Stat.RADIACION) > RAD_MAX_PEQUENOS) {
            necesidades.establecer(Stat.RADIACION, RAD_MAX_PEQUENOS);
        }
    }

    /** Mecánica característica: con radiación ≥ 95 sostenida, el núcleo se funde. */
    private void revisarFusion(double minutos) {
        boolean puedeFundirse = personaje.getEtapa().ordinal() >= Etapa.JUNIOR.ordinal();
        if (puedeFundirse && necesidades.get(Stat.RADIACION) >= Necesidades.RAD_FUSION) {
            fusion += minutos;
            if (!avisoFusion) {
                avisoFusion = true;
                eventos.add(new Evento(Evento.Tipo.FUSION_INMINENTE,
                        "☢ ¡ALERTA DE FUSIÓN! Enfría a " + personaje.getNombre() + " antes de que su núcleo estalle."));
            }
            if (fusion >= MINUTOS_FUSION) fundirse();
        } else {
            fusion = Math.max(0, fusion - minutos * 2);
            if (fusion == 0) avisoFusion = false;
        }
    }

    /**
     * Fusión (canon de Godzilla vs. Destoroyah): Godzilla se funde y Godzilla Junior absorbe su
     * radiación. Empieza una nueva generación con un bono de experiencia heredado.
     */
    void fundirse() {
        String anterior = personaje.nombreForma();
        generacion++;
        fusion = 0;
        avisoFusion = false;
        personaje.setRuta(RutaEvolucion.NINGUNA);
        personaje.setEtapa(Etapa.JUNIOR);
        personaje.despertar();
        necesidades.establecer(Stat.RADIACION, 50);
        necesidades.establecer(Stat.SALUD, 80);
        necesidades.establecer(Stat.ENERGIA, 70);
        necesidades.establecer(Stat.FELICIDAD, 60);
        xp = Math.min(xp, Niveles.UMBRALES[Evolucion.NIVEL_JUNIOR - 1]);
        contadores.nuevaGeneracion();
        cargaAliento = ALIENTO_MAX;
        evolucionPendiente = true;
        registrarForma();
        descubrir(Secreto.FUSION);
        eventos.add(new Evento(Evento.Tipo.FUSION, "☢ " + anterior + " se fundió… pero Godzilla Junior absorbió su "
                + "radiación. ¡Comienza la generación " + generacion + " con +" + (int) Math.round((multLegado() - 1) * 100)
                + "% de experiencia!"));
    }

    /** Bono de legado: +10% de experiencia por generación (máx. +30%). */
    public double multLegado() {
        return 1 + 0.1 * Math.min(3, generacion - 1);
    }

    public double minutosParaFusion() {
        return Math.max(0, MINUTOS_FUSION - fusion);
    }

    public boolean fusionEnCurso() {
        return fusion > 0 && necesidades.get(Stat.RADIACION) >= Necesidades.RAD_FUSION;
    }

    private void revisarAlertas(long ahora, double velocidad) {
        if (getNivel() < Evolucion.NIVEL_JUNIOR) return;
        if (alerta != null && ahora >= alerta.expira) {
            necesidades.cambiar(Stat.FELICIDAD, -15);
            necesidades.cambiar(Stat.SALUD, -5);
            eventos.add(new Evento(Evento.Tipo.ALERTA_FALLIDA, "Nadie detuvo a " + alerta.monstruo
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

    void programarSiguienteAlerta(long ahora, double velocidad) {
        double min = MIN_ALERTA + azar.nextDouble() * (MAX_ALERTA - MIN_ALERTA);
        proximaAlerta = ahora + (long) (min * 60000 / velocidad);
    }

    void forzarAlerta(long ahora, double velocidad) {
        alerta = new AlertaKaiju(AlertaKaiju.MONSTRUOS[azar.nextInt(AlertaKaiju.MONSTRUOS.length)],
                AlertaKaiju.LUGARES[azar.nextInt(AlertaKaiju.LUGARES.length)],
                ahora + (long) (DURACION_ALERTA * 60000 / velocidad));
        eventos.add(new Evento(Evento.Tipo.ALERTA_NUEVA, alerta.descripcion()));
    }

    /** Mecánica exclusiva: responder a la Alerta Kaiju. */
    public Resultado defenderCiudad(long ahora, double velocidad) {
        if (alerta == null) return Resultado.error("No hay ninguna alerta activa.");
        if (personaje.isDormido()) return Resultado.error("Está dormido. ¡Despiértalo o espera!");
        if (necesidades.get(Stat.ENERGIA) < 20) return Resultado.error("Necesita 20 de energía para pelear.");
        if (necesidades.get(Stat.SALUD) < 30) return Resultado.error("Está demasiado enfermo para pelear.");
        AlertaKaiju a = alerta;
        alerta = null;
        programarSiguienteAlerta(ahora, velocidad);
        necesidades.cambiar(Stat.ENERGIA, -20);
        boolean usoAliento = cargaAliento >= ALIENTO_MAX;
        double fuerza = (necesidades.get(Stat.ENERGIA) + necesidades.get(Stat.SALUD)
                + necesidades.calidad(Stat.RADIACION)) / 300.0;
        double prob = 0.3 + 0.55 * fuerza;
        boolean gano = usoAliento || personaje.getRuta() == RutaEvolucion.BURNING || azar.nextDouble() < prob;
        if (usoAliento) consumirAliento();
        if (gano) {
            contadores.defensas++;
            int xpGanada = personaje.getRuta() == RutaEvolucion.BURNING ? 45 : 30;
            necesidades.cambiar(Stat.FELICIDAD, 10);
            int m = ganar(40, xpGanada);
            cargarAliento(10);
            return Resultado.exito((usoAliento ? "¡ALIENTO ATÓMICO! " : "") + "Derrotó a " + a.monstruo
                    + ". +" + m + " monedas, +" + xpGanada + " XP", usoAliento ? Reaccion.ALIENTO : Reaccion.ENTRENAR);
        }
        necesidades.cambiar(Stat.FELICIDAD, -10);
        necesidades.cambiar(Stat.SALUD, -10);
        ganar(0, 10);
        return Resultado.error(a.monstruo + " escapó y lo lastimó (-10 ❤). Entrénalo y equilibra su radiación.");
    }

    // ---------------------------------------------------------------- escenarios, secretos y álbum

    public Resultado viajar(Escenario e) {
        if (e == escenario) return Resultado.error("Ya está en " + e.nombre + ".");
        if (getNivel() < e.nivelRequerido) return Resultado.error(e.nombre + " se desbloquea en el nivel " + e.nivelRequerido + ".");
        if (personaje.getEtapa() == Etapa.HUEVO) return Resultado.error("El huevo no puede viajar todavía.");
        if (personaje.isDormido()) return Resultado.error("Está dormido. Viajarán cuando despierte.");
        escenario = e;
        boolean nuevo = visitados.add(e);
        if (nuevo) sumarXp(10);
        return Resultado.exito("Viajaron a " + e.nombre + " (" + e.kanji + "). " + e.descripcion
                + (nuevo ? " +10 XP por explorar." : ""), Reaccion.JUGAR);
    }

    /** Registra un easter egg. Devuelve true si era nuevo. */
    public boolean descubrir(Secreto s) {
        if (!secretos.add(s)) return false;
        eventos.add(new Evento(Evento.Tipo.SECRETO, "🔓 Secreto descubierto: " + s.titulo + " — " + s.dato));
        sumarXp(15);
        return true;
    }

    void registrarForma() {
        formasVistas.add(claveForma(personaje.getEtapa(), personaje.getRuta()));
    }

    private boolean esperaLibre(long ahora) {
        if (ahora - ultimaInteraccion < ESPERA_INTERACCION_MS) return false;
        ultimaInteraccion = ahora;
        return true;
    }

    /** Agitar el teléfono: Godzilla da un pisotón que hace temblar la ciudad. */
    public Resultado terremoto(long ahora) {
        if (personaje.getEtapa() == Etapa.HUEVO) return Resultado.error("El huevo se tambaleó… pero aún no nace.");
        if (personaje.isDormido()) {
            personaje.despertar();
            necesidades.cambiar(Stat.FELICIDAD, -8);
            return Resultado.exito("¡El terremoto lo despertó de golpe! Está gruñón (-8 😊).", Reaccion.TERREMOTO);
        }
        descubrir(Secreto.TERREMOTO);
        if (!esperaLibre(ahora)) return Resultado.exito("¡PISOTÓN! La tierra tiembla.", Reaccion.TERREMOTO);
        necesidades.cambiar(Stat.FELICIDAD, 4);
        necesidades.cambiar(Stat.ENERGIA, -2);
        return Resultado.exito("¡PISOTÓN! Hizo temblar toda la costa. Le encanta (+4 😊).", Reaccion.TERREMOTO);
    }

    /** El jugador le rugió por el micrófono: él responde. */
    public Resultado rugidoEscuchado(long ahora) {
        if (personaje.getEtapa() == Etapa.HUEVO) return Resultado.error("El huevo vibra con tu voz…");
        if (personaje.isDormido()) return Resultado.error("Está dormido. No lo despiertes a gritos.");
        descubrir(Secreto.RUGIDO);
        if (!esperaLibre(ahora)) return Resultado.exito("¡GRAAAOOOR! Te contestó el rugido.", Reaccion.RUGIDO);
        necesidades.cambiar(Stat.FELICIDAD, 6);
        sumarXp(3);
        return Resultado.exito("¡GRAAAOOOR! Te reconoció como parte de su manada (+6 😊).", Reaccion.RUGIDO);
    }

    /** Easter egg: 7 toques en la cabeza → el baile ¡Shie! de 1965. */
    public Resultado shie() {
        if (personaje.getEtapa() == Etapa.HUEVO) return Resultado.error("Aún es un huevo.");
        boolean nuevo = descubrir(Secreto.SHIE);
        necesidades.cambiar(Stat.FELICIDAD, nuevo ? 15 : 3);
        return Resultado.exito("¡SHIE! Hizo el baile de la victoria de 1965.", Reaccion.SHIE);
    }

    /** Easter egg: tocar la punta de la Torre de Tokio. */
    public Resultado visitaMothra() {
        if (escenario != Escenario.TORRE_TOKIO) return Resultado.error("Aquí no hay ninguna torre.");
        boolean nuevo = descubrir(Secreto.MOTHRA);
        necesidades.cambiar(Stat.FELICIDAD, nuevo ? 20 : 2);
        return Resultado.exito("Una polilla gigante pasó volando y dos pequeñas hadas cantaron una canción.", Reaccion.MOTHRA);
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
        int real = (int) Math.round(cantidad * personaje.getRuta().multXp() * multLegado());
        int antes = getNivel();
        xp += real;
        int despues = getNivel();
        for (int n = antes + 1; n <= despues; n++) {
            eventos.add(new Evento(Evento.Tipo.SUBIO_NIVEL,
                    "¡Nivel " + n + "! Desbloqueado: " + Niveles.desbloqueo(n)));
        }
        revisarEvolucion();
    }

    private void revisarEvolucion() {
        int nivel = getNivel();
        Etapa e = personaje.getEtapa();
        if (e == Etapa.HUEVO) return;
        if (e == Etapa.BEBE && nivel >= Evolucion.NIVEL_PEQUENO) evolucionarA(Etapa.PEQUENO,
                "¡" + personaje.getNombre() + " creció y ahora es Little Godzilla!");
        e = personaje.getEtapa();
        if (e == Etapa.PEQUENO && nivel >= Evolucion.NIVEL_JUNIOR) evolucionarA(Etapa.JUNIOR,
                "¡" + personaje.getNombre() + " es ahora Godzilla Junior! Su aliento ya es un rayo atómico.");
        e = personaje.getEtapa();
        if (e == Etapa.JUNIOR && nivel >= Evolucion.NIVEL_FINAL) {
            RutaEvolucion ruta = Evolucion.decidir(getPromedioBienestar(), getPromedioRadiacion(),
                    contadores.entrenosGeneracion(), contadores.cuidadosGeneracion());
            personaje.setRuta(ruta);
            evolucionarA(Etapa.FINAL, "¡" + personaje.getNombre() + " se convirtió en " + ruta.nombre + " ("
                    + ruta.kanji + ")! " + ruta.ventaja);
        }
    }

    private void evolucionarA(Etapa etapa, String mensaje) {
        personaje.setEtapa(etapa);
        evolucionPendiente = true;
        registrarForma();
        eventos.add(new Evento(Evento.Tipo.EVOLUCION, mensaje));
    }

    public void cargarAliento(double cantidad) {
        if (personaje.getEtapa().ordinal() < Etapa.JUNIOR.ordinal()) cantidad = cantidad / 2;
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
            personaje.setEtapa(Etapa.BEBE);
            registrarForma();
            eventos.add(new Evento(Evento.Tipo.ECLOSION, "¡El huevo eclosionó! Nació " + personaje.getNombre()
                    + ", un Baby Godzilla."));
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

    public Resultado iniciarMinijuego(Minijuego juego) {
        Resultado r = puedeJugar(juego);
        if (!r.ok) return r;
        necesidades.cambiar(Stat.ENERGIA, -juego.costoEnergia);
        necesidades.cambiar(Stat.HIGIENE, -3);
        return r;
    }

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

    /** Usa el Aliento Atómico dentro de un minijuego. */
    public boolean usarAlientoEnMinijuego() {
        if (!alientoDesbloqueado() || cargaAliento < ALIENTO_MAX) return false;
        consumirAliento();
        return true;
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

    public double getPromedioRadiacion() {
        return contadores.promedioRadiacion(necesidades.get(Stat.RADIACION));
    }

    public Record getRecord(String juegoId) {
        Record r = records.get(juegoId);
        if (r == null) {
            r = new Record(0, 0, 0);
            records.put(juegoId, r);
        }
        return r;
    }

    /** El aliento existe desde que nace; antes de Junior son anillos de humo (como Minilla). */
    public boolean alientoDesbloqueado() { return personaje.getEtapa() != Etapa.HUEVO; }
    public boolean alientoEsAnilloDeHumo() { return personaje.getEtapa().ordinal() < Etapa.JUNIOR.ordinal(); }
    public int getNivel() { return Niveles.nivelPara(xp); }
    public Personaje getPersonaje() { return personaje; }
    public Necesidades getNecesidades() { return necesidades; }
    public Inventario getInventario() { return inventario; }
    public Contadores getContadores() { return contadores; }
    public Map<Slot, String> getDecoraciones() { return decoraciones; }
    public Map<String, String> getMisionesReclamadas() { return misionesReclamadas; }
    public Map<String, Record> getRecords() { return records; }
    public Set<Secreto> getSecretos() { return secretos; }
    public Set<Escenario> getVisitados() { return visitados; }
    public Set<String> getFormasVistas() { return formasVistas; }
    public Escenario getEscenario() { return escenario; }
    public double getFusion() { return fusion; }
    public int getGeneracion() { return generacion; }
    public int getMonedas() { return monedas; }
    public int getXp() { return xp; }
    public double getCargaAliento() { return cargaAliento; }
    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public AlertaKaiju getAlerta() { return alerta; }
    public long getProximaAlerta() { return proximaAlerta; }
    public long getUltimoJugar() { return ultimoJugar; }
    public long getUltimoEntrenar() { return ultimoEntrenar; }
    public long getUltimoBanoGratis() { return ultimoBanoGratis; }
    public long getUltimaCentral() { return ultimaCentral; }
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

    /** Restauración de los datos agregados en la versión 2. */
    public void restaurarV2(Escenario escenario, double fusion, int generacion, long ultimaCentral,
                            Set<Secreto> secretos, Set<Escenario> visitados, Set<String> formas) {
        this.escenario = escenario == null ? Escenario.ISLA_ODO : escenario;
        this.fusion = Math.max(0, fusion);
        this.generacion = Math.max(1, generacion);
        this.ultimaCentral = ultimaCentral;
        this.secretos.addAll(secretos);
        this.visitados.addAll(visitados);
        this.visitados.add(this.escenario);
        this.formasVistas.addAll(formas);
        registrarForma();
        this.avisoFusion = this.fusion > 0;
    }

    /** Sólo para pruebas: azar determinista. */
    public void setAzar(Random azar) { this.azar = azar; }

    void consumirAliento() {
        cargaAliento = 0;
        contadores.alientos++;
    }

    // ---- acceso para el modo profesor (mismo paquete)
    void setXp(int xp) { this.xp = Math.max(0, xp); }
    void setMonedas(int monedas) { this.monedas = Math.max(0, Math.min(999_999, monedas)); }
    void setCargaAliento(double c) { this.cargaAliento = Math.max(0, Math.min(ALIENTO_MAX, c)); }
    void setFusion(double f) { this.fusion = Math.max(0, f); }
    void setEscenario(Escenario e) { this.escenario = e; visitados.add(e); }
    void setUltimaActualizacion(long t) { this.ultimaActualizacion = t; }
    void setEvolucionPendiente() { this.evolucionPendiente = true; }
    void agregarEvento(Evento e) { eventos.add(e); }
}
