package com.cesba.godzipet.vista;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.Log;

import com.cesba.godzipet.modelo.Escenario;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Motor 2.5D: fondos por capas pre-renderizadas en Blender
 * (assets/escenas/&lt;escenario&gt;/&lt;dia|noche&gt;_&lt;0-4&gt;.webp, ver docs/ESPEC_CAPAS.md) con
 * desplazamiento parallax según la inclinación del teléfono.
 * Capas 0-3 = fondo (detrás del kaiju); capa 4 = primer plano (delante del kaiju).
 *
 * Memoria: solo se conservan en RAM las 10 imágenes (5 día + 5 noche) del escenario actual; al
 * cambiar de escenario o de tamaño se reciclan las anteriores antes de decodificar las nuevas.
 * La capa 0 (cielo, opaca) usa RGB_565 y el resto ARGB_8888.
 * Hilos: la decodificación corre en un único executor; el hilo de dibujo y el de carga se
 * sincronizan con {@code lock} (el intercambio y el reciclado son atómicos respecto al dibujo).
 */
public final class Parallax {
    private static final String TAG = "Parallax";
    private static final int CAPAS = 5;
    /** Ancho mínimo de cada capa respecto al ancho de la vista (margen para el parallax). */
    private static final float ANCHO_MIN = 1.25f;
    /** Desplazamiento horizontal por capa, como fracción del ancho de la vista. */
    private static final float[] FACTOR = {0.02f, 0.05f, 0.09f, 0.14f, 0.22f};
    private static final float FACTOR_VERTICAL = 0.4f;
    /** Velocidad angular (rad/s) y amplitud relativa de la deriva automática de cámara. */
    private static final float DERIVA_VEL = 0.05f;
    private static final float DERIVA_AMP = 0.25f;
    private static final float ATARDECER_MAX = 0.7f;

    /** Conjunto de bitmaps de un escenario. */
    private static final class Capas {
        final Bitmap[] dia = new Bitmap[CAPAS];
        final Bitmap[] noche = new Bitmap[CAPAS];

        void reciclar() {
            reciclar(dia);
            reciclar(noche);
        }

        private static void reciclar(Bitmap[] a) {
            for (int i = 0; i < a.length; i++) {
                if (a[i] != null) {
                    a[i].recycle();
                    a[i] = null;
                }
            }
        }
    }

    private final AssetManager assets;
    private final Object lock = new Object();
    private final Map<Escenario, Boolean> cacheDisponible = new EnumMap<>(Escenario.class);

    private ExecutorService ejecutor;
    private Capas actual;
    private Escenario escenarioPedido;
    private boolean diaPedido = true, nochePedido = true;
    private int anchoPedido, altoPedido;
    private volatile int generacion;

    // Objetos de dibujo reutilizados (cero asignaciones por cuadro)
    private final Paint pCapa = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint pTinte = new Paint();
    private LinearGradient gradTinte;
    private float gradAlto = -1;

