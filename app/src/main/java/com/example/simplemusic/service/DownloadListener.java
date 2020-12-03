package com.example.simplemusic.service;

public interface DownloadListener {

    //在接口中定义一系列抽象函数
    void onProgress(int progress);//法用于通知当前的下载进度
    void onSuccess();//用于通知下载成功事件
    void onFailed();//法用于通知下载失败事件
    void onPaused();//用于通知下载暂停事件
    void onCanceled();//用于通知下载取消事件

}
