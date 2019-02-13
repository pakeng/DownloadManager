package cn.pinode.downloaddemo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import cn.pinode.DownloadReceiver
import cn.pinode.downloadmanagerlib.DownloadManager
import cn.pinode.downloadmanagerlib.interfaces.ResultCallback
import cn.pinode.downloadmanagerlib.models.DownloadTask
import cn.pinode.downloadmanagerlib.models.State
import cn.pinode.downloadmanagerlib.utils.APPUtil
import cn.pinode.io.FileUtil

class MainActivity : Activity() {
    var download1_id: Int = 0
    var download2_id: Int = 0
    var downloadReceiver: DownloadReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val d1url = "http://192.168.3.251:8080/download2/readme.pdf"
        val d2url = "http://192.168.3.251:8080/download2/接入文档.pdf"
        val d3url = "http://192.168.3.251:8080/download2/bigfile.zip"
        val d4url = "http://192.168.3.251:8080/download2/app-debug.apk"

        downloadReceiver = DownloadReceiver.registerReceiver(this
        ) {task: DownloadTask -> onDownloadSuccess(task) }


        val d1:Button = findViewById(R.id.d1_btn)
        val d1_cancel:Button = findViewById(R.id.d1_cancel)
        val d2:Button = findViewById(R.id.d2_btn)
        val d2_cancel:Button = findViewById(R.id.d2_cancel)
        val d1_progresser: ProgressBar = findViewById(R.id.download1)
        val d2_progresser: ProgressBar = findViewById(R.id.download2)
        val d1_tv: TextView = findViewById(R.id.d1_tv)
        d1_tv.text = "0%"
        val d2_tv: TextView = findViewById(R.id.d2_tv)
        d2_tv.text = "0%"
        // fix address
        d1.setOnClickListener {download(d3url, object :NextAction{
            override fun next(id: Int) {
                if(DownloadManager.getInstance().getDownloadStateById(id)==State.DOWNLOADED){
                    onDownloadSuccess(DownloadManager.getInstance().getDownloadTaskById(id))
                    return
                }

                val callback1: ResultCallback<DownloadTask> = DownloadManager.getInstance()
                        .getDownloadStateCallBackById(id)
                callback1.setCallback(object :ResultCallback<DownloadTask>(){

                    @SuppressLint("SetTextI18n")
                    override fun onProgress(total: Long, current: Long) {
                        super.onProgress(total, current)
                        d1_tv.text = String.format("%.2f", (current*100/total).toFloat()) + "%"
                        d1_progresser.progress = (current*1.0f /total *100).toInt()
                    }
                })
            }
        })
        }
        d1_cancel.setOnClickListener {
            if(download1_id<0)
                return@setOnClickListener
            cancel(download1_id) }
        d2.setOnClickListener {
            download(d4url, object :NextAction{
                override fun next(id: Int) {
                    if(DownloadManager.getInstance().getDownloadStateById(id)==State.DOWNLOADED){
                        onDownloadSuccess(DownloadManager.getInstance().getDownloadTaskById(id))
                        return
                    }

                    val callback2: ResultCallback<DownloadTask> = DownloadManager.getInstance()
                            .getDownloadStateCallBackById(id)
                    callback2.setCallback(object :ResultCallback<DownloadTask>(){

                        @SuppressLint("SetTextI18n")
                        override fun onProgress(total: Long, current: Long) {
                            super.onProgress(total, current)
                            d2_tv.text = String.format("%.2f", (current*100 / total.toFloat())) + "%"
                            d2_progresser.progress = (current*100 /total).toInt()
                        }
                    })
                }
            })
        }
        d2_cancel.setOnClickListener {
            if(download2_id<0)
                return@setOnClickListener
            cancel(download2_id) }

    }

    private fun onDownloadSuccess(task: DownloadTask) {
        // TODO install
        if(task.fileName.endsWith(".apk"))
            APPUtil.installApkWithTask(this, task)
    }


    private fun download(url: String, nextAction: NextAction) {
        val file = FileUtil.getDestinationDir(this, "myFile")
        val task = DownloadTask()
        task.setDownloadDestination(this, file)
        task.url = url
        DownloadManager.getInstance().init(this, object : DownloadManager.InitListener {
            override fun onSuccess(manager: DownloadManager?) {
                nextAction.next(manager!!.startDownloadTask(task))
            }
            override fun onError(code: Int, msg: String?) {
            }

        })
    }

    private fun cancel(id: Int) {
        DownloadManager.getInstance().cancelDown(id)
    }



    override fun onDestroy() {
        DownloadManager.getInstance().destroy()
        unregisterReceiver(downloadReceiver)
        super.onDestroy()
    }

    interface NextAction{
        fun next(id: Int)
    }

}

