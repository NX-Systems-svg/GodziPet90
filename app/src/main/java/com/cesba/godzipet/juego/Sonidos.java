package com.cesba.godzipet.juego;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.util.Log;

import java.util.Random;

/** Efectos de sonido generados por código (sin archivos de audio). */
public final class Sonidos {
    private static final int TASA = 22050;
    private static short[] rugidoCache;

    private Sonidos() { }

    private static boolean activo(Context c) {
        return Juego.get(c).prefs().sonido();
    }

    public static void ok(Context c) { tono(c, ToneGenerator.TONE_PROP_ACK, 120); }
    public static void error(Context c) { tono(c, ToneGenerator.TONE_PROP_NACK, 180); }
    public static void moneda(Context c) { tono(c, ToneGenerator.TONE_CDMA_PIP, 150); }
    public static void toque(Context c) { tono(c, ToneGenerator.TONE_PROP_BEEP, 60); }

    private static void tono(Context c, int tipo, int ms) {
        if (!activo(c)) return;
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            tg.startTone(tipo, ms);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(tg::release, ms + 100L);
        } catch (RuntimeException e) {
            Log.w("Sonidos", "Sin audio disponible", e);
        }
    }

    /** Rugido sintetizado: diente de sierra descendente + ruido, con envolvente. */
    public static void rugido(Context c) {
        if (!activo(c)) return;
        new Thread(() -> {
            try {
                short[] pcm = generarRugido();
                AudioTrack t = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(TASA)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(pcm.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                t.write(pcm, 0, pcm.length);
                t.play();
                Thread.sleep(pcm.length * 1000L / TASA + 100);
                t.release();
            } catch (Exception e) {
                Log.w("Sonidos", "No se pudo reproducir el rugido", e);
            }
        }, "rugido").start();
    }

    private static synchronized short[] generarRugido() {
        if (rugidoCache != null) return rugidoCache;
        int n = (int) (TASA * 1.3);
        short[] s = new short[n];
        Random r = new Random(7);
        double fase = 0, ruido = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double freq = 190 - 120 * t + 15 * Math.sin(i / 600.0);
            fase += freq / TASA;
            double sierra = 2 * (fase - Math.floor(fase)) - 1;
            ruido = 0.85 * ruido + 0.15 * (r.nextDouble() * 2 - 1);
            double env = Math.min(1, t * 12) * Math.pow(1 - t, 1.4);
            double v = (0.55 * sierra + 0.6 * ruido) * env;
            s[i] = (short) (Math.max(-1, Math.min(1, v)) * 26000);
        }
        rugidoCache = s;
        return s;
    }
}
