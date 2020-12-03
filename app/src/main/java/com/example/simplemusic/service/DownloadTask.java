package com.example.simplemusic.service;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// 三个泛型参数
// 第一个参数表示 执行该任务需要传入一个字符串
// 第二个参数表示 使用整型数据作为进度显示单位
// 第三个参数表示 使用整型数据来反馈执行结果
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    // 四个整型常量表示下载状态
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSE = 2;
    public static final int TYPE_CANCELLED = 3;

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    // 构造参数传入回调接口，下载状态通过这个回调
    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    // 这个方法中的代码都会在子线程中运行，在这里处理耗时任务。并返回执行结果【Integer类型】
    @Override
    protected Integer doInBackground(String... strings) {
        InputStream is = null;
        RandomAccessFile saveFile = null;
        File file = null;
        try {
            long downloadedlength = 0; // 记录下载的长度
            String downloadUrl = strings[0]; // 下载链接
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));  // 记录文件名
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(); // 获取下载位置
            file = new File(directory + fileName); // 绝对存储路径
            if (file.exists()) {
                downloadedlength = file.length();       // 文件存在则读取已下载的长度【断点续传功能】
            }
            long contentLength = getContentLength(downloadUrl); // 获取待下载文件的长度
            if (contentLength == 0) {
                return TYPE_FAILED;         // 长度为0，下载链接异常
            } else if (contentLength == downloadedlength) {
                return TYPE_SUCCESS;         // 长度相等，下载完成
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    // 断点续传
                    .addHeader("RANGE", "bytes=" + downloadedlength + "-")    //  传入请求头， 直接从第几字节开始下载
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();  // 字节流
                saveFile = new RandomAccessFile(file, "rw");    // 以读写的方式打开文件
                saveFile.seek(downloadedlength);    // 跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELLED;
                    } else if (isPaused) {
                        return TYPE_PAUSE;
                    } else {
                        total += len;
                        saveFile.write(b, 0, len);      // 写入下载的文件
                        int progress = (int) ((total + downloadedlength) * 100 / contentLength);  // 计算下载的百分比
                        publishProgress(progress);          // 执行UI相关操作【更新下载进度条】
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (saveFile != null) {
                    saveFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    // 获取待下载文件的总长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }

    // 调用了 publishProgress(progress...)，该方法很快就会被调用
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);      // 更新进度条
            lastProgress = progress;            // 记录下载进度
        }
    }

    // 当后台任务执行完毕后，返回的数据会传递到此方法中，可以对UI进行操作
    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSE:
                listener.onPaused();
                break;
            case TYPE_CANCELLED:
                listener.onCanceled();
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }
}
