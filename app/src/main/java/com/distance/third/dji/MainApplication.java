package com.distance.third.dji;

import android.app.Application;
import android.content.Context;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainApplication extends Application {

    private static BaseProduct djiProduct;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == djiProduct) {
            djiProduct = DJISDKManager.getInstance().getProduct();
        }
        return djiProduct;
    }

    public static synchronized void updateProduct(BaseProduct product) {
        djiProduct = product;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        com.secneo.sdk.Helper.install(MainApplication.this);
    }

}
