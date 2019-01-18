package cn.pinode.downloadmanagerlib.models;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import cn.pinode.downloadmanagerlib.utils.FileProviderHelper;

/**
 * 下载任务
 */
public class DownloadTask implements Comparable<DownloadTask> {

    private int taskId = -1;
    private long downloadedByte;
    private long totalByte;
    private Uri downloadDestination;
    private String fileName;
    private String url;
    private State state = State.UNKNOWN;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public long getDownloadedByte() {
        return downloadedByte;
    }

    public void setDownloadedByte(long downloadedByte) {
        if (totalByte == downloadedByte)
            state = State.DOWNLOADED;
        else
            state = State.DOWNLOADING;
        this.downloadedByte = downloadedByte;
    }

    public long getTotalByte() {
        return totalByte;
    }


    public Uri getDownloadDestination() {
        return downloadDestination;
    }

    public String getFileName() {
        return getFileName(url);
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDownloadDestination(Context context, File file) {
        downloadDestination = FileProviderHelper.getUriForFile(context, file);
    }

    public void setTotalByte(long totalByte) {
        state = State.PRE_DOWNLOAD;
        this.totalByte = totalByte;
    }

    public void setDownloadDestination(Uri downloadDestination) {
        this.downloadDestination = downloadDestination;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    //TODO
    private String getFileName(String url) {
        try {
            URL Url = new URL(url);
            String fileName = Url.getFile();
            return fileName.substring(fileName.lastIndexOf("/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis()+"";
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int compareTo(DownloadTask Other) {

        if (taskId>Other.getTaskId()){
            return 1;
        }if (taskId<Other.getTaskId()){
            return -1;
        } else {
            return 0;
        }
    }
}
