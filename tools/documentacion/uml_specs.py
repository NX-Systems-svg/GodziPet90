# Especificación de los 4 diagramas (posiciones en px). Se ejecuta dentro de uml.py.

# ---------------------------------------------------------------- 1. Núcleo del estado
PM = ['Partida', 'nueva', 'actualizar', 'avanzar', 'revisarFusion', 'fundirse', 'revisarAlertas', 'defenderCiudad',
      'viajar', 'descubrir', 'terremoto', 'rugidoEscuchado', 'acariciar', 'shie', 'ganar', 'sumarXp',
      'revisarEvolucion', 'evolucionarA', 'cargarAliento', 'gastarMonedas', 'tocarHuevo', 'puedeJugar',
      'iniciarMinijuego', 'registrarMinijuego', 'reclamarMision', 'consumirEventos', 'getEstado',
      'getPromedioBienestar', 'getPromedioRadiacion', 'getNivel', 'multLegado', 'fusionEnCurso']
PC = ['ALIENTO_MAX', 'MINUTOS_FUSION', 'MAX_MINUTOS_OFFLINE', 'personaje', 'necesidades', 'inventario', 'contadores',
      'decoraciones', 'misionesReclamadas', 'records', 'secretos', 'visitados', 'eventos', 'monedas', 'xp',
      'cargaAliento', 'ultimaActualizacion', 'alerta', 'evolucionPendiente', 'escenario', 'fusion', 'generacion']
c1 = [
    caja('Partida', 470, 60, PC, PM),
    caja('Personaje', 20, 60),
    caja('Necesidades', 20, 520),
    caja('Stat', 20, 930),
    caja('Inventario', 1080, 60),
    caja('Contadores', 1080, 300),
    caja('Record', 1080, 700),
    caja('AlertaKaiju', 1080, 830),
    caja('Evento', 1300, 700, metodos=[]),
    caja('Etapa', 280, 1150, metodos=['desde']),
    caja('RutaEvolucion', 470, 1150, campos=[], metodos=['multXp', 'desde']),
    caja('OrigenHuevo', 20, 1150, metodos=['desde']),
    caja('Escenario', 760, 1150, metodos=['desde']),
    caja('Secreto', 980, 1150, metodos=[]),
]
r1 = [['Partida', 'Personaje', 'comp', '1'], ['Partida', 'Necesidades', 'comp', '1'],
      ['Partida', 'Inventario', 'comp', '1'], ['Partida', 'Contadores', 'comp', '1'],
      ['Partida', 'Record', 'agr', '0..* por juego'], ['Partida', 'AlertaKaiju', 'asoc', '0..1'],
      ['Partida', 'Evento', 'comp', '0..*'], ['Partida', 'Escenario', 'asoc', '1 actual'],
      ['Partida', 'Secreto', 'asoc', '0..* descubiertos'],
      ['Personaje', 'Etapa', 'asoc', '1'], ['Personaje', 'RutaEvolucion', 'asoc', '1'],
      ['Personaje', 'OrigenHuevo', 'asoc', '1'], ['Necesidades', 'Stat', 'dep', 'indexa por']]
pagina('uml1_nucleo', 'Diagrama de clases 1 · Estado del juego (paquete modelo)', c1, r1, [['OrigenHuevo','Etapa','RutaEvolucion','Stat'],['Personaje','Necesidades'],['Partida'],['Inventario','Contadores','Record','Evento'],['AlertaKaiju','Escenario','Secreto','leyenda']])

# ---------------------------------------------------------------- 2. Reglas del juego
c2 = [
    caja('Partida', 0, 0, externo=True, este='«clase del diagrama 1» · estado completo de la partida', ancho=520),
    caja('Cuidados', 0, 0),
    caja('Resultado', 0, 0), caja('Reaccion', 0, 0, metodos=[]),
    caja('Tienda', 0, 0),
    caja('Catalogo', 0, 0, campos=['ONIGIRI', 'ATUN', 'ISOTOPO', 'CADMIO', 'DESTRUCTOR', 'ARTICULOS']),
    caja('Articulo', 0, 0, metodos=['consumible', 'prohibido', 'decoracion', 'esConsumible', 'resumenEfectos']),
    caja('TipoArticulo', 0, 0, metodos=[]),
    caja('Guarida', 0, 0), caja('Slot', 0, 0, metodos=[]),
    caja('GestorMisiones', 0, 0), caja('Mision', 0, 0),
    caja('Deterioro', 0, 0),
    caja('Necesidades', 0, 0, externo=True, este='«diagrama 1»'),
    caja('Evolucion', 0, 0),
    caja('RutaEvolucion', 0, 0, externo=True, este='«enumeration» diagrama 1'),
    caja('Etapa', 0, 0, externo=True, este='«enumeration» diagrama 1'),
    caja('Niveles', 0, 0),
    caja('ModoProfesor', 0, 0),
    caja('Minijuego', 0, 0),
]
r2 = [['Cuidados', 'Partida', 'dep', 'modifica'], ['Tienda', 'Partida', 'dep', 'cobra'],
      ['Guarida', 'Partida', 'dep', 'equipa'], ['ModoProfesor', 'Partida', 'dep', 'modifica'],
      ['Partida', 'GestorMisiones', 'dep', 'reclamarMision()'], ['Partida', 'Deterioro', 'dep', 'avanzar()'],
      ['Partida', 'Evolucion', 'dep', 'decidir()'], ['Partida', 'Niveles', 'dep', 'getNivel()'],
      ['Partida', 'Minijuego', 'dep', 'registrarMinijuego()'],
      ['Cuidados', 'Resultado', 'dep', 'devuelve'], ['Resultado', 'Reaccion', 'asoc', '1'],
      ['Tienda', 'Catalogo', 'dep', 'buscar()'], ['Catalogo', 'Articulo', 'comp', '22'],
      ['Articulo', 'TipoArticulo', 'asoc', '1'], ['Articulo', 'Slot', 'asoc', '0..1'],
      ['Guarida', 'Slot', 'dep', ''], ['GestorMisiones', 'Mision', 'comp', '11'],
      ['Deterioro', 'Necesidades', 'dep', 'aplicar()'], ['Evolucion', 'RutaEvolucion', 'dep', 'elige'],
      ['RutaEvolucion', 'Etapa', 'dep', '']]
