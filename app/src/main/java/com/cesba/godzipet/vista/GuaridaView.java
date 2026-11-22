package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.cesba.godzipet.modelo.Slot;

import java.util.EnumMap;
import java.util.Map;

/** Guarida del kaiju: 4 posiciones fijas donde se dibujan las decoraciones equipadas. */
public class GuaridaView extends View {
    public interface AlTocarSlot {
        void tocado(Slot slot);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint etiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final KaijuPainter kaiju = new KaijuPainter();
    private final EscenaPainter escena = new EscenaPainter();
    private final Map<Slot, RectF> zonas = new EnumMap<>(Slot.class);
    private final long inicio = System.currentTimeMillis();

    private Map<Slot, String> equipadas = new EnumMap<>(Slot.class);
    private Apariencia apariencia;
    private Slot resaltado;
    private AlTocarSlot alTocar;

    public GuaridaView(Context c) { this(c, null); }

    public GuaridaView(Context c, AttributeSet a) {
        super(c, a);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(4);
        borde.setColor(0xFF0B0F14);
        etiqueta.setTextAlign(Paint.Align.CENTER);
        etiqueta.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        setContentDescription("Guarida del kaiju. Toca un espacio para decorarlo.");
    }

    public void setDatos(Map<Slot, String> equipadas, Apariencia apariencia) {
        this.equipadas = new EnumMap<>(Slot.class);
        this.equipadas.putAll(equipadas);
        this.apariencia = apariencia;
        invalidate();
    }

    public void setResaltado(Slot s) {
        resaltado = s;
        invalidate();
    }

    public void setAlTocar(AlTocarSlot l) { alTocar = l; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        zonas.put(Slot.VENTANA, new RectF(w * 0.24f, h * 0.07f, w * 0.76f, h * 0.43f));
        zonas.put(Slot.IZQUIERDA, new RectF(w * 0.02f, h * 0.42f, w * 0.3f, h * 0.84f));
        zonas.put(Slot.DERECHA, new RectF(w * 0.72f, h * 0.3f, w * 0.98f, h * 0.84f));
        zonas.put(Slot.SUELO, new RectF(w * 0.26f, h * 0.83f, w * 0.74f, h * 0.98f));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP && alTocar != null) {
            for (Map.Entry<Slot, RectF> z : zonas.entrySet()) {
                if (z.getValue().contains(e.getX(), e.getY())) {
                    alTocar.tocado(z.getKey());
                    performClick();
                    return true;
                }
            }
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w == 0 || zonas.isEmpty()) return;
        float t = (System.currentTimeMillis() - inicio) / 1000f;
        // pared de ladrillo
        p.setShader(new LinearGradient(0, 0, 0, h * 0.7f, 0xFF3A3F55, 0xFF262A3B, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h * 0.7f, p);
        p.setShader(null);
        p.setColor(0x22000000);
        float lad = h / 18f;
        for (int fila = 0; fila * lad < h * 0.7f; fila++) {
            float off = (fila % 2) * lad;
            c.drawLine(0, fila * lad, w, fila * lad, p);
            for (float x = off; x < w; x += lad * 2) c.drawLine(x, fila * lad, x, (fila + 1) * lad, p);
        }
        // piso
        p.setColor(0xFF4A3426);
        c.drawRect(0, h * 0.7f, w, h, p);
        p.setColor(0xFF5E4331);
        for (float y = h * 0.7f; y < h; y += h / 14f) c.drawRect(0, y + 2, w, y + h / 14f - 2, p);
        p.setColor(0xFF1E1510);
        c.drawRect(0, h * 0.695f, w, h * 0.71f, p);

        dibujarVentana(c, zonas.get(Slot.VENTANA), equipadas.get(Slot.VENTANA), t);
        dibujarSuelo(c, zonas.get(Slot.SUELO), equipadas.get(Slot.SUELO), t);
        dibujarObjeto(c, zonas.get(Slot.IZQUIERDA), equipadas.get(Slot.IZQUIERDA), t);
        dibujarObjeto(c, zonas.get(Slot.DERECHA), equipadas.get(Slot.DERECHA), t);

        if (apariencia != null) {
            float tam = h * 0.52f;
            kaiju.dibujar(c, w * 0.52f, h * 0.9f, tam, apariencia, t, 0, false);
        }
        for (Slot s : Slot.values()) {
            if (equipadas.get(s) == null || s == resaltado) marcoVacio(c, s, zonas.get(s), s == resaltado);
        }
        postInvalidateDelayed(40);
    }

