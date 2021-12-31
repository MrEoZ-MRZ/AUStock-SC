package com.mrz.austock.activity;

import static com.mrz.austock.data.ProductDbHelper.DATABASE_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
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
import com.mrz.austock.utils.StringXORer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Base64;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    //Declarar boton de registro
    private TextView tvRegister;
    //Declarar editexts de correo y constrseña
    private EditText etLoginGmail,etLoginPassword;
    //Declarar boton de login
    private Button loginButton;
    //Declarar clase de preferencias
    private Preferencias prefs;
    //Declarar numero de permiso
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Verificar si no tiene el permiso de sobreponerse a otras aplicaciones
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            //Ejecuccion de la funcion declarada
            ComprobarServidores(this);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                ComprobarServidores(this);
            } else { //Permission is not available
                Toast.makeText(this,
                        "Permiso no disponible, cerrando aplicacion",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
    //Ejecuccion de funcion declarada
    @SuppressLint("StaticFieldLeak")
    public void ComprobarServidores(Context ctx) {
        //Crear una tarea de ejecuccion
        (new AsyncTask<Void, Void, String>() {
            //Crear una variable con una url
            String urllogin = "";
            //Crear una variable con el dialogo
            ProgressDialog pDialog;
            //Crear la funcion en segundo plano
            protected String doInBackground(Void... param1VarArgs) {
                //intentar
                try {
                    //Si no hay internet enviar ?
                    if (!LoginActivity.isInternetAvailable(ctx))
                        return "?";
                    //Crear una tarea de lectura en la url declarada
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((new URL("https://mreoz-mrz.github.io/AUStock/server.html")).openConnection().getInputStream()));
                    //Si es verdadero
                    while (true) {
                        //Leer el contenido de la url
                        String str = bufferedReader.readLine();
                        //Si el contenido no es nulo
                        if (str != null) {
                            //guardar contenido en la variable urlogin
                            StringBuilder stringBuilder = new StringBuilder();
                            (urllogin) = stringBuilder.append(urllogin).append(str).toString();
                            continue;
                        }
                        break;
                    }
                    //cerrar lectura
                    bufferedReader.close();
                    //enviar respuesta positiva
                    return "1";
                } catch (Exception param1VarArg) {
                    Log.e("MREOZ","BUSCAR SERVIDORES ERROR : "+param1VarArg);
                    return "";
                }
            }

            //Funcion despues de ejecutar
            protected void onPostExecute(String param1String) {
                //declarar boton
                Button close = findViewById(R.id.btnLogin);
                //mostrar un dialogo de alerta que no hay internet
                if (param1String.equals("?")) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(ctx);
                    builder1.setTitle("Attention!").setMessage("No estas conectado a internet");
                    builder1.setNeutralButton("Ok", (param2DialogInterface, param2Int) -> close.setVisibility(View.GONE)).setCancelable(false);
                    builder1.create();
                    builder1.show();
                    return;
                }
                //Verificar si la url no es nula
                if(!urllogin.equals("")) {
                    String URL = "URL";
                    prefs = Preferencias.with(ctx);
                    //guardar la url
                    prefs.write(URL, StringXORer.decode(urllogin) +"/login.php");
                    //dismunir el progresdialog
                    pDialog.dismiss();
                    //iniciar vista de login con la url de consulta desencriptada
                    Ask(StringXORer.decode(urllogin)+"/login.php");
                    return;
                }
                //si no hay ningun contenido mostrar un dialogo de alerta con los servidores en mantenimiento
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Error!").setMessage("El servidor se encuentra en mantenimiento, intente mas tarde");
                builder.setNeutralButton("Ok", (param2DialogInterface, param2Int) -> close.setVisibility(View.GONE)).setCancelable(false);
                builder.create();
                builder.show();
            }

            //Antes de ejecutuar crear un progresdialog
            @SuppressWarnings("deprecation")
            protected void onPreExecute() {
                pDialog = new ProgressDialog(ctx);
                pDialog.setCancelable(false);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.setMessage("Buscando servidor..");
                pDialog.show();
            }
        }).execute();
    }
    //Ejecuccion de la funcion declarada
    private void Login(String urllogin) {
        //Asociar variables con el layout
        tvRegister = findViewById(R.id.tvRegister);
        etLoginGmail = findViewById(R.id.etLogGmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        loginButton = findViewById(R.id.btnLogin);
        prefs = Preferencias.with(this);
        //Rellenar el correo y la contraseña con los valores puestos anteriormente
        //(Si es que existen o se colocaron)
        etLoginGmail.setText(prefs.read("USER", ""));
        etLoginPassword.setText(prefs.read("PASS", ""));
        if (!etLoginGmail.getText().toString().isEmpty() && !etLoginPassword.getText().toString().isEmpty()){
            //Iniciar sesion automaticamente si el correo y la contraseña
            //Estan completados
            new Auth(LoginActivity.this).execute(etLoginGmail.getText().toString(), etLoginPassword.getText().toString(), urllogin);
        }
        //Dar funcion onclick al boton de login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etLoginGmail.getText().toString().trim();
                String password = etLoginPassword.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()) {
                    //Mostrar un mensaje de error si no esta la contraseña o el mail
                    Toast.makeText(LoginActivity.this, "Coloca tu email y contraseña para iniciar sesion", Toast.LENGTH_SHORT).show();
                } else {
                    //Guardar el usuario y la contraseña y ejecutar inicio de sesion
                    prefs.write("USER", etLoginGmail.getText().toString());
                    prefs.write("PASS", etLoginPassword.getText().toString());
                    //Ejecucion de funcion Auth con consulta php
                    new Auth(LoginActivity.this).execute(etLoginGmail.getText().toString(), etLoginPassword.getText().toString(), urllogin);
                }
            }
        });
        //Declarar boton de registro con cambio de actividad
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
                finish();
            }
        });
    }

    public static void copyFile(File oldLocation, File newLocation) throws IOException {
        if ( oldLocation.exists( )) {
            BufferedInputStream reader = new BufferedInputStream( new FileInputStream(oldLocation) );
            BufferedOutputStream writer = new BufferedOutputStream( new FileOutputStream(newLocation, false));
            try {
                byte[]  buff = new byte[8192];
                int numChars;
                while ( (numChars = reader.read(  buff, 0, buff.length ) ) != -1) {
                    writer.write( buff, 0, numChars );
                }
            } catch( IOException ex ) {
                throw new IOException("IOException when transferring " + oldLocation.getPath() + " to " + newLocation.getPath());
            } finally {
                try {
                    if ( reader != null ){
                        writer.close();
                        reader.close();
                    }
                } catch( IOException ex ){
                    Log.e("AUSTOCK", "Error closing files when transferring " + oldLocation.getPath() + " to " + newLocation.getPath() );
                }
            }
        } else {
            throw new IOException("Old location does not exist when transferring " + oldLocation.getPath() + " to " + newLocation.getPath() );
        }
    }

    @SuppressLint("NewApi")
    public static void importDB(Context ctx , String db) {

        Log.d("AUSTOCK","Cargando base de datos");
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        try {
            byte[] bdata = db.getBytes();
            byte[] decoded = Base64.getDecoder().decode(bdata);
            String database = ctx.getCacheDir().getPath() + "/database.sql";
            FileOutputStream fo = new FileOutputStream(database);
            fo.write(decoded);
            fo.flush();
            fo.close();
            PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            String backupDBPath = "/data/"+ pinfo.packageName +"/databases/"+DATABASE_NAME;
            String backupDBPa = "/data/"+ pinfo.packageName +"/databases/";
            File currentDB = new File(database);
            File backupDB = new File(data, backupDBPath);
            File backupD = new File(data, backupDBPa);
            if(!backupDB.exists()){
                backupD.mkdir();
                copyFile(currentDB,backupDB);
                Thread.sleep(2000);
                Intent intent = new Intent(((Activity)ctx), MainActivity.class);
                ctx.startActivity(intent);
                ((Activity)ctx).finish();
                return;
            }
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Thread.sleep(2000);
            Intent intent = new Intent(((Activity)ctx), MainActivity.class);
            ctx.startActivity(intent);
            ((Activity)ctx).finish();
        } catch(IOException | PackageManager.NameNotFoundException e) {
            Log.d("AUSTOCK",e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void Ask(String urllogin) {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Login(urllogin);
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(LoginActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                AlertDialog.Builder mrz = new AlertDialog.Builder(LoginActivity.this);
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
}
