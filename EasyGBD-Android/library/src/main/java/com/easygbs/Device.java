package com.easygbs;

import android.content.Context;

import org.easydarwin.push.Pusher;
import org.easydarwin.util.SIP;

public class Device implements Pusher {

    public static final int VIDEO_CODEC_NONE = 0;
    public static final int VIDEO_CODEC_H264 = 1;
    public static final int VIDEO_CODEC_MP4 = 2;
    public static final int VIDEO_CODEC_IPEG = 3;
    public static final int VIDEO_CODEC_H265 = 4;

    public static final int AUDIO_CODEC_NONE = 0;
    public static final int AUDIO_CODEC_G711A = 1;
    public static final int AUDIO_CODEC_G711U = 2;
    public static final int AUDIO_CODEC_G726 = 3;
    public static final int AUDIO_CODEC_AAC = 4;
    public static final int AUDIO_CODEC_G722 = 5;
    public static final int AUDIO_CODEC_OPUS = 6;

    private boolean pushed = false;

    static {
        System.loadLibrary("EasyGBSDevice");
    }

    public native static int getActiveDays(Context context, String key);

    public native static int activate(String key, Context context);

    public native int setVideoFormat(int codec, int width, int height, int frameRate);

    public native int setAudioFormat(int codec, int sampleRate, int channels, int bitPerSamples);

    /**
     * 创建链接
     *
     * @param serverIp          SIP服务器地址
     * @param serverPort        SIP服务器端口
     * @param serverId          SIP服务器ID
     * @param serverDomain      SIP服务器域
     * @param deviceId          SIP用户名
     * @param channelId         SIP用户认证ID
     * @param password          SIP用户认证密码
     * @param protocol          0:udp，1:tcp
     * @param regExpires        注册有效期
     * @param heartbeatInterval 心跳周期
     * @param heartbeatCount    最大心跳超时次数
     */
    public native int create(String serverIp, int serverPort, String serverId, String serverDomain,
                             String deviceId, String channelId, String password, int protocol,
                             int regExpires, int heartbeatInterval, int heartbeatCount);

    /**
     * pushVideo
     *
     * @param buffer
     * @param frameSize
     * @param keyframe  关键帧1 其他0
     */
    public native int pushVideo(byte[] buffer, int frameSize, int keyframe);

    public native int pushAudio(byte[] buffer, int frameSize, int nbSamples);

    public native int release();

    @Override
    public void initPush(SIP sip) {
        if (sip == null) {
            return;
        }

        create(sip.getServerIp(),
                sip.getServerPort(),
                sip.getServerId(),
                sip.getServerDomain(),
                sip.getDeviceId(),
                sip.getChannelId(),
                sip.getPassword(),
                sip.getProtocol(),
                sip.getRegExpires(),
                sip.getHeartbeatInterval(),
                sip.getHeartbeatCount());
        pushed = true;
    }

    @Override
    public void setVFormat(int codec, int width, int height, int frameRate) {
        setVideoFormat(codec, width, height, frameRate);
    }

    @Override
    public void setAFormat(int codec, int sampleRate, int channels, int bitPerSamples) {
        setAudioFormat(codec, sampleRate, channels, bitPerSamples);
    }

    @Override
    public void pushV(byte[] buffer, int length, int keyframe) {
        if (pushed) {
            pushVideo(buffer, length, keyframe);
        }
    }

    @Override
    public void pushA(byte[] buffer, int length, int nbSamples) {
        if (pushed) {
            pushAudio(buffer, length, nbSamples);
        }
    }

    @Override
    public void stop() {
        if (pushed) {
            release();
        }

        pushed = false;
    }

    /**
     * 回调函数
     *
     * @param prt
     * @param eventType
     * @param param
     * @param paramLength
     */
    public static void OnGB28181DeviceCALLBACK(int prt, int eventType, byte[] param, int paramLength) {
        if (callback != null) {
            callback.onCallback(eventType, OnInitPusherCallback.CODE.getName(eventType));
        }
    }

    public interface OnInitPusherCallback {
        void onCallback(int code, String name);

        class CODE {
            public static final int GB28181_DEVICE_EVENT_CONNECTING = 1;
            public static final int GB28181_DEVICE_EVENT_REGISTER_ING = 2;
            public static final int GB28181_DEVICE_EVENT_REGISTER_OK = 3;
            public static final int GB28181_DEVICE_EVENT_REGISTER_AUTH_FAIL = 4;
            public static final int GB28181_DEVICE_EVENT_START_VIDEO = 5;
            public static final int GB28181_DEVICE_EVENT_STOP_VIDEO = 6;
            public static final int GB28181_DEVICE_EVENT_TALK_AUDIO_DATA = 7;
            public static final int GB28181_DEVICE_EVENT_DISCONNECT = 8;

            public static String getName(int code) {
                String res;
                switch (code) {
                    case GB28181_DEVICE_EVENT_CONNECTING:
                        res = "连接中";
                        break;
                    case GB28181_DEVICE_EVENT_REGISTER_ING:
                        res = "注册中";
                        break;
                    case GB28181_DEVICE_EVENT_REGISTER_OK:
                        res = "注册成功";
                        break;
                    case GB28181_DEVICE_EVENT_REGISTER_AUTH_FAIL:
                        res = "注册鉴权失败";
                        break;
                    case GB28181_DEVICE_EVENT_START_VIDEO:
                        res = "开始视频";
                        break;
                    case GB28181_DEVICE_EVENT_STOP_VIDEO:
                        res = "停止视频";
                        break;
                    case GB28181_DEVICE_EVENT_TALK_AUDIO_DATA:
                        res = "对讲发过来的音频数据";
                        break;
                    case GB28181_DEVICE_EVENT_DISCONNECT:
                        res = "已断线";
                        break;
                    default:
                        res = "";
                        break;
                }
                return res;
            }
        }
    }

    private static OnInitPusherCallback callback;

    public static void setCallback(OnInitPusherCallback callback) {
        Device.callback = callback;
    }
}
