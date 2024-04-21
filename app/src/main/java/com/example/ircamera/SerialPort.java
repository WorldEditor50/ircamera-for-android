package com.example.ircamera;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SerialPort {
    public static final String ACTION_USB_PERMISSION = "com.example.serial.action.USB_PERMISSION";
    public static final int REQ_USB_PERMISSION = 0;
    public interface OnDataReceive {
        void process(byte[] data) throws InterruptedException;
    }

    protected OnDataReceive onDataReceive = null;
    private String TAG = "SerialPort";
    protected UsbManager usbManager = null;
    protected List<UsbSerialDriver> availableDrivers = new ArrayList<UsbSerialDriver>();
    protected UsbSerialDriver usbSerialDriver = null;
    protected UsbDeviceConnection usbConnection = null;
    protected UsbSerialPort usbSerialPort = null;
    protected SerialInputOutputManager inputOutputManager = null;
    protected Context activity = null;
    protected boolean connectFlag = false;
    public void setOnDataReceive(OnDataReceive receive) {
        this.onDataReceive = receive;
    }
    public void registerActivity(@NonNull Context context) {
        activity = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }
    int openDevice(int vendorID, int productID) {
        if (connectFlag) {
            return 1;
        }
        availableDrivers.clear();
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            return -1;
        }
        /* find devices */
        usbSerialDriver = null;
        for (int i = 0; i < availableDrivers.size(); i++) {
            UsbSerialDriver driver = availableDrivers.get(i);
            UsbDevice device = driver.getDevice();
            if (device.getVendorId() == vendorID && device.getProductId() == productID) {
                usbSerialDriver = driver;
                break;
            }
        }
        usbSerialPort = usbSerialDriver.getPorts().get(0);
        /* request permission */
        UsbDevice device = usbSerialDriver.getDevice();
        if (!usbManager.hasPermission(device)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity.getApplicationContext(),
                    REQ_USB_PERMISSION, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(device, permissionIntent);
            return 2;
        }
        return 0;
    }

    void closeDevice() {
        if (inputOutputManager != null) {
            inputOutputManager.stop();
            inputOutputManager = null;
        }
        if (usbSerialPort != null) {
            usbSerialPort = null;
        }
        if (usbConnection != null) {
            usbConnection = null;
        }
        if (usbSerialDriver != null) {
            usbSerialDriver = null;
        }
        availableDrivers.clear();
        connectFlag = false;
    }

    public int write(byte[] data, int timeout) {
        if (usbSerialPort == null) {
            return -1;
        }
        if (!usbSerialPort.isOpen()) {
            return -2;
        }
        try {
            usbSerialPort.write(data, timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    int start(int baudRate, int databits, int stopbits, int parity, SerialInputOutputManager.Listener listener) {
        if (usbSerialDriver == null) {
            return -1;
        }
        /* connect device */
        UsbDevice device = usbSerialDriver.getDevice();
        usbConnection = usbManager.openDevice(device);
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, databits, stopbits, parity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!usbSerialPort.isOpen()) {
            return -2;
        }
        inputOutputManager = new SerialInputOutputManager(usbSerialPort, listener);
        inputOutputManager.start();
        connectFlag = true;
        return 0;
    }


    public boolean requestPermission(Context context, int vendorId, int productId) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            UsbDevice device = null;
            device = usbDevices.values().iterator().next();
            if (device.getVendorId() != vendorId || device.getProductId() != productId) {
                return false;
            }
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                    REQ_USB_PERMISSION, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(device, permissionIntent);
            return usbManager.hasPermission(device);
        }
        return false;
    }

    public static UsbDevice findUsbDevice(Context context, int vendorId, int productId) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for (UsbDevice device : usbDevices.values()) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }
}
