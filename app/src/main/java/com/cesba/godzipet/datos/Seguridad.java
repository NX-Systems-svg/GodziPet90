package com.cesba.godzipet.datos;

import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de contraseñas con PBKDF2 y sal aleatoria (nunca se guarda la contraseña). */
public final class Seguridad {
    private static final int ITERACIONES = 10_000;
    private static final int BITS = 256;

    private Seguridad() { }

    public static String nuevaSal() {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return Base64.encodeToString(sal, Base64.NO_WRAP);
    }

    public static String hash(String contrasena, String sal) {
        try {
            KeySpec spec = new PBEKeySpec(contrasena.toCharArray(), Base64.decode(sal, Base64.NO_WRAP), ITERACIONES, BITS);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            return Base64.encodeToString(h, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }

    /** Comparación en tiempo constante. */
    public static boolean iguales(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
