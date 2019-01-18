package cn.pinode.downloadmanagerlib.okhttp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import cn.pinode.downloadmanagerlib.DownloadManager;
import cn.pinode.downloadmanagerlib.IDownloadExecutor;
import cn.pinode.downloadmanagerlib.interfaces.ResultCallback;
import cn.pinode.downloadmanagerlib.models.DownloadTask;
import cn.pinode.downloadmanagerlib.models.State;
import cn.pinode.downloadmanagerlib.utils.DataHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpDownloader implements IDownloadExecutor {
    private static OkHttpDownloader instance = null;
    private final Context mContext;
    private Handler mDelivery;     //主线程返回
    private OkHttpClient mOkHttpClient;
    @SuppressLint("UseSparseArrays")
    private Map<Integer, Call> callMap = new HashMap<>();

    private OkHttpDownloader(Context context){
        mContext = context;
        mOkHttpClient = new OkHttpClient();
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
        Call call = callMap.get(id);
        if (call!=null){
            call.cancel();
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
        final Request request = buildRequest(task);
        //下载的call
        Call downCall = mOkHttpClient.newCall(request);
        callMap.put(task.getTaskId(), downCall);
        DownloadManager.getInstance().getStateCallBackMap().put(task.getTaskId(), callback);
        downCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sendFailedCallback(task, e, callback);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {

                InputStream is = null;
                byte[] buf = new byte[1024*100];
                int len;
                FileOutputStream fos = null;
                try {
                    long current = task.getDownloadedByte();
                    assert response.body() != null;
                    long total = response.body().contentLength();
                    if (total > task.getTotalByte()){
                        task.setTotalByte(total);
                    }
                    is = response.body().byteStream();
                    File file = new File(task.getDownloadDestination().getPath(), task.getFileName());
                    fos = new FileOutputStream(file, true); // 使用追加模式写入
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        task.setDownloadedByte(current); // 更新下载数据
                        sendProgressCallBack(task.getTotalByte(), current, callback);
                    }
                    fos.flush();
                    //如果下载文件成功，第一个参数为文件的绝对路径
                    sendSuccessResultCallback(task, callback);
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

            }
        });
    }

    private Request buildRequest(DownloadTask task){
        // 确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
        Request.Builder builder = new Request.Builder()
                .url(task.getUrl());
        if (task.getDownloadedByte()>0&&task.getTotalByte()>task.getDownloadedByte()){
            builder.addHeader("RANGE", "bytes=" +task.getDownloadedByte()+ "-" + task.getTotalByte());
        }

        return builder.build();
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
                    callback.onResponse(object);
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

    public static OkHttpDownloader getInstance(Context context) {

        if (instance==null) {
            instance = new OkHttpDownloader(context);
        }
        return instance;
    }

}
