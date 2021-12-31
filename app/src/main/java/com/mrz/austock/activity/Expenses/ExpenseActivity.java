package com.mrz.austock.activity.Expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.mrz.austock.R;
import com.mrz.austock.activity.MainActivity;
import com.mrz.austock.activity.SettingsActivity;

public class ExpenseActivity extends AppCompatActivity {
    public static int activeExpenseId = -1;

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ExpenseActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            ExpenseListFragment expenseListFragment = new ExpenseListFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, expenseListFragment).commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(ExpenseActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        return true;
    }

    @Override
    public void onResume() {
        NotificationPublisher.scheduleNotification(this);
        super.onResume();
    }

}
