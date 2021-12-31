package com.mrz.austock.activity;

import android.os.Bundle;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.mrz.austock.R;

public class AppIntroActivity  extends IntroActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title("HOLA")
                .description("TESTEO")
                .background(R.color.grey_20)
                .scrollable(false)
                .build());
    }
}