package com.example.ircamera;

import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IRCamera extends SerialPort {
    private final String TAG = "IRCamera";
    public static final String ACTION_TEST = "com.example.ircamera.action.TEST";
    private static final int MODE_PUSH = 0;
    private static final int MODE_REQ = 1;
    private static final int FREQ_8HZ = 0;
    private static final int FREQ_4HZ = 1;
    private static final int FREQ_2HZ = 2;
    public static int vendorId = 0x1a86;
    public static int productId = 0x7523;
    private static final int BAUD_RATE = 460800;//115200;//460800;
    private static final int DATABITS = 8;
    private static final int STOPBITS = UsbSerialPort.STOPBITS_1;
    private static final int PARITY = UsbSerialPort.PARITY_NONE;
    private static final int MAX_DATA_LENGTH = 1544;
    protected ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    protected byte[] packet = new byte[MAX_DATA_LENGTH];
    public int IMAGE_WIDTH = 640;
    public int IMAGE_HEIGHT = 480;
    public byte[] image = null;
    public interface OnUpdateImage {
        void process(int w, int h, byte[] data);
    }
    protected OnUpdateImage onUpdateImage = null;
    public class ProcessThread extends Thread {
        public static final int JOB_NONE = 0;
        public static final int JOB_FINISHED = 1;
        public static final int JOB_PREPEND = 2;
        public static final int JOB_READY_FOR_PROCESS = 3;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        public int jobState = JOB_PREPEND;
        public ProcessThread() {

        }
        /** @noinspection BusyWait*/
        public void run() {
            while (!isInterrupted()) {
                lock.lock();
                try {
                    while (jobState == JOB_PREPEND) {
                        condition.await();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    continue;
                }
                if (jobState == JOB_FINISHED) {
                    Log.i(TAG, "LEAVE IMAGE PROCESS THREAD");
                    lock.unlock();
                    break;
                } else if (jobState == JOB_READY_FOR_PROCESS) {
                    if (image != null) {
                        parseToImage(packet, packet.length, IMAGE_HEIGHT, IMAGE_WIDTH, image);
                        //Log.i(TAG, " PROCESS finished");
                        if (onUpdateImage != null) {
                            onUpdateImage.process(IMAGE_WIDTH, IMAGE_HEIGHT, image);
                        }
                    }
                }
                jobState = JOB_PREPEND;
                lock.unlock();
            }
        }
        void leave() {
           lock.lock();
           jobState = JOB_FINISHED;
           condition.signal();
           lock.unlock();
           interrupt();
        }
        void readyForProcess() {
            lock.lock();
            jobState = JOB_READY_FOR_PROCESS;
            condition.signal();
            lock.unlock();
        }
    }
    private ProcessThread processThread = null;

    static {
        System.loadLibrary("ircamera");
    }
    private static final IRCamera ircamera = new IRCamera();
    private IRCamera() {

    }
    public static IRCamera getInstance() {
        return ircamera;
    }

    private native void parseToImage(byte[] data, int len, int h, int w, byte[] image);
    public void openDevice() {
        int ret = openDevice(vendorId, productId);
        if (ret == 0) {
            start();
        } else if (ret == -1) {
            Toast.makeText(activity, "no device", Toast.LENGTH_SHORT).show();
        } else if (ret == 1) {
            Toast.makeText(activity, "device connected", Toast.LENGTH_SHORT).show();
        } else if (ret == 2) {
            Toast.makeText(activity, "no usb permission", Toast.LENGTH_SHORT).show();
        }
    }

    public void start() {
        outputStream.reset();
        int ret = start(BAUD_RATE, DATABITS, STOPBITS, PARITY, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                if (data[0]==0x5a && data[1]==0x5a && data[2]==0x02 && data[3]==0x06) {
                    outputStream.reset();
                }
                outputStream.write(data, 0, data.length);
                int bufferSize = outputStream.size();
                if (bufferSize >= MAX_DATA_LENGTH) {
                    if (processThread.jobState == ProcessThread.JOB_PREPEND) {
                        packet = outputStream.toByteArray();
                        //Log.i(TAG, "data ready");
                        processThread.readyForProcess();
                    }
                }
            }
            @Override
            public void onRunError(Exception e) {
                closeDevice();
                Log.e(TAG, "disconnected");
                e.printStackTrace();
            }
        });
        if (ret != 0) {
            Toast.makeText(activity, "failed to open device", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(activity, "open device successfully", Toast.LENGTH_SHORT).show();
        //setMode(MODE_PUSH);
        setEmissivity(0.95f);
        processThread = new ProcessThread();
        processThread.start();
    }
    @Override
    public void closeDevice() {
        super.closeDevice();
        disconnectFromView();
    }

    public void setOnUpdateImage(int w, int h, OnUpdateImage updateImage) {
        this.IMAGE_WIDTH = w;
        this.IMAGE_HEIGHT = h;
        this.image = new byte[w*h*4];
        this.onUpdateImage = updateImage;
    }
    public void disconnectFromView() {
        if (connectFlag && processThread != null) {
            processThread.leave();
        }
    }
    public void setFreq(int freq) {
        byte[] data = new byte[4];
        if (freq == FREQ_8HZ) {
            data[0] = (byte) 0xa5;
            data[1] = (byte) 0x25;
            data[2] = (byte) 0x01;
            data[3] = (byte) 0xcb;
        } else if (freq == FREQ_4HZ) {
            data[0] = (byte) 0xa5;
            data[1] = (byte) 0x25;
            data[2] = (byte) 0x02;
            data[3] = (byte) 0xcc;
        } else if (freq == FREQ_2HZ){
            data[0] = (byte) 0xa5;
            data[1] = (byte) 0x25;
            data[2] = (byte) 0x03;
            data[3] = (byte) 0xcd;
        }
        write(data, 100);
    }
    public void setMode(int mode) {
        byte[] data = new byte[4];
        if (mode == IRCamera.MODE_PUSH) {
            data[0] = (byte) 0xa5;
            data[1] = (byte) 0x35;
            data[2] = (byte) 0x01;
            data[3] = (byte) 0xdb;
        } else if(mode == IRCamera.MODE_REQ) {
            data[0] = (byte) 0xa5;
            data[1] = (byte) 0x35;
            data[2] = (byte) 0x02;
            data[3] = (byte) 0xdc;
        }
        write(data, 100);
    }
    public void setEmissivity(float value) {
        byte[] data = new byte[4];
        data[0] = (byte) 0xa5;
        data[1] = (byte) 0x45;
        data[2] = (byte) (value*100);
        data[3] = (byte) 0xdc;
        write(data, 100);
    }

}
