package com.example.pbl5client;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.dang_nhap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handle(R.layout.login);
            }
        });

        findViewById(R.id.dang_ky).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handle(R.layout.dangky);
            }
        });


    }

    public void handle(int type) {

        View loginView = getLayoutInflater().inflate(type, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(loginView);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Xử lý sự kiện click của nút trong dialog
        if (type == R.layout.login) {
            Button loginButton = loginView.findViewById(R.id.loginbtn);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Chuyển đến LoginActivity
                    Intent intent = new Intent(MainActivity.this, MatHang.class);
                    startActivity(intent);
                    dialog.dismiss();
                }
            });
        }



    }
}