    private void marcoVacio(Canvas c, Slot s, RectF r, boolean activo) {
        Paint d = new Paint(Paint.ANTI_ALIAS_FLAG);
        d.setStyle(Paint.Style.STROKE);
        d.setStrokeWidth(activo ? 6 : 3);
        d.setColor(activo ? 0xFFFFD23F : 0x99FFFFFF);
        d.setPathEffect(new DashPathEffect(new float[]{14, 10}, 0));
        c.drawRoundRect(r, 14, 14, d);
        if (equipadas.get(s) == null) {
            etiqueta.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
            etiqueta.setColor(0xCCFFFFFF);
            String corto = s == Slot.VENTANA ? "Ventanal" : s == Slot.IZQUIERDA ? "Izquierda"
                    : s == Slot.DERECHA ? "Derecha" : "Suelo";
            float y = s == Slot.SUELO ? r.centerY() + etiqueta.getTextSize() / 3 : r.top + etiqueta.getTextSize() * 1.4f;
            c.drawText("+ " + corto, r.centerX(), y, etiqueta);
        }
    }

    // ------------------------------------------------------------ decoraciones

    private void dibujarVentana(Canvas c, RectF r, String id, float t) {
        c.save();
        c.clipRect(r);
        if ("v_skyline".equals(id)) {
            escena.cielo(c, r.right, r.bottom, 2, t, true);
            c.save();
            c.translate(r.left, 0);
            escena.ciudad(c, r.width(), r.bottom, r.height() * 0.6f, 2, true);
            c.restore();
        } else if ("v_arrecife".equals(id)) {
            p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xFF2EA6D8, 0xFF0B3F73, Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            int[] colores = {0xFFFF6F91, 0xFFFFB347, 0xFFB388FF};
            for (int i = 0; i < 6; i++) {
                p.setColor(colores[i % 3]);
                float x = r.left + r.width() * (0.08f + i * 0.17f);
                c.drawCircle(x, r.bottom - 6, r.height() * (0.12f + (i % 2) * 0.06f), p);
            }
            p.setColor(0xFFFFE066);
            for (int i = 0; i < 3; i++) {
                float x = r.left + ((t * 30 + i * 70) % (r.width() + 40)) - 20;
                float y = r.top + r.height() * (0.25f + i * 0.18f);
                c.drawOval(x - 9, y - 5, x + 9, y + 5, p);
            }
        } else if ("v_isla".equals(id)) {
            escena.cielo(c, r.right, r.bottom, 1, t, false);
            p.setColor(0xFF3B2430);
            path.reset();
            path.moveTo(r.left + r.width() * 0.2f, r.bottom);
            path.lineTo(r.left + r.width() * 0.45f, r.top + r.height() * 0.4f);
            path.lineTo(r.left + r.width() * 0.55f, r.top + r.height() * 0.4f);
            path.lineTo(r.left + r.width() * 0.85f, r.bottom);
            path.close();
            c.drawPath(path, p);
            p.setColor(0x88B0A0A0);
            for (int i = 0; i < 3; i++) {
                float f = (t * 0.3f + i / 3f) % 1f;
                c.drawCircle(r.left + r.width() * 0.5f + f * 20, r.top + r.height() * (0.35f - f * 0.3f), 8 + f * 14, p);
            }
            p.setColor(0xFFFF6A2B);
            c.drawRect(r.left + r.width() * 0.47f, r.top + r.height() * 0.4f, r.left + r.width() * 0.53f, r.top + r.height() * 0.44f, p);
        } else {
            p.setColor(0xFF4C5670);
            c.drawRect(r, p);
            p.setColor(0x55FFFFFF);
            for (int i = 0; i < 18; i++) {
                float x = r.left + (i * 37 % 100) / 100f * r.width();
                float y = r.top + ((t * 200 + i * 53) % r.height());
                c.drawLine(x, y, x - 4, y + 12, p);
            }
        }
        c.restore();
        // marco de la ventana
        borde.setStrokeWidth(8);
        borde.setColor(0xFF161A26);
        c.drawRect(r, borde);
        c.drawLine(r.centerX(), r.top, r.centerX(), r.bottom, borde);
        c.drawLine(r.left, r.centerY(), r.right, r.centerY(), borde);
        borde.setStrokeWidth(4);
        borde.setColor(0xFF0B0F14);
    }

