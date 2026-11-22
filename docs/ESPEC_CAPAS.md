# Especificación de capas parallax (Blender → Android)

Ruta: `app/src/main/assets/escenas/<ESCENARIO>/<momento>_<capa>.webp`
- `<ESCENARIO>`: nombre del enum `Escenario` (ISLA_ODO, BAHIA_TOKIO, GINZA, MONTE_FUJI, TORRE_TOKIO, OSAKA, KAMAKURA, ISLA_MONSTRUOS).
- `<momento>`: `dia` o `noche` (el atardecer se logra mezclando + tinte en código).
- `<capa>`: 0 = cielo (opaca), 1 = lejos (montañas/horizonte, transparente arriba),
  2 = medio (ciudad/monumento), 3 = cerca (agua/costa/suelo; debe cubrir TODO desde el 74% de la
  altura hacia abajo — ahí camina el kaiju, pies al 93%), 4 = primer plano (casi todo transparente:
  ramas, faroles, cables, pétalos SOLO en los bordes laterales e inferior; nunca en el centro 50%).
- Tamaño de cada imagen: **1536 × 1024 px** (más ancha que la vista para el parallax). La vista
  muestra un recorte centrado de proporción ~1.1:1 y desplaza cada capa: 0 → 2%, 1 → 5%, 2 → 9%,
  3 → 14%, 4 → 22% del ancho.
- Horizonte aproximado al 50% de la altura. WebP con alfa, calidad ~80, < 180 KB por archivo.
- Estilo: diorama 3D estilizado (no fotorrealista), luz suave, niebla atmosférica que aclara/azula
  lo lejano, desenfoque de profundidad ligero en capas 0-1, colores coherentes con el juego.
- Sin personajes ni logotipos con derechos de autor (los monumentos reales sí: Fuji, Torre de Tokio…).
