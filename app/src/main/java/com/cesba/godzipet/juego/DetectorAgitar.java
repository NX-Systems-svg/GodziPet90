package com.cesba.godzipet.juego;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** Detecta cuando el jugador agita el teléfono (aceleración &gt; 2.6 g) → terremoto. */
public final class DetectorAgitar implements SensorEventListener {
    private static final float UMBRAL_G = 2.6f;
    private static final long ESPERA_MS = 1500;

    public interface Oyente { void agitado(); }

    private final SensorManager sensores;
    private final Sensor acelerometro;
    private final Oyente oyente;
    private long ultimo;

    public DetectorAgitar(Context c, Oyente oyente) {
        this.sensores = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        this.acelerometro = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.oyente = oyente;
    }

    public void iniciar() {
        if (acelerometro != null) sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    public void detener() {
        if (sensores != null) sensores.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float gx = e.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = e.values[1] / SensorManager.GRAVITY_EARTH;
        float gz = e.values[2] / SensorManager.GRAVITY_EARTH;
        double fuerza = Math.sqrt(gx * gx + gy * gy + gz * gz);
        long ahora = System.currentTimeMillis();
        if (fuerza > UMBRAL_G && ahora - ultimo > ESPERA_MS) {
            ultimo = ahora;
            oyente.agitado();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
