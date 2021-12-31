package com.mrz.austock.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.mrz.austock.R;
import com.mrz.austock.activity.Expenses.ExpenseActivity;

import java.util.Calendar;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AboutActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String version = null;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        @SuppressLint("DefaultLocale") View AboutPage = new AboutPage(this)
                .setDescription("AUStock es una aplicacion diseñada para la administracion, recuento de productos con identificacion de codigo de barras o codigo QR y supervision de ganacias y perdidas generadas")
                .setImage(R.drawable.ic_app_logo)
                .isRTL(false)
                .addItem(new Element().setTitle("Desarrolado por Nazareno Busi"))
                .addItem(new Element().setTitle("Version de la app "+version))
                .addGroup("Contactate conmigo")
                .addEmail("nazaabusi@gmail.com","Email")
                .addWebsite("https://mreoz-mrz.github.io/index.html","Sobre Mi")
                .addYoutube("MrEoZ","Youtube")
                .addGitHub("MrEoZ-MRZ","Github")
                .addInstagram("nazabusii_","Instagram")
                .addItem(new Element().setTitle(String.format("© Copyright %d MRZ", Calendar.getInstance().get(Calendar.YEAR))))
                .create();
        setContentView(AboutPage);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(AboutActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        return true;
    }
}