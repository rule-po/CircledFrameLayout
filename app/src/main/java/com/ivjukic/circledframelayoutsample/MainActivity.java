package com.ivjukic.circledframelayoutsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ivjukic.circledframelayout.CircledFrameLayout;

public class MainActivity extends AppCompatActivity {

    CircledFrameLayout circledFrameLayout;
    Button btnPlus, btnMinus, btnPlay;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        circledFrameLayout = findViewById(R.id.clCircle);
        circledFrameLayout.setProgressListener(new CircledFrameLayout.ProgressListener() {
            @Override
            public void onProgressChanged(float progress) {
                textView.setText(String.format("%.2f", progress));
            }
        });


        btnPlus = findViewById(R.id.btnProgressPlus);
        btnMinus = findViewById(R.id.btnProgressMinus);
        btnPlay = findViewById(R.id.btnAutomaticPlay);

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                circledFrameLayout.setProgressAnimate(circledFrameLayout.getProgress() + 10, true, 0, 150);
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                circledFrameLayout.setProgressAnimate(circledFrameLayout.getProgress() - 10, true, 0, 150);
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                circledFrameLayout.animateProgress(0, 100, 1000);
            }
        });

    }

}