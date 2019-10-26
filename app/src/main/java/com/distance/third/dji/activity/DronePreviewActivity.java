package com.distance.third.dji.activity;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.distance.third.dji.R;
import com.distance.third.dji.log.Logger;
import com.distance.third.dji.model.DroneModel;
import com.distance.third.dji.util.FileUtils;

import java.io.File;
import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author 李文烙
 * @date 2019/10/26
 * @desc 无人机预览页面
 */
public class DronePreviewActivity extends AppCompatActivity implements DroneModel.DJIProductStateCallBack,
        DroneModel.DroneH264VideoDataCallBack,DroneModel.DroneYUVDataCallBack, View.OnClickListener {

    private static final String TAG = "DJIPreviewActivity";

    @BindView(R.id.fl_dji_preview)
    FrameLayout videoPreviewFrameLayout;
    @BindView(R.id.btn_shoot_pic)
    Button btnShootPic;
    @BindView(R.id.btn_surface_preview)
    Button btnSurfacePreview;
    private SurfaceView videostreamPreviewSf;
    private long lastUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dji_preview);
        ButterKnife.bind(this);
        initValues();
        initListeners();
    }


    protected void initValues() {
        if (videostreamPreviewSf == null) {
            videostreamPreviewSf = new SurfaceView(this);
            videostreamPreviewSf.setVisibility(View.VISIBLE);
            videostreamPreviewSf.setBackgroundColor(Color.BLACK);
        }
        DroneModel.getInstance(this).registerDJIVideoDataListener();
        DroneModel.getInstance(this).videoPreview(videostreamPreviewSf);
        videoPreviewFrameLayout.addView(videostreamPreviewSf);
        videostreamPreviewSf.setZOrderOnTop(true);
        ViewGroup.LayoutParams layoutParams = videostreamPreviewSf.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videostreamPreviewSf.setLayoutParams(layoutParams);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DroneModel.getInstance(this).registerDJIVideoDataListener();
    }

    protected void initListeners() {
        DroneModel.getInstance(this).setDroneYUVDataCallBack(this);
        DroneModel.getInstance(this).setDjiVideoDataCallBack(this);
        DroneModel.getInstance(this).setDjiProductStateCallBack(this);
        btnShootPic.setOnClickListener(this);
        btnSurfacePreview.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DroneModel.getInstance(this).setDjiProductStateCallBack(null);
        DroneModel.getInstance(this).setDroneYUVDataCallBack(null);
        DroneModel.getInstance(this).setDjiVideoDataCallBack(null);
        DroneModel.getInstance(this).unRegisterDJIVideoDataListener();
        if (videostreamPreviewSf != null) {
            videoPreviewFrameLayout.removeView(videostreamPreviewSf);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onAirPlaneStateChange(Boolean isConnect) {
        Logger.info(TAG, "无人机连接回调：" + isConnect);
    }

    @Override
    public void onReceviedVideoData(byte[] videoBuffer, int size) {
        Logger.info(TAG, "无人机数据回调：" + size);
        FileUtils.writeToLocal(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "drone" + File.separator + "test.h264", videoBuffer);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_shoot_pic:
                if (btnShootPic.isSelected()) {
                    btnShootPic.setSelected(false);
                    DroneModel.getInstance(this).setReceviedYuvData(false);
                } else {
                    DroneModel.getInstance(this).setReceviedYuvData(true);
                }
                break;
            case R.id.btn_surface_preview:
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceviedYUVData(ByteBuffer yuvFrame, int dataSize, int width, int height) {
        if (System.currentTimeMillis() - lastUpdate > 1000) {
            lastUpdate = System.currentTimeMillis();
            Logger.debug(TAG, "onYuvDataReceived " + dataSize);
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);
            Logger.debug(TAG, "onYuvDataReceived width: " + width + " ,height:" + height);
            AsyncTask.execute(() -> FileUtils.saveYuvDataToJpeg(bytes, width, height));
        }
    }
}
