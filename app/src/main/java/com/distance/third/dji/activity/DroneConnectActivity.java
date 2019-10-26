package com.distance.third.dji.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.distance.third.dji.MainApplication;
import com.distance.third.dji.R;
import com.distance.third.dji.model.DroneModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * @author 李文烙
 * @date 2019/10/26
 * @desc 无人机连接页面
 */
public class DroneConnectActivity extends Activity  implements View.OnClickListener, DroneModel.DJIProductStateCallBack {

    @BindView(R.id.text_connection_status)
    TextView textConnectionStatus;
    @BindView(R.id.text_product_info)
    TextView textProductInfo;
    @BindView(R.id.text_model_available)
    TextView textModelAvailable;
    @BindView(R.id.btn_open)
    Button btnOpen;
    @BindView(R.id.textView2)
    TextView textView2;
    @BindView(R.id.textView)
    TextView textView;

    private static final String TAG = DroneConnectActivity.class.getName();
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private KeyListener firmVersionListener;
    private DJIKey firmkey = ProductKey.create(ProductKey.FIRMWARE_PACKAGE_VERSION);
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private List<String> missingPermission = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_dji);
        ButterKnife.bind(this);
        initValue();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTitleBar();
    }

    @Override
    protected void onDestroy() {
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().removeListener(firmVersionListener);
        }
        super.onDestroy();
    }

    private void initValue() {
        isRegistrationInProgress = new AtomicBoolean(false);
        btnOpen.setEnabled(false);
    }

    private void initListener() {
        firmVersionListener = (oldValue, newValue) -> updateVersion();
        btnOpen.setOnClickListener(this);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        missingPermission = new ArrayList<>();
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            DroneModel.getInstance(this).setDjiProductStateCallBack(this);
            DroneModel.getInstance(this).startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void notifyStatusChange() {
        runOnUiThread(() -> refreshSDKRelativeUI());
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            DroneModel.getInstance(this).setDjiProductStateCallBack(this);
            DroneModel.getInstance(this).startSDKRegistration();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void updateTitleBar() {
        boolean ret = false;
        BaseProduct product = MainApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                showToast(MainApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        showToast("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.
            showToast("Disconnected");
        }
    }

    /**
     * showToast
     * @param msg msg
     */
    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(DroneConnectActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private void updateVersion() {
        if (MainApplication.getProductInstance() != null) {
            final String version = MainApplication.getProductInstance().getFirmwarePackageVersion();
            this.runOnUiThread(() -> {
                if (TextUtils.isEmpty(version)) {
                    textModelAvailable.setText("N/A"); //Firmware version:
                } else {
                    textModelAvailable.setText(version); //"Firmware version: " +
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btn_open: {
                Intent intent = new Intent(this, DronePreviewActivity.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }
    }


    private void refreshSDKRelativeUI() {

        BaseProduct product = MainApplication.getProductInstance();
        Log.v(TAG, "refreshSDKRelativeUI");

        if (null != product && product.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            btnOpen.setEnabled(true);

            String str = product instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            textConnectionStatus.setText("Status: " + str + " connected");

            if (null != product.getModel()) {
                textProductInfo.setText("" + product.getModel().getDisplayName());
            } else {
                textProductInfo.setText("产品信息");
            }
            if (KeyManager.getInstance() != null) {
                KeyManager.getInstance().addListener(firmkey, firmVersionListener);
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            btnOpen.setEnabled(false);

            textProductInfo.setText("产品信息");
            textConnectionStatus.setText("连接失败");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    @Override
    public void onAirPlaneStateChange(Boolean isConnect) {
        notifyStatusChange();
    }
}
