package com.cesba.godzipet.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cesba.godzipet.R;
import com.cesba.godzipet.juego.Juego;
import com.cesba.godzipet.juego.Sonidos;
import com.cesba.godzipet.modelo.Partida;
import com.cesba.godzipet.vista.Apariencia;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** "Godzilla en tu cuarto": cámara trasera con el kaiju superpuesto, foto y compartir. */
public class CamaraActivity extends AppCompatActivity {
    private static final String TAG = "Camara";

    private PreviewView vista;
    private KaijuArView capa;
    private View destello;
    private ProcessCameraProvider proveedor;
    private boolean traseraActiva = true;
    private String nombre = "Godzilla";
    private final ExecutorService fondo = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pedirPermiso =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    Toast.makeText(this, "Sin permiso de cámara no puedo mostrar a tu kaiju en tu cuarto.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_camara);
        Juego.get(this).asegurar(this::iniciar, this::finish);
    }

    private void iniciar() {
        if (isFinishing() || isDestroyed()) return;
        Partida p = Juego.get(this).partida();
        if (p == null) {
            finish();
            return;
        }
        nombre = p.getPersonaje().getNombre();
        vista = findViewById(R.id.camaraVista);
        vista.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        vista.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        destello = findViewById(R.id.camaraDestello);

        capa = new KaijuArView(this);
        capa.setApariencia(Apariencia.de(p));
        ((FrameLayout) findViewById(R.id.camaraCapa)).addView(capa,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        findViewById(R.id.camaraCerrar).setOnClickListener(v -> finish());
        findViewById(R.id.camaraEspejo).setOnClickListener(v -> capa.alternarEspejo());
        findViewById(R.id.camaraGirar).setOnClickListener(v -> {
            traseraActiva = !traseraActiva;
            enlazar();
        });
        findViewById(R.id.camaraObturador).setOnClickListener(v -> disparar());
        ajustarBordes();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso de cámara")
                    .setMessage("Necesito la cámara para mostrar a tu kaiju en tu cuarto. "
                            + "La imagen no sale de tu teléfono salvo que tú la compartas.")
                    .setCancelable(false)
                    .setPositiveButton("Continuar", (d, w) -> pedirPermiso.launch(Manifest.permission.CAMERA))
                    .setNegativeButton("Ahora no", (d, w) -> finish())
                    .show();
        }
    }

    /** Evita que la barra inferior y el botón de cerrar queden bajo las barras del sistema. */
    private void ajustarBordes() {
        View raiz = findViewById(R.id.camaraRaiz);
        View barra = findViewById(R.id.camaraBarra);
        View cerrar = findViewById(R.id.camaraCerrar);
        final int base = barra.getPaddingBottom();
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) cerrar.getLayoutParams();
        final int margenBase = lp.topMargin;
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            barra.setPadding(barra.getPaddingLeft(), barra.getPaddingTop(), barra.getPaddingRight(), base + s.bottom);
            lp.topMargin = margenBase + s.top;
            cerrar.setLayoutParams(lp);
            return insets;
        });
        ViewCompat.requestApplyInsets(raiz);
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(this);
        f.addListener(() -> {
            try {
                proveedor = f.get();
                enlazar();
            } catch (Exception e) {
                Log.e(TAG, "proveedor", e);
                Toast.makeText(this, "No se pudo abrir la cámara.", Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void enlazar() {
        if (proveedor == null || vista == null) return;
        try {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(vista.getSurfaceProvider());
            CameraSelector sel = traseraActiva ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
            proveedor.unbindAll();
            proveedor.bindToLifecycle(this, sel, preview);
        } catch (Exception e) {
            Log.e(TAG, "enlazar", e);
            Toast.makeText(this, "Esa cámara no está disponible.", Toast.LENGTH_SHORT).show();
            if (!traseraActiva) {
                traseraActiva = true;
                enlazar();
            }
        }
    }

    private void disparar() {
        Bitmap original = vista.getBitmap();
        if (original == null) {
            Toast.makeText(this, "La cámara aún no está lista.", Toast.LENGTH_SHORT).show();
            return;
        }
        Sonidos.ok(this);
        destello.setAlpha(0.9f);
        destello.animate().alpha(0f).setDuration(350).start();
        final int vistaAncho = Math.max(1, vista.getWidth());
        final float t = SystemClock.uptimeMillis() / 1000f;
        final Bitmap base = original.copy(Bitmap.Config.ARGB_8888, true);
        fondo.execute(() -> {
            try {
                Uri uri = componer(base, vistaAncho, t);
                runOnUiThread(() -> compartir(uri));
            } catch (Exception e) {
                Log.e(TAG, "foto", e);
                runOnUiThread(() -> Toast.makeText(this, "No se pudo guardar la foto.", Toast.LENGTH_LONG).show());
            }
        });
    }

    /** Compone kaiju + leyenda sobre la foto y la guarda como PNG en la caché compartible. */
    private Uri componer(Bitmap foto, int vistaAncho, float t) throws IOException {
        Canvas c = new Canvas(foto);
        float factor = foto.getWidth() / (float) vistaAncho;
        capa.dibujar(c, foto.getWidth() / factor, foto.getHeight() / factor, factor, t);
        dibujarLeyenda(c, foto.getWidth(), foto.getHeight());

        File dir = new File(getCacheDir(), "fotos");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Sin carpeta de fotos");
        File archivo = new File(dir, "godzilla_ar.png");
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            foto.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        foto.recycle();
        return FileProvider.getUriForFile(this, getPackageName() + ".fotos", archivo);
    }

    private void dibujarLeyenda(Canvas c, int w, int h) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(w * 0.036f);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Color.WHITE);
        p.setShadowLayer(w * 0.008f, 0, 0, Color.BLACK);
        float margen = w * 0.03f;
        c.drawText("Godzi-Pet 90 · " + nombre, margen, h - margen, p);
    }

    private void compartir(Uri uri) {
        if (isFinishing() || isDestroyed()) return;
        Intent enviar = new Intent(Intent.ACTION_SEND).setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, nombre + " en mi cuarto · Godzi-Pet 90")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(enviar, "Compartir foto"));
    }

    @Override
    protected void onDestroy() {
        fondo.shutdown();
        if (proveedor != null) proveedor.unbindAll();
        super.onDestroy();
    }
}