pagina('uml2_interacciones', 'Diagrama de clases 2 · Interacciones, tienda, guarida y misiones (paquete modelo)',
       [x for x in c2 if not re.search(r'id="(Deterioro|Necesidades|Evolucion|RutaEvolucion|Etapa|Niveles|ModoProfesor|Minijuego)"', x)], r2,
       [['@top', 'Partida'], ['Cuidados', 'Resultado', 'Reaccion'], ['Tienda', 'Catalogo', 'Articulo', 'TipoArticulo'],
        ['Guarida', 'Slot'], ['GestorMisiones', 'Mision', 'leyenda']])
pagina('uml3_progreso', 'Diagrama de clases 3 · Tiempo, progreso y evolución (paquete modelo)',
       [x for x in c2 if re.search(r'id="(Partida|Deterioro|Necesidades|Evolucion|RutaEvolucion|Etapa|Niveles|ModoProfesor|Minijuego)"', x)], r2,
       [['@top', 'Partida'], ['Deterioro', 'Necesidades'], ['Evolucion', 'RutaEvolucion', 'Etapa'], ['Niveles'],
        ['ModoProfesor'], ['Minijuego', 'leyenda']])

# ---------------------------------------------------------------- 3. Persistencia
c3 = [
    caja('Juego', 20, 60),
    caja('Preferencias', 20, 560, metodos=['Preferencias', 'usuarioId', 'setUsuarioId', 'cerrarSesion', 'notificaciones',
                                           'modoDemo', 'profesor', 'setProfesor', 'velocidad', 'efectosGpu']),
    caja('Repositorio', 470, 60),
    caja('Seguridad', 470, 620),
    caja('BaseDatos', 1080, 60, este='«abstract» @Database v2'),
    caja('JuegoDao', 1080, 250, este='«abstract» @Dao'),
    caja('Instantanea', 520, 940),
    caja('UsuarioEntity', 1080, 820, este='@Entity'),
    caja('PartidaEntity', 1560, 60, campos='all', metodos=[], este='@Entity'),
    caja('InventarioEntity', 1080, 1030, metodos=[], este='@Entity'),
    caja('DecoracionEntity', 1330, 1030, metodos=[], este='@Entity'),
    caja('MisionEntity', 1080, 1200, metodos=[], este='@Entity'),
    caja('RecordEntity', 1330, 1200, metodos=[], este='@Entity'),
    caja('RoomDatabase', 1400, 60, externo=True, este='androidx.room'),
]
r3 = [['Juego', 'Repositorio', 'asoc', '1'], ['Juego', 'Preferencias', 'asoc', '1'],
      ['Repositorio', 'JuegoDao', 'asoc', '1'], ['Repositorio', 'Seguridad', 'dep', 'hash'],
      ['Repositorio', 'Instantanea', 'dep', 'crea'], ['BaseDatos', 'RoomDatabase', 'her', ''],
      ['BaseDatos', 'JuegoDao', 'dep', 'dao()'], ['JuegoDao', 'UsuarioEntity', 'dep', ''],
      ['Instantanea', 'PartidaEntity', 'comp', '1'], ['Instantanea', 'InventarioEntity', 'comp', '0..*'],
      ['Instantanea', 'DecoracionEntity', 'comp', '0..*'], ['Instantanea', 'MisionEntity', 'comp', '0..*'],
      ['Instantanea', 'RecordEntity', 'comp', '0..*'], ['Repositorio', 'BaseDatos', 'dep', 'get()']]
