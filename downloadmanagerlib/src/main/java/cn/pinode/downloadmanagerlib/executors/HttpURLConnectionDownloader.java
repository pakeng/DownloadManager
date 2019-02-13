package cn.pinode.downloadmanagerlib.executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.pinode.downloadmanagerlib.Constants;
import cn.pinode.downloadmanagerlib.DownloadManager;
import cn.pinode.downloadmanagerlib.IDownloadExecutor;
import cn.pinode.downloadmanagerlib.interfaces.ResultCallback;
import cn.pinode.downloadmanagerlib.models.DownloadTask;
import cn.pinode.downloadmanagerlib.models.State;
import cn.pinode.downloadmanagerlib.utils.DataHelper;


public class HttpURLConnectionDownloader implements IDownloadExecutor {
    private static HttpURLConnectionDownloader instance = null;
    private final Context mContext;
    private Handler mDelivery;     //主线程返回

    @SuppressLint("UseSparseArrays")
    private Map<Integer, HttpURLConnection> callMap = new HashMap<>();

    private HttpURLConnectionDownloader(Context context){
        mContext = context;
        mDelivery = new Handler(Looper.getMainLooper());
    }

    /**
     * 异步下载文件
     *
     * @param url         文件的下载地址
     * @param destination 本地文件存储的Uri
     * @param callback 回调
     */
    public void HttpDownload(final String url, final Uri destination, final ResultCallback callback) {
        HttpDownload(url, destination.getPath(), callback);
    }
    /**
     * 异步下载文件
     *
     * @param url         文件的下载地址
     * @param destFileDir 本地文件存储的文件夹
     * @param callback  回调
     */
    public void HttpDownload(final String url, final String destFileDir, final ResultCallback callback){
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.setUrl(url);
        downloadTask.setDownloadDestination(Uri.fromFile(new File(destFileDir)));
        downloadTask.setTaskId(DataHelper.getInstance(mContext).getNextDownloadId());
        HttpDownload(downloadTask, callback);
    }


    @Override
    public void cancelDown(int id) {
        DownloadTask task = DataHelper.getInstance(mContext).getDownloadTaskById(id);
        if (task==null)
            return;
        task.setCanceled(true);
    }

    private void disconnect(int id){
        HttpURLConnection call = callMap.get(id);
        if (call!=null){
            call.disconnect();
        }
        callMap.remove(id);
    }

    /**
     * 异步下载文件
     * @param task 下载任务
     * @param callback 回调
     */
    @Override
    public void HttpDownload(final DownloadTask task, final ResultCallback callback) {
        DownloadManager.getInstance().getStateCallBackMap().put(task.getTaskId(), callback);
        task.setCanceled(false);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                runInBackgroundThread(task, callback);
            }
        }, 0, TimeUnit.MILLISECONDS);

    }

    private void runInBackgroundThread(final DownloadTask task, final ResultCallback callback) {
        final HttpURLConnection connection = buildRequest(task);
        if(connection==null) {
            task.setState(State.ERROR);
            sendFailedCallback(task, new NullPointerException("connect == null"), callback);
            return;
        }

        //下载的call
        callMap.put(task.getTaskId(), connection);

        try {

            InputStream is = connection.getInputStream();
            byte[] buf = new byte[1024*100];
            int len;
            FileOutputStream fos = null;
            try {
                long current = task.getDownloadedByte();
                long total;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    total = connection.getContentLengthLong();
                }else {
                    total = connection.getContentLength();
                }

                if (total > task.getTotalByte()){
                    task.setTotalByte(total);
                }

                File file = new File(task.getDownloadDestination().getPath(), task.getFileName());
                fos = new FileOutputStream(file, true); // 使用追加模式写入
                while ((len = is.read(buf)) != -1&&!task.isCanceled()) {
                    current += len;
                    fos.write(buf, 0, len);
                    task.setDownloadedByte(current); // 更新下载数据
                    sendProgressCallBack(task.getTotalByte(), current, callback);
                }
                fos.flush();

                if (task.isCanceled()){
                    // 取消下载
                    disconnect(task.getTaskId());
                    task.setState(State.PAUSE);
                    sendFailedCallback(task, new Exception("canceled"),callback);
                }else{
                    // 下载成功
                    sendSuccessResultCallback(task, callback);
                }
            } catch (IOException e) {
                task.setState(State.PAUSE);
                sendFailedCallback(task, e, callback);
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HttpURLConnection buildRequest(String task_url) {
        URL url;
        try {
            url = new URL(task_url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setInstanceFollowRedirects(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return connection;

    }
    private HttpURLConnection buildRequest(DownloadTask task){
        HttpURLConnection connection = buildRequest(task.getUrl());
        if (connection==null)
            return null;
        // 确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
        if (task.getDownloadedByte()>0&&task.getTotalByte()>task.getDownloadedByte()){
            connection.addRequestProperty("RANGE", "bytes=" +task.getDownloadedByte()+ "-" + task.getTotalByte());
        }
        try {
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return connection;
    }



    //下载失败ui线程回调
    private void sendFailedCallback(final DownloadTask request, final Exception e, final ResultCallback callback) {
        DataHelper.getInstance(mContext).saveData();
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null)
                    callback.onError(request, e);
            }
        });
    }

    //下载成功ui线程回调
    private void sendSuccessResultCallback(final DownloadTask object, final ResultCallback callback) {
        DataHelper.getInstance(mContext).saveData();
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(object);

                    mContext.sendBroadcast(buildIntent(object));
                }
            }
        });
    }

    /**
     * 进度信息ui线程回调
     *
     * @param total    总计大小
     * @param current  当前进度
     * @param callBack 回调
     * @param <T> 类型
     */
    private <T> void sendProgressCallBack(final long total, final long current, final ResultCallback<T> callBack) {
        DataHelper.getInstance(mContext).saveData();
        mDelivery.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onProgress(total, current);
                }
            }
        });
    }

    public static HttpURLConnectionDownloader getInstance(Context context) {

        if (instance==null) {
            instance = new HttpURLConnectionDownloader(context);
        }
        return instance;
    }

    private Intent buildIntent(DownloadTask task){
        Intent intent = new Intent();
        intent.setAction(Constants.BROADCASTINTENT);
        intent.putExtra(Constants.DOWNLOADTASK, task.toString());
        return intent;
    }

}
