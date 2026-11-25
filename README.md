# Godzi-Pet 90 · ゴジラ

Mascota virtual (tamagotchi) para Android inspirada en el Godzilla japonés de Toho.
Proyecto final **RetroPet 90** · Programación Móvil I · CESBA Querétaro.

El jugador incuba un huevo y cuida al kaiju mientras crece de Baby Godzilla a Little Godzilla,
Godzilla Junior y una de cuatro formas finales (Showa, Heisei, Burning o Shin) que dependen de
cómo fue cuidado. Funciona sin internet: la cuenta y el progreso se guardan en el teléfono.

## Características

- 6 necesidades con deterioro en tiempo real (también con la app cerrada).
- **Radiación**: mecánica exclusiva; el núcleo debe mantenerse entre 40 y 70 o se funde.
- 4 minijuegos, tienda, inventario, guarida personalizable, misiones diarias y logros.
- Evolución por decisiones, alertas locales, widget, cámara AR y póster para compartir.
- Fondos 2.5D renderizados en Blender (parallax de 5 capas, día y noche).
- Modo evaluador para el docente (código `GOJIRA1954`).

## Tecnologías

Java 11 · Android SDK 36 (mínimo 24) · Gradle 9.6 / AGP 9.4 · Room 2.6.1 · CameraX 1.5.3 ·
AppCompat · Material · JUnit 4.

## Compilar

```bash
git clone https://github.com/NX-Systems-svg/GodziPet90.git
cd GodziPet90
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 30 pruebas unitarias
```

O abrir la carpeta en Android Studio y pulsar **Run**.

## Estructura

```
app/src/main/java/com/cesba/godzipet/
  modelo/   reglas del juego (Java puro, probado con JUnit)
  datos/    Room, SharedPreferences, seguridad de contraseñas
  juego/    reloj del juego, alertas, sensores, audio, widget
  ui/       Activities y Fragments
  vista/    dibujo personalizado (Canvas, 2.5D, partículas)
app/src/test/   pruebas unitarias
app/schemas/    esquemas de la base de datos (v1 y v2)
docs/           documentación, diagramas UML y evidencias de pruebas
tools/          scripts de Blender, adb y generación de documentos
```

## Documentación

- `docs/tarea2/` · diagramas UML (`uml/`), capturas de Android Studio y de las pruebas.
- `tools/documentacion/` · generadores de los documentos (Word/PDF) y de los diagramas.
