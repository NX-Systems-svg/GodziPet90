#!/bin/bash
# Abre la app desde el título hasta el Hogar. uso: abrir.sh [serial]
SER=${1:-emulator-5554}; A="$HOME/Android/Sdk/platform-tools/adb -s $SER"; D=$(dirname "$0")
$A shell am force-stop com.cesba.godzipet
$A shell am start -n com.cesba.godzipet/.ui.TituloActivity >/dev/null; sleep 4
$A shell input tap 540 2050; sleep 3
$D/tap.sh CONTINUAR $SER; sleep 4
