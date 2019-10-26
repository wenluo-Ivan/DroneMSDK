package com.distance.third.dji.model;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.distance.third.dji.MainApplication;
import com.distance.third.dji.domain.DJICameraParams;
import com.distance.third.dji.log.Logger;
import com.distance.third.dji.util.FileUtils;
import com.distance.third.dji.util.HandlerUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

/**
 * @author 李文烙
 * @date 2019/10/26
 * @Desc 无人机Api调用模块类
 */
public class DroneModel {

    private static final String TAG = "DJIModel";
    private Context context;
    private long lastUpdate;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private Camera airPlaneCamera;
    private DJIProductStateCallBack djiProductStateCallBack;
    private VideoFeeder.VideoDataListener receviedVideoDataListener = null;
    private DJIVideoDataCallBack djiVideoDataCallBack;
    private static DroneModel djiInstance;
    private SurfaceHolder.Callback surfaceCallback;
    private DJICodecManager codecManager;

    private DroneModel(Context context) {
        this.context = context;
    }

    /**
     * 单例模式获取实例
     *
     * @param context 上下文
     * @return 单例模式
     */
    public static DroneModel getInstance(Context context) {
        if (djiInstance == null) {
            synchronized (DroneModel.class) {
                if (djiInstance == null) {
                    djiInstance = new DroneModel(context);
                }
            }
        }
        return djiInstance;
    }

    public void setDjiProductStateCallBack(DJIProductStateCallBack djiProductStateCallBack) {
        this.djiProductStateCallBack = djiProductStateCallBack;
    }

    public void setDjiVideoDataCallBack(DJIVideoDataCallBack djiVideoDataCallBack) {
        this.djiVideoDataCallBack = djiVideoDataCallBack;
    }

    /**
     * 设置无人机摄像头参数
     *
     * @param djiCameraParams 参数列表，仅支持分辨率、帧率等调节，具体请查看实现类
     * @param callback        设置回调
     */
    public void setAirPlaneCameraParam(DJICameraParams djiCameraParams, CommonCallbacks.CompletionCallback callback) {
        final BaseProduct product = MainApplication.getProductInstance();
        if (airPlaneCamera == null && product == null && product.getCamera() == null) {
            return;
        }
        airPlaneCamera.setVideoResolutionAndFrameRate(djiCameraParams.getResolutionAndFrameRate(), callback);
        airPlaneCamera.setVideoStandard(djiCameraParams.getVideoStandard(), callback);
        airPlaneCamera.setFocusMode(djiCameraParams.getFocusMode(), callback);
        airPlaneCamera.setMode(djiCameraParams.getCameraMode(), callback);
        airPlaneCamera.setOrientation(djiCameraParams.getCameraOrientation(), callback);
    }

