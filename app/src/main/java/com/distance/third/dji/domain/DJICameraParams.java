package com.distance.third.dji.domain;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;

/**
 * 无人机参数类
 * 这里仅使用了一部分参数,需要更多参数后续进行扩展
 */
public class DJICameraParams {

    // 相机工作模式：照片捕获、录像、播放、媒体下载、广播模式
    private SettingsDefinitions.CameraMode cameraMode;
    // 对焦模式：AUTO 、 AFC 模式，AFC模式仅支持X4S相机，Mavic 2 Zoom相机和Mavic 2 Pro相机。
    private SettingsDefinitions.FocusMode focusMode;
    // 视频标准值： 默认为NTSC
    private SettingsDefinitions.VideoStandard videoStandard;
    // 相机的物理方向。
    private SettingsDefinitions.Orientation orientation;
    // 帧速率：支持23.976、24、25、29.970、30、47.950、48、50、59.940、60、90、96、100、120、8.7fps
    // 分辨率：支持336*256、640*360、640*480、640*512、1280*720、1920*1080、2704*1520、2720*1530、3712*2088
    // 4096*2160、4608*2160、4608*2592、5280*2160、5760*3240、6016*3200、2048*1080、2688*1512、5280*2972
    private ResolutionAndFrameRate resolutionAndFrameRate;

    /**
     * 构造函数，含有初始化值
     */
    public DJICameraParams() {
        this.cameraMode = SettingsDefinitions.CameraMode.PLAYBACK;
        this.focusMode = SettingsDefinitions.FocusMode.AUTO;
        this.resolutionAndFrameRate = new ResolutionAndFrameRate(SettingsDefinitions.VideoResolution
                .RESOLUTION_640x480, SettingsDefinitions.VideoFrameRate.FRAME_RATE_23_DOT_976_FPS);
        this.videoStandard = SettingsDefinitions.VideoStandard.NTSC;
        this.orientation = SettingsDefinitions.Orientation.LANDSCAPE;
    }

    public ResolutionAndFrameRate getResolutionAndFrameRate() {
        return resolutionAndFrameRate;
    }

    public void setResolutionAndFrameRate(ResolutionAndFrameRate resolutionAndFrameRate) {
        this.resolutionAndFrameRate = resolutionAndFrameRate;
    }

    public SettingsDefinitions.CameraMode getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        this.cameraMode = cameraMode;
    }

    public SettingsDefinitions.FocusMode getFocusMode() {
        return focusMode;
    }

    public void setFocusMode(SettingsDefinitions.FocusMode focusMode) {
        this.focusMode = focusMode;
    }

    public SettingsDefinitions.VideoStandard getVideoStandard() {
        return videoStandard;
    }

    public void setVideoStandard(SettingsDefinitions.VideoStandard videoStandard) {
        this.videoStandard = videoStandard;
    }

    public SettingsDefinitions.Orientation getCameraOrientation() {
        return orientation;
    }

    public void setCameraOrientation(SettingsDefinitions.Orientation orientation) {
        this.orientation = orientation;
    }
}
