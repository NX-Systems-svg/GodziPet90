package com.cesba.godzipet.ui;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.DetectorAgitar;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Microfono;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Articulo;
import com.cesba.godzipet.modelo.Catalogo;
import com.cesba.godzipet.modelo.Cuidados;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.Necesidades;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Reaccion;
import com.cesba.godzipet.modelo.Resultado;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.modelo.Stat;
import com.cesba.godzipet.modelo.TipoArticulo;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.BarraView;
import com.cesba.godzipet.vista.Filtro;
import com.cesba.godzipet.vista.PetView;
import com.cesba.godzipet.vista.PosterPainter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hogar: el kaiju en su escenario, sus 6 indicadores, el mensaje contextual y las acciones. */
public class HogarFragment extends Fragment implements Refrescable, PetView.Oyente {
    private static final long DURACION_MENSAJE_MS = 4500;
    private static final int[] COLORES_STAT = {R.color.stat_alim, R.color.stat_feliz, R.color.stat_energia,
            R.color.stat_higiene, R.color.stat_salud, R.color.atomico};

    private PetView petView;
    private TextView tvEstado, tvLugar, tvMensaje, btnAliento;
    private final BarraView[] barras = new BarraView[Stat.values().length + 1];
    private final TextView[] valores = new TextView[Stat.values().length + 1];
    private final List<TextView> botones = new ArrayList<>();
    private String ultimoMensaje;
    private long mensajeHasta;
    private DetectorAgitar agitar;
    private final Microfono microfono = new Microfono();