    /**
     * 预览视频
     *
     * @param surfaceView 需要预览的视图
     */
    public void videoPreview(SurfaceView surfaceView) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager.destroyCodec();
            codecManager = null;
        }
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                if (codecManager == null) {
                    codecManager = new DJICodecManager(context, holder, surfaceView.getWidth(),
                            surfaceView.getHeight());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (codecManager != null) {
                    codecManager.cleanSurface();
                    codecManager.destroyCodec();
                    codecManager = null;
                }
                if (airPlaneCamera != null && VideoFeeder.getInstance().provideTranscodedVideoFeed() != null) {
                    VideoFeeder.getInstance().provideTranscodedVideoFeed().removeVideoDataListener(receviedVideoDataListener);
                }
            }
        };
        surfaceHolder.addCallback(surfaceCallback);
    }

    /**
     * 注册大疆SDK，如果不执行注册，则无法使用大疆的任何接口数据
     */
    public void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() ->
                    DJISDKManager.getInstance().registerApp(context, new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                Logger.error(TAG, DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                HandlerUtils.postToMain(() -> loginDJIUserAccount());
                                Logger.info(TAG, "Register SDK Success");
                            } else {
                                Logger.info(TAG, "Register sdk fails, check network is available");
                            }
                            Logger.info(TAG, djiError.getDescription());
                            isRegistrationInProgress.set(false);
                        }

                        @Override
                        public void onProductDisconnect() {
                            Logger.info(TAG, "Product Disconnected");
                            notifyStatusChange(false);
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            notifyStatusChange(true);
                            isRegistrationInProgress.set(false);
                            if (baseProduct != null) {
                                MainApplication.updateProduct(baseProduct);
                            }
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {
                            if (newComponent != null && oldComponent == null) {
                                Logger.info(TAG, componentKey.name() + ",index:" + newComponent.getIndex());
                            }
                            if (newComponent != null) {
                                newComponent.setComponentListener(isConnect -> {
                                    Logger.info(TAG, " Component " + (isConnect ? "connected" : "disconnected"));
                                    notifyStatusChange(isConnect);
                                });
                            }
                        }
                    }));
        }
    }

    /**
     * 登录大疆账号，国内必须执行登录账号后，方可使用无人机，每3个月登录一次
     */
    private void loginDJIUserAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(context, new CommonCallbacks
                .CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(final UserAccountState userAccountState) {
                Logger.info(TAG, "login success! Account state is:" + userAccountState.name());
            }

            @Override
            public void onFailure(DJIError error) {
                Logger.info(TAG, "login Fail! Error info:" + error.getDescription());
            }
        });

    }


    /**
     * 状态变更方法，根据无人机不同的产品状态来回调给使用者
     *
     * @param isConnect 是否连接
     */
    private void notifyStatusChange(Boolean isConnect) {
        if (djiProductStateCallBack != null) {
            djiProductStateCallBack.onAirPlaneStateChange(isConnect);
        }
        registerDJIVideoDataListener();
        if (isConnect) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().provideTranscodedVideoFeed().addVideoDataListener(receviedVideoDataListener);
            }
        }
    }

    /**
     * 注册大疆无人机视频数据监听事件
     */
    public void registerDJIVideoDataListener() {
        final BaseProduct product = MainApplication.getProductInstance();
        receviedVideoDataListener = (videoBuffer, size) -> {
            Log.d(TAG, "camera recv video data size: " + size);
            if (djiVideoDataCallBack != null) {
                djiVideoDataCallBack.onReceviedVideoData(videoBuffer, size);
            }
        };
        if (null == product || !product.isConnected()) {
            airPlaneCamera = null;
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                airPlaneCamera = product.getCamera();
                airPlaneCamera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, djiError -> {
                    if (djiError != null) {
                        Logger.info(TAG, "can't change mode of cam era, error:" + djiError.getDescription());
                    }
                });
                airPlaneCamera.setVideoResolutionAndFrameRate(new ResolutionAndFrameRate(
                        SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080, SettingsDefinitions.VideoFrameRate.FRAME_RATE_30_FPS), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Logger.info(TAG, "can't change mode of cam era, error:" + djiError.getDescription());
                        }
                    }
                });

                VideoFeeder.getInstance().provideTranscodedVideoFeed().addVideoDataListener(receviedVideoDataListener);
                //VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(receviedVideoDataListener);
            }
        }
    }

    /**
     * 关闭时，应该反注销下视频流的获取
     */
    public void unRegisterDJIVideoDataListener() {
        if (VideoFeeder.getInstance().provideTranscodedVideoFeed() != null) {
            VideoFeeder.getInstance().provideTranscodedVideoFeed().removeVideoDataListener(receviedVideoDataListener);
        }
    }

    /**
     * 大疆产品状态回调
     */
    public interface DJIProductStateCallBack {
        void onAirPlaneStateChange(Boolean isConnect);
    }

    /**
     * 大疆无人机H264视频数据回调
     */
    public interface DJIVideoDataCallBack {
        void onReceviedVideoData(byte[] videoBuffer, int size);
    }
}
