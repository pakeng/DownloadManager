package cn.pinode.downloadmanagerlib.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.pinode.downloadmanagerlib.models.DownloadTask;
import cn.pinode.downloadmanagerlib.models.State;
import cn.pinode.io.FileUtil;

/**
 * 数据工具
 */
public class DataHelper {

    private static DataHelper instance;
    private int nextId = -1;
    private Set<DownloadTask> downloadTaskSet = new HashSet<>();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(Uri.class, new UriSerializer())
            .registerTypeAdapter(Uri.class, new UriDeserializer())
            .create();
    private Context mContext;

    private final static String CONFIG_FILE = "download.cfg";
    private final static String CONFIG_FILE_OLD = "download.cfg.old";
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE_ID_RECORDER = "download_manager.cfg";
    private static final String CONFIG_FILE_ID_RECORDER_OLD = "download_manager.cfg.old";

    private DataHelper(Context context){
        mContext = context;
        initData();
    }

    public static DataHelper getInstance(Context context){
        if (instance==null){
            synchronized (DataHelper.class){
                if (instance==null)
                    instance = new DataHelper(context);
            }
        }
        return instance;
    }

    public int getNextDownloadId(){
        if (nextId<0)
            resetId();
        return nextId;
    }


    public synchronized List<DownloadTask> getDownloadTasks(){
        List<DownloadTask> list = new ArrayList<>(downloadTaskSet);
        Collections.sort(list);
        return list;
    }

    public synchronized List<DownloadTask> putDownloadTask(DownloadTask downloadTask){
        downloadTaskSet.add(downloadTask);
        return getDownloadTasks();
    }

    public synchronized List<DownloadTask> removeDownloadTask(DownloadTask downloadTask){
        downloadTaskSet.remove(downloadTask);
        return getDownloadTasks();
    }

    public synchronized List<DownloadTask> removeDownloadTaskById(int ID){
        DownloadTask downloadTask = null;
        for (DownloadTask task: downloadTaskSet){
            if (task.getTaskId() == ID){
                downloadTask = task;
                break;
            }
        }
        if (downloadTask!=null)
            downloadTaskSet.remove(downloadTask);
        return getDownloadTasks();
    }

    // 保存数据到本地文件
    public synchronized void saveData(){
        String data =  gson.toJson(downloadTaskSet);
        File configDir = FileUtil.getDestinationDir(mContext, CONFIG_DIR);
        File configFile = new File(configDir, CONFIG_FILE);
        File configFileOld = new File(configDir, CONFIG_FILE_OLD);
        // 备份文件
        if (configFile.exists()) {

            FileUtil.renameFile(configFile.getAbsolutePath(), configFileOld.getAbsolutePath());
        }
        // 写入新的内容
        boolean result = FileUtil.writeToFile(data, configDir.getAbsolutePath(), "/"+CONFIG_FILE);
            if (result){
            FileUtil.deleteFile(configFileOld.getAbsolutePath());
        }
        record_Id();
    }


    public synchronized void record_Id(){
        File configDir = FileUtil.getDestinationDir(mContext, CONFIG_DIR);

        File configFile = new File(configDir, CONFIG_FILE_ID_RECORDER);
        File configFileOld = new File(configDir, CONFIG_FILE_ID_RECORDER_OLD);
        // 备份文件
        if (configFile.exists()) {

            FileUtil.renameFile(configFile.getAbsolutePath(), configFileOld.getAbsolutePath());
        }
        // 写入新的内容
        boolean result = FileUtil.writeToFile(nextId+"", configDir.getAbsolutePath(), "/"+CONFIG_FILE_ID_RECORDER);
        if (result){
            FileUtil.deleteFile(configFileOld.getAbsolutePath());
        }


    }

    public void resetId(){
        File configDir = FileUtil.getDestinationDir(mContext, CONFIG_DIR);
        File configFile = new File(configDir, CONFIG_FILE_ID_RECORDER);
        File configFileOld = new File(configDir, CONFIG_FILE_ID_RECORDER_OLD );

        String data = "";
        if (configFile.exists()){
            data = FileUtil.getString(configFile.getAbsolutePath(),"");
        }else if (configFileOld.exists()){
            data = FileUtil.getString(configFileOld.getAbsolutePath(),"");
        }
        if (TextUtils.isEmpty(data)){
            nextId = 0;
        }else {
            data = data.replaceAll("\r|\n", "");
            nextId = Integer.valueOf(data);
        }
    }

    // 读取本地文件
    public synchronized void initData() {
        File configDir = FileUtil.getDestinationDir(mContext, CONFIG_DIR);
        File configFile = new File(configDir, CONFIG_FILE);
        File configFileOld = new File(configDir, CONFIG_FILE_OLD );

        String data = "";
        if (configFile.exists()){
            data = FileUtil.getString(configFile.getAbsolutePath(),"");
        }else if (configFileOld.exists()){
            data = FileUtil.getString(configFileOld.getAbsolutePath(),"");
        }

        Type type = new TypeToken <HashSet<DownloadTask>>(){}.getType();
        downloadTaskSet = gson.fromJson(data, type);

        if (configFile.exists()&&configFileOld.exists()){
            data = FileUtil.getString(configFile.getAbsolutePath(),"");
            downloadTaskSet = gson.fromJson(data, type);
            FileUtil.deleteFile(configFile.getAbsolutePath());
            FileUtil.renameFile(configFileOld.getAbsolutePath(), configFile.getAbsolutePath());
        }
        if (downloadTaskSet==null)
            downloadTaskSet = new HashSet<>();
        for (DownloadTask task: downloadTaskSet){
            if (task.getState()==State.DOWNLOADING){
                task.setState(State.PAUSE);
            }
        }
    }
}
