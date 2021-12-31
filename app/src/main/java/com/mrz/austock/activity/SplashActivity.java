package com.mrz.austock.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.mrz.austock.R;
import com.mrz.austock.data.Preferencias;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SplashActivity extends AppCompatActivity {

    //Decir a la actividad que el spash dure 2,5s
    private final int SPLASH_TIME = 2500;

    //Declarar la imagen
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Iniciar la vista
        initializateView();
        animatedLogo();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Ir a la actividad principal
                goToMainActivity();
            }
        },SPLASH_TIME);
    }
    //Funcion declarada para ir a la actividad principal
    private void goToMainActivity() {
        //Verificar si se inicio o no por primera vez la aplicacion
        if(Preferencias.with(this).read("BOOT","").equals("")){
            //Generar una ventana emergente
            AlertDialog.Builder mrz = new AlertDialog.Builder(this);
            mrz.setTitle("Bienvenido a AUStock").setMessage("Quiere iniciar sesion siempre?\nSi no lo desea puede afectar a la comprobacion de cambios en su base de datos");
            mrz.setCancelable(false);
            mrz.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Guardar los datos y decir que ya se inicio por primera vez
                    Preferencias.with(SplashActivity.this).write("BOOT","BOOTED");
                    Preferencias.with(SplashActivity.this).write("AUTOLOGIN","TRUE");
                    //Ejecucion del a funcion declarada
                    AutoLogin();

                }
            });
            mrz.setNegativeButton("Solo una vez", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Guardar los datos y decir que ya se inicio por primera vez
                    Preferencias.with(SplashActivity.this).write("BOOT","BOOTED");
                    Preferencias.with(SplashActivity.this).write("AUTOLOGIN","FALSE");
                    //Ejecucion de la funcion declarada
                    Login();
                }
            });
            //mostrar dialogo
            mrz.show();
            return;
        }
        //De lo contrario si se inicio por primera vez ejecutar
        if(Preferencias.with(this).read("AUTOLOGIN").equals("TRUE")) {
            AutoLogin();
        } else if (Preferencias.with(this).read("AUTOLOGIN").equals("FALSE")) {
            Login();
        }
    }

    //Funcion declarada para ir automaticamente al actividad de incio de sesion
    private void AutoLogin() {
        //Iniciar cambio de actividad al loginactivity
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        //finalizar actividad splash
        finish();
    }

    //Funcion declarada para ir a la actividad de MainAcitivity
    private void Login() {
        //Verificar si el usuario no inicio sesion
        if(!Preferencias.with(this).read("AUTH","").equals("TRUE")) {
            //Si no inicio enviarlo a la actividad de login
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            //Finalizar actividad splashscreen
            finish();
            Log.d("AUSTOCK","INICIANDO SESION");
            return;
        }
        //Verificar si el usuario inicio sesion
        if(Preferencias.with(this).read("AUTH","").equals("TRUE")) {
            //Iniciar la actividad MainAcitivity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            //Finalizar actividad splashscreen
            finish();
            Log.d("AUSTOCK","INICIANDO SESION");
        }
    }

    //Asociar la imagen con la imagen del layout
    private void initializateView() {
        imageView = findViewById(R.id.splash);
    }

    //Iniciar animacion de logo
    private void animatedLogo() {
        Animation fade = AnimationUtils.loadAnimation(this, R.anim.faded);
        fade.setDuration(SPLASH_TIME);
        imageView.startAnimation(fade);
    }
}