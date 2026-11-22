package com.cesba.godzipet.vista;

import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

/**
 * Efectos de GPU con un único shader AGSL (Android 13+, API 33) aplicado a toda la vista con
 * {@link RenderEffect}. En versiones anteriores {@link #aplicar} no hace nada.
 *
 * El shader combina: calor (distorsión), onda de choque con aberración cromática, ondas de agua,
 * rayos de luz volumétricos (muestreo radial, 12 muestras), bloom barato y viñeta.
 * Si todos los parámetros son neutros se retira el efecto para no gastar GPU.
 */
public final class Efectos3D {
    private static final String TAG = "Efectos3D";
    private static final float EPS = 0.001f;

    /** Parámetros del cuadro actual (se reutiliza el mismo objeto). */
    public static final class Params {
        /** Distorsión por calor 0..1 (Burning / fusión). */
        public float calor;
        /** Onda de choque: centro en px y progreso 0..1 (o -1 si no hay). */
        public float choqueX, choqueY, choqueProgreso = -1;
        /** Onda en el agua: centro en px y progreso 0..1 (o -1). */
        public float ondaX, ondaY, ondaProgreso = -1;
        /** Rayos de luz volumétricos 0..1 desde (solX, solY) px. */
        public float rayos, solX, solY;
        /** Resplandor (bloom) 0..1 de las zonas brillantes. */
        public float bloom;
        /** Viñeta cinematográfica 0..1. */
        public float vineta;
    }

    public static boolean soportado() {
        return android.os.Build.VERSION.SDK_INT >= 33;
    }

    /** Implementación API 33; aislada en una clase interna para no cargarla en versiones viejas. */
    private Gpu gpu;
    private boolean fallo;
    private boolean activo;

    /** Aplica o actualiza el efecto combinado sobre la vista (llamar una vez por cuadro). */
    public void aplicar(View v, float t, Params p) {
        if (!soportado() || fallo || v == null || p == null) return;
        if (esNeutro(p) || v.getWidth() <= 0 || v.getHeight() <= 0) {
            quitar(v);
            return;
        }
        try {
            if (gpu == null) gpu = new Gpu();
            gpu.aplicar(v, t, p);
            activo = true;
        } catch (Throwable e) {
            // Error de compilación AGSL u otro fallo: se desactivan los efectos para siempre.
            Log.e(TAG, "Efectos AGSL desactivados", e);
            fallo = true;
            gpu = null;
            activo = false;
            v.setRenderEffect(null);
        }
    }

    /** Quita cualquier efecto de la vista. */
    public void quitar(View v) {
        if (!activo || v == null) return;
        activo = false;
        if (soportado()) v.setRenderEffect(null);
    }

    private static boolean esNeutro(Params p) {
        return p.calor < EPS && !(p.choqueProgreso >= 0 && p.choqueProgreso <= 1)
                && !(p.ondaProgreso >= 0 && p.ondaProgreso <= 1)
                && p.rayos < EPS && p.bloom < EPS && p.vineta < EPS;
    }

    private static boolean activa(float prog) {
        return prog >= 0f && prog <= 1f;
    }

    /** Shader + RenderEffect (solo se carga en API 33+). */
    @RequiresApi(33)
    private static final class Gpu {
        private final RuntimeShader shader = new RuntimeShader(AGSL);