pagina('uml4_datos', 'Diagrama de clases 4 · Persistencia (paquete datos)', c3, r3, [['Juego','Preferencias','leyenda'],['Repositorio','Seguridad'],['BaseDatos','JuegoDao','Instantanea'],['RoomDatabase','UsuarioEntity','InventarioEntity','DecoracionEntity','MisionEntity','RecordEntity'],['PartidaEntity']])

# ---------------------------------------------------------------- 4. Interfaz y sistema
c4 = [
    caja('AppCompatActivity', 640, 50, externo=True, este='androidx.appcompat'),
    caja('Fragment', 1500, 50, externo=True, este='androidx.fragment'),
    caja('MainActivity', 20, 160, metodos=['onCreate', 'onResume', 'onPause', 'onStop', 'mostrarPestana',
                                          'despuesDeAccion', 'refrescarTodo', 'procesarEventos', 'defender']),
    caja('Refrescable', 1500, 330),
    caja('HogarFragment', 1100, 450, campos=['petView', 'barras', 'agitar', 'microfono', 'inclinacion'],
         metodos=['onCreateView', 'accion', 'elegirComida', 'ofrecer', 'caricia', 'terremoto', 'escucharRugido',
                  'ejecutar', 'reaccionar', 'refrescar']),
    caja('TiendaFragment', 1500, 900),
    caja('GuaridaFragment', 1880, 450),
    caja('MisionesFragment', 1880, 760),
    caja('JuegosFragment', 1880, 1000),
    caja('MinijuegoActivity', 620, 200, este='«abstract»'),
    caja('DefensaActivity', 560, 620, campos=['vista'], metodos=['minijuego', 'comenzarRonda', 'acierto', 'fin']),
    caja('AnillosActivity', 860, 760, campos=[], metodos=['minijuego', 'comenzarRonda']),
    caja('BatallaActivity', 860, 900, campos=[], metodos=['minijuego', 'comenzarRonda']),
    caja('MemoriaActivity', 560, 900, campos=[], metodos=['minijuego', 'comenzarRonda']),
    caja('PetView', 1100, 1200, campos=['kaiju', 'escena', 'particulas', 'parallax', 'efectos', 'render', 'oyente'],
         metodos=['ofrecerComida', 'setApariencia', 'setEscenario', 'reaccionar', 'mostrarEvolucion', 'onTouchEvent',
                  'onDraw', 'capturar']),
    caja('Juego', 20, 700, campos=['partida', 'repo', 'prefs'], metodos=['get', 'partida', 'tick', 'guardar']),
    caja('Alertas', 20, 980, campos=['CANAL'], metodos=['crearCanal', 'programar', 'cancelar', 'mostrar', 'minutosHastaCritico']),
    caja('AlarmaReceiver', 20, 1300),
    caja('WidgetKaiju', 420, 1200, metodos=['actualizar', 'onUpdate', 'construir']),
]
r4 = [['MainActivity', 'AppCompatActivity', 'her', ''], ['MinijuegoActivity', 'AppCompatActivity', 'her', ''],
      ['DefensaActivity', 'MinijuegoActivity', 'her', ''], ['AnillosActivity', 'MinijuegoActivity', 'her', ''],
      ['BatallaActivity', 'MinijuegoActivity', 'her', ''], ['MemoriaActivity', 'MinijuegoActivity', 'her', ''],
      ['HogarFragment', 'Fragment', 'her', ''], ['TiendaFragment', 'Fragment', 'her', ''],
      ['GuaridaFragment', 'Fragment', 'her', ''], ['MisionesFragment', 'Fragment', 'her', ''],
      ['JuegosFragment', 'Fragment', 'her', ''],
      ['HogarFragment', 'Refrescable', 'real', ''], ['TiendaFragment', 'Refrescable', 'real', ''],
      ['GuaridaFragment', 'Refrescable', 'real', ''], ['MisionesFragment', 'Refrescable', 'real', ''],
      ['JuegosFragment', 'Refrescable', 'real', ''],
      ['MainActivity', 'HogarFragment', 'agr', 'pestañas 0..5'], ['MainActivity', 'Juego', 'asoc', '1'],
      ['MainActivity', 'Alertas', 'dep', 'programar()'], ['HogarFragment', 'PetView', 'comp', '1'],
      ['AlarmaReceiver', 'Alertas', 'dep', 'mostrar()'], ['MinijuegoActivity', 'Juego', 'asoc', '1'],
      ['WidgetKaiju', 'Juego', 'dep', '']]
pagina('uml5_ui', 'Diagrama de clases 5 · Interfaz y servicios del sistema (paquetes ui, vista, juego)', c4, r4, [['@top','AppCompatActivity'],['MainActivity','Juego','Alertas','AlarmaReceiver','WidgetKaiju'],['HogarFragment','TiendaFragment','GuaridaFragment','MisionesFragment','JuegosFragment'],['PetView','Fragment','Refrescable'],['MinijuegoActivity'],['DefensaActivity','MemoriaActivity','AnillosActivity','BatallaActivity','leyenda']])
