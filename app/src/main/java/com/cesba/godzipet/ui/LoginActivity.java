package com.cesba.godzipet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.EstadoAnimo;
import com.cesba.godzipet.modelo.Etapa;
import com.cesba.godzipet.modelo.OrigenHuevo;
import com.cesba.godzipet.modelo.RutaEvolucion;
import com.cesba.godzipet.vista.Apariencia;
import com.cesba.godzipet.vista.KaijuMiniView;

/** Inicio de sesión local (obligatorio para guardar el avance). */
public class LoginActivity extends AppCompatActivity {
    private static final String PATRON_USUARIO = "^[A-Za-z0-9_]{3,20}$";
    private EditText etUsuario, etContrasena;
    private TextView tvError, btnEntrar, btnRegistrar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Juego juego = Juego.get(this);
        if (juego.prefs().usuarioId() > 0) {
            irAInicio();
            return;
        }
        setContentView(R.layout.activity_login);
        KaijuMiniView logo = findViewById(R.id.logo);
        logo.setApariencia(new Apariencia(Etapa.FINAL, RutaEvolucion.GUARDIAN, OrigenHuevo.MANHATTAN, EstadoAnimo.FELIZ, 0));
        etUsuario = findViewById(R.id.etUsuario);
        etContrasena = findViewById(R.id.etContrasena);
        tvError = findViewById(R.id.tvError);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnEntrar.setOnClickListener(v -> enviar(false));
        btnRegistrar.setOnClickListener(v -> enviar(true));
    }

    private void enviar(boolean registro) {
        String usuario = etUsuario.getText().toString().trim();
        String pass = etContrasena.getText().toString();
        if (!usuario.matches(PATRON_USUARIO)) {
            error("El usuario debe tener de 3 a 20 letras, números o guion bajo.");
            return;
        }
        if (pass.length() < 4) {
            error("La contraseña debe tener al menos 4 caracteres.");
            return;
        }
        ocupado(true);
        Juego juego = Juego.get(this);
        if (registro) {
            juego.repo().registrar(usuario, pass, this::exito, this::error);
        } else {
            juego.repo().iniciarSesion(usuario, pass, this::exito, this::error);
        }
    }

    private void exito(long id) {
        Sonidos.ok(this);
        Juego.get(this).olvidar();
        Juego.get(this).prefs().setUsuarioId(id);
        irAInicio();
    }

    private void error(String msg) {
        ocupado(false);
        Sonidos.error(this);
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void ocupado(boolean si) {
        btnEntrar.setEnabled(!si);
        btnRegistrar.setEnabled(!si);
        if (si) tvError.setVisibility(View.GONE);
    }

    private void irAInicio() {
        startActivity(new Intent(this, InicioActivity.class));
        finish();
    }
}
