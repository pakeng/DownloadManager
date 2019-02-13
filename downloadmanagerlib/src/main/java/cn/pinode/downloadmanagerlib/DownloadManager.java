package cn.pinode.downloadmanagerlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.pinode.DownloadService;
import cn.pinode.downloadmanagerlib.interfaces.ResultCallback;
import cn.pinode.downloadmanagerlib.models.DownloadTask;
import cn.pinode.downloadmanagerlib.models.State;
import cn.pinode.downloadmanagerlib.okhttp.OkHttpDownloader;
import cn.pinode.downloadmanagerlib.utils.DataHelper;

/**
 *  下载工具
 *
 */
public class DownloadManager {

    private static DownloadManager instance = null;
    private Context mContext;
    private SoftReference<Context> contextSoftReference;
    private IDownloadExecutor executor;
    private ConcurrentMap<Integer, ResultCallback> stateCallBackMap = new ConcurrentHashMap<>(); // 记录回调
    private DownloadService.DownloadServiceBinder binder ;
    private InitListener initListener;
    public final static int ERROR = 1000;
    public final static int SUCCESS = 0;

    private ServiceConnection cnn = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (DownloadService.DownloadServiceBinder) service;
            initContext();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public interface InitListener{
        void onSuccess(DownloadManager manager);
        void onError(int code, String msg);
    }

    private DownloadManager() {
        // 开启一个服务，然后获取对应的上下文
    }

    public boolean isInited(){
        if (mContext!=null){
            return true;
        }
        return false;
    }

    private void initContext() {
        mContext = binder.getService();
        executor = OkHttpDownloader.getInstance(mContext);
        if (initListener!=null){
            initListener.onSuccess(instance);
        }
    }

    public void startService(Context context){
//
        boolean result = context.getApplicationContext().bindService(new Intent(context, DownloadService.class), cnn, Context.BIND_AUTO_CREATE);
        if (!result&&initListener!=null){
            initListener.onError(ERROR, "bind services error");
        }
    }

    public void init(Context context, InitListener listener){
        initListener = listener;
        if (mContext!=null&&listener!=null){
            listener.onSuccess(instance);
        }
        contextSoftReference = new SoftReference<>(context.getApplicationContext());
        startService(context);
    }

    public static DownloadManager getInstance() {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null)
                    instance = new DownloadManager();
            }
        }
        return instance;
    }

    public void destroy(){
        DataHelper.getInstance(mContext).saveData();
        Context context = contextSoftReference.get();
        if (context!=null){
            context.unbindService(cnn);
        }
    }


    // TODO 初始化数据 读取本地已有的下载信息
    private void initData(){
        // 读取本地文件然后判断是否下载完成，记录是否正确
    }


    /**
     * 添加下载任务
     *
     * @param task 下载任务
     * @return 下载任务id
     */
    public int startDownloadTask(DownloadTask task) {
        task = getDownloadTask(task);
        if (task.getState() == State.DOWNLOADED){
            return task.getTaskId();
        }

        switch (task.getState()){
            case DOWNLOADED:
            case ERROR:
            case DOWNLOADING:
                return task.getTaskId();
            case UNKNOWN:
            case PRE_DOWNLOAD:
                task.setTaskId(DataHelper.getInstance(mContext).getNextDownloadId());
                break;
            case PAUSE:
                break;
        }

        executor.HttpDownload(task, new ResultCallback<DownloadTask>() {
            @Override
            public void onError(DownloadTask task, Exception e) {
                super.onError(task, e);
            }

            @Override
            public void onSuccess(DownloadTask task) {
                super.onSuccess(task);
            }

            @Override
            public void onProgress(long total, long current) {
                super.onProgress(total, current);
                Log.e("DOWNLOAD", "download progress = " + (current * 1.0f / total) * 100);
            }
        });
        return task.getTaskId();
    }

    /**
     * 下载文件
     *
     * @param url      文件链接
     * @param destDir  下载保存地址
     * @param callback 回调
     */
    public void downloadFile(String url, String destDir, ResultCallback callback) {
        executor.HttpDownload(url, destDir, callback);
    }

    /**
     * 下载文件
     * @param downloadTask  下载任务
     * @param callback 回调
     */
    public void downloadFile(DownloadTask downloadTask, ResultCallback callback) {
        executor.HttpDownload(downloadTask, callback);
    }

    /**
     * 下载文件
     * @param downloadTask  下载任务
     */
    public void downloadFile(DownloadTask downloadTask) {
        executor.HttpDownload(downloadTask.getUrl(), downloadTask.getDownloadDestination(),
                new ResultCallback<DownloadTask>(){

            @Override
            public void onError(DownloadTask task, Exception e) {
                super.onError(task, e);
            }

            @Override
            public void onSuccess(DownloadTask task) {
                super.onSuccess(task);
            }

            @Override
            public void onProgress(long total, long current) {
                super.onProgress(total, current);
            }
        });
    }

    /**
     * 获取task
     */
    public DownloadTask getDownloadTask(DownloadTask task){
        for (DownloadTask task1: DataHelper.getInstance(mContext).getDownloadTasks()){
            if (task1.getUrl().equalsIgnoreCase(task.getUrl())
                    &&task1.getDownloadDestination().equals(task.getDownloadDestination())){
                return task1;
            }
        }
        DataHelper.getInstance(mContext).putDownloadTask(task);
        return task;
    }


    /**
     * 取消下载
     */
    public void cancelDown(int id) {
        // TODO 根据ID查找到对应的下载任务然后取消

        executor.cancelDown(id);
    }


    public <T> ResultCallback<T> getDownloadStateCallBackById(int id){
        return stateCallBackMap.get(id);
    }

    public State getDownloadStateById(int id){
        return DataHelper.getInstance(mContext).getDownloadTasks().get(id).getState();
    }


    public DownloadTask getDownloadTaskById(int id){
        return DataHelper.getInstance(mContext).getDownloadTasks().get(id);
    }

    public ConcurrentMap<Integer, ResultCallback> getStateCallBackMap() {
        return stateCallBackMap;
    }

    public void setStateCallBackMap(ConcurrentMap<Integer, ResultCallback> stateCallBackMap) {
        this.stateCallBackMap = stateCallBackMap;
    }
}