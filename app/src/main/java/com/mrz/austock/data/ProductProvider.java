package com.mrz.austock.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import com.mrz.austock.data.ProductContract.ProductEntry;

/**
 * {@link ContentProvider} para la aplicación Inventory.
 */
public class ProductProvider extends ContentProvider{

    /** Código de coincidencia de URI para el URI de contenido de la tabla de productos */
    private static final int PRODUCTS = 100;

    /** Código de coincidencia de URI para el URI de contenido de un solo producto en la tabla de productos */
    private static final int PRODUCT_ID = 101;

    /**
     * Objeto UriMatcher para hacer coincidir un URI de contenido con un código correspondiente.
     * La entrada pasada al constructor representa el código que se devolverá para el URI raíz.
     * Es común usar NO_MATCH como entrada para este caso.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Inicializador estático. Esto se ejecuta la primera vez que se llama a algo desde esta clase.
    static {
        // Las llamadas a addURI () van aquí, para todos los patrones de URI de contenido que el proveedor
        // debería reconocer. Todas las rutas agregadas al UriMatcher tienen un código correspondiente para devolver
        // cuando se encuentra una coincidencia.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCT, PRODUCTS);
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCT + "/#", PRODUCT_ID);
    }

    /** TAG para los mensajes de registro */
    private static final String LOG_TAG = ProductProvider.class.getSimpleName();

    /** Objeto auxiliar de base de datos */
    private ProductDbHelper mDbHelper;

