package com.cesba.godzipet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.ModoProfesor;
import com.cesba.godzipet.vista.TituloView;

/**
 * Pantalla de título animada. Tocar 7 veces el título ゴジラ abre el acceso al modo profesor
 * (código {@link ModoProfesor#CODIGO}).
 */
public class TituloActivity extends AppCompatActivity {
    private static final int TOQUES_SECRETOS = 7;
    private int toques;
    private long ultimoToque;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TituloView vista = new TituloView(this);
        vista.setContentDescription("Godzi-Pet 90. Toca para comenzar");
        setContentView(vista);
        vista.setOnClickListener(v -> continuar());
        vista.setAlTocarLogo(this::toqueLogo);
        Sonidos.rugido(this);
    }

    private void toqueLogo() {
        long ahora = System.currentTimeMillis();
        toques = ahora - ultimoToque < 1500 ? toques + 1 : 1;
        ultimoToque = ahora;
        Sonidos.toque(this);
        if (toques >= TOQUES_SECRETOS) {
            toques = 0;
            pedirCodigo();
        }
    }

    private void pedirCodigo() {
        EditText campo = new EditText(this);
        campo.setHint("Código de evaluador");
        campo.setSingleLine(true);
        LinearLayout cont = new LinearLayout(this);
        int m = Ui.dp(this, 20);
        cont.setPadding(m, 0, m, 0);
        cont.addView(campo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle("🎓 Modo profesor")
                .setMessage("Ingresa el código de evaluador. Tu partida real se respaldará y se restaurará al salir del modo.")
                .setView(cont)
                .setPositiveButton("Activar", (d, w) -> {
                    if (ModoProfesor.codigoValido(campo.getText().toString())) {
                        Juego.get(this).prefs().setProfesorSolicitado(true);
                        Sonidos.ok(this);
                        Ui.mensaje(this, "Código correcto: el panel se abrirá al entrar a tu partida.", 0xFF2E8B57);
                    } else {
                        Sonidos.error(this);
                        Ui.mensaje(this, "Código incorrecto.", 0xFFB3122E);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void continuar() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
