package com.example.simplemusic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.example.simplemusic.R;
import com.example.simplemusic.activity.OnlineMusicActivity;

import java.io.File;

public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;
    private DownloadBinder mBinder = new DownloadBinder();

    public class DownloadBinder extends Binder{
        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1, getNotifaction("开始下载...", 0, true));
                Toast.makeText(DownloadService.this, "开始下载...", Toast.LENGTH_SHORT).show();
            }
        }

        public void pauseDownload(){
            if(downloadTask != null){
                downloadTask.pauseDownload();
                Toast.makeText(DownloadService.this, "任务暂停", Toast.LENGTH_SHORT).show();
            }
        }

        public void cancelDownload(){
            if(downloadTask != null){
                downloadTask.cancelDownload();
            }
            if (downloadUrl != null){
                // 取消下载就将文件删除
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if(file.exists()){
                    file.delete();
                }
                // 并将通知关闭
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this, "下载被取消", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotifaction("下载中......", progress, false));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            // 下载成功则关闭下载中的通知
            stopForeground(true);
            // 显示下载成功的通知
            getNotificationManager().notify(1, getNotifaction("下载成功", -1, false));
            Toast.makeText(DownloadService.this, "下载完成啦", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            // 下载失败则关闭下载中的通知
            stopForeground(true);
            // 显示下载失败的通知
            getNotificationManager().notify(1, getNotifaction("下载失败", -1, false));
            Toast.makeText(DownloadService.this, "下载失败了", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this, "已暂停", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "任务取消", Toast.LENGTH_SHORT).show();
        }
    };


    private NotificationManager getNotificationManager() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 适配Android8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel("channelId", "name", NotificationManager.IMPORTANCE_HIGH);// 注意此处channellId要和前面一样
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return notificationManager;
    }

    private Notification getNotifaction(String title, int progress, Boolean isStartForeground) {
        Intent intent = new Intent(this, OnlineMusicActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId");
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setContentTitle(title);
        if(progress >= 0){
            // 当进度大于等于0时才显示下载进度
            builder.setContentText(progress+"%");
            // 参数一：通知最大进度
            // 参数二：当前进度
            // 参数三：是否模糊进度条
            builder.setProgress(100, progress, false);
        }
        if (isStartForeground){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = null;
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationChannel = new NotificationChannel("channelId", "name", NotificationManager.IMPORTANCE_HIGH);// 注意此处channellId要和前面一样
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        return builder.build();
    }
}