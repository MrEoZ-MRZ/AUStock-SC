package com.mrz.austock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mrz.austock.R;
import com.mrz.austock.activity.Expenses.Expense;
import com.mrz.austock.activity.Expenses.ExpenseContract;
import com.mrz.austock.activity.Expenses.ExpenseDBHelper;
import com.mrz.austock.data.Preferencias;
import com.mrz.austock.data.ProductContract.ProductEntry;
import com.mrz.austock.data.ProductDbHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mrz.austock.data.ProductContract.ProductEntry.MONEY_TABLE_NAME;

/**
 * DetailActivity muestra los detalles del producto que se almacenan en la base de datos.
 */
public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Tag para los mensajes de registro */
    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /** Identificador del cargador de datos de productos */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /** URI de contenido para el producto existente */
    private Uri mCurrentProductUri;

    /** TextView para el nombre del producto */
    @BindView(R.id.detail_product_name) TextView mProductNameTextView;


    /** Campo TextView para ingresar al proovedor */
    @BindView(R.id.detail_product_provider) TextView mPublisherTextView;

    /** Campo TextView para ingresar el RQ */
    @BindView(R.id.detail_product_qr) TextView mQRTextView;

    /** Campo TextView para ingresar el precio del producto */
    @BindView(R.id.detail_product_price) TextView mPriceTextView;

    /** Campo TextView para ingresar la cantidad del producto */
    @BindView(R.id.detail_product_quantity) TextView mQuantityTextView;

    /** ImageView para el producto */
    @BindView(R.id.detail_product_image) ImageView mImageView;

    /** Campo TextView para ingresar el nombre del proveedor */
    @BindView(R.id.detail_supplier_name) TextView mSupplierNameTextView;

    /** Campo TextView para ingresar el correo electrónico del proveedor */
    @BindView(R.id.detail_supplier_email) TextView mSupplierEmailTextView;

    /** Campo TextView para ingresar el número de teléfono del proveedor */
    @BindView(R.id.detail_supplier_phone) TextView mSupplierPhoneTextView;

    /** ImageButton para el correo electrónico del proveedor */
    @BindView(R.id.detail_email_button) ImageButton mSupplierEmailButton;

    private static final int MY_PERMISSONS_REQUEST_READ_CONTACTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mImageView = findViewById(R.id.detail_product_image);
        // Examine the intent that was used to launch this activity
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Bind the view using ButterKnife
        ButterKnife.bind(this);

        // Find all relevant button that we will need to increment and decrement the quantity
        Button plusButton = findViewById(R.id.detail_plus_button);
        Button minusButton = findViewById(R.id.detail_minus_button);
        ImageButton supplierPhoneButton = findViewById(R.id.detail_phone_button);

        // Set OnClickListener on the plus button. We can increment the available quantity displayed.
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increment();
            }
        });

        // Set OnClickListener on the minus button. We can decrement the available quantity displayed.
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decrement();
            }
        });
        mSupplierEmailButton = findViewById(R.id.detail_email_button);
        mSupplierEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create order message and email to the supplier of the product.
                composeEmail();
            }
        });
        supplierPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Make a phone call
                call();
            }
        });

        // Initialize a loader to read the product data from the database
        // and display the current values in the editor
        getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);

        // Allow Up navigation with the app icon in the app bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the contacts-related task you need to do.
                    Toast.makeText(this, getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    Toast.makeText(this, getString(R.string.permission_denied),
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other permissions this app might request
        }
    }

    /**
     * Inicie una llamada telefónica cuando se haga clic en el botón de teléfono del proveedor.
     */
    private  void call() {
        // Read from text field
        String phoneString = mSupplierPhoneTextView.getText().toString().trim();

        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(Uri.parse(getString(R.string.tel_colon) + phoneString));
        // Check whether the app has a given permission
        if (ActivityCompat.checkSelfPermission(DetailActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(DetailActivity.this,
                    Manifest.permission.CALL_PHONE)) {

            } else {
                // Request permission to be granted to this application
                ActivityCompat.requestPermissions(DetailActivity.this,
                        new String[]{ Manifest.permission.CALL_PHONE},
                        MY_PERMISSONS_REQUEST_READ_CONTACTS);
            }
            return;
        }
        startActivity(Intent.createChooser(phoneIntent, getString(R.string.make_a_phone_call)));
    }

    /**
     * Incrementar la cantidad disponible mostrada en 1.
     */
    private void increment() {
        // Read from text fields
        String quantityString = mQuantityTextView.getText().toString().trim();

        // Parse the string into an Integer value.
        int quantity = Integer.parseInt(quantityString);
        quantity = quantity + 1;

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the textView fields are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        // Update the product with content URI: mCurrentProductUri
        // and pass in the new ContentValues. Pass in null for the selection and selection args
        // because mCurrentProductUri will already identify the correct row in the database that
        // we want to modify.
        ProductDbHelper dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select * from " + ProductEntry.TABLE_NAME + " where " + ProductEntry.COLUMN_PRODUCT_QR + " = " + mQRTextView.getText().toString().trim(), null);
        Cursor cursor2 = sqLiteDatabase.rawQuery("select * from " + MONEY_TABLE_NAME + " where " +ProductEntry._ID +" = " + 1, null);
        if (cursor2 != null && cursor2.moveToFirst() && cursor != null && cursor.moveToFirst()){
            double Recaudado = cursor2.getDouble(cursor2.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MONEY));
            double Precio = cursor.getDouble(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_COST));
            String name = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
            double total = Recaudado - Precio;
            sqLiteDatabase.execSQL("UPDATE `" + ProductEntry.MONEY_TABLE_NAME + "` SET `" + ProductEntry.COLUMN_PRODUCT_MONEY + "` = '" + total + "' WHERE `" + ProductEntry._ID + "` = " + 1);
            Expense activeExpense = new Expense();
            activeExpense.setAmount("-"+ Precio);
            activeExpense.setCategory("Compra de " +name);
            activeExpense.setDescription("Usted compro una unidad individual del producto");
            activeExpense.setDate(Calendar.getInstance().getTimeInMillis());
            ExpenseDBHelper db = ExpenseDBHelper.getInstance(this);
            db.addExpense(activeExpense);
        }
        int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                null, null);
    }

    /**
     * Disminuya la cantidad disponible mostrada en 1 y verifique que no se muestren cantidades negativas.
     */
    private void decrement() {
        // Read from text fields
        String quantityString = mQuantityTextView.getText().toString().trim();

        // Parse the string into an Integer value.
        int quantity = Integer.parseInt(quantityString);
        // If the quantity is more than 0, decrement the quantity by 1.
        // If quantity is 0, show a toast message.
        if (quantity > 0) {
            quantity = quantity - 1;
        } else {
            Toast.makeText(DetailActivity.this, getString(R.string.detail_update_zero_quantity),
                    Toast.LENGTH_SHORT).show();
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the textView fields are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        // Update the product with content URI: mCurrentProductUri
        // and pass in the new ContentValues. Pass in null for the selection and selection args
        // because mCurrentProductUri will already identify the correct row in the database that
        // we want to modify.
        int rowsAffected = getContentResolver().update(mCurrentProductUri, values,
                null, null);
    }

    /**
     * Si un usuario hace clic en el botón de correo electrónico, crea un mensaje de pedido y un correo electrónico al proveedor del producto..
     */
    private void composeEmail() {
        // Read from text fields
        String productNameString = mProductNameTextView.getText().toString().trim();
        String publisherString = mPublisherTextView.getText().toString().trim();
        String isbnString = mQRTextView.getText().toString().trim();
        String[] supplierEmailString = {mSupplierEmailTextView.getText().toString().trim()};

        // Create order message
        String subject = getString(R.string.email_subject);
        String message = getString(R.string.place_an_order) + " " + getString(R.string.copies_of) +
                " " + productNameString + getString(R.string.period);
        message += getString(R.string.nn) + getString(R.string.app_product_details);
        message += getString(R.string.nn) + getString(R.string.category_product_name) +
                getString(R.string.colon) + " " + productNameString;
        message += getString(R.string.n) + getString(R.string.category_product_price) +
                getString(R.string.colon) + " $" + publisherString;
        message += getString(R.string.n) + getString(R.string.category_product_isbn) +
                getString(R.string.colon) + " "+ isbnString;
        message += getString(R.string.nn) + getString(R.string.best);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse(getString(R.string.mailto)));
        // Email address
        emailIntent.putExtra(Intent.EXTRA_EMAIL, supplierEmailString);
        // Email subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        // The body of the email
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        if(emailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.compose_email)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_edit:
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(DetailActivity.this, EditorActivity.class);
                // Set the current product URI on the data field of the intent
                intent.putExtra("Existente", "Si");
                intent.setData(mCurrentProductUri);
                // Launch the {@link EditorActivity} to display the data for the current product.
                startActivity(intent);
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Exit activity
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Realizar el borrado del producto en la base de datos.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null,
                    null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    /**
     * Solicitar al usuario que confirme que desea eliminar este producto..
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_COST,
                ProductEntry.COLUMN_PRODUCT_QR,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_SUPPLIER_PHONE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,             // Query the content URI for the current product
                projection,                        // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,               // No selection arguments
                null);                 // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int titleColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int publisherColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_COST);
            int isbnColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QR);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int supplierNameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_PHONE);

            // Extract out the value from the Cursor for the given column index
            String title = cursor.getString(titleColumnIndex);
            double publisher = cursor.getDouble(publisherColumnIndex);
            String isbn = cursor.getString(isbnColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            final String imageString = cursor.getString(imageColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);
            String supplierPhone = cursor.getString(supplierPhoneColumnIndex);

            // Update the views on the screen with the values from the database
            DecimalFormat formatter = new DecimalFormat("#,###.00");
            mProductNameTextView = findViewById(R.id.detail_product_name);
            mProductNameTextView.setText(title);
            mPublisherTextView = findViewById(R.id.detail_product_provider);
            mPublisherTextView.setText(formatter.format(publisher));
            mQRTextView = findViewById(R.id.detail_product_qr);
            mQRTextView.setText(isbn);
            mPriceTextView = findViewById(R.id.detail_product_price);
            mPriceTextView.setText(formatter.format(price));
            mQuantityTextView = findViewById(R.id.detail_product_quantity);
            mQuantityTextView.setText(String.valueOf(quantity));

            if(imageString != null) {
                // Attach a ViewTreeObserver listener to ImageView.
                ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        @SuppressLint({"NewApi", "LocalSuppress"}) byte[] arrayOfByte = Base64.getDecoder().decode(imageString.getBytes());
                        Bitmap bitmap = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                        mImageView.setImageBitmap(bitmap);
                    }
                });
            } else {
                mImageView.setImageResource(R.drawable.ic_image_black_24dp);
            }
            mSupplierNameTextView = findViewById(R.id.detail_supplier_name);
            mSupplierNameTextView.setText(supplierName);
            // If supplierEmail string is empty, hide the email button
            if(TextUtils.isEmpty(supplierEmail)) {
                mSupplierEmailButton.setVisibility(View.GONE);
            }
            mSupplierEmailTextView = findViewById(R.id.detail_supplier_email);
            mSupplierEmailTextView.setText(supplierEmail);
            mSupplierPhoneTextView = findViewById(R.id.detail_supplier_phone);
            mSupplierPhoneTextView.setText(supplierPhone);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mProductNameTextView.setText("");
        mPublisherTextView.setText("");
        mQRTextView.setText("");
        mPriceTextView.setText(String.valueOf(""));
        mQuantityTextView.setText(String.valueOf(""));
        mSupplierNameTextView.setText("");
        mSupplierEmailTextView.setText("");
        mSupplierPhoneTextView.setText("");
    }
}
