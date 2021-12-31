package com.mrz.austock.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mrz.austock.activity.Expenses.ExpenseContract;
import com.mrz.austock.data.ProductContract.ProductEntry;
import static com.mrz.austock.data.ProductContract.ProductEntry.MONEY_TABLE_NAME;
import static com.mrz.austock.data.ProductContract.ProductEntry.TABLE_NAME;

/**
 * Ayudante de base de datos para la aplicación Inventario. Gestiona la creación de bases de datos y la gestión de versiones.
 */
public class ProductDbHelper extends SQLiteOpenHelper {

    /** Nombre del archivo de la base de datos */
    public static final String DATABASE_NAME = "AUStock.db";

    /** Versión de la base de datos. Si cambia el esquema de la base de datos, debe incrementar la versión de la base de datos. */
    private static final int DATABASE_VERSION = 1;

    /**
     * Construye una nueva instancia de {@link ProductDbHelper}.
     * @param context de la aplicación
     */
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Esto se llama cuando se crea la base de datos por primera vez.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cree una cadena que contenga la declaración SQL para crear la tabla de productos
        String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_COST + " REAL NOT NULL DEFAULT 0.0, "
                + ProductEntry.COLUMN_PRODUCT_QR + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_PRICE + " REAL NOT NULL DEFAULT 0.0, "
                + ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT, "
                + ProductEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_SUPPLIER_EMAIL + " TEXT, "
                + ProductEntry.COLUMN_SUPPLIER_PHONE + " TEXT NOT NULL);";

        String SQL_CREATE_RECAUDATED = "CREATE TABLE " + MONEY_TABLE_NAME + " ("
                + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_MONEY + " REAL NOT NULL DEFAULT 0.0);";

        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);

        db.execSQL(SQL_CREATE_RECAUDATED);

        db.execSQL(ExpenseContract.SQL_CREATE_ENTRIES);

        db.execSQL("INSERT INTO "+MONEY_TABLE_NAME+" ("+ ProductEntry.COLUMN_PRODUCT_MONEY +") VALUES ("+ 0.0 +")");
    }

    /**
     * Esto se llama cuando es necesario actualizar la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // La base de datos todavía está en la versión 1, por lo que no hay nada que hacer aquí.
    }
}
