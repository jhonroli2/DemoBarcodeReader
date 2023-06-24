package com.example.demobarcodereader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cafsoft.colombianidinfo.ColombianIDInfo;


public class BarcodeScannerActivity extends AppCompatActivity {

    private ListenableFuture cameraProviderFuture;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private MyImageAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        previewView = findViewById(R.id.previewView);
        getWindow().setFlags(1024, 1024);
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        analyzer = new MyImageAnalyzer(getSupportFragmentManager());

        cameraProviderFuture.addListener(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(BarcodeScannerActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(BarcodeScannerActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                }else{
                    ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                    bindPreview(processCameraProvider);
                }

            } catch (ExecutionException e) {

            } catch (InterruptedException e) {

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider processCameraProvider) {

        Preview preview =new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(720,1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(cameraExecutor,analyzer);
        processCameraProvider.unbindAll();
        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==101 && grantResults.length>0){
            ProcessCameraProvider processCameraProvider;
            try {
                processCameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                bindPreview(processCameraProvider);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class MyImageAnalyzer implements ImageAnalysis.Analyzer{

        private FragmentManager fragmentManager;

        public MyImageAnalyzer (FragmentManager newFragmentManager){
            fragmentManager = newFragmentManager;
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            scanBarcode(image);

        }

        private void scanBarcode(ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError")
            Image image = imageProxy.getImage();
            assert image != null;

            InputImage inputImage = InputImage.fromMediaImage(image,imageProxy.getImageInfo().getRotationDegrees());

            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();
            BarcodeScanner scanner = BarcodeScanning.getClient(options);
            Task<List<Barcode>> result = scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                       readerBarcodeData(barcodes);
                    })
                    .addOnFailureListener(e -> {

                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        }

        private void readerBarcodeData(List<Barcode> barcodes) {
            for(Barcode barcode : barcodes){
                //Rect bounds = barcode.getBoundingBox();
                String rawValue = barcode.getRawValue();
                int valueType = barcode.getValueType();
                byte[] bytes = barcode.getRawBytes();
                String text = new String(bytes, StandardCharsets.ISO_8859_1);
                Log.d("BarcodeData", text);
                Log.d("BarcodeFormat", String.valueOf(barcode.getFormat()));

                try {
                    ColombianIDInfo info = ColombianIDInfo.decode(text);
                    Intent intent = new Intent();
                    intent.putExtra("data", info);
                    setResult(RESULT_OK, intent);
                    finish();

                }catch (Exception e){

                }
            }
        }
    }

}