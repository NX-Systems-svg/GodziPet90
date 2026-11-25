import re,sys,json,os,glob
ROOT=os.path.expanduser('~/AndroidStudioProjects/GodziPet90/app/src/main/java/com/cesba/godzipet')
out={}
for f in sorted(glob.glob(ROOT+'/*/*.java')):
    pkg=f.split('/')[-2]; src=open(f).read()
    src_nc=re.sub(r'/\*.*?\*/','',src,flags=re.S); src_nc=re.sub(r'//[^\n]*','',src_nc)
    m=re.search(r'(public\s+)?(final\s+|abstract\s+)*(class|interface|enum|@interface)\s+(\w+)([^{]*)\{',src_nc)
    if not m: continue
    kind,name,head=m.group(3),m.group(4),m.group(5)
    ext=re.search(r'extends\s+([\w.<>]+)',head); imp=re.search(r'implements\s+([\w.<>, ]+)',head)
    body=src_nc[m.end():]
    # top-level members only: track depth
    depth=0; buf=''; members=[]
    for ch in body:
        if depth==0: buf+=ch
        if ch=='{':
            if depth==0: members.append(buf); buf=''
            depth+=1
        elif ch=='}':
            depth-=1
            if depth<0: break
        elif ch==';' and depth==0: members.append(buf); buf=''
    fields=[];methods=[];consts=[]
    for s in members:
        s=' '.join(s.split()).strip(' {;')
        s=re.sub(r'@\w+(\([^)]*\))?\s*','',s)
        if not s: continue
        mm=re.match(r'((?:public|private|protected|static|final|synchronized|abstract|native|default)\s+)*([\w<>\[\],.? ]+?)\s+(\w+)\s*\(([^)]*)\)',s)
        cm=re.match(r'((?:public|private|protected)\s+)?'+name+r'\s*\(([^)]*)\)',s)
        if cm: methods.append({'vis':(cm.group(1) or '').strip(),'static':False,'ret':'','name':name,'params':cm.group(2).strip()}); continue
        if mm and '=' not in s.split('(')[0]:
            mods=s[:s.find(mm.group(2))] if mm.group(2) in s else ''
            methods.append({'vis':('private' if 'private' in mods else 'public' if 'public' in mods else 'protected' if 'protected' in mods else ''),'static':'static' in mods,'ret':mm.group(2).strip(),'name':mm.group(3),'params':mm.group(4).strip()}); continue
        fm=re.match(r'((?:public|private|protected|static|final|volatile|transient)\s+)*([\w<>\[\],.? ]+?)\s+(\w+)\s*(=.*)?$',s)
        if fm and kind!='enum':
            mods=s.split(fm.group(2))[0]
            d={'vis':('private' if 'private' in mods else 'public' if 'public' in mods else 'protected' if 'protected' in mods else ''),'static':'static' in mods,'final':'final' in mods,'type':fm.group(2).strip(),'name':fm.group(3),'init':(fm.group(4) or '')[1:].strip()[:40]}
            # multiple declarators like "int a, b"
            fields.append(d)
    enumvals=[]
    if kind=='enum':
        first=body.split(';')[0]
        first=re.sub(r'\([^()]*(\([^()]*\))*[^()]*\)','',first)
        enumvals=[e.strip() for e in re.sub(r'\{.*?\}','',first,flags=re.S).split(',') if re.match(r'^\s*[A-Z_0-9]+\s*$',e)]
    out[name]={'pkg':pkg,'kind':kind,'extends':ext.group(1) if ext else None,'implements':[x.strip() for x in imp.group(1).split(',')] if imp else [],'fields':fields,'methods':methods,'enum':enumvals,'lines':src.count('\n')}
json.dump(out,open('clases.json','w'),indent=1,ensure_ascii=False)
for n,c in out.items():
    if c['pkg'] in ('modelo','datos'):
        print(f"{c['kind']} {n} ext={c['extends']} impl={c['implements']} enum={c['enum'][:8]}")
        for f in c['fields']: print('   F',f['vis'][:3],'S' if f['static'] else ' ',f['type'],f['name'],'=',f['init'])
        for m in c['methods']: print('   M',m['vis'][:3],'S' if m['static'] else ' ',m['ret'],m['name'],'(',m['params'],')')
