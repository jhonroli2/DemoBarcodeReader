package com.example.demobarcodereader;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cafsoft.colombianidinfo.ColombianIDInfo;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> actResultLauncher;
    private Button butScan;
    private TextView txtInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        ActivityResultContracts.StartActivityForResult actForRes;
        actForRes = new ActivityResultContracts.StartActivityForResult();
        actResultLauncher = registerForActivityResult(actForRes, result -> {
            switch (result.getResultCode()){
                case RESULT_OK:
                    ColombianIDInfo info;
                    info = (ColombianIDInfo) result.getData().getSerializableExtra("data");
                    txtInfo.setText(info.getFullname());
                    break;
                    }
                });
        initEvents();
    }

    public void initViews(){
        butScan = findViewById(R.id.butScan);
        txtInfo = findViewById(R.id.textInfo);
    }

    public void initEvents(){
        butScan.setOnClickListener(view -> {
            Intent intent = new Intent(this, BarcodeScannerActivity.class);
            actResultLauncher.launch(intent);
        });

    }
}