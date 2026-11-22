#!/bin/bash
# Toca el elemento cuyo texto/descripción contiene $1. uso: tap.sh "texto" [serial] [indice]
A="$HOME/Android/Sdk/platform-tools/adb -s ${2:-emulator-5554}"
$A shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
$A shell cat /sdcard/ui.xml | python3 -c "
import sys,re
x=sys.stdin.read(); q=sys.argv[1]; r=[]
for m in re.finditer(r'<node [^>]*>', x):
    n=m.group(0)
    t=re.search(r'text=\"([^\"]*)\"',n).group(1)+' '+re.search(r'content-desc=\"([^\"]*)\"',n).group(1)
    if q in t:
        b=list(map(int,re.findall(r'\d+',re.search(r'bounds=\"([^\"]*)\"',n).group(1))))
        r.append(((b[0]+b[2])//2,(b[1]+b[3])//2))
if r: i=min(int(sys.argv[2]),len(r)-1); print(*r[i])
" "$1" "${3:-0}" | { read X Y; if [ -n "$X" ]; then $A shell input tap $X $Y; echo "tap $1 @ $X,$Y"; else echo "NO ENCONTRADO: $1"; fi; }
