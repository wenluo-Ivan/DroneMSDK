package com.distance.third.dji.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.distance.third.dji.R;
import com.distance.third.dji.log.Logger;
import com.distance.third.dji.model.DroneModel;
import com.distance.third.dji.util.FileUtils;

import java.io.File;

import butterknife.BindView;

/**
 * @author 李文烙
 * @date 2019/10/26
 * @desc 无人机预览页面
 */
public class DronePreviewActivity extends Activity implements DroneModel.DJIProductStateCallBack, DroneModel.DJIVideoDataCallBack {

    private static final String TAG = "DJIPreviewActivity";

    @BindView(R.id.fl_dji_preview)
    FrameLayout videoPreviewFrameLayout;
    private SurfaceView videostreamPreviewSf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dji_preview);
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
        DroneModel.getInstance(this).setDjiVideoDataCallBack(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        DroneModel.getInstance(this).registerDJIVideoDataListener();
    }

    protected void initListeners() {
        DroneModel.getInstance(this).setDjiProductStateCallBack(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        long dblSampleTime = System.currentTimeMillis() / 1000;
        FileUtils.writeToLocal(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + "fsmeeting" + File.separator + "test.h264", videoBuffer);
    }
}
