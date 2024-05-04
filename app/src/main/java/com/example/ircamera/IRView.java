package com.example.ircamera;

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IRView extends SurfaceView implements SurfaceHolder.Callback, Runnable{
    private final SurfaceHolder surfaceHolder;
    private boolean isRunning;
    private Bitmap bitmap = null;
    private final Lock lock = new ReentrantLock();

    public IRView(Context context) {
        this(context, null);
    }

    public IRView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IRView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setWillNotCacheDrawing(false);
        surfaceHolder.setKeepScreenOn(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
    }

    public void setFixedSize(int w, int h) {
        surfaceHolder.setFixedSize(w, h);
        bitmap = Bitmap.createBitmap(w, h, ARGB_8888);
    }

    public void updateImage(byte[] img) {
        synchronized (surfaceHolder) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(img).position(0));
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null && bitmap != null) {
                try {
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                    synchronized (surfaceHolder) {
                        canvas.drawBitmap(bitmap, 0, 0, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

}
