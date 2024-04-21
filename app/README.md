# IRCamera

[TOC]



## 1. android开发总结
### 1.1 jni调用
- 约定动态库放在/app/src/main/cpp/jniLibs

- libopencv_java4.so依赖libc++_shared.so

  
### 1.2 导入jar包

- 约定jar包放在/app/libs

- 在/app/build.gradle.kts添加模块

  ```kotlin
  implementation(libs.appcompat)
  ```

  

### 1.3 java接口回调

```java
    public interface OnUpdate {
        void process(byte[] data);
    }
    private OnUpdate onUpdate;
    public void setOnUpdate(OnUpdate update) {onUpdate = update;}
```
### 1.4广播
#### 1.4.1 静态广播
- 在AndroidManifest注册广播
- 实现广播消息接收类
```java
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
```
#### 1.4.2 动态广播
- 动态注册广播
```java
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    receiver = new UsbReceiver();
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
```


## 2. MLX90640