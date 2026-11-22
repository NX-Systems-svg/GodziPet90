package com.cesba.godzipet.juego;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.cesba.godzipet.R;
import com.cesba.godzipet.datos.Preferencias;
import com.cesba.godzipet.datos.Repositorio;
import com.cesba.godzipet.modelo.Necesidades;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.modelo.Stat;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuPainter;

/** Widget de pantalla de inicio: muestra al kaiju, su ánimo y tres barras de estado. */
public class WidgetKaiju extends AppWidgetProvider {
    private static final String TAG = "WidgetKaiju";
    private static final int LADO_PX = 300;

    /** Pide redibujar todos los widgets (se llama al salir de la app). */
    public static void actualizar(Context c) {
        try {
            Context app = c.getApplicationContext();
            AppWidgetManager mgr = AppWidgetManager.getInstance(app);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(app, WidgetKaiju.class));
            if (ids == null || ids.length == 0) return;
            app.sendBroadcast(new Intent(app, WidgetKaiju.class)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids));
        } catch (Exception e) {
            Log.e(TAG, "actualizar", e);
        }
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        final PendingResult pendiente = goAsync();
        try {
            Juego juego = Juego.get(ctx);
            if (juego.cargada()) {
                pintar(ctx, mgr, ids, juego.partida());
                pendiente.finish();
                return;
            }
            long uid = new Preferencias(ctx).usuarioId();
            if (uid <= 0) {
                pintar(ctx, mgr, ids, null);
                pendiente.finish();
                return;
            }
            Repositorio.get(ctx).cargar(uid, p -> {
                try {
                    pintar(ctx, mgr, ids, p);
                } finally {
                    pendiente.finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "onUpdate", e);
            pendiente.finish();
        }
    }

    private static void pintar(Context ctx, AppWidgetManager mgr, int[] ids, Partida p) {
        try {
            RemoteViews rv = construir(ctx, p);
            mgr.updateAppWidget(ids, rv);
        } catch (Exception e) {
            Log.e(TAG, "pintar", e);
        }
    }

    private static RemoteViews construir(Context ctx, Partida p) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_kaiju);
        Intent abrir = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (abrir != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, abrir, PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widgetRaiz, pi);
        }
        if (p == null) {
            rv.setTextViewText(R.id.widgetNombre, "Godzi-Pet 90");
            rv.setTextViewText(R.id.widgetEstado, "Abre Godzi-Pet 90 para empezar");
            for (int id : new int[]{R.id.widgetComidaBarra, R.id.widgetFelizBarra, R.id.widgetRadBarra,
                    R.id.widgetComidaTxt, R.id.widgetFelizTxt, R.id.widgetRadTxt, R.id.widgetRadTexto, R.id.widgetKaiju}) {
                rv.setViewVisibility(id, View.GONE);
            }
            return rv;
        }
        for (int id : new int[]{R.id.widgetComidaBarra, R.id.widgetFelizBarra, R.id.widgetRadBarra,
                R.id.widgetComidaTxt, R.id.widgetFelizTxt, R.id.widgetRadTxt, R.id.widgetRadTexto, R.id.widgetKaiju}) {
            rv.setViewVisibility(id, View.VISIBLE);
        }
        Necesidades n = p.getNecesidades();
        int rad = n.valor(Stat.RADIACION);
        rv.setTextViewText(R.id.widgetNombre, p.getPersonaje().getNombre());
        rv.setTextViewText(R.id.widgetEstado, p.getPersonaje().nombreForma() + " · " + p.getEstado().nombre);
        rv.setProgressBar(R.id.widgetComidaBarra, 100, n.valor(Stat.ALIMENTACION), false);
        rv.setProgressBar(R.id.widgetFelizBarra, 100, n.valor(Stat.FELICIDAD), false);
        rv.setProgressBar(R.id.widgetRadBarra, 100, rad, false);
        rv.setTextViewText(R.id.widgetRadTexto, textoRadiacion(n, rad));
        Bitmap bmp = dibujarKaiju(p);
        if (bmp != null) rv.setImageViewBitmap(R.id.widgetKaiju, bmp);
        return rv;
    }

    private static String textoRadiacion(Necesidades n, int rad) {
        if (rad >= Necesidades.RAD_ALTA) return "⚠ " + rad + " · sobrecalentado";
        if (n.enZonaIdeal()) return "☢ " + rad + " · zona ideal";
        if (rad < Necesidades.RAD_BAJA) return "☢ " + rad + " · sin radiación";
        return "☢ " + rad + " · fuera de zona ideal";
    }

    /** Renderiza al kaiju fuera de pantalla sobre fondo transparente. */
    private static Bitmap dibujarKaiju(Partida p) {
        try {
            Bitmap bmp = Bitmap.createBitmap(LADO_PX, LADO_PX, Bitmap.Config.ARGB_8888);
            Apariencia a = Apariencia.de(p);
            float tam = LADO_PX * 0.8f / Math.max(0.62f, a.escalaEtapa());
            new KaijuPainter().dibujar(new Canvas(bmp), LADO_PX / 2f, LADO_PX * 0.95f, tam, a, 0f, 0f, false);
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "dibujarKaiju", e);
            return null;
        }
    }
}