    private void dibujarSuelo(Canvas c, RectF r, String id, float t) {
        if ("s_algas".equals(id)) {
            p.setColor(0xFF2F7D4A);
            c.drawOval(r, p);
            p.setColor(0xFF4FB06B);
            for (int i = 0; i < 7; i++) {
                float x = r.left + r.width() * (0.15f + i * 0.11f);
                c.drawLine(x, r.centerY() + 6, x + (float) Math.sin(t * 2 + i) * 6, r.top - 2, p);
            }
            c.drawOval(r, borde);
        } else if ("s_conos".equals(id)) {
            for (int i = 0; i < 3; i++) {
                float x = r.left + r.width() * (0.2f + i * 0.3f);
                float base = r.bottom - 4;
                float alto = r.height() * (i == 1 ? 0.5f : 0.9f);
                path.reset();
                path.moveTo(x - 16, base);
                path.lineTo(x + (i == 1 ? 14 : 0), base - alto);
                path.lineTo(x + 16, base);
                path.close();
                p.setColor(0xFFFF7A1A);
                c.drawPath(path, p);
                c.drawPath(path, borde);
                p.setColor(Color.WHITE);
                c.drawRect(x - 9, base - alto * 0.5f, x + 9, base - alto * 0.38f, p);
            }
        } else if ("s_charco".equals(id)) {
            float pulso = 0.7f + 0.3f * (float) Math.sin(t * 3);
            p.setShader(new RadialGradient(r.centerX(), r.centerY(), r.width() / 2,
                    Color.argb((int) (255 * pulso), 155, 255, 122), 0x002E8B57, Shader.TileMode.CLAMP));
            c.drawOval(r, p);
            p.setShader(null);
        }
    }

