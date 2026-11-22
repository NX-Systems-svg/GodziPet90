package com.cesba.godzipet.juego;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.cesba.godzipet.modelo.Escenario;

import java.util.Random;

/**
 * Música ambiental sintetizada en tiempo real (sin archivos): cuerdas tipo koto con el algoritmo
 * Karplus-Strong sobre la escala japonesa "In" (miyako-bushi) y un tambor taiko grave.
 * Cada escenario tiene su propia nota base y tempo.
 */
public final class Musica {
    private static final int TASA = 22050;
    private static final float VOLUMEN = 0.22f;
    // escala In: semitonos 0, 1, 5, 7, 8 (dos octavas)
    private static final int[] ESCALA = {0, 1, 5, 7, 8, 12, 13, 17, 19, 20};

    private static Musica instancia;

    /** Bandera de la sesión de música actual: cada hilo tiene la suya, así nunca suenan dos. */
    private java.util.concurrent.atomic.AtomicBoolean sesion;
    private volatile boolean sonando;
    private volatile Escenario escenario = Escenario.ISLA_ODO;
    private Thread hilo;

    public static synchronized Musica get() {
        if (instancia == null) instancia = new Musica();
        return instancia;
    }

    public void setEscenario(Escenario e) {
        if (e != null) escenario = e;
    }

    public synchronized void iniciar() {
        if (sonando) return;
        sonando = true;
        final java.util.concurrent.atomic.AtomicBoolean activa = new java.util.concurrent.atomic.AtomicBoolean(true);
        sesion = activa;
        hilo = new Thread(() -> bucle(activa), "musica");
        hilo.setPriority(Thread.MIN_PRIORITY + 1);
        hilo.start();
    }

    public synchronized void detener() {
        sonando = false;
        if (sesion != null) sesion.set(false); // el hilo viejo termina aunque se reinicie enseguida
        sesion = null;
        hilo = null;
    }

    private static float baseHz(Escenario e) {
        switch (e) {
            case GINZA: case TORRE_TOKIO: return 220f;      // ciudad: más brillante
            case ISLA_MONSTRUOS: return 196f;
            case KAMAKURA: case MONTE_FUJI: return 164.8f;  // templos: más grave y lento
            default: return 185f;
        }
    }

    private static int tempoMs(Escenario e) {
        switch (e) {
            case GINZA: case TORRE_TOKIO: return 380;
            case KAMAKURA: case MONTE_FUJI: return 620;
            default: return 500;
        }
    }

    private void bucle(java.util.concurrent.atomic.AtomicBoolean activa) {
        AudioTrack pista = null;
        try {
            int min = AudioTrack.getMinBufferSize(TASA, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            pista = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(TASA).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(Math.max(min, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM).build();
            pista.play();
            generar(pista, activa);
        } catch (Exception e) {
            Log.w("Musica", "Sin música ambiental", e);
        } finally {
            if (pista != null) {
                try { pista.stop(); } catch (IllegalStateException ignorada) { Log.d("Musica", "stop", ignorada); }
                pista.release();
            }
        }
    }

    /** Mezcla dos voces de koto + taiko en bloques de un "tiempo" musical. */
    private void generar(AudioTrack pista, java.util.concurrent.atomic.AtomicBoolean activa) {
        Random azar = new Random();
        float[] cuerdaA = new float[TASA / 40];
        float[] cuerdaB = new float[TASA / 40];
        int largoA = 1, largoB = 1, posA = 0, posB = 0;
        float taiko = 0, faseTaiko = 0;
        int paso = 0, nota = 4;
        while (activa.get()) {
            Escenario e = escenario;
            int muestras = TASA * tempoMs(e) / 1000;
            short[] bloque = new short[muestras];
            // melodía: caminata aleatoria por la escala; silencios para que respire
            if (azar.nextFloat() < 0.78f) {
                nota = Math.max(0, Math.min(ESCALA.length - 1, nota + azar.nextInt(5) - 2));
                largoA = pulsar(cuerdaA, frecuencia(e, ESCALA[nota]), azar);
                posA = 0;
            }
            if (paso % 4 == 0) { // acompañamiento grave en el primer tiempo
                largoB = pulsar(cuerdaB, frecuencia(e, ESCALA[azar.nextInt(3)] - 12), azar);
                posB = 0;
            }
            if (paso % 8 == 0) taiko = 1f;
            for (int i = 0; i < muestras; i++) {
                float a = cuerdaA[posA];
                int sigA = (posA + 1) % largoA;
                cuerdaA[posA] = 0.4985f * (a + cuerdaA[sigA]);
                posA = sigA;
                float b = cuerdaB[posB];
                int sigB = (posB + 1) % largoB;
                cuerdaB[posB] = 0.4975f * (b + cuerdaB[sigB]);
                posB = sigB;
                faseTaiko += (55f + 40f * taiko) / TASA;
                float tambor = (float) Math.sin(faseTaiko * 2 * Math.PI) * taiko * taiko;
                taiko *= 0.99965f;
                float v = (a * 0.55f + b * 0.4f + tambor * 0.6f) * VOLUMEN;
                bloque[i] = (short) (Math.max(-1f, Math.min(1f, v)) * 32000);
            }
            if (pista.write(bloque, 0, bloque.length) < 0) break;
            paso++;
        }
    }

    private static float frecuencia(Escenario e, int semitonos) {
        return (float) (baseHz(e) * Math.pow(2, semitonos / 12.0));
    }

    /** Karplus-Strong: llena la "cuerda" con ruido; su largo define la nota. */
    private static int pulsar(float[] cuerda, float hz, Random azar) {
        int largo = Math.max(2, Math.min(cuerda.length, Math.round(TASA / hz)));
        for (int i = 0; i < largo; i++) cuerda[i] = azar.nextFloat() * 2 - 1;
        return largo;
    }
}
