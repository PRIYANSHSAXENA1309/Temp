package com.example.xraytextextractor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class welcome_screen extends AppCompatActivity {

    CardView cardView;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cardView = findViewById(R.id.cardView);
        imageView = findViewById(R.id.arrow);

        cardView.post(new Runnable() {
            @Override
            public void run() {
                int width = cardView.getWidth();
                float radius = width / 6f;

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setColor(Color.parseColor("#A91486"));

                shape.setCornerRadii(new float[]{
                        0f, 0f,
                        0f, 0f,
                        radius, radius,
                        radius, radius
                });

                cardView.setBackground(shape);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent next = new Intent(welcome_screen.this, MainActivity.class);
                startActivity(next);
                finish();
            }
        });
    }
}