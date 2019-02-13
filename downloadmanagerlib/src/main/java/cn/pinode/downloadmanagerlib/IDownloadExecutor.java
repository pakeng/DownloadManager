package cn.pinode.downloadmanagerlib;

import android.net.Uri;

import cn.pinode.downloadmanagerlib.interfaces.ResultCallback;
import cn.pinode.downloadmanagerlib.models.DownloadTask;

public interface IDownloadExecutor {

    void cancelDown(int id);
    void HttpDownload(DownloadTask task, final ResultCallback callback);
    void HttpDownload(String url, String destDir, ResultCallback callback);
    void HttpDownload(String url, Uri destDir, ResultCallback callback);
}
