package com.cesba.godzipet.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.modelo.Articulo;
import com.cesba.godzipet.modelo.Catalogo;
import com.cesba.godzipet.modelo.Cuidados;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Resultado;
import com.cesba.godzipet.modelo.Tienda;

import java.util.ArrayList;
import java.util.List;

/** Tienda (precios, saldo, bloqueo por nivel) e inventario (cantidades, usar/equipar). */
public class TiendaFragment extends Fragment implements Refrescable {
    private boolean verInventario;
    private TextView tabTienda, tabInventario;
    private final Adaptador adaptador = new Adaptador();
    private String huella = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup cont, Bundle b) {
        View v = inf.inflate(R.layout.fragment_tienda, cont, false);
        tabTienda = v.findViewById(R.id.tabTienda);
        tabInventario = v.findViewById(R.id.tabInventario);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adaptador);
        tabTienda.setOnClickListener(x -> { verInventario = false; refrescar(); });
        tabInventario.setOnClickListener(x -> { verInventario = true; refrescar(); });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
    }

    @Override
    public void refrescar() {
        Partida p = Juego.get(requireContext()).partida();
        if (p == null || tabTienda == null) return;
        String nueva = verInventario + "|" + p.getMonedas() + "|" + p.getNivel() + "|"
                + p.getInventario().comoMapa() + "|" + p.getDecoraciones();
        if (nueva.equals(huella)) return; // sin cambios: no redibujar la lista
        huella = nueva;
        tabTienda.setBackgroundResource(verInventario ? 0 : R.drawable.bg_tab_activa);
        tabInventario.setBackgroundResource(verInventario ? R.drawable.bg_tab_activa : 0);
        List<Articulo> items = new ArrayList<>();
        for (Articulo a : Catalogo.todos()) {
            if (!verInventario || p.getInventario().posee(a.id)) items.add(a);
        }
        adaptador.mostrar(items, p, verInventario);
    }

    private void accion(Articulo a) {
        Partida p = Juego.get(requireContext()).partida();
        Resultado r;
        if (!verInventario) {
            r = Tienda.comprar(p, a.id);
        } else if (a.esConsumible()) {
            r = Cuidados.usarArticulo(p, a.id, System.currentTimeMillis());
        } else {
            ((MainActivity) requireActivity()).mostrarPestana(3, false);
            return;
        }
        ((MainActivity) requireActivity()).despuesDeAccion(r);
    }

    private final class Adaptador extends RecyclerView.Adapter<Adaptador.Fila> {
        private List<Articulo> items = new ArrayList<>();
        private Partida partida;
        private boolean inventario;

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        void mostrar(List<Articulo> items, Partida p, boolean inventario) {
            this.items = items;
            this.partida = p;
            this.inventario = inventario;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fila onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
            return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_articulo, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Fila f, int pos) {
            Articulo a = items.get(pos);
            int cantidad = partida.getInventario().cantidad(a.id);
            f.icono.setText(a.icono);
            f.nombre.setText(a.nombre + (cantidad > 0 && a.esConsumible() ? "  ×" + cantidad : ""));
            f.descripcion.setText(a.descripcion);
            String extra = a.esConsumible() ? a.tipo.nombre + " · " + a.resumenEfectos()
                    : "Decoración · " + a.slot.nombre;
            f.extra.setText(extra);
            boolean bloqueado = partida.getNivel() < a.nivelRequerido;
            if (inventario) {
                boolean equipada = a.slot != null && a.id.equals(partida.getDecoraciones().get(a.slot));
                f.boton.setText(a.esConsumible() ? "USAR" : equipada ? "EN USO ›" : "COLOCAR ›");
                f.boton.setBackgroundResource(a.esConsumible() ? R.drawable.bg_boton_azul : R.drawable.bg_boton_turquesa);
            } else if (bloqueado) {
                f.boton.setText("🔒 NV " + a.nivelRequerido);
                f.boton.setBackgroundResource(R.drawable.bg_boton_gris);
            } else if (!a.esConsumible() && cantidad > 0) {
                f.boton.setText("✔ TUYA");
                f.boton.setBackgroundResource(R.drawable.bg_boton_gris);
            } else {
                boolean alcanza = partida.getMonedas() >= a.precio;
                f.boton.setText("🪙 " + a.precio);
                f.boton.setBackgroundResource(alcanza ? R.drawable.bg_boton_amarillo : R.drawable.bg_boton_gris);
            }
            f.boton.setContentDescription((inventario ? "Usar " : "Comprar ") + a.nombre);
            f.boton.setOnClickListener(x -> accion(a));
            f.itemView.setAlpha(!inventario && bloqueado ? 0.6f : 1f);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class Fila extends RecyclerView.ViewHolder {
            final TextView icono, nombre, descripcion, extra, boton;

            Fila(View v) {
                super(v);
                icono = v.findViewById(R.id.tvIcono);
                nombre = v.findViewById(R.id.tvNombre);
                descripcion = v.findViewById(R.id.tvDescripcion);
                extra = v.findViewById(R.id.tvExtra);
                boton = v.findViewById(R.id.btnAccion);
            }
        }
    }
}
