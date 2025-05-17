package de.daubli.ndimonitor.uvc;

import android.hardware.usb.UsbDevice;
import de.daubli.ndimonitor.sources.VideoSource;

public class UVCSource implements VideoSource {

    UsbDevice usbDevice;

    public UVCSource(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    @Override
    public String getSourceName() {
        return usbDevice.getProductName();
    }
}
