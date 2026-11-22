package com.cesba.godzipet.datos;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cesba.godzipet.modelo.AlertaKaiju;
import com.cesba.godzipet.modelo.Contadores;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Inventario;
import com.cesba.godzipet.modelo.Necesidades;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Personaje;
import com.cesba.godzipet.modelo.Record;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Slot;
import com.cesba.godzipet.modelo.Stat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Guarda y recupera el estado persistente. Todo acceso a disco ocurre en un hilo de fondo. */
public final class Repositorio {
    private static final String TAG = "Repositorio";
    private static volatile Repositorio instancia;

    private final JuegoDao dao;
    private final ExecutorService disco = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void listo(T valor);
    }

    private Repositorio(Context ctx) {
        dao = BaseDatos.get(ctx).dao();
    }

    public static Repositorio get(Context ctx) {
        if (instancia == null) {
            synchronized (Repositorio.class) {
                if (instancia == null) instancia = new Repositorio(ctx);
            }
        }
        return instancia;
    }

    // ------------------------------------------------------------ cuentas

    /** Registra un usuario. Devuelve id &gt; 0 o un mensaje de error en el callback de error. */
    public void registrar(String usuario, String contrasena, Callback<Long> ok, Callback<String> error) {
        disco.execute(() -> {
            try {
                if (dao.buscarUsuario(usuario) != null) {
                    principal.post(() -> error.listo("Ese usuario ya existe."));
                    return;
                }
                UsuarioEntity u = new UsuarioEntity();
                u.usuario = usuario;
                u.sal = Seguridad.nuevaSal();
                u.hash = Seguridad.hash(contrasena, u.sal);
                u.creado = System.currentTimeMillis();
                long id = dao.insertarUsuario(u);
                principal.post(() -> ok.listo(id));
            } catch (Exception e) {
                Log.e(TAG, "registrar", e);
                principal.post(() -> error.listo("No se pudo crear la cuenta."));
            }
        });
    }

    public void iniciarSesion(String usuario, String contrasena, Callback<Long> ok, Callback<String> error) {
        disco.execute(() -> {
            try {
                UsuarioEntity u = dao.buscarUsuario(usuario);
                if (u == null || !Seguridad.iguales(u.hash, Seguridad.hash(contrasena, u.sal))) {
                    principal.post(() -> error.listo("Usuario o contraseña incorrectos."));
                    return;
                }
                principal.post(() -> ok.listo(u.id));
            } catch (Exception e) {
                Log.e(TAG, "iniciarSesion", e);
                principal.post(() -> error.listo("Error al iniciar sesión."));
            }
        });
    }

    public void nombreUsuario(long uid, Callback<String> cb) {
        disco.execute(() -> {
            UsuarioEntity u = dao.usuarioPorId(uid);
            principal.post(() -> cb.listo(u == null ? null : u.usuario));
        });
    }

    // ------------------------------------------------------------ partida

    public void cargar(long uid, Callback<Partida> cb) {
        disco.execute(() -> {
            Partida p = null;
            try {
                p = cargarSincrono(uid);
            } catch (Exception e) {
                Log.e(TAG, "cargar", e);
            }
            Partida r = p;
            principal.post(() -> cb.listo(r));
        });
    }

    Partida cargarSincrono(long uid) {
        PartidaEntity e = dao.partida(uid);
        if (e == null) return null;
        Personaje k = new Personaje(e.nombre, OrigenHuevo.desde(e.origen), etapa(e.etapa),
                RutaEvolucion.desde(e.ruta), e.dormido, e.despertarEn, e.toquesHuevo);
        Necesidades n = new Necesidades(e.alimentacion, e.felicidad, e.energia, e.higiene, e.salud);
        Inventario inv = new Inventario();
        for (InventarioEntity i : dao.inventario(uid)) {
            try {
                if (i.cantidad > 0) inv.agregar(i.articuloId, i.cantidad);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Artículo ignorado: " + i.articuloId);
            }
        }
        Contadores c = new Contadores();
        c.alimentar = e.cAlimentar; c.jugar = e.cJugar; c.dormir = e.cDormir; c.banar = e.cBanar;
        c.curar = e.cCurar; c.entrenar = e.cEntrenar; c.minijuegos = e.cMinijuegos;
        c.victorias = e.cVictorias; c.defensas = e.cDefensas; c.alientos = e.cAlientos;
        c.sumaBienestar = e.sumaBienestar; c.minutosBienestar = e.minutosBienestar;
        c.diaClave = e.diaClave; c.hoyAlimentar = e.hoyAlimentar; c.hoyMinijuegos = e.hoyMinijuegos;
        c.hoyEntrenar = e.hoyEntrenar;

        Partida p = new Partida(k, n, inv, c, e.monedas, e.xp, e.cargaAliento, e.ultimaActualizacion);
        AlertaKaiju alerta = e.alertaMonstruo == null ? null
                : new AlertaKaiju(e.alertaMonstruo, e.alertaLugar == null ? "la ciudad" : e.alertaLugar, e.alertaExpira);
        p.restaurar(alerta, e.proximaAlerta, e.ultimoJugar, e.ultimoEntrenar, e.ultimoBanoGratis, e.evolucionPendiente);
        for (DecoracionEntity d : dao.decoraciones(uid)) {
            try {
                Slot s = Slot.valueOf(d.slot);
                if (inv.posee(d.articuloId)) p.getDecoraciones().put(s, d.articuloId);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Slot ignorado: " + d.slot);
            }
        }
        for (MisionEntity m : dao.misiones(uid)) p.getMisionesReclamadas().put(m.misionId, m.reclamadaEn);
        for (RecordEntity r : dao.records(uid)) {
            p.getRecords().put(r.juegoId, new Record(r.mejorPuntaje, r.victorias, r.jugadas));
        }
        return p;
    }

    private static Etapa etapa(String s) {
        try {
            return Etapa.valueOf(s);
        } catch (Exception ex) {
            return Etapa.HUEVO;
        }
    }

    /** Toma la instantánea en el hilo actual y escribe en segundo plano. */
    public void guardar(long uid, Partida p) {
        Instantanea s = instantanea(uid, p);
        disco.execute(() -> {
            try {
                dao.guardarTodo(s);
            } catch (Exception e) {
                Log.e(TAG, "guardar", e);
            }
        });
    }

    public void borrarPartida(long uid, Runnable listo) {
        disco.execute(() -> {
            try {
                dao.borrarTodo(uid);
            } catch (Exception e) {
                Log.e(TAG, "borrar", e);
            }
            principal.post(listo);
        });
    }

    static Instantanea instantanea(long uid, Partida p) {
        PartidaEntity e = new PartidaEntity();
        e.usuarioId = uid;
        Personaje k = p.getPersonaje();
        e.nombre = k.getNombre();
        e.origen = k.getOrigen().name();
        e.etapa = k.getEtapa().name();
        e.ruta = k.getRuta().name();
        e.dormido = k.isDormido();
        e.despertarEn = k.getDespertarEn();
        e.toquesHuevo = k.getToquesHuevo();
        Necesidades n = p.getNecesidades();
        e.alimentacion = n.get(Stat.ALIMENTACION);
        e.felicidad = n.get(Stat.FELICIDAD);
        e.energia = n.get(Stat.ENERGIA);
        e.higiene = n.get(Stat.HIGIENE);
        e.salud = n.get(Stat.SALUD);
        e.monedas = p.getMonedas();
        e.xp = p.getXp();
        e.cargaAliento = p.getCargaAliento();
        e.ultimaActualizacion = p.getUltimaActualizacion();
        e.evolucionPendiente = p.isEvolucionPendiente();
        if (p.getAlerta() != null) {
            e.alertaMonstruo = p.getAlerta().monstruo;
            e.alertaLugar = p.getAlerta().lugar;
            e.alertaExpira = p.getAlerta().expira;
        }
        e.proximaAlerta = p.getProximaAlerta();
        e.ultimoJugar = p.getUltimoJugar();
        e.ultimoEntrenar = p.getUltimoEntrenar();
        e.ultimoBanoGratis = p.getUltimoBanoGratis();
        Contadores c = p.getContadores();
        e.cAlimentar = c.alimentar; e.cJugar = c.jugar; e.cDormir = c.dormir; e.cBanar = c.banar;
        e.cCurar = c.curar; e.cEntrenar = c.entrenar; e.cMinijuegos = c.minijuegos;
        e.cVictorias = c.victorias; e.cDefensas = c.defensas; e.cAlientos = c.alientos;
        e.sumaBienestar = c.sumaBienestar; e.minutosBienestar = c.minutosBienestar;
        e.diaClave = c.diaClave; e.hoyAlimentar = c.hoyAlimentar; e.hoyMinijuegos = c.hoyMinijuegos;
        e.hoyEntrenar = c.hoyEntrenar;

        List<InventarioEntity> inv = new ArrayList<>();
        for (Map.Entry<String, Integer> i : p.getInventario().comoMapa().entrySet()) {
            inv.add(new InventarioEntity(uid, i.getKey(), i.getValue()));
        }
        List<DecoracionEntity> dec = new ArrayList<>();
        for (Map.Entry<Slot, String> d : p.getDecoraciones().entrySet()) {
            dec.add(new DecoracionEntity(uid, d.getKey().name(), d.getValue()));
        }
        List<MisionEntity> mis = new ArrayList<>();
        for (Map.Entry<String, String> m : p.getMisionesReclamadas().entrySet()) {
            mis.add(new MisionEntity(uid, m.getKey(), m.getValue()));
        }
        List<RecordEntity> rec = new ArrayList<>();
        for (Map.Entry<String, Record> r : p.getRecords().entrySet()) {
            Record v = r.getValue();
            rec.add(new RecordEntity(uid, r.getKey(), v.mejorPuntaje, v.victorias, v.jugadas));
        }
        return new Instantanea(e, inv, dec, mis, rec);
    }
}
