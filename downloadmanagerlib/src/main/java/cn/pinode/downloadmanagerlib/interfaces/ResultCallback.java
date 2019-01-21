package cn.pinode.downloadmanagerlib.interfaces;


import android.support.annotation.CallSuper;

//下载回调接口
    public abstract class ResultCallback<T> {
        private ResultCallback<T> callback;
        //下载错误
        @CallSuper
        public void onError(T task, Exception e){
            if (callback!=null){
                callback.onError(task, e);
            }
        }

        //下载成功
        @CallSuper
        public void onSuccess(T task){
            if (callback!=null){
                callback.onSuccess(task);
            }
        }

        //下载进度
        @CallSuper
        public void onProgress(long total, long current){
            if (callback!=null){
                callback.onProgress(total, current);
            }
        }

    public void setCallback(ResultCallback<T> callback) {
        this.callback = callback;
    }
}