package com.cesba.godzipet.juego;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Inclinación suavizada del teléfono en [-1, 1] para el parallax 2.5D. Usa el vector de
 * gravedad (acelerómetro) respecto a la postura inicial, así funciona aunque no haya giroscopio.
 */
public final class Inclinacion implements SensorEventListener {
    private static final float RANGO_G = 0.35f;   // inclinación (en g) que equivale a 1.0
    private static final float SUAVIZADO = 0.12f;

    private final SensorManager sensores;
    private final Sensor sensor;
    private float baseX = Float.NaN, baseY;
    private volatile float x, y;

    public Inclinacion(Context c) {
        sensores = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        Sensor g = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensor = g != null ? g : sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void iniciar() {
        baseX = Float.NaN;
        if (sensor != null) sensores.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void detener() {
        if (sensores != null) sensores.unregisterListener(this);
        x = 0;
        y = 0;
    }

    public float x() { return x; }
    public float y() { return y; }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float gx = e.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = e.values[1] / SensorManager.GRAVITY_EARTH;
        if (Float.isNaN(baseX)) {
            baseX = gx;
            baseY = gy;
        }
        // la postura de referencia se adapta lentamente (si el jugador se acomoda)
        baseX += (gx - baseX) * 0.002f;
        baseY += (gy - baseY) * 0.002f;
        float objX = Math.max(-1, Math.min(1, -(gx - baseX) / RANGO_G));
        float objY = Math.max(-1, Math.min(1, (gy - baseY) / RANGO_G));
        x += (objX - x) * SUAVIZADO;
        y += (objY - y) * SUAVIZADO;
    }

    @Override
    public void onAccuracyChanged(Sensor s, int a) { }
}
