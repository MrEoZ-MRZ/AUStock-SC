package com.mrz.austock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mrz.austock.data.Download;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mrz.austock.utils.EmptyRecyclerView;
import com.mrz.austock.data.ProductCursorAdapter;
import com.mrz.austock.R;
import com.mrz.austock.activity.Expenses.Expense;
import com.mrz.austock.activity.Expenses.ExpenseActivity;
import com.mrz.austock.activity.Expenses.ExpenseDBHelper;
import com.mrz.austock.data.Database;
import com.mrz.austock.data.Preferencias;
import com.mrz.austock.data.ProductContract.ProductEntry;
import com.mrz.austock.data.ProductDbHelper;
import com.mrz.austock.data.SaveDT;
import com.mrz.austock.utils.StringXORer;
import com.mrz.austock.utils.ViewAnimation;

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
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

import static android.content.ContentValues.TAG;
import static com.mrz.austock.data.ProductContract.ProductEntry.MONEY_TABLE_NAME;
import static com.mrz.austock.data.ProductContract.ProductEntry.TABLE_NAME;
import static com.mrz.austock.data.ProductDbHelper.DATABASE_NAME;


/**
 * Muestra la lista de productos que se ingresaron y almacenaron en la aplicación.
 */
@SuppressWarnings({"deprecation", "unused"})
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /** Identificador del cargador de datos de productos */
    private static final int PRODUCT_LOADER = 0;

    /** Adaptador para el RecyclerView */
    private ProductCursorAdapter mCursorAdapter;
    boolean isRotate = false;
    String savePath = MainActivity.this.getExternalFilesDir("").getPath() + "/AUStock/Products/";
    Bitmap bitmap;
    QRGEncoder qrgEncoder;

    public static void exportDB(Context ctx){
        File sd = ctx.getExternalFilesDir("");
        File data = Environment.getDataDirectory();
        FileChannel source;
        FileChannel destination;
        try {
            PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            String currentDBPath = "/data/"+ pinfo.packageName +"/databases/"+DATABASE_NAME;
            String backupDBPath = "/AUStock/Databases/"+DATABASE_NAME;
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, backupDBPath);
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch(IOException | PackageManager.NameNotFoundException e) {
            Log.d("AUSTOCK",e.toString());
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
                ((Activity)ctx).finish();
                ctx.startActivity(((Activity)ctx).getIntent());
                return;
            }
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Thread.sleep(2000);
            ((Activity)ctx).finish();
            ctx.startActivity(((Activity)ctx).getIntent());
        } catch(IOException | PackageManager.NameNotFoundException e) {
            Log.d("AUSTOCK",e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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


    private void generarqr() {
        AlertDialog.Builder preguntar = new AlertDialog.Builder(this);
        preguntar.setTitle("Colocar su codigo QR");
        EditText rq = new EditText(this);
        rq.setInputType(InputType.TYPE_CLASS_NUMBER);
        preguntar.setView(rq);
        preguntar.setCancelable(false);
        preguntar.setPositiveButton("Aceptar", (dialog, which) -> {
            boolean save;
            String result;
            try {
                WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                @SuppressWarnings("deprecation") Display display = manager.getDefaultDisplay();
                Point point = new Point();
                //noinspection deprecation
                display.getSize(point);
                int width = point.x;
                int height = point.y;
                int smallerDimension = Math.min(width, height);
                smallerDimension = smallerDimension * 3 / 4;
                qrgEncoder = new QRGEncoder(
                        rq.getText().toString(), null,
                        QRGContents.Type.TEXT,
                        smallerDimension);
                try {
                    bitmap = qrgEncoder.encodeAsBitmap();
                } catch (WriterException e) {
                    Log.v(TAG, e.toString());
                }
                save = QRGSaver.save(savePath, rq.getText().toString(), bitmap, QRGContents.ImageType.IMAGE_JPEG);
                result = save ? "Codigo QR guardado en " + savePath : "Imagen No guardada";
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        preguntar.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
            Cancelado();
        });
        preguntar.show();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Preferencias.with(this).read("SALIR").equals("true")){
            Preferencias.with(this).write("SALIR","false");
            stopService(new Intent(this, AdminService.class));
            logauth();
            return;
        }
        Timer timer = new Timer();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
        String v1 = getApplicationContext().getExternalFilesDir("").getPath() + "/AUStock/Products/";
        String v2 = getApplicationContext().getExternalFilesDir("").getPath() + "/AUStock/";
        String v3 = getApplicationContext().getExternalFilesDir("").getPath() + "/AUStock/Databases/";
        if(Preferencias.with(this).read("PRODUCTOSEXCASOS").equals("TRUE")) {
            Log.d("AUTSTOCK","Notificaciones activadas");
            timer.scheduleAtFixedRate(new ProductosExcasos(), 0, 300000);
        }
        if (!new File(v2).exists()){
            new File(v2).mkdir();
        }
        if (!new File(v1).exists()){
            new File(v1).mkdir();
        }
        if (!new File(v3).exists()){
            new File(v3).mkdir();
        }
        // Busque una referencia a {@link RecyclerView} en el diseño
        // Reemplazo de RecyclerView con EmptyRecyclerView
        EmptyRecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);

        // Establezca layoutManager en {@link RecyclerView}
        recyclerView.setLayoutManager(layoutManager);

        // Encuentra el diseño vacío y configúralo en la nueva vista del reciclador
        RelativeLayout mEmptyLayout = findViewById(R.id.empty_view);
        recyclerView.setEmptyLayout(mEmptyLayout);

        // Configure un ProductCursorAdapter para crear un elemento de tarjeta para cada fila de datos de producto en el Cursor.
        mCursorAdapter = new ProductCursorAdapter(this);
        // Establece el adaptador en el {@link recyclerView}
        recyclerView.setAdapter(mCursorAdapter);

        // Arranca el cargador
        ViewAnimation.init(findViewById(R.id.rqgen));
        ViewAnimation.init(findViewById(R.id.manual));
        ViewAnimation.init(findViewById(R.id.scann));
        ViewAnimation.init(findViewById(R.id.buscar));
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        findViewById(R.id.flotante).setOnClickListener(v -> {
            isRotate = ViewAnimation.rotateFab(findViewById(R.id.flotante), !isRotate);
            if(isRotate){
                ViewAnimation.showIn(findViewById(R.id.rqgen));
                ViewAnimation.showIn(findViewById(R.id.manual));
                ViewAnimation.showIn(findViewById(R.id.scann));
                ViewAnimation.showIn(findViewById(R.id.buscar));
            }else{
                ViewAnimation.showOut(findViewById(R.id.rqgen));
                ViewAnimation.showOut(findViewById(R.id.manual));
                ViewAnimation.showOut(findViewById(R.id.scann));
                ViewAnimation.showOut(findViewById(R.id.buscar));
            }
        });
        findViewById(R.id.manual).setOnClickListener(v -> {
            // Crea una nueva intención para abrir la {@link EditorActivity}
            Random random = new Random();
            long n = (long) (100000000000000L + random.nextFloat() * 900000000000000L);
            Intent intentw = new Intent(MainActivity.this, EditorActivity.class);
            // Send the data
            intentw.putExtra("Existente", "No");
            intentw.putExtra(getString(R.string.qrcode), String.valueOf(n));
            // start the new activity
            startActivity(intentw);
        });
        findViewById(R.id.rqgen).setOnClickListener(v -> generarqr());
        findViewById(R.id.buscar).setOnClickListener(v -> ScanButton());
        findViewById(R.id.scann).setOnClickListener(v -> ScanProduct());
        dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getWritableDatabase();
        @SuppressLint("Recycle") Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " +ProductEntry._ID +" = " + 1, null);
        if (cursor2 != null && cursor2.moveToFirst()) {
            double Recaudado = cursor2.getDouble(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MONEY));
            TextView total = findViewById(R.id.totall);
            DecimalFormat formatter = new DecimalFormat("###,###.###");
            if(Recaudado == 0.0){
                total.setText("Sin Fondos");
            } else {
                total.setText("$ "+formatter.format(Recaudado));
            }
        }
        if(!Preferencias.with(this).read("AUTH","").equals("TRUE")) {
            finish();
            Log.d("AUSTOCK","LA APLICACCION SE CERRO");
        }
    }

    private void irhistorial() {
        Intent intent = new Intent(MainActivity.this, ExpenseActivity.class);
        startActivity(intent);
        finish();
    }

    private void ScanProduct() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Escanea los productos vendidos").setBeepEnabled(true);
        Intent intent = integrator.createScanIntent();
        startActivityForResult(intent, 120);
    }

    /**
     * Método auxiliar para insertar datos de productos codificados en la base de datos. Solo con fines de depuración.
     */
    @SuppressWarnings("unused")
    private void insertDummyProduct() {
        // Create a ContentValues object where column names are the keys,
        // and product attributes are the values.
        Random random = new Random();
        long n = (long) (100000000000000L + random.nextFloat() * 900000000000000L);
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, "Leche Entera");
        values.put(ProductEntry.COLUMN_PRODUCT_COST, 40);
        values.put(ProductEntry.COLUMN_PRODUCT_QR, String.valueOf(n));
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, 120);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, 10);
        values.put(ProductEntry.COLUMN_SUPPLIER_NAME, "Sancor");
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, "Sancor@gmail.com");
        values.put(ProductEntry.COLUMN_SUPPLIER_PHONE, "3425465631");

        // Inserte una nueva fila para "Lenche Entera" en el proveedor usando ContentResolver.
        // Use el {@link ProductEntry.CONTENT_URI} para indicar que queremos insertar
        // en la tabla de la base de datos de productos.
        // Recibir la nueva UrI de contenido que nos permitirá acceder a los datos de Leche Entera en el futuro.
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
        boolean save;
        String result;
        try {
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int smallerDimension = Math.min(width, height);
            smallerDimension = smallerDimension * 3 / 4;
            qrgEncoder = new QRGEncoder(
                    String.valueOf(n), null,
                    QRGContents.Type.TEXT,
                    smallerDimension);
            try {
                bitmap = qrgEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                Log.v(TAG, e.toString());
            }
            save = QRGSaver.save(savePath, "Leche Entera", bitmap, QRGContents.ImageType.IMAGE_JPEG);
            result = save ? "Codigo QR guardado en " + savePath : "Imagen No guardada";
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para eliminar todos los productos de la base de datos.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI,
                null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Crear las opciones del menú del archivo res/menu/menu_catalog.xml.
        // Esto agrega elementos de menú a la barra de la aplicación.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // El usuario hizo clic en una opción de menú en el menú de desbordamiento de la barra de la aplicación
        switch (item.getItemId()) {
            // Responder a un clic en la opción de menú "Insertar datos ficticios"
            case R.id.action_insert_dummy_data:
                insertDummyProduct();
                return true;
            // Responder a un clic en la opción de menú "Eliminar todas las entradas"
            case R.id.config:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.about:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                about();
                return true;
            case R.id.guardar:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                save();
                return true;

            case R.id.descargar:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                download();
                return true;
            case R.id.historial:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                irhistorial();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void about() {
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
        finish();
    }


    @SuppressWarnings("SameParameterValue")
    private void addNotification(String Tittle, String body, String QR, String name) {
        ProductDbHelper dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery("select * from " + ProductEntry.TABLE_NAME + " where " + ProductEntry.COLUMN_PRODUCT_QR + " = " + QR, null);
        if (cursor != null && cursor.moveToFirst()) {
            Log.d("AUSTOCK", "Creando notf");
            final long id = cursor.getLong(cursor.getColumnIndex(ProductEntry._ID));
            NotificationManager mNotificationManager;

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this.getApplicationContext(), QR);
            Intent notificationIntent = new Intent(this, DetailActivity.class);
            Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
            // Set the URI on the data field of the intent
            notificationIntent.setData(currentProductUri);
            @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(contentIntent);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
            mBuilder.setContentTitle(Tittle);
            mBuilder.setContentText(body);
            mBuilder.setPriority(Notification.PRIORITY_DEFAULT);

            mNotificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                NotificationChannel channel = new NotificationChannel(
                        QR,
                        name,
                        NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(channel);
                mBuilder.setChannelId(QR);
            }

            mNotificationManager.notify(Math.toIntExact(cursor.getLong(cursor.getColumnIndex(ProductEntry._ID))), mBuilder.build());
        }
    }
    private static boolean isInternetAvailable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    @SuppressLint("StaticFieldLeak")
    public void ComprobarServidores(Context ctx,String cc) {
        (new AsyncTask<Void, Void, String>() {
            String urllogin = "";
            ProgressDialog pDialog;
            protected String doInBackground(Void... param1VarArgs) {
                try {
                    if (!isInternetAvailable(ctx))
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
                if(!urllogin.equals("¿?")){
                    Preferencias prefs;
                    prefs = Preferencias.with(ctx);
                    prefs.write("DATABASE", StringXORer.decode(urllogin)+"/upload.php");
                    pDialog.dismiss();
                    if(cc.equals("DATABASE")){
                        new Database(MainActivity.this).execute();
                    }
                    if(cc.equals("DATADOWN")){
                        new Download(MainActivity.this).execute();
                    }
                    if(cc.equals("SAVEDT")){
                        new SaveDT(MainActivity.this).execute();
                    }
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Error!").setMessage("El servidor se encuentra en mantenimiento, intente mas tarde");
                builder.setNeutralButton("Ok", (param2DialogInterface, param2Int) -> close.setVisibility(View.GONE)).setCancelable(false);
                builder.create();
                builder.show();
            }

            @SuppressWarnings("deprecation")
            protected void onPreExecute() {
                pDialog = new ProgressDialog(ctx);
                pDialog.setCancelable(false);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.setMessage("Actualizando informacion..");
                pDialog.show();
            }
        }).execute();
    }

    private void save() {
        ComprobarServidores(this, "DATABASE");
    }

    private void download() {
        ComprobarServidores(this, "DATADOWN");
    }

    private void logauth() {
        AlertDialog.Builder mrz = new AlertDialog.Builder(this);
        mrz.setTitle("Atencion").setMessage("Desea guardar la base de datos antes de cerrar sesion?");
        mrz.setPositiveButton("Si", (dialog, which) -> {
            Preferencias.with(MainActivity.this).write("MYDB", "");
            SaveDT();
        });
        mrz.setNegativeButton("No", (dialog, which) -> {
            Preferencias.with(MainActivity.this).write("USER", "");
            Preferencias.with(MainActivity.this).write("PASS", "");
            Preferencias.with(MainActivity.this).write("MYDB", "");
            Preferencias.with(MainActivity.this).write("AUTH","FALSE");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        mrz.show();
    }

    private void SaveDT() {
        ComprobarServidores(this, "SAVEDT");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Definir una proyección que especifique las columnas de la tabla que nos interesan.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE};

        // Este cargador ejecutará el método de consulta de ContentProvider en un hilo de fondo
        return new CursorLoader(this, // Contexto de la actividad principal
                ProductEntry.CONTENT_URI,       // URI de contenido del proveedor para consultar
                projection,                       // Columnas para incluir en el Cursor resultante
                null,                  // Sin cláusula de selección
                null,              // Sin argumentos de selección
                null);                // Orden de clasificación predeterminado
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Actualiza {@link ProductCursorAdapter} con este nuevo cursor que contiene datos de productos actualizados
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback llamado cuando los datos necesitan ser eliminados
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Solicite al usuario que confirme que desea eliminar este producto.
     */
    private void showDeleteConfirmationDialog() {
        // Cree un AlertDialog.Builder y configure el mensaje, y haga clic en oyentes
        // para los botones positivos y negativos del diálogo.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, (dialogInterface, id) -> {
            // El usuario hizo clic en el botón "Eliminar", así que elimine todos los productos.
            deleteAllProducts();
        });

        // El usuario hizo clic en el botón "Cancelar", así que cierre el cuadro de diálogo y continúe mostrando
        // la lista de productos. Cualquier botón descartará el cuadro de diálogo emergente de forma predeterminada,
        // por lo que todo el OnClickListener es nulo.
        builder.setNegativeButton(R.string.cancel, null);

        // Crea y muestra el AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " +ProductEntry._ID +" = " + 1, null);
        if (cursor2 != null && cursor2.moveToFirst()){
            double Recaudado = cursor2.getDouble(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MONEY));
            TextView total = findViewById(R.id.totall);
            DecimalFormat formatter = new DecimalFormat("###,###.###");
            if(Recaudado == 0.0){
                total.setText("Sin Fondos");
            } else {
                total.setText("$ " + formatter.format(Recaudado));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 32: {
                // Si se cancela la solicitud, las matrices de resultados están vacías.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ¡Se concedió el permiso, Vamos! Realice la tarea relacionada con los contactos que necesita hacer.
                    Toast.makeText(this, getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // permiso denegado, Noo! Desactive la funcionalidad que depende de este permiso.
                    Toast.makeText(this, getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT).show();
                }
            }
            case 31: {
                // Si se cancela la solicitud, las matrices de resultados están vacías.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ¡Se concedió el permiso, Vamos! Realice la tarea relacionada con los contactos que necesita hacer.
                    Toast.makeText(this, getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // permiso denegado, Noo! Desactive la funcionalidad que depende de este permiso.
                    Toast.makeText(this, getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT).show();
                }
            }
            case 30: {
                // Si se cancela la solicitud, las matrices de resultados están vacías.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ¡Se concedió el permiso, Vamos! Realice la tarea relacionada con los contactos que necesita hacer.
                    Toast.makeText(this, getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // permiso denegado, Noo! Desactive la funcionalidad que depende de este permiso.
                    Toast.makeText(this, getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT).show();
                }
            }
            // otras líneas de 'caso' para verificar otros permisos que esta aplicación podría solicitar
        }
    }

    public SQLiteDatabase dbSqlite;
    private ProductDbHelper dbHelper;
    private StringBuilder productos = new StringBuilder();
    private StringBuilder informacion = new StringBuilder();
    private StringBuilder escaneo = new StringBuilder();
    private double TOTALLs = 0;
    private int unidad = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 120 && requestCode != 131 && requestCode != IntentIntegrator.REQUEST_CODE) {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
        if(result.getContents() == null) {
            Intent originalIntent = result.getOriginalIntent();
            if (originalIntent == null) {
                if(requestCode == 120 && !productos.toString().equals("")) {

                    LayoutInflater inflater= LayoutInflater.from(this);
                    View dialogView=inflater.inflate(R.layout.alertdialog, null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setView(dialogView);
                    TextView tittle = dialogView.findViewById(R.id.tittle);
                    TextView message = dialogView.findViewById(R.id.mesassge);
                    Button ok = dialogView.findViewById(R.id.buttonpositive);
                    Button no = dialogView.findViewById(R.id.buttonnegative);
                    AlertDialog alertDialog = builder.create();
                    tittle.setText("Resumen");
                    String Total = productos.toString();
                    String[] total = Total.split("_");

                    informacion.append("Usted vendio: \n");
                    escaneo.append("Usted escaneo: \n\n");
                    for (String datos : total) {

                        String[] datosIndividuales = datos.split("#");
                        DecimalFormat formatter = new DecimalFormat("###,###.###");
                        String nombre = datosIndividuales[0];
                        String precio2 = datosIndividuales[1];

                        double precio = Double.parseDouble(precio2);
                        Double calculo = precio + TOTALLs;
                        TOTALLs = calculo;

                        escaneo.append(unidad + "- " + nombre + " $" + precio + " C/U\n");

                        informacion.append(unidad + "- " + nombre + " a " + precio + " C/U\n");
                        unidad = unidad + 1;

                    }
                    escaneo.append("\n\nTiene un total de $" +TOTALLs+ " vendido, Desea procesar los datos?");
                    message.setText(escaneo);
                    no.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                            unidad();
                            escaneo = new StringBuilder();
                            productos = new StringBuilder();
                            TOTALLs = 0;
                            Cancelado();
                        }
                    });
                    dbHelper = new ProductDbHelper(this);
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            unidad();
                            String producto = productos.toString();
                            String[] total12 = producto.split("_");
                            Log.d("AUSTOCK ", "OnClick");
                            StringBuilder vendido = new StringBuilder();
                            for (String datos : total12) {
                                Log.d("AUSTOCK ", datos);
                                String[] datosIndividuales = datos.split("#");
                                String RQ = datosIndividuales[3];
                                Log.d("AUSTOCK ", datosIndividuales[3]);
                                SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
                                @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery("select * from " + ProductEntry.TABLE_NAME + " where " + ProductEntry.COLUMN_PRODUCT_QR + " = " + RQ, null);
                                @SuppressLint("Recycle") Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " + ProductEntry._ID + " = " + 1, null);
                                if (cursor != null && cursor.moveToFirst() && cursor2 != null && cursor2.moveToFirst()) {
                                    double Precio = cursor.getDouble(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE));
                                    double Recaudado = cursor2.getDouble(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MONEY));
                                    int Cantidad = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));
                                    String name = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
                                    int VENDER = Cantidad - 1;
                                    if (VENDER == 0) {
                                        LayoutInflater inflater= LayoutInflater.from(MainActivity.this);
                                        View dialogView=inflater.inflate(R.layout.alertdialog, null);
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                        builder.setView(dialogView);
                                        TextView tittle = dialogView.findViewById(R.id.tittle);
                                        TextView message = dialogView.findViewById(R.id.mesassge);
                                        Button ok = dialogView.findViewById(R.id.buttonpositive);
                                        Button no = dialogView.findViewById(R.id.buttonnegative);
                                        AlertDialog alertDialog = builder.create();
                                        tittle.setText("Alerta");
                                        message.setText("Te has quedado sin stock de " + name + " para poder finalizar su venta y solo ha vendido \n" +
                                                "\n" + vendido + "\n\n Los demas no han podido venderse");
                                        ok.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_QUANTITY + "` = '" + VENDER + "' WHERE `" + ProductEntry.COLUMN_PRODUCT_QR + "` = " + RQ);
                                                Log.d("TOTAL PROD", String.valueOf(Cantidad));
                                                double TOTAL = Precio + Recaudado;
                                                Log.d("TOTAL", String.valueOf(TOTAL));
                                                sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.MONEY_TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_MONEY + "` = '" + TOTAL + "' WHERE `" + ProductEntry._ID + "` = " + 1);
                                                TextView total1 = findViewById(R.id.totall);
                                                TextView cantidad2 = findViewById(R.id.product_quantity_card);
                                                String tota2 = (String) cantidad2.getText();
                                                int totall = Integer.parseInt(tota2);
                                                int asd = totall - 1;
                                                cantidad2.setText(String.valueOf(asd));
                                                total1.setText(String.valueOf(TOTAL));
                                                Expense activeExpense = new Expense();
                                                activeExpense.setAmount(String.valueOf(TOTAL));
                                                activeExpense.setCategory("Venta de productos");
                                                activeExpense.setDescription(String.valueOf(vendido));
                                                activeExpense.setDate(Calendar.getInstance().getTimeInMillis());
                                                ExpenseDBHelper db = ExpenseDBHelper.getInstance(MainActivity.this);
                                                db.addExpense(activeExpense);
                                                escaneo = new StringBuilder();
                                                productos = new StringBuilder();
                                                finish();
                                                startActivity(getIntent());
                                                Toast.makeText(MainActivity.this, "Venta procesada", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                        no.setVisibility(View.GONE);
                                        alertDialog.setCancelable(false);
                                        alertDialog.show();
                                        return;
                                    }
                                    vendido.append(unidad + "- " + name + " $" + Precio + " C/U\n");
                                    unidad = unidad + 1;
                                    sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_QUANTITY + "` = '" + VENDER + "' WHERE `" + ProductEntry.COLUMN_PRODUCT_QR + "` = " + RQ);
                                    Log.d("TOTAL PROD", String.valueOf(Cantidad));
                                    double TOTAL = Precio + Recaudado;
                                    Log.d("TOTAL", String.valueOf(TOTAL));
                                    sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.MONEY_TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_MONEY + "` = '" + TOTAL + "' WHERE `" + ProductEntry._ID + "` = " + 1);
                                    TextView total1 = findViewById(R.id.totall);
                                    TextView cantidad2 = findViewById(R.id.product_quantity_card);
                                    String tota2 = (String) cantidad2.getText();
                                    int totall = Integer.parseInt(tota2);
                                    int asd = totall - 1;
                                    cantidad2.setText(String.valueOf(asd));
                                    total1.setText(String.valueOf(TOTAL));
                                }
                            }
                            Expense activeExpense = new Expense();
                            activeExpense.setAmount(String.valueOf(TOTALLs));
                            activeExpense.setCategory("Venta de productos");
                            activeExpense.setDescription(String.valueOf(informacion));
                            activeExpense.setDate(Calendar.getInstance().getTimeInMillis());
                            ExpenseDBHelper db = ExpenseDBHelper.getInstance(MainActivity.this);
                            db.addExpense(activeExpense);
                            escaneo = new StringBuilder();
                            productos = new StringBuilder();
                            unidad();
                            finish();
                            startActivity(getIntent());
                            Toast.makeText(MainActivity.this, "Venta procesada", Toast.LENGTH_LONG).show();
                        }
                    });
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                } else {
                    escaneo = new StringBuilder();
                    productos = new StringBuilder();
                    Toast.makeText(this, "Cancelado",Toast.LENGTH_SHORT).show();
                }
            } else if(originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                Log.d("MainActivity", "Cancelled scan due to missing camera permission");
                Toast.makeText(this, "Cancelado por la falta del permiso de la camara", Toast.LENGTH_LONG).show();
            }
        } else {
            if(requestCode == 120) {
                dbHelper = new ProductDbHelper(this);
                SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
                @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery("select * from " + ProductEntry.TABLE_NAME + " where " + ProductEntry.COLUMN_PRODUCT_QR + " = " + result.getContents(), null);
                @SuppressLint("Recycle") Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " +ProductEntry._ID +" = " + 1, null);
                if (cursor != null && cursor.moveToFirst() && cursor2 != null && cursor2.moveToFirst()) {
                    int Cantidad = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));
                    String nombre = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
                    String QR = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QR));
                    double precio = cursor.getDouble(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE));
                    productos.append(nombre).append("#").append(precio).append("#").append(Cantidad).append("#").append(QR).append("_");
                    Toast.makeText(this, nombre + " añadido al carrito", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
                }
                ScanProduct();
            }
            if (requestCode == 131) {
                dbHelper = new ProductDbHelper(this);
                SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
                @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery("select * from " + ProductEntry.TABLE_NAME + " where " + ProductEntry.COLUMN_PRODUCT_QR + " = " + result.getContents(), null);
                if (cursor != null && cursor.moveToFirst()) {
                    Toast.makeText(this, "Producto encontrado", Toast.LENGTH_SHORT).show();
                    Intent intentw = new Intent(this, EditorActivity.class);
                    // Send the data
                    intentw.putExtra("Existente", "Si");
                    intentw.putExtra("ID", cursor.getLong(0));
                    // start the new activity
                    startActivity(intentw);
                } else {
                    Toast.makeText(this, "Producto producto creado", Toast.LENGTH_SHORT).show();
                    Intent intentw = new Intent(this, EditorActivity.class);
                    // Send the data
                    intentw.putExtra("Existente", "No");
                    intentw.putExtra(getString(R.string.qrcode), result.getContents());
                    // start the new activity
                    startActivity(intentw);
                }
            }
        }
    }
    private void unidad(){
        unidad = 1;
    }
    private void Cancelado() {
        Toast.makeText(this,"Cancelado",Toast.LENGTH_SHORT).show();
    }

    public void ScanButton() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Escanea un codigo QR o de BARRAS");
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setCameraId(0);
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        Intent intent = integrator.createScanIntent();
        startActivityForResult(intent, 131);
    }

    class ProductosExcasos extends TimerTask {
        @Override
        public void run() {
            dbHelper = new ProductDbHelper(MainActivity.this);
            SQLiteDatabase sqLiteDatabase = dbHelper.getWritableDatabase();
            Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + TABLE_NAME + " where " +ProductEntry.COLUMN_PRODUCT_QUANTITY +" < 6 or "+ ProductEntry.COLUMN_PRODUCT_QUANTITY+ " = 0", null);
            if (cursor2 != null) {
                if (cursor2.getCount() > 0)
                {
                    cursor2.moveToFirst();
                    do {
                        String name = cursor2.getString(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
                        Log.d("AUSTOCK",name);
                        String qr = cursor2.getString(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QR));
                        addNotification("Te estas quedando sin "+name,"El producto se esta quedando sin stock, contactate con el proovedor para pedir mas unidades",qr,name);
                    } while (cursor2.moveToNext());
                    cursor2.close();
                }
            }

        }
    }
}
