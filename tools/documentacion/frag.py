# frag.py Clase metodo [n-esima]  -> imprime el método con números de línea (incluye javadoc previo)
import sys,glob,re,json
R='/home/tortacubana/AndroidStudioProjects/GodziPet90/app/src/main/java/com/cesba/godzipet'
def extraer(clase, metodo, nth=0, conDoc=True):
    f=glob.glob(f'{R}/*/{clase}.java')[0]; L=open(f).read().split('\n')
    pat=re.compile(r'^\s+(public|private|protected|static|final|synchronized|void|boolean|int|double|long|String|Resultado|\w+(<[^>]*>)?)\b[^;=]*\b'+re.escape(metodo)+r'\s*\(')
    hits=[i for i,l in enumerate(L) if pat.search(l) and not l.strip().startswith(('//','*'))]
    i=hits[nth]; ini=i
    if conDoc:
        j=i-1
        while j>=0 and (L[j].strip().startswith(('*','/**','@')) or L[j].strip().endswith('*/')): j-=1
        ini=j+1
    d=0; k=i; started=False
    while k<len(L):
        d+=L[k].count('{')-L[k].count('}')
        if '{' in L[k]: started=True
        if started and d<=0: break
        if not started and L[k].rstrip().endswith(';'): break
        k+=1
    return f.split('godzipet/')[1], ini+1, k+1, '\n'.join(L[ini:k+1])
if __name__=='__main__':
    a=sys.argv
    p,i,k,t=extraer(a[1],a[2],int(a[3]) if len(a)>3 else 0)
    print(f'// {p}  (líneas {i}-{k})'); print(t)
