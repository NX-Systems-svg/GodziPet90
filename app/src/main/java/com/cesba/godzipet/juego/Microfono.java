package com.cesba.godzipet.juego;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Escucha el micrófono hasta 4 segundos: si el jugador ruge (volumen alto), avisa.
 * Nada se graba ni se guarda: sólo se mide la amplitud en memoria.
 */
public final class Microfono {
    private static final int TASA = 16000;
    private static final int UMBRAL = 9000;      // amplitud pico (0-32767)
    private static final long DURACION_MS = 4000;

    public interface Oyente {
        void nivel(float nivel01);
        void rugido();
        void fin(boolean detectado);
    }

    private final Handler principal = new Handler(Looper.getMainLooper());
    private volatile boolean escuchando;

    public static boolean permitido(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean escuchando() { return escuchando; }

    public void detener() { escuchando = false; }

    @SuppressWarnings("MissingPermission")
    public void escuchar(Context c, Oyente o) {
        if (escuchando || !permitido(c)) {
            if (!escuchando) o.fin(false);
            return;
        }
        escuchando = true;
        new Thread(() -> {
            boolean detectado = false;
            AudioRecord rec = null;
            try {
                int buf = Math.max(AudioRecord.getMinBufferSize(TASA, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT), 2048);
                rec = new AudioRecord(MediaRecorder.AudioSource.MIC, TASA, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, buf);
                short[] datos = new short[buf / 2];
                rec.startRecording();
                long fin = System.currentTimeMillis() + DURACION_MS;
                while (escuchando && System.currentTimeMillis() < fin) {
                    int n = rec.read(datos, 0, datos.length);
                    int pico = 0;
                    for (int i = 0; i < n; i++) pico = Math.max(pico, Math.abs(datos[i]));
                    float nivel = Math.min(1f, pico / (float) UMBRAL);
                    principal.post(() -> o.nivel(nivel));
                    if (pico >= UMBRAL) {
                        detectado = true;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w("Microfono", "No se pudo usar el micrófono", e);
            } finally {
                if (rec != null) {
                    try { rec.stop(); } catch (Exception ignorada) { Log.d("Microfono", "stop", ignorada); }
                    rec.release();
                }
                escuchando = false;
            }
            boolean r = detectado;
            principal.post(() -> {
                if (r) o.rugido();
                o.fin(r);
            });
        }, "microfono").start();
    }
}
