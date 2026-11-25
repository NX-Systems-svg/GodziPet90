import json,re,sys
sys.argv=['x']; exec(open('frag.py').read().split("if __name__")[0])
R='/home/tortacubana/AndroidStudioProjects/GodziPet90/app/src/main/java/com/cesba/godzipet'
L=[('Necesidades','Necesidades',0,None),('Necesidades','cambiar',0,None),('Necesidades','establecer',0,None),('Necesidades','limitar',0,None),
('Necesidades','calidad',0,None),('Necesidades','promedio',0,None),('Necesidades','enZonaIdeal',0,None),
('Cuidados','alimentar',0,None),('Cuidados','central',0,None),('Cuidados','costoEntrenar',0,None),
('Partida','acariciar',0,None),('Partida','actualizar',0,None),('Partida','avanzar',0,None),('Partida','revisarFusion',0,None),('Partida','fundirse',0,None),
('Partida','ganar',0,None),('Partida','sumarXp',1,None),('Partida','registrarMinijuego',0,None),('Partida','iniciarMinijuego',0,None),
('Partida','reclamarMision',0,None),('Partida','revisarEvolucion',0,None),('Partida','nueva',0,None),('Partida','getPromedioBienestar',0,None),
('Deterioro','aplicar',1,None),('Niveles','nivelPara',0,None),('Tienda','comprar',0,None),('Inventario','agregar',0,None),('Inventario','consumir',0,None),
('Guarida','equipar',0,None),('Guarida','retirar',0,None),('GuaridaFragment','elegir',1,None),('GestorMisiones','reclamada',0,None),('Mision','progreso',0,None),
('Evolucion','decidir',0,None),('Minijuego','monedas',0,None),('Minijuego','experiencia',0,None),('MinijuegoActivity','terminar',0,None),
('Alertas','programar',0,None),('Alertas','minutosHastaCritico',0,None),('AlarmaReceiver','onReceive',0,None),
('EstadoAnimo','calcular',0,None),('Seguridad','verificar',0,None),('Seguridad','hash',0,None),('Personaje','validarNombre',0,None),
('Contadores','muestrear',0,None),('Contadores','renovarDia',0,None),('HogarFragment','ofrecer',0,None),('PetView','soltarComida',0,None),
('Repositorio','guardar',0,None),('Repositorio','cargar',0,None),('Repositorio','cargarSincrono',0,30),('Repositorio','instantanea',1,22),
('JuegoDao','guardarTodo',0,None),('BaseDatos','get',0,None),('Juego','tick',0,None),('Juego','guardar',0,None),('Juego','asegurar',0,None),
('MainActivity','onPause',0,None),('MainActivity','onStop',0,None),('MainActivity','despuesDeAccion',0,None),('Repositorio','iniciarSesion',0,None)]
out={}
for c,m,n,mx in L:
    if c=='Necesidades' and m=='Necesidades':
        f=f'{R}/modelo/Necesidades.java'; T=open(f).read().split('\n'); i=T.index([l for l in T if 'public final class Necesidades' in l][0])
        txt='\n'.join(T[i:i+18]); out['Necesidades.campos']={'ruta':'modelo/Necesidades.java','ini':i+1,'fin':i+18,'v':txt}; continue
    p,i,k,t=extraer(c,m,n)
    lines=t.split('\n')
    # quitar indentación común
    ind=min(len(l)-len(l.lstrip()) for l in lines if l.strip())
    lines=[l[ind:] for l in lines]
    if mx and len(lines)>mx: lines=lines[:mx]+['    // … (continúa: el resto de los campos se copian igual)','}']
    key=f'{c}.{m}'; 
    if key in out: key+=str(n)
    out[key]={'ruta':p,'ini':i,'fin':k,'v':'\n'.join(lines)}
# extras: bloque estático de misiones, umbrales, tick de MainActivity, migración
def rango(ruta,a,b):
    T=open(f'{R}/{ruta}').read().split('\n')[a-1:b]; ind=min(len(l)-len(l.lstrip()) for l in T if l.strip())
    return {'ruta':ruta,'ini':a,'fin':b,'v':'\n'.join(l[ind:] for l in T)}
import subprocess
def linea(ruta,pat): return int(subprocess.run(['grep','-n','-m1',pat,f'{R}/{ruta}'],capture_output=True,text=True).stdout.split(':')[0])
a=linea('modelo/GestorMisiones.java','static {'); out['GestorMisiones.static']=rango('modelo/GestorMisiones.java',a,a+9)
a=linea('modelo/Niveles.java','UMBRALES'); out['Niveles.UMBRALES']=rango('modelo/Niveles.java',a,a+1)
a=linea('ui/MainActivity.java','private final Runnable tick'); out['MainActivity.tick']=rango('ui/MainActivity.java',a,a+11)
a=linea('ui/MainActivity.java','TICK_MS ='); out['MainActivity.const']=rango('ui/MainActivity.java',a,a+1)
a=linea('datos/BaseDatos.java','@Database'); out['BaseDatos.decl']=rango('datos/BaseDatos.java',a,a+5)
a=linea('datos/BaseDatos.java','MIGRACION_1_2 ='); out['BaseDatos.migracion']=rango('datos/BaseDatos.java',a,a+20)
a=linea('datos/Preferencias.java','usuarioId()'); out['Preferencias.sesion']=rango('datos/Preferencias.java',a-6,a+2)
a=linea('datos/PartidaEntity.java','@Entity'); out['PartidaEntity.decl']=rango('datos/PartidaEntity.java',a,a+20)
json.dump(out,open('/home/tortacubana/AndroidStudioProjects/GodziPet90/tools/documentacion/fragmentos.json','w'),ensure_ascii=False,indent=1)
print(len(out)); print(out['Preferencias.sesion']['v']); print(out['BaseDatos.migracion']['v'][-300:]); print(out['MainActivity.const']['v'])