    private final ActivityResultLauncher<String> permisoMic =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {
                if (ok) escucharRugido();
                else mostrarMensaje("🎤 Sin micrófono: toca 🎤 otra vez para que ruja de todos modos.");
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_hogar, cont, false);
        petView = v.findViewById(R.id.petView);
        tvEstado = v.findViewById(R.id.tvEstado);
        tvLugar = v.findViewById(R.id.tvLugar);
        tvMensaje = v.findViewById(R.id.tvMensaje);
        btnAliento = v.findViewById(R.id.btnAliento);
        construirStats(inf, v.findViewById(R.id.panelStats));
        construirAcciones(v.findViewById(R.id.gridAcciones));
        construirExtras(v.findViewById(R.id.filaExtras));
        petView.setOyente(this);
        petView.setMovimientoReducido(Ui.movimientoReducido(requireContext()));
        btnAliento.setOnClickListener(x -> ejecutar(Cuidados.alientoAtomico(partida())));
        tvLugar.setOnClickListener(x -> startActivity(new Intent(requireContext(), MapaActivity.class)));
        agitar = new DetectorAgitar(requireContext(), this::terremoto);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Juego.get(requireContext()).prefs().agitar()) agitar.iniciar();
        petView.setFiltro(Filtro.desde(Juego.get(requireContext()).prefs().filtro()));
        refrescar();
    }

    @Override
    public void onPause() {
        super.onPause();
        agitar.detener();
        microfono.detener();
    }

    private Partida partida() {
        return Juego.get(requireContext()).partida();
    }

    // ------------------------------------------------------------ construcción

    private void construirStats(LayoutInflater inf, LinearLayout panel) {
        panel.addView(Ui.texto(requireContext(), "Bienestar", R.style.Subtitulo));
        for (Stat s : Stat.values()) {
            View fila = inf.inflate(R.layout.item_stat, panel, false);
            ((TextView) fila.findViewById(R.id.tvEtiqueta)).setText(s.icono + " " + s.nombre);
            barras[s.ordinal()] = fila.findViewById(R.id.barra);
            barras[s.ordinal()].configurar(requireContext().getColor(COLORES_STAT[s.ordinal()]), 20);
            if (s == Stat.RADIACION) barras[s.ordinal()].setZona(Necesidades.RAD_IDEAL_MIN, Necesidades.RAD_IDEAL_MAX);
            valores[s.ordinal()] = fila.findViewById(R.id.tvValor);
            panel.addView(fila);
        }
        TextView ayuda = Ui.texto(requireContext(), "☢ La radiación no se llena: se equilibra entre 40 y 70. Con 95 o más su núcleo se funde.",
                R.style.Cuerpo);
        ayuda.setTextSize(12);
        panel.addView(ayuda);
        View fila = inf.inflate(R.layout.item_stat, panel, false);
        ((TextView) fila.findViewById(R.id.tvEtiqueta)).setText("🔥 Aliento");
        int i = Stat.values().length;
        barras[i] = fila.findViewById(R.id.barra);
        barras[i].configurar(requireContext().getColor(R.color.amarillo), 0);
        valores[i] = fila.findViewById(R.id.tvValor);
        panel.addView(fila);
    }

    private void construirAcciones(GridLayout grid) {
        Object[][] acciones = {
                {"🍙", "Comer", R.drawable.bg_boton_naranja},
                {"🎾", "Jugar", R.drawable.bg_boton_amarillo},
                {"💤", "Dormir", R.drawable.bg_boton_azul},
                {"🌊", "Nadar", R.drawable.bg_boton_turquesa},
                {"⛑", "Curar", R.drawable.bg_boton_rosa},
                {"💪", "Entrenar", R.drawable.bg_boton_rojo},
                {"☢", "Central", R.drawable.bg_boton_verde},
                {"🗾", "Viajar", R.drawable.bg_boton_gris}};
        for (int i = 0; i < acciones.length; i++) {
            TextView b = Ui.boton(requireContext(), acciones[i][0] + "\n" + acciones[i][1], (Integer) acciones[i][2]);
            b.setTextSize(13);
            b.setLineSpacing(0, 1.05f);
            b.setGravity(Gravity.CENTER);
            b.setPadding(Ui.dp(requireContext(), 2), b.getPaddingTop(), Ui.dp(requireContext(), 2), b.getPaddingBottom());
            if (i == 7) b.setTextColor(requireContext().getColor(R.color.texto));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(i / 4), GridLayout.spec(i % 4, 1f));
            lp.width = 0;
            int m = Ui.dp(requireContext(), 3);
            lp.setMargins(m, m, m, m);
            final int indice = i;
            b.setOnClickListener(x -> {
                Ui.pulsar(x);
                accion(indice);
            });
            grid.addView(b, lp);
            botones.add(b);
        }
    }

    private void construirExtras(LinearLayout fila) {
        TextView foto = Ui.boton(requireContext(), "📷 Foto póster", R.drawable.bg_boton_amarillo);
        TextView rugir = Ui.boton(requireContext(), "🎤 Rugirle", R.drawable.bg_boton_rosa);
        foto.setTextSize(13);
        rugir.setTextSize(13);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        a.setMarginEnd(Ui.dp(requireContext(), 6));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        bp.setMarginStart(Ui.dp(requireContext(), 6));
        fila.addView(foto, a);
        fila.addView(rugir, bp);
        foto.setOnClickListener(x -> compartirFoto());
        rugir.setOnClickListener(x -> pedirRugido());
    }

    // ------------------------------------------------------------ acciones

    private void accion(int i) {
        Partida p = partida();
        if (p == null) return;
        long ahora = System.currentTimeMillis();
        Juego j = Juego.get(requireContext());
        switch (i) {
            case 0: elegirComida(p); return;
            case 1: ejecutar(Cuidados.jugar(p, ahora)); return;
            case 2: ejecutar(Cuidados.dormir(p, ahora, j.velocidad())); return;
            case 3: ejecutar(Cuidados.banar(p, ahora)); return;
            case 4: ejecutar(Cuidados.curar(p)); return;
            case 5: ejecutar(Cuidados.entrenar(p, ahora)); return;
            case 6: ejecutar(Cuidados.central(p, ahora)); return;
            default: startActivity(new Intent(requireContext(), MapaActivity.class));
        }
    }

    private void elegirComida(Partida p) {
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) {
            ejecutar(Cuidados.alimentar(p, Catalogo.ONIGIRI));
            return;
        }
        List<Articulo> comidas = new ArrayList<>();
        for (Articulo a : Catalogo.porTipo(TipoArticulo.COMIDA)) if (p.getInventario().posee(a.id)) comidas.add(a);
        if (comidas.isEmpty()) {
            Sonidos.error(requireContext());
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin comida 🍙")
                    .setMessage("No tienes alimentos en el inventario. Cómpralos en la tienda con tus monedas.")
                    .setPositiveButton("Ir a la tienda", (d, w) -> ((MainActivity) requireActivity()).mostrarPestana(2, false))
                    .setNegativeButton("Cerrar", null)
                    .show();
            return;
        }
        String[] opciones = new String[comidas.size()];
        for (int k = 0; k < comidas.size(); k++) {
            Articulo a = comidas.get(k);
            opciones[k] = a.icono + "  " + a.nombre + "  ×" + p.getInventario().cantidad(a.id) + "\n      " + a.resumenEfectos();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Qué le das de comer?")
                .setItems(opciones, (d, w) -> ejecutar(Cuidados.alimentar(p, comidas.get(w).id)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void terremoto() {
        if (!isResumed() || partida() == null) return;
        ejecutar(partida().terremoto(System.currentTimeMillis()));
    }

    private void pedirRugido() {
        if (!Juego.get(requireContext()).prefs().microfono() || microfono.escuchando()) {
            ejecutar(partida().rugidoEscuchado(System.currentTimeMillis()));
            return;
        }
        if (Microfono.permitido(requireContext())) escucharRugido();
        else permisoMic.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void escucharRugido() {
        mostrarMensaje("🎤 ¡Ruge fuerte al teléfono! (4 segundos)");
        microfono.escuchar(requireContext(), new Microfono.Oyente() {
            @Override
            public void nivel(float n) {
                if (!isAdded()) return;
                int barras = Math.round(n * 10);
                StringBuilder sb = new StringBuilder("🎤 Escuchando ");
                for (int i = 0; i < 10; i++) sb.append(i < barras ? "▮" : "▯");
                mostrarMensaje(sb.toString());
            }

            @Override
            public void rugido() {
                if (!isAdded()) return;
                Sonidos.rugido(requireContext());
                ejecutar(partida().rugidoEscuchado(System.currentTimeMillis()));
            }

            @Override
            public void fin(boolean detectado) {
                if (isAdded() && !detectado) mostrarMensaje("🎤 No te escuchó. ¡Ruge más fuerte e inténtalo de nuevo!");
            }
        });
    }

    /** Modo foto: captura la escena dentro de un póster de película y la comparte. */
    private void compartirFoto() {
        Partida p = partida();
        if (p == null || petView.getWidth() == 0) return;
        try {
            Bitmap escena = petView.capturar();
            Bitmap poster = PosterPainter.crear(escena, p.getPersonaje().getNombre(),
                    p.getPersonaje().nombreForma(), p.getEscenario().nombre + " · " + p.getEscenario().kanji);
            File dir = new File(requireContext().getCacheDir(), "fotos");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Sin carpeta de fotos");
            File archivo = new File(dir, "godzipet_poster.png");
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                poster.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fotos", archivo);
            Sonidos.ok(requireContext());
            Intent enviar = new Intent(Intent.ACTION_SEND).setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, p.getPersonaje().getNombre() + " en " + p.getEscenario().nombre + " · Godzi-Pet 90")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(enviar, "Compartir póster"));
        } catch (Exception e) {
            android.util.Log.e("Hogar", "foto", e);
            Ui.aviso(requireActivity(), Resultado.error("No se pudo crear la foto."));
        }
    }

    private void ejecutar(Resultado r) {
        if (r == null || !isAdded()) return;
        reaccionar(r);
        if (r.reaccion == Reaccion.ALIENTO || r.reaccion == Reaccion.ECLOSION || r.reaccion == Reaccion.TERREMOTO
                || r.reaccion == Reaccion.SHIE || r.reaccion == Reaccion.ANILLO_HUMO) {
            Sonidos.rugido(requireContext());
        }
        ((MainActivity) requireActivity()).despuesDeAccion(r);
    }

    /** Animación + mensaje contextual que explica la reacción. */
    public void reaccionar(Resultado r) {
        if (petView == null) return;
        petView.reaccionar(r.reaccion);
        mostrarMensaje((r.ok ? "✔ " : "✖ ") + r.mensaje);
    }

    private void mostrarMensaje(String m) {
        ultimoMensaje = m;
        mensajeHasta = System.currentTimeMillis() + DURACION_MENSAJE_MS;
        if (tvMensaje != null) tvMensaje.setText(m);
    }

    // ------------------------------------------------------------ toques secretos

    @Override
    public void tocado() {
        Partida p = partida();
        if (p == null) return;
        if (p.getPersonaje().getEtapa() == Etapa.HUEVO) ejecutar(p.tocarHuevo());
    }

    @Override
    public void toqueCabeza(int seguidos) {
        Partida p = partida();
        if (p == null) return;
        if (seguidos >= 7) {
            ejecutar(p.shie());
        } else {
            Sonidos.toque(requireContext());
            mostrarMensaje(seguidos >= 4 ? "¿Eh? Parece que quiere bailar… (" + seguidos + "/7)"
                    : p.getPersonaje().getNombre() + ": " + p.getEstado().mensaje);
        }
    }

    @Override
    public void toqueTorre() {
        Partida p = partida();
        if (p != null) ejecutar(p.visitaMothra());
    }

    // ------------------------------------------------------------ refresco

    @Override
    public void refrescar() {
        Partida p = partida();
        if (p == null || petView == null) return;
        Apariencia ap = Apariencia.de(p);
        if (p.consumirEvolucionPendiente()) {
            petView.mostrarEvolucion(aparienciaAnterior(ap));
            Sonidos.rugido(requireContext());
            Juego.get(requireContext()).guardar();
            if (getView() instanceof android.widget.ScrollView) ((android.widget.ScrollView) getView()).smoothScrollTo(0, 0);
        }
        petView.setApariencia(ap);
        petView.setEscenario(p.getEscenario());
        boolean huevo = p.getPersonaje().getEtapa() == Etapa.HUEVO;
        tvEstado.setText(huevo ? "🥚 Huevo" : p.getPersonaje().nombreForma() + " · " + p.getEstado().nombre);
        tvLugar.setText("📍 " + p.getEscenario().nombre + " " + p.getEscenario().kanji);

        if (System.currentTimeMillis() > mensajeHasta) {
            tvMensaje.setText(huevo
                    ? "🥚 ¡Toca el huevo para ayudarlo a eclosionar! (" + p.getPersonaje().getToquesHuevo() + "/5)"
                    : "💬 " + p.getEstado().mensaje + pista(p));
        } else if (ultimoMensaje != null) {
            tvMensaje.setText(ultimoMensaje);
        }
        for (Stat s : Stat.values()) {
            int v = p.getNecesidades().valor(s);
            barras[s.ordinal()].setValor(v, 100);
            valores[s.ordinal()].setText(String.valueOf(v));
            valores[s.ordinal()].setTextColor(requireContext().getColor(
                    p.getNecesidades().esCritico(s) ? R.color.rojo : R.color.texto));
        }
        int i = Stat.values().length;
        int carga = (int) p.getCargaAliento();
        barras[i].setValor(carga, 100);
        valores[i].setText(String.valueOf(carga));
        boolean listo = !huevo && carga >= 100 && !p.getPersonaje().isDormido();
        btnAliento.setVisibility(listo ? View.VISIBLE : View.GONE);
        btnAliento.setText(p.alientoEsAnilloDeHumo() ? "💨  ¡SOPLAR ANILLO DE HUMO!" : "🔥  ¡ALIENTO ATÓMICO!");

        botones.get(0).setText("🍙\nComer (" + contarComida(p) + ")");
        botones.get(2).setText(p.getPersonaje().isDormido() ? "☀\nDespertar" : "💤\nDormir");
        botones.get(4).setText("⛑\nCurar (" + p.getInventario().cantidad(Catalogo.BOTIQUIN) + ")");
        for (TextView b : botones) b.setAlpha(huevo ? 0.45f : 1f);
    }

    private String pista(Partida p) {
        if (p.fusionEnCurso()) {
            return String.format(Locale.US, "\n☢ ¡FUSIÓN EN %.0f MIN DE JUEGO! Nada en el mar o usa cadmio.",
                    Math.ceil(p.minutosParaFusion()));
        }
        Stat critico = p.getNecesidades().peorCritico();
        if (critico != null) return "\n⚠ ¡" + critico.nombre + " en nivel crítico!";
        if (p.getPersonaje().isDormido()) {
            long seg = Math.max(0, (p.getPersonaje().getDespertarEn() - System.currentTimeMillis()) / 1000);
            return String.format(Locale.US, "\n⏰ Despierta en %d:%02d", seg / 60, seg % 60);
        }
        if (p.getGeneracion() > 1) return "\n🧬 Generación " + p.getGeneracion() + " (+" + Math.round((p.multLegado() - 1) * 100) + "% XP)";
        return "";
    }

    private int contarComida(Partida p) {
        int n = 0;
        for (Articulo a : Catalogo.porTipo(TipoArticulo.COMIDA)) n += p.getInventario().cantidad(a.id);
        return n;
    }

    private static Apariencia aparienciaAnterior(Apariencia a) {
        Etapa previa = a.etapa == Etapa.FINAL ? Etapa.JUNIOR : a.etapa == Etapa.JUNIOR ? Etapa.PEQUENO
                : a.etapa == Etapa.PEQUENO ? Etapa.BEBE : Etapa.HUEVO;
        return new Apariencia(previa, RutaEvolucion.NINGUNA, a.origen, a.animo, 4, a.radiacion);
    }
}
