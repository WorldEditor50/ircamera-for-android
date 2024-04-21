package com.example.ircamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

public class UsbReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
            case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
                Toast.makeText(context, "usb device attached", Toast.LENGTH_SHORT).show();
                IRCamera.getInstance().openDevice();
                break;
            }
            case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
            case UsbManager.ACTION_USB_DEVICE_DETACHED: {
                Toast.makeText(context, "usb device detached", Toast.LENGTH_SHORT).show();
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getVendorId() == IRCamera.vendorId &&
                            device.getProductId() == IRCamera.productId) {
                        IRCamera.getInstance().closeDevice();
                    }
                }
                break;
            }
            case IRCamera.ACTION_USB_PERMISSION: {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Toast.makeText(context, "grant usb permission successfully.", Toast.LENGTH_SHORT).show();
                        IRCamera.getInstance().start();
                    } else {
                        Toast.makeText(context, "failed to grant usb permission.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case IRCamera.ACTION_TEST: {
                Toast.makeText(context, "receive test action", Toast.LENGTH_SHORT).show();
            }
            default:
                break;
        }
    }

}
