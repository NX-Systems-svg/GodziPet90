package com.cesba.godzipet.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Alertas;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.AlertaKaiju;
import com.cesba.godzipet.modelo.Evento;
import com.cesba.godzipet.modelo.GestorMisiones;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Resultado;

/** Hogar principal: barra superior, alerta kaiju, pestañas y reloj del juego (1 s). */
public class MainActivity extends AppCompatActivity {
    private static final long TICK_MS = 1000;
    private static final int GUARDAR_CADA = 10;
    private static final String[][] PESTANAS = {
            {"🏯", "Hogar"}, {"🎮", "Juegos"}, {"🛒", "Tienda"}, {"🎎", "Guarida"}, {"🎯", "Misiones"}};

    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final TextView[] tabs = new TextView[PESTANAS.length];
    private Juego juego;
    private int pestana = 0;
    private int ticks = 0;
    private boolean activo;

    private final ActivityResultLauncher<String> permisoNotif =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (!concedido) Ui.mensaje(this, "Sin permiso no recibirás alertas de hambre ni de fin del descanso.", 0xFF3A4670);
            });

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!activo) return;
            juego.tick();
            procesarEventos();
            refrescarTodo();
            if (++ticks % GUARDAR_CADA == 0) juego.guardar();
            reloj.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        juego = Juego.get(this);
        Alertas.crearCanal(this);
        if (b != null) pestana = b.getInt("pestana", 0);
        construirBarra();
        findViewById(R.id.btnAjustes).setOnClickListener(v -> startActivity(new Intent(this, AjustesActivity.class)));
        findViewById(R.id.btnKaijupedia).setOnClickListener(v -> startActivity(new Intent(this, KaijupediaActivity.class)));
        findViewById(R.id.bannerProfesor).setOnClickListener(v -> startActivity(new Intent(this, ProfesorActivity.class)));
        findViewById(R.id.chipNivel).setOnClickListener(v -> startActivity(new Intent(this, ProgresoActivity.class)));
        findViewById(R.id.btnDefender).setOnClickListener(v -> defender());
        juego.asegurar(() -> mostrarPestana(pestana, true), this::volverAlInicio);
        pedirPermisoNotificaciones();
    }

    private void volverAlInicio() {
        startActivity(new Intent(this, InicioActivity.class));
        finish();
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 && juego.prefs().notificaciones() && !Alertas.permisoConcedido(this)) {
            permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("pestana", pestana);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Alertas.cancelar(this);
        juego.asegurar(() -> {
            if (juego.prefs().profesorSolicitado()) {
                juego.prefs().setProfesorSolicitado(false);
                juego.activarProfesor(() -> startActivity(new Intent(this, ProfesorActivity.class)));
            }
            findViewById(R.id.bannerProfesor).setVisibility(juego.prefs().profesor() ? View.VISIBLE : View.GONE);
            activo = true;
            reloj.removeCallbacks(tick);
            reloj.post(tick);
        }, this::volverAlInicio);
    }

    @Override
    protected void onPause() {
        super.onPause();
        activo = false;
        reloj.removeCallbacks(tick);
        if (juego.partida() != null) {
            juego.tick();
            juego.guardar();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (juego.partida() != null) Alertas.programar(this, juego.partida(), juego.velocidad());
    }

    // ------------------------------------------------------------ navegación

    private void construirBarra() {
        LinearLayout barra = findViewById(R.id.barraNav);
        for (int i = 0; i < PESTANAS.length; i++) {
            TextView t = new TextView(this);
            t.setText(PESTANAS[i][0] + "\n" + PESTANAS[i][1]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(12);
            t.setLineSpacing(0, 1.1f);
            t.setTextColor(getColor(R.color.texto_sec));
            t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            t.setClickable(true);
            t.setFocusable(true);
            t.setContentDescription(PESTANAS[i][1]);
            final int indice = i;
            t.setOnClickListener(v -> {
                Sonidos.toque(this);
                mostrarPestana(indice, false);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            barra.addView(t, lp);
            tabs[i] = t;
        }
    }

    public void mostrarPestana(int i, boolean forzar) {
        if (isFinishing() || isDestroyed()) return;
        if (!forzar && i == pestana && getSupportFragmentManager().findFragmentById(R.id.contenedor) != null) return;
        pestana = i;
        Fragment f;
        switch (i) {
            case 1: f = new JuegosFragment(); break;
            case 2: f = new TiendaFragment(); break;
            case 3: f = new GuaridaFragment(); break;
            case 4: f = new MisionesFragment(); break;
            default: f = new HogarFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.contenedor, f).commitAllowingStateLoss();
        for (int k = 0; k < tabs.length; k++) {
            boolean sel = k == i;
            tabs[k].setTextColor(getColor(sel ? R.color.atomico : R.color.texto_sec));
            tabs[k].setBackgroundResource(sel ? R.drawable.bg_tab_activa : 0);
            tabs[k].setTypeface(null, sel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        refrescarEncabezado();
    }

    // ------------------------------------------------------------ estado

    /** Llamar después de cualquier acción del jugador: aviso, guardado y refresco. */
    public void despuesDeAccion(Resultado r) {
        // En Hogar el resultado ya se explica en el globo de mensaje: sólo sonido
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.contenedor);
        if (f instanceof HogarFragment) Ui.sonido(this, r);
        else Ui.aviso(this, r);
        procesarEventos();
        juego.guardar();
        refrescarTodo();
    }

    public void refrescarTodo() {
        refrescarEncabezado();
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.contenedor);
        if (f instanceof Refrescable && f.isAdded()) ((Refrescable) f).refrescar();
    }

    private void refrescarEncabezado() {
        Partida p = juego.partida();
        if (p == null) return;
        ((TextView) findViewById(R.id.tvNombre)).setText(p.getPersonaje().getNombre());
        ((TextView) findViewById(R.id.tvForma)).setText(p.getPersonaje().getEtapa() == com.cesba.godzipet.modelo.Etapa.HUEVO
                ? "Huevo · tócalo para que nazca" : p.getPersonaje().nombreForma() + " · " + p.getEstado().nombre);
        ((TextView) findViewById(R.id.chipNivel)).setText("NV " + p.getNivel() + " ›");
        ((TextView) findViewById(R.id.chipMonedas)).setText("🪙 " + p.getMonedas());
        int pendientes = GestorMisiones.pendientesDeReclamar(p);
        tabs[4].setText(PESTANAS[4][0] + "\n" + PESTANAS[4][1] + (pendientes > 0 ? " (" + pendientes + ")" : ""));
        AlertaKaiju a = p.getAlerta();
        View banner = findViewById(R.id.bannerAlerta);
        if (a == null) {
            banner.setVisibility(View.GONE);
        } else {
            banner.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvAlerta)).setText(a.descripcion());
            long seg = Math.max(0, (a.expira - System.currentTimeMillis()) / 1000);
            ((TextView) findViewById(R.id.tvAlertaTiempo)).setText(String.format(java.util.Locale.US,
                    "ALERTA KAIJU · quedan %d:%02d · cuesta 20 ⚡", seg / 60, seg % 60));
        }
    }

    private void defender() {
        Partida p = juego.partida();
        if (p == null) return;
        Resultado r = p.defenderCiudad(System.currentTimeMillis(), juego.velocidad());
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.contenedor);
        if (f instanceof HogarFragment) ((HogarFragment) f).reaccionar(r);
        if (r.ok) Sonidos.rugido(this);
        despuesDeAccion(r);
    }

    private void procesarEventos() {
        Partida p = juego.partida();
        if (p == null) return;
        for (Evento e : p.consumirEventos()) {
            switch (e.tipo) {
                case EVOLUCION:
                case ECLOSION:
                    Sonidos.rugido(this);
                    if (pestana != 0) mostrarPestana(0, false);
                    Ui.mensaje(this, e.mensaje, 0xFF2E8B57);
                    break;
                case ALERTA_NUEVA:
                    Sonidos.rugido(this);
                    Ui.mensaje(this, "🚨 " + e.mensaje + " ¡Pulsa DEFENDER!", 0xFFB3122E);
                    break;
                case ALERTA_FALLIDA:
                    Sonidos.error(this);
                    Ui.mensaje(this, e.mensaje, 0xFFB3122E);
                    break;
                case FUSION:
                    Sonidos.rugido(this);
                    if (pestana != 0) mostrarPestana(0, false);
                    Ui.mensaje(this, e.mensaje, 0xFFC8102E);
                    break;
                case FUSION_INMINENTE:
                    Sonidos.error(this);
                    Ui.mensaje(this, e.mensaje, 0xFFC8102E);
                    break;
                case SECRETO:
                    Sonidos.moneda(this);
                    Ui.mensaje(this, e.mensaje, 0xFF6A3DB8);
                    break;
                case SUBIO_NIVEL:
                    Sonidos.moneda(this);
                    Ui.mensaje(this, "⭐ " + e.mensaje, 0xFF8A5A00);
                    break;
                default:
                    Ui.mensaje(this, e.mensaje, 0xFF3A4670);
            }
        }
    }
}
