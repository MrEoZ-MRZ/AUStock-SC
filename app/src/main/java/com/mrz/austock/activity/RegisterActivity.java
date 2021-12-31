package com.mrz.austock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.mrz.austock.R;
import com.mrz.austock.data.Auth;
import com.mrz.austock.data.Preferencias;
import com.mrz.austock.data.Register;
import com.mrz.austock.utils.StringXORer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private Button registerBtn;
    private TextView gotoLoginBtn;
    private EditText regName,regGmail,regPassword;
    private Preferencias prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ComprobarServidores(this);
        prefs = Preferencias.with(this);
    }
    boolean twice;
    @Override
    public void onBackPressed() {
        if(twice){
            System.exit(0);
        }
        Toast.makeText(this,"Preciona nuevamente para salir",Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> twice = false,2000);
        twice = true;
    }

    @SuppressWarnings("deprecation")
    private static boolean isInternetAvailable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @SuppressLint("StaticFieldLeak")
    public void ComprobarServidores(Context ctx) {
        (new AsyncTask<Void, Void, String>() {
            String urllogin = "";
            ProgressDialog pDialog;
            protected String doInBackground(Void... param1VarArgs) {
                try {
                    if (!RegisterActivity.isInternetAvailable(ctx))
                        return "?";
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((new URL("https://mreoz-mrz.github.io/AUStock/server.html")).openConnection().getInputStream()));
                    while (true) {
                        String str = bufferedReader.readLine();
                        if (str != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            (urllogin) = stringBuilder.append(urllogin).append(str).toString();
                            continue;
                        }
                        break;
                    }
                    bufferedReader.close();
                    return "1";
                } catch (Exception param1VarArg) {
                    Log.e("MREOZ","BUSCAR SERVIDORES ERROR : "+param1VarArg);
                    return "";
                }
            }

            protected void onPostExecute(String param1String) {
                Button close = findViewById(R.id.btnLogin);
                if (param1String.equals("?")) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(ctx);
                    builder1.setTitle("Attention!").setMessage("No estas conectado a internet");
                    builder1.setNeutralButton("Ok", (param2DialogInterface, param2Int) -> close.setVisibility(View.GONE)).setCancelable(false);
                    builder1.create();
                    builder1.show();
                    return;
                }

                if(!urllogin.equals("")){
                    pDialog.dismiss();
                    //Toast.makeText(ctx,"Servidor conectado: " + urllogin.replaceAll(".000webhostapp.com/register.php","").replaceAll("https://",""),Toast.LENGTH_SHORT).show();
                    Ask(StringXORer.decode(urllogin)+"/register.php");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setTitle("Error!").setMessage("El servidor se encuentra en mantenimiento, intente mas tarde");
                builder.setNeutralButton("Ok", (param2DialogInterface, param2Int) -> close.setVisibility(View.GONE)).setCancelable(false);
                builder.create();
                builder.show();
            }

            @SuppressWarnings("deprecation")
            protected void onPreExecute() {
                pDialog = new ProgressDialog(ctx);
                pDialog.setMessage("Buscando servidor...");
                pDialog.setCancelable(false);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.show();
            }
        }).execute();
    }

    private void Ask(String urllogin) {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                register(urllogin);
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(RegisterActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                AlertDialog.Builder mrz = new AlertDialog.Builder(RegisterActivity.this);
                mrz.setTitle("ERROR!").setMessage("Lamentablemente no puedes usar la app porque no tienes habilitados los permisos " + deniedPermissions.toString() + " abre de nuevo la aplicacion y permitelos");
                mrz.setCancelable(false);
                mrz.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        };

        //check all needed permissions together
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("Si no activas los permisos esta app no puede funcionar correctamente, por favor ve a [CONFIGURACION] - [APLICACIONES] - [AUSTOCK] - [PERMISOS] y activa todos los permisos")
                .setPermissions(Manifest.permission.VIBRATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
                .check();
    }

    private void register(String urllogin) {
        registerBtn = findViewById(R.id.btnRegLogin);
        gotoLoginBtn = findViewById(R.id.btnGotoLogin);
        regName = findViewById(R.id.etRegName);
        regGmail = findViewById(R.id.etRegGmail);
        regPassword = findViewById(R.id.etRegPassword);

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fname = regName.getText().toString().trim();
                String fGmail = regGmail.getText().toString().trim();
                String fPassword = regPassword.getText().toString().trim();
                if (fname.isEmpty() || fPassword.isEmpty() || fGmail.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Completa todos tus datos", Toast.LENGTH_SHORT).show();
                } else {
                    prefs.write("NOMBRE", fname);
                    prefs.write("CORREO", fGmail);
                    prefs.write("CONTRASEÑA", fPassword);
                    new Register(RegisterActivity.this).execute(fname, fGmail, fPassword, urllogin);
                }
            }
        });

        gotoLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                finish();
            }
        });
    }
}
