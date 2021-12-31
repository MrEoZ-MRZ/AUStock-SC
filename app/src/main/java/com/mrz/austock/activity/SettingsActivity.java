package com.mrz.austock.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import com.mrz.austock.R;
import com.mrz.austock.activity.Expenses.Expense;
import com.mrz.austock.activity.Expenses.ExpenseContract;
import com.mrz.austock.activity.Expenses.ExpenseDBHelper;
import com.mrz.austock.data.Preferencias;
import com.mrz.austock.data.ProductContract;
import com.mrz.austock.data.ProductDbHelper;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefrences);
        //asociar con el boton de resetar base de datos
        //eliminar ambas bases de datos si se solicita
        Preference button = (Preference)getPreferenceManager().findPreference("resetdb");
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    showDeleteConfirmationDialog();
                    return true;
                }
            });
        }
        //asociar con el boton de cerrar sesion
        //borrar los datos guardados y cerrar la sesion actual
        Preference butto = (Preference)getPreferenceManager().findPreference("logauth");
        if (butto != null) {
            butto.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    cerrarsesion();
                    return true;
                }
            });
        }
        //verificar si esta activado del swich de notificacion de productos
        SwitchPreference button2 = (SwitchPreference)getPreferenceManager().findPreference("productntf");
        if(Preferencias.with(SettingsActivity.this).read("PRODUCTOSEXCASOS").equals("TRUE")){
            button2.setChecked(true);
        }
        //activar o desactivar las preferencias de notificaciones de productos
        if (button2 != null) {
            button2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(button2.isChecked()){
                        Preferencias.with(SettingsActivity.this).write("PRODUCTOSEXCASOS","TRUE");
                        return true;
                    } else if (!button2.isChecked()){
                        Preferencias.with(SettingsActivity.this).write("PRODUCTOSEXCASOS","FALSE");
                        return false;
                    }
                    return false;
                }
            });
        }
        //verificar si el swich de inicio de sesion automatico esta activado
        SwitchPreference button3 = (SwitchPreference)getPreferenceManager().findPreference("autologin");
        if(Preferencias.with(SettingsActivity.this).read("AUTOLOGIN").equals("TRUE")){
            button3.setChecked(true);
        }
        //activar o desactivar preferencias
        if (button3 != null) {
            button3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(button3.isChecked()){
                        Preferencias.with(SettingsActivity.this).write("AUTOLOGIN","TRUE");
                        return true;
                    } else if (!button3.isChecked()){
                        Preferencias.with(SettingsActivity.this).write("AUTOLOGIN","FALSE");
                        return false;
                    }
                    return false;
                }
            });
        }
    }

    private void cerrarsesion() {
        Preferencias.with(this).write("SALIR","true");
        stopService(new Intent(this, AdminService.class));
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private void deleteAllProducts() {
        ProductDbHelper dbHelper = new ProductDbHelper(this);
        SQLiteDatabase sqLiteDatabase = dbHelper.getReadableDatabase();
        sqLiteDatabase.execSQL("UPDATE `" + ProductContract.ProductEntry.MONEY_TABLE_NAME + "` SET `" + ProductContract.ProductEntry.COLUMN_PRODUCT_MONEY + "` = '" + 0.0 + "' WHERE `" + ProductContract.ProductEntry._ID + "` = " + 1);
        Preferencias.with(SettingsActivity.this).write("MYDB","");
        ExpenseDBHelper db = ExpenseDBHelper.getInstance(SettingsActivity.this);
        SQLiteDatabase lolcito = db.getWritableDatabase();
        db.Delete(lolcito);
        int rowsDeleted = getContentResolver().delete(ProductContract.ProductEntry.CONTENT_URI,
                null, null);
    }

    private void showDeleteConfirmationDialog() {
        // Cree un AlertDialog.Builder y configure el mensaje, y haga clic en oyentes
        // para los botones positivos y negativos del diálogo.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                // El usuario hizo clic en el botón "Eliminar", así que elimine todos los productos.
                deleteAllProducts();
            }
        });

        // El usuario hizo clic en el botón "Cancelar", así que cierre el cuadro de diálogo y continúe mostrando
        // la lista de productos. Cualquier botón descartará el cuadro de diálogo emergente de forma predeterminada,
        // por lo que todo el OnClickListener es nulo.
        builder.setNegativeButton(R.string.cancel, null);

        // Crea y muestra el AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}