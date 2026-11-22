package com.cesba.godzipet.vista;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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

    private static final String[] ETIQUETAS = {"+ Ventanal", "+ Izquierda", "+ Derecha", "+ Suelo"};
    private static final int MADERA = 0xFF5A3A22, MADERA_OSCURA = 0xFF3A2416;
    private final Paint marco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private Shader shPared, shDia, shOdo, shMons, shLluvia;
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
        marco.setStyle(Paint.Style.STROKE);
        marco.setPathEffect(new DashPathEffect(new float[]{14, 10}, 0));
        etiqueta.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
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
        // degradados cacheados (se rehacen solo al cambiar el tamaño)
        RectF v = zonas.get(Slot.VENTANA);
        shPared = new LinearGradient(0, 0, 0, h * 0.7f, 0xFFE3D3AB, 0xFFC9B68B, Shader.TileMode.CLAMP);
        shDia = new LinearGradient(0, v.top, 0, v.bottom, 0xFF5FB0E8, 0xFFE9F4FF, Shader.TileMode.CLAMP);
        shOdo = new LinearGradient(0, v.top, 0, v.bottom, 0xFF050A24, 0xFF1B2F5E, Shader.TileMode.CLAMP);
        shMons = new LinearGradient(0, v.top, 0, v.bottom, 0xFF4B2A5E, 0xFFFF9A5A, Shader.TileMode.CLAMP);
        shLluvia = new LinearGradient(0, v.top, 0, v.bottom, 0xFF0A1030, 0xFF26345E, Shader.TileMode.CLAMP);
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
        if (w == 0 || zonas.isEmpty() || shPared == null) return;
        float t = (System.currentTimeMillis() - inicio) / 1000f;
        dibujarSala(c, w, h, t);

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

    private void rect(Canvas c, float x0, float y0, float x1, float y1, int color) {
        p.setColor(color);
        c.drawRect(x0, y0, x1, y1, p);
    }

    private void trazo(Canvas c, float x0, float y0, float x1, float y1, float ancho, int color) {
        borde.setStrokeWidth(ancho);
        borde.setColor(color);
        c.drawLine(x0, y0, x1, y1, borde);
    }

    private void restaurarBorde() {
        borde.setStrokeWidth(4);
        borde.setColor(0xFF0B0F14);
    }

    /** Sala washitsu: pared de yeso, vigas, tokonoma, fusuma y piso de tatami. */
    private void dibujarSala(Canvas c, float w, float h, float t) {
        p.setShader(shPared);
        c.drawRect(0, 0, w, h * 0.7f, p);
        p.setShader(null);
        // fusuma (puerta corrediza) a la izquierda, con montañas pintadas
        rect(c, w * 0.045f, h * 0.1f, w * 0.215f, h * 0.68f, 0xFFEFE3C2);
        p.setColor(0x33406080);
        c.drawCircle(w * 0.09f, h * 0.62f, h * 0.1f, p);
        c.drawCircle(w * 0.17f, h * 0.64f, h * 0.13f, p);
        trazo(c, w * 0.13f, h * 0.1f, w * 0.13f, h * 0.68f, 3, MADERA);
        borde.setStyle(Paint.Style.STROKE);
        rf.set(w * 0.045f, h * 0.1f, w * 0.215f, h * 0.68f);
        borde.setColor(MADERA);
        borde.setStrokeWidth(5);
        c.drawRect(rf, borde);
        restaurarBorde();
        // tokonoma (nicho) a la derecha, con rollo colgante
        float ax0 = w * 0.775f, ax1 = w * 0.965f;
        rect(c, ax0, h * 0.15f, ax1, h * 0.7f, 0xFFB9A57A);
        rect(c, ax0, h * 0.15f, ax0 + w * 0.012f, h * 0.7f, 0x44000000);
        rect(c, ax0, h * 0.15f, ax1, h * 0.17f, 0x33000000);
        rect(c, ax0 - 4, h * 0.12f, ax1 + 4, h * 0.15f, MADERA_OSCURA);
        rect(c, ax0, h * 0.665f, ax1, h * 0.7f, 0xFF2C1A12);
        rect(c, ax0, h * 0.665f, ax1, h * 0.671f, 0x55FFFFFF);
        float sx = (ax0 + ax1) / 2;
        rect(c, sx - w * 0.04f, h * 0.17f, sx + w * 0.04f, h * 0.285f, 0xFFF1E6C8);
        rect(c, sx - w * 0.045f, h * 0.165f, sx + w * 0.045f, h * 0.175f, 0xFF7A1F1F);
        rect(c, sx - w * 0.045f, h * 0.28f, sx + w * 0.045f, h * 0.29f, 0xFF7A1F1F);
        rf.set(sx - w * 0.022f, h * 0.2f, sx + w * 0.022f, h * 0.2f + w * 0.044f);
        borde.setStrokeWidth(3);
        borde.setColor(0xFF111111);
        c.drawArc(rf, -60, 300, false, borde);
        restaurarBorde();
        // vigas y postes
        rect(c, 0, 0, w, h * 0.05f, MADERA_OSCURA);
        rect(c, 0, h * 0.05f, w, h * 0.056f, 0x44FFFFFF);
        rect(c, 0, h * 0.05f, w * 0.035f, h * 0.7f, MADERA);
        rect(c, w * 0.965f, h * 0.05f, w, h * 0.7f, MADERA);
        rect(c, 0, h * 0.665f, w, h * 0.7f, MADERA_OSCURA);
        // piso de tatami
        float y0 = h * 0.7f, mw = w * 0.36f, mh = (h - y0) / 2f;
        borde.setStrokeWidth(3);
        borde.setColor(0xFF2B3A29);
        for (int fila = 0; fila < 2; fila++) {
            float off = fila * mw * 0.5f;
            for (float x = -off; x < w; x += mw) {
                rect(c, x, y0 + fila * mh, x + mw, y0 + (fila + 1) * mh, ((int) (x / mw) + fila) % 2 == 0 ? 0xFFC7BD83 : 0xFFBFB57A);
                c.drawRect(x, y0 + fila * mh, x + mw, y0 + (fila + 1) * mh, borde);
            }
        }
        restaurarBorde();
        rect(c, 0, y0, w, y0 + h * 0.012f, 0x44000000);
    }

    private void marcoVacio(Canvas c, Slot s, RectF r, boolean activo) {
        marco.setStrokeWidth(activo ? 6 : 3);
        marco.setColor(activo ? 0xFFFFD23F : 0x99FFFFFF);
        c.drawRoundRect(r, 14, 14, marco);
        if (equipadas.get(s) == null) {
            etiqueta.setColor(0xFFFFFFFF);
            etiqueta.setShadowLayer(3, 0, 1, 0xFF000000);
            float y = s == Slot.SUELO ? r.centerY() + etiqueta.getTextSize() / 3 : r.top + etiqueta.getTextSize() * 1.4f;
            c.drawText(ETIQUETAS[s.ordinal()], r.centerX(), y, etiqueta);
            etiqueta.clearShadowLayer();
        }
    }

    // ------------------------------------------------------------ decoraciones

    private void dibujarVentana(Canvas c, RectF r, String id, float t) {
        float w = r.width(), h = r.height();
        c.save();
        c.clipRect(r);
        if ("v_fuji".equals(id)) {
            p.setShader(shDia);
            c.drawRect(r, p);
            p.setShader(null);
            nubeVentana(c, r, t);
            // Monte Fuji con cumbre nevada
            float cx = r.centerX(), base = r.bottom - h * 0.16f, pk = h * 0.62f;
            path.reset();
            path.moveTo(cx - w * 0.03f, base - pk);
            path.lineTo(cx - w * 0.42f, base);
            path.lineTo(cx + w * 0.42f, base);
            path.lineTo(cx + w * 0.03f, base - pk);
            path.close();
            p.setColor(0xFF5C7699);
            c.drawPath(path, p);
            c.save();
            c.clipPath(path);
            path.reset();
            path.moveTo(cx - w * 0.3f, base - pk - 2);
            path.lineTo(cx + w * 0.3f, base - pk - 2);
            path.lineTo(cx + w * 0.2f, base - pk * 0.62f);
            path.lineTo(cx + w * 0.1f, base - pk * 0.74f);
            path.lineTo(cx, base - pk * 0.6f);
            path.lineTo(cx - w * 0.1f, base - pk * 0.75f);
            path.lineTo(cx - w * 0.2f, base - pk * 0.62f);
            path.close();
            p.setColor(0xFFF5F1E6);
            c.drawPath(path, p);
            c.restore();
            rect(c, r.left, base, r.right, r.bottom, 0xFF4F8A4A);
            p.setColor(0xFFF0A5BD);
            for (int i = 0; i < 4; i++) c.drawCircle(r.left + w * (0.1f + i * 0.09f), base + h * 0.02f, h * 0.09f, p);
            p.setColor(0xCCF8B8CC);
            for (int i = 0; i < 6; i++) {
                float f = (t * 0.12f + i * 0.17f) % 1f;
                c.drawCircle(r.left + ((i * 0.23f + f * 0.2f) % 1f) * w, r.top + f * h, 2.5f, p);
            }
        } else if ("v_odo".equals(id)) {
            p.setShader(shOdo);
            c.drawRect(r, p);
            p.setShader(null);
            float mx = r.left + w * 0.7f, my = r.top + h * 0.28f, mr = h * 0.12f;
            p.setColor(0x22F4F1D0);
            c.drawCircle(mx, my, mr * 1.8f, p);
            p.setColor(0xFFF4F1D0);
            c.drawCircle(mx, my, mr, p);
            float mar = r.top + h * 0.62f;
            rect(c, r.left, mar, r.right, r.bottom, 0xFF0F2347);
            for (int i = 0; i < 5; i++) {
                float ancho = w * (0.14f - i * 0.02f) * (0.7f + 0.3f * (float) Math.sin(t * 2 + i));
                trazo(c, mx - ancho, mar + h * (0.05f + i * 0.07f), mx + ancho, mar + h * (0.05f + i * 0.07f), 2, 0x88F4F1D0);
            }
            restaurarBorde();
            path.reset();
            path.moveTo(r.left - 4, mar + 4);
            path.quadTo(r.left + w * 0.2f, r.top + h * 0.35f, r.left + w * 0.5f, mar + 4);
            path.close();
            p.setColor(0xFF0A1226);
            c.drawPath(path, p);
            for (int i = 0; i < 3; i++) {
                float hx = r.left + w * (0.12f + i * 0.1f), hy = mar - h * (0.06f + (i == 1 ? 0.05f : 0f));
                rect(c, hx, hy, hx + w * 0.07f, mar, 0xFF0A1226);
                tri(c, hx - 3, hy, hx + w * 0.07f + 3, hy, hx + w * 0.035f, hy - h * 0.06f, 0xFF0A1226);
                rect(c, hx + w * 0.025f, hy + h * 0.02f, hx + w * 0.045f, hy + h * 0.05f, 0xFFFFD86B);
            }
        } else if ("v_monstruos".equals(id)) {
            p.setShader(shMons);
            c.drawRect(r, p);
            p.setShader(null);
            float cx = r.left + w * 0.62f, top = r.top + h * 0.3f, base = r.bottom - h * 0.2f;
            tri(c, cx - w * 0.38f, base, cx + w * 0.38f, base, cx, top, 0xFF4A2F33);
            rect(c, cx - w * 0.07f, top - 2, cx + w * 0.07f, top + h * 0.03f, 0xFFFF7A2B);
            for (int k = 0; k < 4; k++) {
                float f = (t * 0.15f + k * 0.25f) % 1f;
                p.setColor(0xFF8A7A80);
                p.setAlpha((int) (150 * (1 - f)));
                c.drawCircle(cx + f * w * 0.1f, top - f * h * 0.3f, h * (0.04f + 0.08f * f), p);
            }
            rect(c, r.left, base, r.right, r.bottom, 0xFF16667A);
            palmaVentana(c, r.left + w * 0.12f, base, h * 0.55f, 1f);
            palmaVentana(c, r.right - w * 0.1f, base, h * 0.45f, -1f);
        } else {
            // por defecto: noche de lluvia con pinos
            p.setShader(shLluvia);
            c.drawRect(r, p);
            p.setShader(null);
            p.setColor(0x33F4F1D0);
            c.drawCircle(r.left + w * 0.75f, r.top + h * 0.25f, h * 0.1f, p);
            for (int i = 0; i < 4; i++) {
                float px = r.left + w * (0.12f + i * 0.26f), by = r.bottom;
                float s = h * (0.5f + (i % 2) * 0.2f);
                rect(c, px - 3, by - s * 0.3f, px + 3, by, 0xFF070B18);
                for (int k = 0; k < 3; k++) {
                    float yb = by - s * (0.2f + 0.2f * k), hw = s * (0.3f - 0.07f * k);
                    tri(c, px - hw, yb, px + hw, yb, px, yb - s * 0.35f, 0xFF070B18);
                }
            }
            for (int i = 0; i < 16; i++) {
                float x = r.left + (i * 37 % 100) / 100f * w;
                float y = r.top + ((t * 200 + i * 53) % h);
                trazo(c, x, y, x - 4, y + 12, 2, 0x66BFD4FF);
            }
            restaurarBorde();
        }
        // papel shoji translúcido sobre la vista
        p.setColor(0x24F6EACB);
        c.drawRect(r, p);
        c.restore();
        // enrejado de madera (kumiko)
        for (int i = 1; i < 4; i++) trazo(c, r.left + w * i / 4f, r.top, r.left + w * i / 4f, r.bottom, 4, MADERA);
        for (int i = 1; i < 5; i++) trazo(c, r.left, r.top + h * i / 5f, r.right, r.top + h * i / 5f, 3, MADERA);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(10);
        borde.setColor(MADERA_OSCURA);
        c.drawRect(r, borde);
        restaurarBorde();
    }

    private void tri(Canvas c, float x0, float y0, float x1, float y1, float x2, float y2, int color) {
        path.reset();
        path.moveTo(x0, y0);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        path.close();
        p.setColor(color);
        c.drawPath(path, p);
    }

    private void nubeVentana(Canvas c, RectF r, float t) {
        float x = r.left + ((t * 5f) % (r.width() + 60)) - 30, y = r.top + r.height() * 0.2f, rr = r.height() * 0.07f;
        p.setColor(0xDDFFFFFF);
        c.drawCircle(x, y, rr, p);
        c.drawCircle(x + rr, y - rr * 0.4f, rr * 1.2f, p);
        c.drawCircle(x + rr * 2.2f, y, rr * 0.9f, p);
    }

    private void palmaVentana(Canvas c, float x, float base, float s, float dir) {
        path.reset();
        path.moveTo(x, base);
        path.quadTo(x + dir * s * 0.25f, base - s * 0.5f, x + dir * s * 0.1f, base - s);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(5);
        borde.setColor(0xFF1E1A18);
        c.drawPath(path, borde);
        for (int k = 0; k < 5; k++) {
            float a = (float) Math.toRadians(200 + k * 35);
            trazo(c, x + dir * s * 0.1f, base - s, x + dir * s * 0.1f + (float) Math.cos(a) * s * 0.4f,
                    base - s + (float) Math.sin(a) * s * 0.25f + s * 0.12f, 4, 0xFF1E1A18);
        }
        restaurarBorde();
    }

    private void dibujarSuelo(Canvas c, RectF r, String id, float t) {
        if (id == null) return;
        float cx = r.centerX(), cy = r.centerY();
        switch (id) {
            case "s_tatami": {
                rect(c, r.left, r.top, r.right, r.bottom, 0xFFD8CF8F);
                rect(c, r.left, r.top, r.right, r.top + r.height() * 0.12f, 0xFF2B3A29);
                rect(c, r.left, r.bottom - r.height() * 0.12f, r.right, r.bottom, 0xFF2B3A29);
                for (float x = r.left + 8; x < r.right; x += 9) trazo(c, x, r.top + r.height() * 0.14f, x, r.bottom - r.height() * 0.14f, 1, 0x22000000);
                restaurarBorde();
                break;
            }
            case "s_ola": {
                p.setColor(0xFF1F3A5F);
                c.drawRoundRect(r, 10, 10, p);
                c.save();
                c.clipRect(r);
                borde.setStrokeWidth(2);
                borde.setColor(0xFFBFD7EA);
                float rad = r.height() * 0.32f, off = (float) Math.sin(t) * 3;
                for (int fila = 0; fila < 3; fila++) {
                    for (float x = r.left - rad + (fila % 2) * rad + off; x < r.right + rad; x += rad * 2) {
                        for (int k = 1; k <= 3; k++) {
                            rf.set(x - rad * k / 3f, r.top + fila * rad * 0.7f + rad * (1 - k / 3f), x + rad * k / 3f, r.top + fila * rad * 0.7f + rad * (1 + k / 3f));
                            c.drawArc(rf, 180, 180, false, borde);
                        }
                    }
                }
                p.setColor(ESPUMA_ALFOMBRA);
                for (int i = 0; i < 5; i++) c.drawCircle(r.left + r.width() * (0.12f + i * 0.19f), r.top + r.height() * 0.3f, 3, p);
                c.restore();
                restaurarBorde();
                borde.setColor(0xFFF2E6C9);
                borde.setStrokeWidth(3);
                c.drawRoundRect(r, 10, 10, borde);
                restaurarBorde();
                break;
            }
            case "s_koi": {
                p.setColor(0xFF1D4E6B);
                c.drawOval(r, p);
                borde.setStrokeWidth(6);
                borde.setColor(0xFF8E8A82);
                c.drawOval(r, borde);
                restaurarBorde();
                p.setColor(0x33FFFFFF);
                float pulso = (t * 0.5f) % 1f;
                rf.set(cx - r.width() * 0.3f * pulso, cy - r.height() * 0.3f * pulso, cx + r.width() * 0.3f * pulso, cy + r.height() * 0.3f * pulso);
                borde.setStrokeWidth(2);
                borde.setColor(0x66FFFFFF);
                c.drawOval(rf, borde);
                restaurarBorde();
                p.setColor(0xFF3F8F4A);
                c.drawCircle(r.left + r.width() * 0.25f, cy, r.height() * 0.16f, p);
                p.setColor(0xFFF4A6BE);
                c.drawCircle(r.left + r.width() * 0.25f, cy, r.height() * 0.05f, p);
                for (int k = 0; k < 2; k++) {
                    float a = t * 0.9f + k * (float) Math.PI;
                    float kx = cx + (float) Math.cos(a) * r.width() * 0.32f, ky = cy + (float) Math.sin(a) * r.height() * 0.28f;
                    c.save();
                    c.translate(kx, ky);
                    c.rotate((float) Math.toDegrees(a) + 90);
                    p.setColor(k == 0 ? 0xFFFF6A2B : 0xFFF2F2F2);
                    rf.set(-r.height() * 0.2f, -r.height() * 0.08f, r.height() * 0.2f, r.height() * 0.08f);
                    c.drawOval(rf, p);
                    tri(c, -r.height() * 0.18f, 0, -r.height() * 0.32f, -r.height() * 0.09f, -r.height() * 0.32f, r.height() * 0.09f, p.getColor());
                    p.setColor(k == 0 ? 0xFFF2F2F2 : 0xFFE03A2E);
                    c.drawCircle(r.height() * 0.02f, 0, r.height() * 0.05f, p);
                    c.restore();
                }
                break;
            }
            default:
                break;
        }
    }

    private static final int ESPUMA_ALFOMBRA = 0xFFF5F1E6;

    private void dibujarObjeto(Canvas c, RectF r, String id, float t) {
        if (id == null) return;
        float cx = r.centerX(), base = r.bottom, w = r.width(), h = r.height();
        switch (id) {
            case "i_toro": {
                float f = 0.7f + 0.3f * (float) Math.sin(t * 9) * (float) Math.sin(t * 5.3f);
                rect(c, cx - w * 0.2f, base - h * 0.07f, cx + w * 0.2f, base, 0xFF7E7A72);
                rect(c, cx - w * 0.07f, base - h * 0.34f, cx + w * 0.07f, base - h * 0.07f, 0xFF9A968D);
                rect(c, cx - w * 0.17f, base - h * 0.4f, cx + w * 0.17f, base - h * 0.34f, 0xFF7E7A72);
                rect(c, cx - w * 0.13f, base - h * 0.62f, cx + w * 0.13f, base - h * 0.4f, 0xFF8E8A82);
                p.setColor(0xFFFFB347);
                p.setAlpha((int) (40 * f));
                c.drawCircle(cx, base - h * 0.51f, w * 0.42f, p);
                p.setColor(0xFFFFB347);
                p.setAlpha((int) (80 * f));
                c.drawCircle(cx, base - h * 0.51f, w * 0.26f, p);
                rect(c, cx - w * 0.07f, base - h * 0.58f, cx + w * 0.07f, base - h * 0.44f, f > 0.8f ? 0xFFFFE08A : 0xFFFFC857);
                tri(c, cx - w * 0.26f, base - h * 0.62f, cx + w * 0.26f, base - h * 0.62f, cx, base - h * 0.78f, 0xFF6F6B64);
                p.setColor(0xFF9A968D);
                c.drawCircle(cx, base - h * 0.8f, w * 0.04f, p);
                break;
            }
            case "i_torre": {
                rect(c, cx - w * 0.22f, base - h * 0.05f, cx + w * 0.22f, base, 0xFF3A2416);
                escena.japon.m = 0;
                escena.japon.t = t;
                escena.japon.torre(c, cx, base - h * 0.05f, r.top + h * 0.04f);
                break;
            }
            case "d_daruma": {
                float by = base - h * 0.3f;
                p.setColor(0xFFC8102E);
                rf.set(cx - w * 0.3f, by - h * 0.28f, cx + w * 0.3f, base);
                c.drawOval(rf, p);
                p.setColor(0xFFF2E6C9);
                rf.set(cx - w * 0.19f, by - h * 0.25f, cx + w * 0.19f, by - h * 0.02f);
                c.drawOval(rf, p);
                trazo(c, cx - w * 0.14f, by - h * 0.2f, cx - w * 0.03f, by - h * 0.17f, 3, 0xFF111111);
                trazo(c, cx + w * 0.14f, by - h * 0.2f, cx + w * 0.03f, by - h * 0.17f, 3, 0xFF111111);
                p.setColor(0xFFFFFFFF);
                c.drawCircle(cx - w * 0.08f, by - h * 0.12f, w * 0.05f, p);
                c.drawCircle(cx + w * 0.08f, by - h * 0.12f, w * 0.05f, p);
                p.setColor(0xFF111111);
                c.drawCircle(cx + w * 0.08f, by - h * 0.12f, w * 0.025f, p);
                c.drawArc(cx - w * 0.07f, by - h * 0.1f, cx + w * 0.07f, by - h * 0.04f, 20, 140, false, borde);
                trazo(c, cx - w * 0.12f, by + h * 0.1f, cx + w * 0.12f, by + h * 0.1f, 4, 0xFFD4A017);
                trazo(c, cx - w * 0.1f, by + h * 0.18f, cx + w * 0.1f, by + h * 0.18f, 4, 0xFFD4A017);
                trazo(c, cx, by + h * 0.06f, cx, by + h * 0.22f, 4, 0xFFD4A017);
                restaurarBorde();
                break;
            }
            case "d_bonsai": {
                float sw = (float) Math.sin(t * 1.2f) * 2;
                path.reset();
                path.moveTo(cx, base - h * 0.14f);
                path.quadTo(cx - w * 0.15f, base - h * 0.32f, cx + w * 0.04f, base - h * 0.5f);
                borde.setStyle(Paint.Style.STROKE);
                borde.setStrokeWidth(8);
                borde.setColor(0xFF4A3426);
                c.drawPath(path, borde);
                restaurarBorde();
                for (int i = 0; i < 3; i++) {
                    float py = base - h * (0.52f + i * 0.11f), pw = w * (0.3f - i * 0.07f);
                    p.setColor(i == 1 ? 0xFF3F7A45 : 0xFF2F5E3A);
                    rf.set(cx - pw + sw * (i + 1) * 0.4f, py - h * 0.06f, cx + pw + sw * (i + 1) * 0.4f, py + h * 0.04f);
                    c.drawOval(rf, p);
                }
                path.reset();
                path.moveTo(cx - w * 0.19f, base - h * 0.14f);
                path.lineTo(cx + w * 0.19f, base - h * 0.14f);
                path.lineTo(cx + w * 0.14f, base);
                path.lineTo(cx - w * 0.14f, base);
                path.close();
                p.setColor(0xFF7A4A2E);
                c.drawPath(path, p);
                rect(c, cx - w * 0.2f, base - h * 0.16f, cx + w * 0.2f, base - h * 0.13f, 0xFF5A3720);
                break;
            }
            case "d_reloj": {
                rect(c, cx - w * 0.22f, base - h * 0.08f, cx + w * 0.22f, base, 0xFF8E8A82);
                rect(c, cx - w * 0.15f, base - h * 0.62f, cx + w * 0.15f, base - h * 0.08f, 0xFFD8CBB0);
                rect(c, cx - w * 0.18f, base - h * 0.64f, cx + w * 0.18f, base - h * 0.6f, 0xFF8B7B5E);
                escena.japon.m = 0;
                escena.japon.reloj(c, cx, base - h * 0.4f, w * 0.12f);
                rf.set(cx - w * 0.15f, base - h * 0.84f, cx + w * 0.15f, base - h * 0.44f);
                p.setColor(0xFF4A8F80);
                c.drawArc(rf, 180, 180, true, p);
                rect(c, cx - 1.5f, base - h * 0.92f, cx + 1.5f, base - h * 0.82f, 0xFF3A3A3A);
                p.setColor(0xFFD4A017);
                c.drawCircle(cx, base - h * 0.93f, 3.5f, p);
                break;
            }
            default:
                break;
        }
    }
}