        void aplicar(View v, float t, Params p) {
            // El RuntimeShader se crea una sola vez; solo se actualizan los uniforms.
            shader.setFloatUniform("resolucion", v.getWidth(), v.getHeight());
            shader.setFloatUniform("tiempo", t);
            shader.setFloatUniform("calor", p.calor);
            shader.setFloatUniform("choque", p.choqueX, p.choqueY,
                    activa(p.choqueProgreso) ? p.choqueProgreso : -1f);
            shader.setFloatUniform("onda", p.ondaX, p.ondaY,
                    activa(p.ondaProgreso) ? p.ondaProgreso : -1f);
            shader.setFloatUniform("sol", p.solX, p.solY, p.rayos);
            shader.setFloatUniform("bloom", p.bloom);
            shader.setFloatUniform("vineta", p.vineta);
            // Al cambiar los uniforms el RuntimeShader genera un shader nativo nuevo que un
            // RenderEffect ya creado no ve, así que el RenderEffect (objeto liviano) se vuelve
            // a crear en cada cuadro.
            v.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "imagen"));
        }
    }

    /** Código AGSL (Java 11: sin bloques de texto, se concatena). */
    private static final String AGSL = ""
            + "uniform shader imagen;\n"
            + "uniform float2 resolucion;\n"
            + "uniform float tiempo;\n"
            + "uniform float calor;\n"
            + "uniform float3 choque;\n"   // x, y, progreso (<0 = inactivo)
            + "uniform float3 onda;\n"      // x, y, progreso (<0 = inactivo)
            + "uniform float3 sol;\n"       // x, y, intensidad de rayos
            + "uniform float bloom;\n"
            + "uniform float vineta;\n"
            + "\n"
            + "float2 limitar(float2 p) {\n"
            + "  return clamp(p, float2(0.5), resolucion - float2(0.5));\n"
            + "}\n"
            + "\n"
            + "float3 muestra(float2 p) {\n"
            + "  return float3(imagen.eval(limitar(p)).rgb);\n"
            + "}\n"
            + "\n"
            + "float brillo(float3 c) {\n"
            + "  return dot(c, float3(0.299, 0.587, 0.114));\n"
            + "}\n"
            + "\n"
            + "half4 main(float2 fc) {\n"
            + "  float s = resolucion.y / 1000.0;\n"
            + "  float2 p = fc;\n"
            + "  float2 dirCa = float2(0.0);\n"
            + "  float ca = 0.0;\n"
            // Calor: ondulación vertical-horizontal de baja amplitud.
            + "  if (calor > 0.001) {\n"
            + "    float k = calor * 5.0 * s;\n"
            + "    p.x += (sin(fc.y * 0.045 + tiempo * 5.0) + 0.5 * sin(fc.y * 0.11 - tiempo * 3.1)) * k;\n"
            + "    p.y += sin(fc.x * 0.03 + tiempo * 4.0) * k * 0.5;\n"
            + "  }\n"
            // Onda de choque: anillo que se expande y se desvanece; empuja los píxeles.
            + "  if (choque.z >= 0.0) {\n"
            + "    float2 d = fc - choque.xy;\n"
            + "    float dist = length(d);\n"
            + "    float2 dir = d / max(dist, 1.0);\n"
            + "    float radio = choque.z * length(resolucion) * 0.6;\n"
            + "    float x = (dist - radio) / (60.0 * s);\n"
            + "    float amp = exp(-x * x) * (1.0 - choque.z) * 28.0 * s;\n"
            + "    p -= dir * amp;\n"
            + "    dirCa = dir;\n"
            + "    ca = amp * 0.35;\n"
            + "  }\n"
            // Ondas de agua: varios anillos concéntricos con envolvente.
            + "  if (onda.z >= 0.0) {\n"
            + "    float2 d = fc - onda.xy;\n"
            + "    float dist = length(d);\n"
            + "    float2 dir = d / max(dist, 1.0);\n"
            + "    float radio = onda.z * resolucion.x * 0.5;\n"
            + "    float x = (dist - radio) / (110.0 * s);\n"
            + "    float env = exp(-x * x) * (1.0 - onda.z);\n"
            + "    p += dir * sin((dist - radio) * 0.12 / s) * env * 6.0 * s;\n"
            + "  }\n"
            + "  float3 col;\n"
            + "  if (ca > 0.01) {\n"
            + "    col = float3(muestra(p + dirCa * ca).r, muestra(p).g, muestra(p - dirCa * ca).b);\n"
            + "  } else {\n"
            + "    col = muestra(p);\n"
            + "  }\n"
            // Rayos de luz: 12 muestras radiales hacia el sol, acumulando zonas brillantes.
            + "  if (sol.z > 0.001) {\n"
            + "    float2 haciaSol = sol.xy - fc;\n"
            + "    float acum = 0.0;\n"
            + "    float peso = 1.0;\n"
            + "    for (int i = 0; i < 12; i++) {\n"
            + "      float f = (float(i) + 1.0) / 12.0 * 0.7;\n"
            + "      float b = brillo(muestra(fc + haciaSol * f));\n"
            + "      acum += max(b - 0.55, 0.0) * peso;\n"
            + "      peso *= 0.88;\n"
            + "    }\n"
            + "    float cerca = 1.0 - smoothstep(0.0, length(resolucion), length(haciaSol));\n"
            + "    col += float3(1.0, 0.86, 0.6) * acum * 0.16 * sol.z * (0.35 + 0.65 * cerca);\n"
            + "  }\n"
            // Bloom barato: 4 muestras diagonales de las zonas brillantes.
            + "  if (bloom > 0.001) {\n"
            + "    float r = 9.0 * s;\n"
            + "    float3 suma = float3(0.0);\n"
            + "    float3 a = muestra(p + float2(r, r));\n"
            + "    float3 b = muestra(p + float2(-r, r));\n"
            + "    float3 c = muestra(p + float2(r, -r));\n"
            + "    float3 d = muestra(p + float2(-r, -r));\n"
            + "    suma += a * max(brillo(a) - 0.6, 0.0);\n"
            + "    suma += b * max(brillo(b) - 0.6, 0.0);\n"
            + "    suma += c * max(brillo(c) - 0.6, 0.0);\n"
            + "    suma += d * max(brillo(d) - 0.6, 0.0);\n"
            + "    col = col * (1.0 + 0.06 * bloom) + suma * 0.6 * bloom;\n"
            + "  }\n"
            // Viñeta: oscurece hacia las esquinas.
            + "  if (vineta > 0.001) {\n"
            + "    float2 uv = fc / resolucion - 0.5;\n"
            + "    float v = length(uv) * 1.4142;\n"
            + "    col *= 1.0 - vineta * smoothstep(0.35, 1.0, v) * 0.7;\n"
            + "  }\n"
            + "  return half4(half3(clamp(col, 0.0, 1.0)), 1.0);\n"
            + "}\n";
}
