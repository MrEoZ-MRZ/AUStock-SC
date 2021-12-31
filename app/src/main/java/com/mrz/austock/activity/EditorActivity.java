package com.mrz.austock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.WriterException;
import com.mrz.austock.R;
import com.mrz.austock.activity.Expenses.Expense;
import com.mrz.austock.activity.Expenses.ExpenseDBHelper;
import com.mrz.austock.data.Preferencias;
import com.mrz.austock.data.ProductContract.ProductEntry;
import com.mrz.austock.data.ProductDbHelper;
import com.mrz.austock.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;
import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.EasyPermissions;

import static android.content.ContentValues.TAG;
import static com.mrz.austock.data.ProductContract.ProductEntry.COLUMN_PRODUCT_QR;
import static com.mrz.austock.data.ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY;
import static com.mrz.austock.data.ProductContract.ProductEntry.MONEY_TABLE_NAME;
import static com.mrz.austock.data.ProductContract.ProductEntry.TABLE_NAME;

/**
 * Permite al usuario crear un nuevo producto o editar uno existente.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /** TAG para los mensajes de registro */
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    /** Identificador del cargador de datos de productos */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    private static final int PICK_IMAGE_REQUEST = 1;

    /** URI para la imagen del producto */
    private String mImageUri;

    /** URI de contenido para el producto existente (nulo si es un producto nuevo) */
    private Uri mCurrentProductUri;

    /** Campo EditText para ingresar el nombre del producto */
    @BindView(R.id.edit_product_name) EditText mProductNameEditText;

    /** Campo EditText para ingresar al proovedor */
    @BindView(R.id.edit_product_provider) EditText mProviderEditText;

    /** Campo EditText para ingresar el QR */
    @BindView(R.id.edit_product_qr) EditText mqrEditText;

    /** Campo EditText para ingresar el precio del producto */
    @BindView(R.id.edit_product_price) EditText mPriceEditText;

    /** Campo EditText para ingresar la cantidad del producto */
    @BindView(R.id.edit_product_quantity) EditText mQuantityEditText;

    /** Campo EditText para ingresar el nombre del proveedor */
    @BindView(R.id.edit_supplier_name) EditText mSupplierNameEditText;

    /** Campo EditText para ingresar el correo electrónico del proveedor */
    @BindView(R.id.edit_supplier_email) EditText mSupplierEmailEditText;

    /** Campo EditText para ingresar el número de teléfono del proveedor */
    @BindView(R.id.edit_supplier_phone) EditText mSupplierPhoneEditText;

    /** ImageView para la imagen del producto */
    @BindView(R.id.edit_product_image) ImageView mImageView;

    @BindView(R.id.edit_add_image_button) Button addImageButton;

    /** Bandera booleana que realiza un seguimiento de si el producto se ha editado (true) o no (false) */
    private boolean mProductHasChanged = false;

    /** TextInputLayout para mostrar la etiqueta flotante en EditText */
    @BindView(R.id.layout_product_name) TextInputLayout layoutProductName;
    @BindView (R.id.layout_product_isbn) TextInputLayout layoutProductIsbn;
    @BindView (R.id.layout_product_price) TextInputLayout layoutProductPrice;
    @BindView (R.id.layout_product_quantity) TextInputLayout layoutProductQuantity;
    @BindView (R.id.layout_supplier_name) TextInputLayout layoutSupplierName;
    @BindView (R.id.layout_supplier_phone) TextInputLayout layoutSupplierPhone;

    /** El valor booleano isValidate es falso si se supone que es un producto nuevo y
     * todos los campos del editor están en blanco. De lo contrario, el valor isValidate es verdadero.
     */
    private boolean isValidate = true;

    Bitmap bitmap;
    QRGEncoder qrgEncoder;

    /**
     * OnTouchListener que escucha si cualquier usuario toca una vista, lo que implica que está modificando
     * la vista, y cambiamos el booleano mProductHasChanged a verdadero.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int GALLERY_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        layoutProductName = findViewById (R.id.layout_supplier_phone);
        layoutProductIsbn = findViewById (R.id.layout_supplier_phone);
        layoutProductPrice = findViewById (R.id.layout_supplier_phone);
        layoutProductQuantity = findViewById(R.id.layout_supplier_phone);
        layoutSupplierName = findViewById(R.id.layout_supplier_phone);
        layoutSupplierPhone = findViewById(R.id.layout_supplier_phone);
        mqrEditText = findViewById(R.id.edit_product_qr);
        // Vincular la vista usando ButterKnife
        ButterKnife.bind(this);

        // Recibe los datos de IsbnActivity. Compruebe si un extra con "título" (o "autor" o
        // "isbn" o "publisher") se pasó en el intent.
        Intent intent3 = getIntent();
        mCurrentProductUri = intent3.getData();
        if( getIntent().hasExtra(getString(R.string.qrcode)) || getIntent().hasExtra("ID") ) {
            // Obtenga los datos de RQActivity y establezca los datos en el campo EditText.
            Intent intent = getIntent();
            String datos = intent.getStringExtra("Existente");
            if(datos.equals("Si")){
                mCurrentProductUri = intent.getData();
                final long id = intent.getLongExtra("ID",0);
                mCurrentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
            }
            if (datos.equals("No")){
                mCurrentProductUri = intent.getData();
                String qr = String.valueOf(intent.getStringExtra(getString(R.string.qrcode)));
                mqrEditText.setText(qr);
            }
        }

        // Si la intención NO contiene un URI de contenido de producto, entonces sabemos que estamos
        // creando un nuevo producto.
        if (mCurrentProductUri == null) {
            // Este es un producto nuevo, así que cambie la barra de la aplicación para que diga "Agregar un producto"
            setTitle(R.string.editor_activity_title_new_product);
            mImageView = findViewById(R.id.edit_product_image);
            mImageView.setImageResource(R.drawable.ic_image_black_24dp);

            // Invalidar el menú de opciones, por lo que la opción de menú "Eliminar" se puede ocultar.
            // (No tiene sentido eliminar un producto que aún no se ha creado).
            invalidateOptionsMenu();
        } else {
            // De lo contrario, este es un producto existente, así que cambie la barra de la aplicación para que diga "Editar producto"
            setTitle(R.string.editor_activity_title_edit_product);

            // Inicializar un cargador para leer los datos del producto de la base de datos
            // y mostrar los valores actuales en el editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }
        addImageButton = findViewById(R.id.edit_add_image_button);
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (EasyPermissions.hasPermissions(EditorActivity.this, galleryPermissions)) {
                    pickImageFromGallery();
                } else {
                    EasyPermissions.requestPermissions(EditorActivity.this, "Access for storage",
                            GALLERY_PERMISSION_REQUEST_CODE, galleryPermissions);
                }
            }
        });

        // Configure OnTouchListeners en todos los campos de entrada, para que podamos determinar si el usuario
        // los ha tocado o modificado. Esto nos permitirá saber si hay cambios sin guardar.
        // o no, si el usuario intenta salir del editor sin guardar.
        mProductNameEditText = findViewById(R.id.edit_product_name);
        mProductNameEditText.setOnTouchListener(mTouchListener);
        mProviderEditText = findViewById(R.id.edit_product_provider);
        mProviderEditText.setOnTouchListener(mTouchListener);
        mqrEditText = findViewById(R.id.edit_product_qr);
        mqrEditText.setOnTouchListener(mTouchListener);
        mPriceEditText = findViewById(R.id.edit_product_price);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText = findViewById(R.id.edit_product_quantity);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierNameEditText = findViewById(R.id.edit_supplier_name);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText = findViewById(R.id.edit_supplier_email);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mSupplierPhoneEditText = findViewById(R.id.edit_supplier_phone);
        mSupplierPhoneEditText.setOnTouchListener(mTouchListener);
        addImageButton.setOnTouchListener(mTouchListener);
        mImageView = findViewById(R.id.edit_product_image);
        mImageView.setOnTouchListener(mTouchListener);
    }

    private void  pickImageFromGallery() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            // Permitir al usuario seleccionar archivos
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            // Permitir al usuario seleccionar y devolver documentos existentes.
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            // Solo se muestran los archivos que se pueden abrir
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // Mostrar archivos de imagen de todos los formatos.
        intent.setType(getString(R.string.image_all_format));
        // Inicie una actividad de selector de archivos con la intención de elegir un archivo y recibir un resultado.
        // Para recibir un resultado, llame a startActivityForResult (). El resultado será el URI del archivo seleccionado.
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE_REQUEST);
    }

    /**
     * Maneja el resultado para la intención "elegir un archivo"
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // El intent ACTION_OPEN_DOCUMENT se envió con el código de solicitud READ_REQUEST_CODE.
        // Si el código de solicitud que se ve aquí no coincide, es la respuesta a alguna otra intención,
        // y el siguiente código no debería ejecutarse en absoluto.

        // Verifique a qué solicitud estamos respondiendo y asegúrese de que la solicitud se haya realizado correctamente
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // La ubicación de la imagen se nos entrega en el tipo de datos URI.
            if (data != null) {
                Uri uri = data.getData();
                String path = FileUtils.getPath(this, uri);
                if (path != null && FileUtils.isLocal(path)) {
                    File file = new File(path);
                    String encoded = encodeFileToBase64(file);
                    mImageUri = encoded;
                    // Mostrar la imagen en ImageView
                    @SuppressLint({"NewApi", "LocalSuppress"}) byte[] arrayOfByte = Base64.getDecoder().decode(mImageUri.getBytes());
                    Bitmap bitmap = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                    mImageView.setImageBitmap(bitmap);
                }
            }
        }
    }
    @SuppressLint("NewApi")
    private static String encodeFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            Log.d("MRZ",e.toString());
        }
        return null;
    }

    /**
     * Obtenga la entrada del usuario del editor y guarde el nuevo producto en la base de datos.
     */
    private void saveProduct() {
        // Leer de los campos de entrada
        // Utilice recortar para eliminar los espacios en blanco iniciales o finales
        String productNameString = mProductNameEditText.getText().toString().trim();
        String productCostString = mProviderEditText.getText().toString().trim();
        String rqString = mqrEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
        String supplierPhoneString = mSupplierPhoneEditText.getText().toString().trim();

        // Crea un objeto ContentValues ​​donde los nombres de las columnas son las claves,
        // y los atributos del producto del editor son los valores.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, productNameString);
        values.put(ProductEntry.COLUMN_PRODUCT_COST, productCostString);
        values.put(COLUMN_PRODUCT_QR, rqString);
        // Si el usuario no proporciona el precio, no intente analizar la cadena en un
        // valor doble. Utilice 0.0 de forma predeterminada.
        double price = 0.0;
        if(!TextUtils.isEmpty(priceString)) {
            price = Double.parseDouble(priceString);
        }
        // Si el usuario no proporciona la cantidad, no intente analizar la cadena en una
        // Valor entero. Utilice 0 de forma predeterminada.
        int quantity = 0;
        if(!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }

        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        // Compruebe si mImageUri no es nulo y luego conviértalo en una cadena
        if (mImageUri != null) {
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, mImageUri.toString());
            Log.e(LOG_TAG, "Uri saveProduct() : " + mImageUri.toString());
        }

        values.put(ProductEntry.COLUMN_SUPPLIER_NAME, supplierNameString);
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL,supplierEmailString);
        values.put(ProductEntry.COLUMN_SUPPLIER_PHONE, supplierPhoneString);

        // Determine si se trata de un producto nuevo o existente comprobando si mCurrentProductUri es nulo o no
        if (mCurrentProductUri == null) {
            // Este es un producto NUEVO, así que inserte un producto nuevo en el proveedor,
            // devolviendo el URI de contenido para el nuevo producto.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Muestra un mensaje dependiendo de si la inserción fue exitosa o no
            if (newUri == null) {
                // Si el nuevo URI de contenido es nulo, entonces hubo un error con la inserción.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                boolean save;
                String result;
                try {
                    WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = manager.getDefaultDisplay();
                    Point point = new Point();
                    display.getSize(point);
                    int width = point.x;
                    int height = point.y;
                    int smallerDimension = width < height ? width : height;
                    smallerDimension = smallerDimension * 3 / 4;
                    qrgEncoder = new QRGEncoder(
                            rqString, null,
                            QRGContents.Type.TEXT,
                            smallerDimension);
                    try {
                        bitmap = qrgEncoder.encodeAsBitmap();
                    } catch (WriterException e) {
                        Log.v(TAG, e.toString());
                    }
                    String savePath = getApplicationContext().getExternalFilesDir("").getPath() + "/AUStock/Products/";
                    save = QRGSaver.save(savePath, productNameString, bitmap, QRGContents.ImageType.IMAGE_JPEG);
                    result = save ? "Codigo QR guardado en " + savePath : "Imagen No guardada";
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                // De lo contrario, la inserción se realizó correctamente y podemos mostrar un mensaje.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            ProductDbHelper dbHelper = new ProductDbHelper(this);
            SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " +ProductEntry._ID +" = " + 1, null);
            Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + TABLE_NAME + " where " + COLUMN_PRODUCT_QR +" = " + rqString, null);
            if(cursor2 != null && cursor2.moveToFirst() && cursor != null && cursor.moveToFirst()){
                if(Integer.parseInt(quantityString) > cursor2.getInt(cursor2.getColumnIndex(COLUMN_PRODUCT_QUANTITY))){
                    int total = Integer.parseInt(quantityString) - cursor2.getInt(cursor2.getColumnIndex(COLUMN_PRODUCT_QUANTITY));
                    String unidades = String.valueOf(total);

                    ViewGroup viewGroup = findViewById(android.R.id.content);
                    View dialogView = LayoutInflater.from(this).inflate(R.layout.alertdialog, viewGroup, false);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setView(dialogView);
                    TextView tittle = dialogView.findViewById(R.id.tittle);
                    TextView message = dialogView.findViewById(R.id.mesassge);
                    Button ok = dialogView.findViewById(R.id.buttonpositive);
                    Button no = dialogView.findViewById(R.id.buttonnegative);
                    AlertDialog alertDialog = builder.create();
                    tittle.setText("Atencion");
                    message.setText("Usted tiene un stock de "+ productNameString + " por " + cursor2.getInt(cursor2.getColumnIndex(COLUMN_PRODUCT_QUANTITY)) + " unidades \n " +
                            "Y detectamos unas " + unidades + " unidades nuevas. \n ¿Desea implementarlas como compra? ");
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int costo = Integer.parseInt(productCostString) * Integer.parseInt(unidades);
                            Expense activeExpense = new Expense();
                            activeExpense.setAmount("-"+ costo);
                            activeExpense.setCategory("Compra de " +productNameString);
                            activeExpense.setDescription("Usted compro " + unidades + " unidades de este producto");
                            activeExpense.setDate(Calendar.getInstance().getTimeInMillis());
                            ExpenseDBHelper db = ExpenseDBHelper.getInstance(EditorActivity.this);
                            db.addExpense(activeExpense);
                            double Recaudado = cursor.getDouble(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MONEY)) - costo;
                            sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.MONEY_TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_MONEY + "` = '" + Recaudado + "' WHERE `" + ProductEntry._ID + "` = " + 1);
                            int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                                    null, null);
                            // Muestra un mensaje dependiendo de si la actualización fue exitosa o no.
                            if (rowsAffected == 0) {
                                // Si no hay filas afectadas, entonces hubo un error con la actualización.
                                Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_failed),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // De lo contrario, la actualización fue exitosa y podemos mostrar un mensaje.
                                Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_successful),
                                        Toast.LENGTH_SHORT).show();
                            }
                            NavUtils.navigateUpFromSameTask(EditorActivity.this);
                        }
                    });
                    no.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            // De lo contrario, este es un producto EXISTENTE, así que actualice el producto con el contenido URI: mCurrentProductUri
                            // y pasar los nuevos ContentValues. Pase nulo para los argumentos de selección y selección
                            // porque mCurrentProductUri ya identificará la fila correcta en la base de datos que
                            // queremos modificar.
                            int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                                    null, null);
                            // Muestra un mensaje dependiendo de si la actualización fue exitosa o no.
                            if (rowsAffected == 0) {
                                // Si no hay filas afectadas, entonces hubo un error con la actualización.
                                Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_failed),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // De lo contrario, la actualización fue exitosa y podemos mostrar un mensaje.
                                Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_successful),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                } else {
                    int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                            null, null);
                    // Muestra un mensaje dependiendo de si la actualización fue exitosa o no.
                    if (rowsAffected == 0) {
                        // Si no hay filas afectadas, entonces hubo un error con la actualización.
                        Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_failed),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // De lo contrario, la actualización fue exitosa y podemos mostrar un mensaje.
                        Toast.makeText(EditorActivity.this, getString(R.string.editor_update_product_successful),
                                Toast.LENGTH_SHORT).show();
                    }
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Crear las opciones del menú del archivo res/menu/menu_editor.xml.
        // Esto agrega elementos de menú a la barra de la aplicación.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * Este método se llama después de invalidateOptionsMenu (), para que el menú se pueda actualizar
     * (algunos elementos del menú se pueden ocultar o hacer visibles).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Si se trata de un producto nuevo, oculte el elemento de menú "Eliminar".
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // El usuario hizo clic en una opción de menú en el menú de desbordamiento de la barra de la aplicación
        switch (item.getItemId()) {
            // Responder a un clic en la opción de menú "Guardar"
            case R.id.action_save:
                Preferencias prefs;
                prefs = Preferencias.with(this);
                prefs.write("AUTH", "TRUE");
                // Verifique que la entrada del usuario esté validada. No se aceptan valores nulos para el nombre del producto,
                // autor, isbn, precio, cantidad, nombre del proveedor, teléfono del proveedor.
                if (isValidateInput()) {
                    // Si la entrada de un usuario es válida, guarde el producto en la base de datos y navegue hasta la actividad principal
                    // que es la {@link MainActivity}.
                    saveProduct();
                } else if (!isValidate) {
                    // Si el valor de isValidate es falso, navegue hasta la actividad principal.
                    // Como no se modificó ningún campo, podemos navegar hasta la actividad principal sin crear un nuevo producto.
                    // No es necesario crear ContentValues ​​y no es necesario realizar ninguna operación de ContentProvider.
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                }
                return true;
            // Responder a un clic en la opción de menú "Eliminar"
            case R.id.action_delete:
                // Cuadro de diálogo emergente de confirmación para su eliminación
                showDeleteConfirmationDialog();
                return true;
            // Responder a un clic en el botón de flecha "Arriba" en la barra de la aplicación
            case android.R.id.home:
                // Si el producto no ha cambiado, continúe navegando hasta la actividad principal
                // que es la {@link MainActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // De lo contrario, si hay cambios sin guardar, configure un diálogo para advertir al usuario.
                // Crea un oyente de clics para manejar al usuario confirmando que
                // los cambios deben descartarse.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // El usuario hizo clic en el botón "Descartar", navegue hasta la actividad principal
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Muestra un cuadro de diálogo que notifica al usuario que tienen cambios sin guardar
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Este método se llama cuando se presiona el botón Atrás.
     */
    @Override
    public void onBackPressed() {
        // Si el producto no ha cambiado, continúe con el manejo y presione el botón Atrás
        if (!mProductHasChanged) {
            super.onBackPressed();
            Preferencias.with(this).write("AUTH","TRUE");
            NavUtils.navigateUpFromSameTask(EditorActivity.this);
            return;
        }

        // De lo contrario, si hay cambios sin guardar, configure un diálogo para advertir al usuario.
        // Cree un detector de clics para manejar al usuario que confirma que los cambios deben descartarse.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // El usuario hizo clic en el botón "Descartar", navegue hasta la actividad principal
                        Preferencias.with(EditorActivity.this).write("AUTH","TRUE");
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };
        // Mostrar diálogo de cambios no guardados
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Dado que el editor muestra todos los atributos del producto, defina una proyección que contenga
        // todas las columnas de la tabla de productos
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_COST,
                COLUMN_PRODUCT_QR,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_SUPPLIER_PHONE};

        // Este cargador ejecutará el método de consulta de ContentProvider en un hilo de fondo
        return new CursorLoader(this,   // Contexto de la actividad principal
                mCurrentProductUri,             // Consulta el URI de contenido para el producto actual
                projection,                       // Columnas para incluir en el Cursor resultante
                null,                  // Sin cláusula de selección
                null,               // Sin argumentos de selección
                null);                 // Orden de clasificación predeterminado
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Salga temprano si el cursor es nulo o hay menos de 1 fila en el cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Continúe moviéndose a la primera fila del cursor y leyendo datos de él
        // (Esta debería ser la única fila en el cursor)
        if (cursor.moveToFirst()) {
            // Busque las columnas de atributos del producto que nos interesan
            int titleColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int publisherColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_COST);
            int isbnColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_QR);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int supplierNameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_PHONE);

            // Extrae el valor del Cursor para el índice de columna dado
            String title = cursor.getString(titleColumnIndex);
            String publisher = cursor.getString(publisherColumnIndex);
            String isbn = cursor.getString(isbnColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            final String imageString = cursor.getString(imageColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);
            String supplierPhone = cursor.getString(supplierPhoneColumnIndex);

            // Actualiza las vistas en pantalla con los valores de la base de datos
            mProductNameEditText.setText(title);
            mProviderEditText.setText(publisher);
            mqrEditText.setText(isbn);
            mPriceEditText.setText(String.valueOf(price));
            mQuantityEditText.setText(String.valueOf(quantity));

            if(imageString != null) {

                // Adjunte un oyente ViewTreeObserver a ImageView.
                ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mImageUri = imageString;
                        Log.e(LOG_TAG, "mImageUri onLoadFinished: " + mImageUri);
                        @SuppressLint({"NewApi", "LocalSuppress"}) byte[] arrayOfByte = Base64.getDecoder().decode(mImageUri.getBytes());
                        Bitmap bitmap = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                        mImageView.setImageBitmap(bitmap);
                    }
                });
            } else {
                mImageView.setImageResource(R.drawable.ic_image_black_24dp);
            }

            mSupplierNameEditText.setText(supplierName);
            mSupplierEmailEditText.setText(supplierEmail);
            mSupplierPhoneEditText.setText(supplierPhone);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Si el cargador está invalidado, borre todos los datos de los campos de entrada.
        mProductNameEditText.setText("");
        mProviderEditText.setText("");
        mqrEditText.setText("");
        mPriceEditText.setText(String.valueOf(""));
        mQuantityEditText.setText(String.valueOf(""));
        mSupplierNameEditText.setText("");
        mSupplierEmailEditText.setText("");
        mSupplierPhoneEditText.setText("");
    }

    /**
     * Muestra un cuadro de diálogo que advierte al usuario que hay cambios no guardados que se perderán
     * si siguen saliendo del editor.
     *
     * @param discardButtonClickListener es el oyente de clics para saber qué hacer cuando
     *                                        el usuario confirma que quiere descartar sus cambios
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Cree un AlertDialog.Builder y configure el mensaje, y haga clic en oyentes
        // para los botones positivos y negativos del diálogo.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // El usuario hizo clic en el botón "Seguir editando", así que cierre el cuadro de diálogo.
                // y continúe editando el producto.
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        // Crea y muestra el AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Solicite al usuario que confirme que desea eliminar este producto.
     */
    private void showDeleteConfirmationDialog() {
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.alertdialog, viewGroup, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        TextView tittle = dialogView.findViewById(R.id.tittle);
        TextView message = dialogView.findViewById(R.id.mesassge);
        Button ok = dialogView.findViewById(R.id.buttonpositive);
        Button no = dialogView.findViewById(R.id.buttonnegative);
        AlertDialog alertDialog = builder.create();
        tittle.setText("Atencion");
        message.setText(R.string.delete_dialog_msg);
        ok.setText(R.string.delete);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProduct();
            }
        });
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    /**
     * Realice la eliminación del producto en la base de datos.
     */
    private void deleteProduct() {
        // Realice la eliminación solo si se trata de un producto existente.
        if (mCurrentProductUri != null) {
            // Llame al ContentResolver para eliminar el producto en el URI de contenido dado.
            // Pase nulo para los argumentos de selección y selección porque mCurrentProductUri
            // El URI de contenido ya identifica el producto que queremos.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null,
                    null);

            // Muestra un mensaje dependiendo de si la eliminación fue exitosa o no.
            if (rowsDeleted == 0) {
                // Si no se eliminó ninguna fila, entonces hubo un error con la eliminación.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // De lo contrario, la eliminación fue exitosa y podemos mostrar un mensaje.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Navegar a la actividad de los parent
        NavUtils.navigateUpFromSameTask(EditorActivity.this);
    }

    /**
     * Verifique que la entrada del usuario esté validada. No se aceptan valores nulos para el nombre del producto,
     * QR, precio, cantidad, nombre del proveedor, teléfono del proveedor.
     */
    private boolean isValidateInput() {
        // Leer de los campos de entrada
        // Utilice recortar para eliminar los espacios en blanco iniciales o finales
        String productNameString = mProductNameEditText.getText().toString().trim();
        String provideString = mProviderEditText.getText().toString().trim();
        String rqString = mqrEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
        String supplierPhoneString = mSupplierPhoneEditText.getText().toString().trim();

        // El valor booleano isValidate es falso si se supone que es un producto nuevo
        // y todos los campos del editor están en blanco
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(productNameString) &&
                TextUtils.isEmpty(provideString) && TextUtils.isEmpty(rqString) &&
                TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString) &&
                TextUtils.isEmpty(supplierNameString) && TextUtils.isEmpty(supplierEmailString) &&
                TextUtils.isEmpty(supplierPhoneString) &&
                mImageUri == null) {
            isValidate = false;
            return false;
        }

        // Si el valor booleano isValidate es verdadero (todos los campos no están en blanco) y la entrada del usuario
        // no está validado, muestra un mensaje de error rojo debajo del texto de edición y
        // crea un mensaje de brindis que solicita al usuario que ingrese la información correcta
        if (isValidate && TextUtils.isEmpty(productNameString)) {
            layoutProductName.setError(getString(R.string.error_product_name));
            Toast.makeText(this, getString(R.string.empty_product_name),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutProductName.setErrorEnabled(false);
        }

        if (isValidate && TextUtils.isEmpty(rqString)) {
            layoutProductIsbn.setError(getString(R.string.error_product_isbn));
            // ocultar el teclado para permitir que un usuario vea qr editar el campo de texto
            hideKeyboard();
            Toast.makeText(this, getString(R.string.empty_product_isbn),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutProductIsbn.setErrorEnabled(false);
        }

        if (isValidate && TextUtils.isEmpty(priceString)) {
            layoutProductPrice.setError(getString(R.string.error_product_price));
            Toast.makeText(this, getString(R.string.empty_product_price),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutProductPrice.setErrorEnabled(false);
        }

        if (isValidate && TextUtils.isEmpty(quantityString)) {
            layoutProductQuantity.setError(getString(R.string.error_product_quantity));
            Toast.makeText(this, getString(R.string.empty_product_quantity),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutProductQuantity.setErrorEnabled(false);
        }

        if (isValidate && TextUtils.isEmpty(supplierNameString)) {
            layoutSupplierName.setError(getString(R.string.error_supplier_name));
            // ocultar el teclado para permitir que un usuario vea el nombre del proveedor editar el campo de texto
            hideKeyboard();
            Toast.makeText(this, getString(R.string.empty_supplier_name),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutSupplierName.setErrorEnabled(false);
        }

        if (isValidate && TextUtils.isEmpty(supplierPhoneString)) {
            layoutSupplierPhone.setError(getString(R.string.error_supplier_phone));
            // ocultar el teclado para permitir que un usuario vea el campo de texto de edición del teléfono del proveedor
            hideKeyboard();
            Toast.makeText(this, getString(R.string.empty_supplier_phone),
                    Toast.LENGTH_SHORT).show();
            return false;
        } else {
            layoutSupplierPhone.setErrorEnabled(false);
        }
        return true;
    }

    /**
     * Cuando se valida la verificación de la entrada del usuario, oculte el teclado para permitir que un usuario vea el campo en blanco.
     */
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