    public Parallax(Context c) {
        assets = c.getApplicationContext().getAssets();
        pTinte.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    /** true si existen las 10 capas de ese escenario en assets (resultado cacheado). */
    public boolean disponible(Escenario e) {
        if (e == null) return false;
        synchronized (cacheDisponible) {
            Boolean v = cacheDisponible.get(e);
            if (v != null) return v;
        }
        boolean ok = comprobar(e);
        synchronized (cacheDisponible) {
            cacheDisponible.put(e, ok);
        }
        return ok;
    }

    private boolean comprobar(Escenario e) {
        try {
            String[] lista = assets.list("escenas/" + e.name());
            if (lista == null) return false;
            Set<String> nombres = new HashSet<>(Arrays.asList(lista));
            for (int i = 0; i < CAPAS; i++) {
                if (!nombres.contains("dia_" + i + ".webp")
                        || !nombres.contains("noche_" + i + ".webp")) return false;
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /** true cuando las capas del escenario pedido ya están cargadas y listas para dibujar. */
    public boolean listo() {
        synchronized (lock) {
            return actual != null;
        }
    }

    /**
     * Prepara (carga/escala y cachea) las capas para el tamaño de la vista. Llamar desde
     * onSizeChanged o al cambiar de escenario. Es idempotente para el mismo escenario y tamaño.
     */
    public void preparar(Escenario e, int anchoVista, int altoVista) {
        preparar(e, anchoVista, altoVista, true, true);
    }

    /**
     * Igual que {@link #preparar(Escenario, int, int)} pero carga sólo los juegos de capas que se
     * van a ver (día, noche o ambos en el atardecer): reduce la memoria a la mitad.
     */
    public void preparar(Escenario e, int anchoVista, int altoVista, boolean conDia, boolean conNoche) {
        if (e == null || anchoVista <= 0 || altoVista <= 0) return;
        synchronized (lock) {
            if (e == escenarioPedido && anchoVista == anchoPedido && altoVista == altoPedido
                    && conDia == diaPedido && conNoche == nochePedido && ejecutor != null) return;
            diaPedido = conDia;
            nochePedido = conNoche;
            escenarioPedido = e;
            anchoPedido = anchoVista;
            altoPedido = altoVista;
            generacion++;
            // Libera lo anterior primero para no sumar dos escenarios en memoria.
            if (actual != null) {
                actual.reciclar();
                actual = null;
            }
            if (ejecutor == null || ejecutor.isShutdown()) {
                ejecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "parallax-carga");
                    t.setDaemon(true);
                    return t;
                });
            }
            final int gen = generacion;
            ejecutor.execute(() -> cargar(e, anchoVista, altoVista, gen, conDia, conNoche));
        }
    }

    /** Se ejecuta en el hilo de carga. */
    private void cargar(Escenario e, int w, int h, int gen, boolean conDia, boolean conNoche) {
        if (!disponible(e)) return;
        Capas nuevas = new Capas();
        try {
            for (int i = 0; i < CAPAS && gen == generacion; i++) {
                String base = "escenas/" + e.name() + "/";
                if (conDia) nuevas.dia[i] = decodificar(base + "dia_" + i + ".webp", w, h, i == 0);
                if (gen != generacion) break;
                if (conNoche) nuevas.noche[i] = decodificar(base + "noche_" + i + ".webp", w, h, i == 0);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Fallo cargando " + e, t);
            nuevas.reciclar();
            return;
        }
        synchronized (lock) {
            if (gen == generacion && (nuevas.dia[0] != null || nuevas.noche[0] != null)) {
                actual = nuevas;
            } else {
                nuevas.reciclar();
            }
        }
    }

    /** Decodifica con inSampleSize y escala a alto >= vista y ancho >= vista * 1.25. */
    private Bitmap decodificar(String ruta, int w, int h, boolean rgb565) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inJustDecodeBounds = true;
        try (InputStream in = assets.open(ruta)) {
            BitmapFactory.decodeStream(in, null, op);
        } catch (IOException ex) {
            Log.w(TAG, "No se pudo abrir " + ruta);
            return null;
        }
        if (op.outWidth <= 0 || op.outHeight <= 0) return null;

        float escala = Math.max(h / (float) op.outHeight, ANCHO_MIN * w / op.outWidth);
        int destW = Math.max(1, Math.round(op.outWidth * escala));
        int destH = Math.max(1, Math.round(op.outHeight * escala));
        int muestreo = 1;
        while (op.outWidth / (muestreo * 2) >= destW && op.outHeight / (muestreo * 2) >= destH) {
            muestreo *= 2;
        }
        op.inJustDecodeBounds = false;
        op.inSampleSize = muestreo;
        op.inPreferredConfig = rgb565 ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
        try (InputStream in = assets.open(ruta)) {
            Bitmap bmp = BitmapFactory.decodeStream(in, null, op);
            if (bmp == null) return null;
            if (bmp.getWidth() == destW && bmp.getHeight() == destH) return bmp;
            Bitmap esc = Bitmap.createScaledBitmap(bmp, destW, destH, true);
            if (esc != bmp) bmp.recycle();
            return esc;
        } catch (IOException | OutOfMemoryError ex) {
            Log.w(TAG, "No se pudo decodificar " + ruta, ex);
            return null;
        }
    }

    /**
     * Dibuja las capas 0-3. incl* en [-1,1] (giroscopio); t en segundos; noche 0..1 mezcla las
     * versiones día/noche; atardecer 0..1 aplica un tinte cálido.
     */
    public void dibujarFondo(Canvas c, float w, float h, float inclX, float inclY, float t,
                             float noche, float atardecer) {
        synchronized (lock) {
            if (actual == null) return;
            for (int i = 0; i < 4; i++) dibujarCapa(c, actual, i, w, h, inclX, inclY, t, noche);
            if (atardecer > 0.01f) dibujarTinte(c, w, h, atardecer);
        }
    }

    /** Dibuja la capa 4 (primer plano) por delante del kaiju. */
    public void dibujarFrente(Canvas c, float w, float h, float inclX, float inclY, float t,
                              float noche) {
        synchronized (lock) {
            if (actual == null) return;
            dibujarCapa(c, actual, 4, w, h, inclX, inclY, t, noche);
        }
    }

    /** Dibuja una capa (día y, encima, noche con alfa) desplazada por parallax y deriva. */
    private void dibujarCapa(Canvas c, Capas capas, int i, float w, float h, float inclX,
                             float inclY, float t, float noche) {
        float n = Math.max(0f, Math.min(1f, noche));
        float f = FACTOR[i];
        float deriva = (float) Math.sin(t * DERIVA_VEL) * f * w * DERIVA_AMP;
        float dx = -inclX * f * w + deriva;
        float dy = -inclY * f * h * FACTOR_VERTICAL;
        Bitmap dia = capas.dia[i], noch = capas.noche[i];
        if (dia == null) {            // sólo se cargó la noche
            dibujarBitmap(c, noch, w, h, dx, dy, 255);
        } else if (noch == null) {    // sólo se cargó el día
            dibujarBitmap(c, dia, w, h, dx, dy, 255);
        } else {
            if (n < 1f) dibujarBitmap(c, dia, w, h, dx, dy, 255);
            if (n > 0f) dibujarBitmap(c, noch, w, h, dx, dy, Math.round(n * 255f));
        }
    }

    private void dibujarBitmap(Canvas c, Bitmap b, float w, float h, float dx, float dy,
                               int alfa) {
        if (b == null || b.isRecycled()) return;
        // El desplazamiento se limita para no descubrir los bordes de la imagen.
        float margenX = Math.max(0f, (b.getWidth() - w) / 2f);
        float margenY = Math.max(0f, (b.getHeight() - h) / 2f);
        float x = Math.max(-margenX, Math.min(margenX, dx));
        float y = Math.max(-margenY, Math.min(margenY, dy));
        pCapa.setAlpha(alfa);
        c.drawBitmap(b, (w - b.getWidth()) / 2f + x, (h - b.getHeight()) / 2f + y, pCapa);
    }

    /** Tinte naranja-magenta multiplicativo; el degradado se recalcula solo si cambia el alto. */
    private void dibujarTinte(Canvas c, float w, float h, float atardecer) {
        if (gradTinte == null || gradAlto != h) {
            gradTinte = new LinearGradient(0, 0, 0, h,
                    new int[]{0xFFE08AA8, 0xFFFF9A50, 0xFFFFC890},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            gradAlto = h;
            pTinte.setShader(gradTinte);
        }
        float a = Math.max(0f, Math.min(1f, atardecer));
        pTinte.setAlpha(Math.round(a * ATARDECER_MAX * 255f));
        c.drawRect(0, 0, w, h, pTinte);
    }

    /** Recicla las capas y detiene el hilo de carga. */
    public void liberar() {
        synchronized (lock) {
            generacion++;
            if (actual != null) {
                actual.reciclar();
                actual = null;
            }
            escenarioPedido = null;
            if (ejecutor != null) {
                ejecutor.shutdownNow();
                ejecutor = null;
            }
        }
    }
}