    private void dibujarObjeto(Canvas c, RectF r, String id, float t) {
        if (id == null) return;
        float cx = r.centerX(), base = r.bottom, w = r.width(), h = r.height();
        switch (id) {
            case "i_helicoptero": {
                float y = base - h * 0.45f + (float) Math.sin(t * 2) * 4;
                p.setColor(0xFF5D6B3A);
                c.drawOval(cx - w * 0.32f, y - h * 0.12f, cx + w * 0.22f, y + h * 0.1f, p);
                c.drawOval(cx - w * 0.32f, y - h * 0.12f, cx + w * 0.22f, y + h * 0.1f, borde);
                c.drawRect(cx + w * 0.15f, y - h * 0.03f, cx + w * 0.46f, y + h * 0.02f, p);
                p.setColor(0xFF9FD8FF);
                c.drawOval(cx - w * 0.3f, y - h * 0.09f, cx - w * 0.12f, y + h * 0.02f, p);
                float aspa = w * 0.45f * (float) Math.abs(Math.cos(t * 25));
                borde.setStrokeWidth(5);
                c.drawLine(cx - aspa, y - h * 0.16f, cx + aspa, y - h * 0.16f, borde);
                c.drawLine(cx - w * 0.05f, y - h * 0.12f, cx - w * 0.05f, y - h * 0.16f, borde);
                c.drawLine(cx - w * 0.3f, y + h * 0.16f, cx + w * 0.15f, y + h * 0.16f, borde);
                borde.setStrokeWidth(4);
                break;
            }
            case "i_barco": {
                float y = base - h * 0.05f;
                path.reset();
                path.moveTo(cx - w * 0.45f, y - h * 0.22f);
                path.lineTo(cx + w * 0.45f, y - h * 0.22f);
                path.lineTo(cx + w * 0.32f, y);
                path.lineTo(cx - w * 0.36f, y);
                path.close();
                p.setColor(0xFFF2F2F2);
                c.drawPath(path, p);
                c.drawPath(path, borde);
                p.setColor(0xFF2E5C8A);
                c.drawRect(cx - w * 0.36f, y - h * 0.06f, cx + w * 0.34f, y - h * 0.02f, p);
                p.setColor(0xFFE8E8E8);
                c.drawRect(cx - w * 0.12f, y - h * 0.42f, cx + w * 0.18f, y - h * 0.22f, p);
                c.drawRect(cx - w * 0.12f, y - h * 0.42f, cx + w * 0.18f, y - h * 0.22f, borde);
                p.setColor(0xFF9FD8FF);
                c.drawRect(cx - w * 0.06f, y - h * 0.37f, cx + w * 0.12f, y - h * 0.3f, p);
                c.drawLine(cx + w * 0.02f, y - h * 0.42f, cx + w * 0.02f, y - h * 0.7f, borde);
                break;
            }
            case "d_antena": {
                borde.setStrokeWidth(5);
                borde.setColor(0xFF9AA3B5);
                float top = r.top + h * 0.05f;
                c.drawLine(cx - w * 0.25f, base, cx, top + h * 0.3f, borde);
                c.drawLine(cx + w * 0.25f, base, cx, top + h * 0.3f, borde);
                for (int i = 1; i < 6; i++) {
                    float y = base - (base - top - h * 0.3f) * i / 6f;
                    float ancho = w * 0.25f * (1 - i / 6f);
                    c.drawLine(cx - ancho, y, cx + ancho, y - 8, borde);
                }
                c.drawLine(cx, top + h * 0.3f, cx + w * 0.22f, top, borde); // parte doblada
                borde.setStrokeWidth(4);
                borde.setColor(0xFF0B0F14);
                p.setColor(((int) (t * 2)) % 2 == 0 ? 0xFFFF3B3B : 0xFF5A1010);
                c.drawCircle(cx + w * 0.22f, top, 8, p);
                break;
            }
            case "d_estatua": {
                int verde = 0xFF5FB59A;
                p.setColor(0xFF8D8F96);
                c.drawRect(cx - w * 0.22f, base - h * 0.22f, cx + w * 0.22f, base, p);
                c.drawRect(cx - w * 0.22f, base - h * 0.22f, cx + w * 0.22f, base, borde);
                path.reset();
                path.moveTo(cx - w * 0.16f, base - h * 0.22f);
                path.lineTo(cx - w * 0.07f, base - h * 0.68f);
                path.lineTo(cx + w * 0.07f, base - h * 0.68f);
                path.lineTo(cx + w * 0.16f, base - h * 0.22f);
                path.close();
                p.setColor(verde);
                c.drawPath(path, p);
                c.drawPath(path, borde);
                c.drawCircle(cx, base - h * 0.74f, w * 0.08f, p);
                c.drawCircle(cx, base - h * 0.74f, w * 0.08f, borde);
                borde.setStrokeWidth(7);
                borde.setColor(verde);
                c.drawLine(cx + w * 0.06f, base - h * 0.62f, cx + w * 0.16f, base - h * 0.9f, borde);
                borde.setStrokeWidth(4);
                borde.setColor(0xFF0B0F14);
                p.setColor(0xFFFFC23D);
                c.drawCircle(cx + w * 0.16f, base - h * 0.93f, 7 + (float) Math.sin(t * 8) * 2, p);
                borde.setStrokeWidth(3);
                for (int i = -2; i <= 2; i++) {
                    c.drawLine(cx, base - h * 0.8f, cx + i * w * 0.05f, base - h * 0.86f, borde);
                }
                borde.setStrokeWidth(4);
                break;
            }
            default:
                break;
        }
    }
}
