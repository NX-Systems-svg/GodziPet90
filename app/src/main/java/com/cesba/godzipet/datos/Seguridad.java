package com.cesba.godzipet.datos;

import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de contraseñas con PBKDF2 y sal aleatoria (nunca se guarda la contraseña). */
public final class Seguridad {
    private static final int ITERACIONES_V1 = 10_000;
    private static final int ITERACIONES_V2 = 120_000;
    private static final int BITS = 256;
    /** Prefijo de los hashes nuevos (PBKDF2-HMAC-SHA256). Los antiguos no tienen prefijo. */
    public static final String PREFIJO_V2 = "v2$";

    private Seguridad() { }

    public static String nuevaSal() {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return Base64.encodeToString(sal, Base64.NO_WRAP);
    }

    /** Hash nuevo: SHA-256 con 120 000 iteraciones (SHA-1 sólo si el dispositivo no lo soporta). */
    public static String hash(String contrasena, String sal) {
        try {
            return PREFIJO_V2 + pbkdf2("PBKDF2WithHmacSHA256", contrasena, sal, ITERACIONES_V2);
        } catch (Exception e) {
            return hashV1(contrasena, sal);
        }
    }

    /** Hash de la versión 1 (cuentas antiguas). */
    public static String hashV1(String contrasena, String sal) {
        try {
            return pbkdf2("PBKDF2WithHmacSHA1", contrasena, sal, ITERACIONES_V1);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }

    /** Verifica contra el formato que tenga el hash guardado. */
    public static boolean verificar(String contrasena, String sal, String guardado) {
        if (guardado.startsWith(PREFIJO_V2)) return iguales(guardado, hash(contrasena, sal));
        return iguales(guardado, hashV1(contrasena, sal));
    }

    public static boolean esAntiguo(String guardado) {
        return !guardado.startsWith(PREFIJO_V2);
    }

    private static String pbkdf2(String algoritmo, String contrasena, String sal, int iteraciones) throws Exception {
        KeySpec spec = new PBEKeySpec(contrasena.toCharArray(), Base64.decode(sal, Base64.NO_WRAP), iteraciones, BITS);
        byte[] h = SecretKeyFactory.getInstance(algoritmo).generateSecret(spec).getEncoded();
        return Base64.encodeToString(h, Base64.NO_WRAP);
    }

    /** Comparación en tiempo constante. */
    public static boolean iguales(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