    /**
     * Inicialice el proveedor y el objeto auxiliar de la base de datos.
     */
    @Override
    public boolean onCreate() {
        // Create and initialize a ProductDbHelper object to gain access to the products database.
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    /**
     * Realice la consulta para el URI dado. Utilice la proyección, la selección, los argumentos de selección y el orden de clasificación dados.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Obtener una base de datos legible
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // Este cursor contendrá el resultado de la consulta
        Cursor cursor;

        // Averigüe si el comparador de URI puede hacer coincidir el URI con un código específico
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Para el código de PRODUCTOS, consulte la tabla de productos directamente con el
                // proyección, selección, argumentos de selección y orden de clasificación. El cursor
                // podría contener varias filas de la tabla de productos.
                // Realizar consulta de base de datos en la tabla de productos
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // Para el código PRODUCT_ID, extraiga el ID del URI.
                // Para un URI de ejemplo como "content: //com.example.android.inventory/products/3",
                // la selección será "_id =?" y el argumento de selección será un
                // Matriz de cadenas que contiene el ID real de 3 en este caso.
                //
                // Para cada "?" en la selección, necesitamos tener un elemento en la selección
                // argumentos que completarán el "?". Dado que tenemos 1 signo de interrogación en el
                // selección, tenemos 1 cadena en la matriz de cadenas de los argumentos de selección.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // Esto realizará una consulta en la tabla de productos donde el _id es igual a 3 para
                // devuelve un Cursor que contiene esa fila de la tabla.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Establecer URI de notificación en el Cursor,
        // para que sepamos para qué URI de contenido se creó el cursor.
        // Si los datos en este URI cambian, entonces sabemos que necesitamos actualizar el Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Devuelve el cursor
        return cursor;
    }

    /**
     * Devuelve el tipo de datos MIME para el URI de contenido.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    /**
     * Inserte nuevos datos en el proveedor con los ContentValues ​​proporcionados.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch(match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insertar un producto en la base de datos con los valores de contenido dados. Devuelve el nuevo URI de contenido
     * para esa fila específica en la base de datos.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Verifica que el nombre del producto no sea nulo
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Product requires a name");
        }
        // No es necesario verificar el editor, cualquier valor es válido (incluido el nulo).

        // Comprueba que el ISBN sea válido
        String isbn = values.getAsString(ProductEntry.COLUMN_PRODUCT_QR);
        if (isbn == null) {
            throw new IllegalArgumentException("Product requires valid ISBN");
        }
        // Comprueba que el precio sea válido
        Double price = values.getAsDouble(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Product requires a valid price");
        }
        // Si se proporciona la cantidad, verifique que sea mayor o igual a 0
        Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Product requires valid quantity");
        }
        // No es necesario comprobar la imagen, cualquier valor es válido

        // Verifica que el nombre del proveedor sea válido
        String supplierName = values.getAsString(ProductEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Product requires a supplier name");
        }
        // No es necesario consultar el correo electrónico del proveedor, cualquier valor es válido (incluido el nulo).

        // Compruebe que el número de teléfono del proveedor sea válido
        String supplierPhone = values.getAsString(ProductEntry.COLUMN_SUPPLIER_PHONE);
        if (supplierPhone == null) {
            throw new IllegalArgumentException("Product requires supplier phone number");
        }

        // Obtener la base de datos grabable
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Inserta el nuevo producto con los valores dados
        long id = database.insert(ProductEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notificar a todos los oyentes que los datos han cambiado para el URI del contenido del producto
        // uri: content: //com.example.android.inventory/products
        getContext().getContentResolver().notifyChange(uri, null);

        // Devuelve el nuevo URI con el ID (de la fila recién insertada) adjunto al final
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Elimina los datos en la selección dada y los argumentos de selección.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Obtener la base de datos grabable
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Seguimiento del número de filas que se eliminaron
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Elimina todas las filas que coinciden con la selección y los argumentos de selección
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Elimina una sola fila dada por el ID en el URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // Si se eliminaron 1 o más filas, notifique a todos los oyentes que los datos en el URI dado
        // ha cambiado.
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    /**
     * Actualiza los datos en la selección dada y los argumentos de selección, con los nuevos ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                // Para el código PRODUCT_ID, extraiga el ID del URI,
                // para que sepamos qué fila actualizar. La selección será "_id =?" y seleccion
                // Los argumentos serán una matriz de cadenas que contiene el ID real.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Actualizar productos en la base de datos con los valores de contenido dados. Aplicar los cambios a las filas.
     * especificado en la selección y los argumentos de selección (que pueden ser 0 o 1 o más productos).
     * Devuelve el número de filas que se actualizaron correctamente.
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Si la clave {@link ProductEntry # COLUMN_PRODUCT_NAME} está presente,
        // verifica que el valor del nombre no sea nulo.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        // No es necesario verificar el editor, cualquier valor es válido (incluido el nulo).

        // Si la clave {@link ProductEntry # COLUMN_PRODUCT_ISBN} está presente,
        // comprueba que el valor del ISBN no sea nulo.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QR)) {
            String isbn = values.getAsString(ProductEntry.COLUMN_PRODUCT_QR);
            if (isbn == null) {
                throw new IllegalArgumentException("Product requires valid ISBN");
            }
        }
        // Si la clave {@link ProductEntry # COLUMN_PRICE} está presente,
        // verifica que el valor del precio sea válido.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Double price = values.getAsDouble(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Product requires a valid price");
            }
        }
        // Si la clave {@link ProductEntry # COLUMN_QUANTITY} está presente,
        // verifica que el valor de la cantidad sea válido.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Product requires valid quantity");
            }
        }
        // No es necesario comprobar la imagen, cualquier valor es válido

        // Si la clave {@link ProductEntry # COLUMN_SUPPLIER_NAME} está presente,
        // verifica que el valor del nombre del proveedor no sea nulo.
        if (values.containsKey(ProductEntry.COLUMN_SUPPLIER_NAME)) {
            String supplierName = values.getAsString(ProductEntry.COLUMN_SUPPLIER_NAME);
            if (supplierName == null) {
                throw new IllegalArgumentException("Product requires a supplier name");
            }
        }
        // No es necesario consultar el correo electrónico del proveedor, cualquier valor es válido (incluido el nulo).

        // Si la clave {@link ProductEntry # COLUMN_SUPPLIER_PHONE_NUMBER} está presente,
        // verifica que el valor del número de teléfono del proveedor no sea nulo.
        if (values.containsKey(ProductEntry.COLUMN_SUPPLIER_PHONE)) {
            String supplierPhone = values.getAsString(ProductEntry.COLUMN_SUPPLIER_PHONE);
            if (supplierPhone == null) {
                throw new IllegalArgumentException("Product requires supplier phone number");
            }
        }

        // Si no hay valores para actualizar, no intente actualizar la base de datos
        if (values.size() == 0) {
            return 0;
        }

        // De lo contrario, obtenga una base de datos grabable para actualizar los datos
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Realice la actualización en la base de datos y obtenga el número de filas afectadas
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // Si se actualizaron 1 o más filas, notifique a todos los oyentes que los datos en el URI dado
        // ha cambiado
        if (rowsUpdated != 0 ) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Devuelve el número de filas actualizadas
        return rowsUpdated;
    }
}
