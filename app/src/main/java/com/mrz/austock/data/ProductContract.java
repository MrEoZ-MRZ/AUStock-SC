package com.mrz.austock.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contrato de API para la aplicación Inventario.
 */

public final class ProductContract {

    // Para evitar que alguien cree accidentalmente una instancia de la clase de contrato,
    // darle un constructor vacío.
    private ProductContract() {
    }

    /**
     * La "Autoridad de contenido" es un nombre para todo el proveedor de contenido, similar al
     * Relación entre un nombre de dominio y su sitio web. Una cuerda conveniente para usar
     * autoridad de contenido es el nombre del paquete de la aplicación, que se garantiza que es único en el
     * dispositivo.
     */
    static final String CONTENT_AUTHORITY = "com.mrz";

    /**
     * Use CONTENT_AUTHORITY para crear la base de todos los URI que las aplicaciones usarán para contactar
     * el proveedor de contenido.
     */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Ruta posible (adjunta al URI del contenido base para posibles URI)
     * Por ejemplo, content: //com.example.android.inventory/products/ es una ruta válida para
     * mirando los datos del producto. content: //com.example.android.inventory/staff/ fallará,
     * ya que el ContentProvider no ha recibido ninguna información sobre qué hacer con el "personal".
     */
    static final String PATH_PRODUCT = "products";


    /**
     * Clase interna que define valores constantes para la tabla de la base de datos de productos.
     * Cada entrada en la tabla representa un solo producto.
     */
    public static final class ProductEntry implements BaseColumns {

        /** El URI de contenido para acceder a los datos del producto en el proveedor. */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCT);
        public static final String COLUMN_PRODUCT_MONEY = "recaudado";

        /**
         * El tipo MIME del {@link #CONTENT_URI} para una lista de productos.
         */
        static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCT;

        /**
         * El tipo MIME del {@link #CONTENT_URI} para un solo producto.
         */
        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCT;

        /** Nombre de la tabla de la base de datos para productos */
        public static final String TABLE_NAME = "products";
        public static final String MONEY_TABLE_NAME = "money";

        /** Nombre del producto. Teclee el texto */
        public static final String COLUMN_PRODUCT_NAME = "product_name";

        /** Precio del producto. Tipo: INTEGER */
        public static final String COLUMN_PRODUCT_PRICE = "price";

        /** Cantidad de producto. Tipo: INTEGER */
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";

        /** Imagen del producto.  */
        public static final String COLUMN_PRODUCT_IMAGE = "product_image";

        /** Nombre del proveedor. Teclee el texto */
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";

        /** Correo electrónico del proveedor. Teclee el texto */
        public static final String COLUMN_SUPPLIER_EMAIL = "supplier_email";

        /** Número de teléfono del proveedor. Teclee el texto */
        public static final String COLUMN_SUPPLIER_PHONE= "supplier_phone";

        /** Editorial del producto. Teclee el texto */
        public static final String COLUMN_PRODUCT_COST = "cost";

        /** QR del producto. Teclee el texto */
        public static final String COLUMN_PRODUCT_QR = "qr";
    }
}
