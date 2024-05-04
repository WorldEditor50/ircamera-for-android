package com.example.ircamera;

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    UsbReceiver receiver;
    Button startBtn;
    Button stopBtn;
    Button captureBtn;
    IRView irView;
    AtomicBoolean isReadyForCapture = new AtomicBoolean(false);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        startBtn = (Button) findViewById(R.id.btn_start);
        stopBtn = (Button) findViewById(R.id.btn_stop);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        irView = (IRView) findViewById(R.id.tv_image);
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        captureBtn.setOnClickListener(this);
        /* register broadcast receiver */
        IntentFilter intentFilter = getIntentFilter();
        receiver = new UsbReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

        IRCamera.getInstance().registerActivity(this);
        int width = 640;
        int height = 480;
        irView.setFixedSize(width, height);
        IRCamera.getInstance().setOnUpdateImage(width, height, new IRCamera.OnUpdateImage() {
            @Override
            public void process(int w, int h, byte[] img) {
                irView.updateImage(img);
                if (isReadyForCapture.get()) {
                    onCapture(w, h, img);
                    isReadyForCapture.set(false);
                }
            }
        });
    }

    @NonNull
    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        intentFilter.addAction(IRCamera.ACTION_USB_PERMISSION);
        intentFilter.addAction(IRCamera.ACTION_TEST);
        return intentFilter;
    }
    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

    }
    @Override
    protected void onResume() {
        super.onResume();
        IRCamera.getInstance().openDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IRCamera.getInstance().closeDevice();
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_start) {
            IRCamera.getInstance().openDevice();
        } else if (vid == R.id.btn_stop) {
            IRCamera.getInstance().closeDevice();
        } else if (vid == R.id.btn_capture) {
            //Intent intent = new Intent(IRCamera.ACTION_TEST);
            //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            if (!isReadyForCapture.get()) {
                isReadyForCapture.set(true);
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void onCapture(int w, int h, byte[] img) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String currentDate = simpleDateFormat.format(date);
        String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        String fileName = String.format("%s.jpg", currentDate);
        File file = new File(galleryPath, fileName);
        try {
            /* save image */
            FileOutputStream outputStream = new FileOutputStream(file);
            Bitmap bitmap = Bitmap.createBitmap(w, h, ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(img).position(0));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            /* display image */
            MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri imageUri = Uri.parse(MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    file.getAbsolutePath(),
                    null,
                    null
            ));
            intent.setDataAndType(imageUri, "image/*");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

    }
    public boolean isSupportUsbHost() {
        return this.getPackageManager().hasSystemFeature(
                "android.hardware.usb.host");
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "grant permission successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "failed to grant permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private boolean checkPermissions() {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void requestPermissions() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSION_REQUEST_CODE);
        }
    }

}