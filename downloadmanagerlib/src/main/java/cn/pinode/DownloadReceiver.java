package cn.pinode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import cn.pinode.downloadmanagerlib.Constants;
import cn.pinode.downloadmanagerlib.models.DownloadTask;

public class DownloadReceiver extends BroadcastReceiver {
    private Handler mDelivery = new Handler(Looper.getMainLooper());     //主线程返回
    private Receiver mReceiver;

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (mDelivery==null)
            mDelivery = new Handler(Looper.getMainLooper());
        if (mReceiver!=null){
            mDelivery.post(new Runnable() {
                @Override
                public void run() {
                    mReceiver.onReceiver(parseIntent(intent));
                }
            });
        }
    }

    public void setCallBack(Receiver receiver){
        mReceiver = receiver;
    }

    public DownloadTask parseIntent(Intent intent){
        if (intent!=null&&intent.getAction()!=null&&intent.getAction().equals(Constants.BROADCASTINTENT)){
            return DownloadTask.fromJson(intent.getStringExtra(Constants.DOWNLOADTASK));
        }
        return new DownloadTask();
    }

    public static DownloadReceiver registerReceiver(Context context, Receiver receiver){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCASTINTENT);
        DownloadReceiver downloadReceiver = new DownloadReceiver();
        downloadReceiver.mReceiver = receiver;
        context.registerReceiver(downloadReceiver, intentFilter);
        return downloadReceiver;
    }

    public interface Receiver{
        void onReceiver(DownloadTask task);
    }

}
