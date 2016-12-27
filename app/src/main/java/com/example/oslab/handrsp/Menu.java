package com.example.oslab.handrsp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.oslab.handrsp.ImageProcessor.MainActivity;
import com.example.oslab.handrsp.databases.DbMainActivity;

/**
 * Created by oslab on 2016-11-25.
 */
public class Menu extends AppCompatActivity
{
    Button startBtn, desBtn, dbBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("혼자가위바위보");
        setContentView(R.layout.start_activity);

        regListener();
    }

    void regListener() {

        startBtn =  (Button)findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
        desBtn =  (Button)findViewById(R.id.desBtn);
        desBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Description.class);
                startActivity(intent);
            }
        });
        dbBtn =  (Button)findViewById(R.id.dbBtn);
        dbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DbMainActivity.class);
                intent.putExtra("pass", 0);
                startActivity(intent);
            }
        });
    }
}
