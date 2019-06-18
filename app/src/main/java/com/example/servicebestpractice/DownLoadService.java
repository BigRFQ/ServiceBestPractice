package com.example.servicebestpractice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownLoadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("Downloading...",progress));
        }

        @Override
        public void onSuccess() {
            //下载成功时将前台服务通知关闭，并创建一个下载完成通知。
            downloadTask =null;
            stopForeground(true);//删除现有通知
            getNotificationManager().notify(1,getNotification("Download Success",-1));//启动一个新通知
            Toast.makeText(DownLoadService.this,"Download Success",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onFailed() {
            //下载失败时将前台服务通知关闭，并创建一个下载失败通知。
            downloadTask =null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            Toast.makeText(DownLoadService.this,"Download Failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask =null;
            stopForeground(true);
            Toast.makeText(DownLoadService.this,"Download Paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask =null;
            Toast.makeText(DownLoadService.this,"Download Canceled",Toast.LENGTH_SHORT).show();
        }
    };
    private DownloadBinder mBinder = new DownloadBinder();


    public DownLoadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    class DownloadBinder extends Binder{//Binder实现进程间通讯
        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("Downloading...",0));
                Toast.makeText(DownLoadService.this,"Downloading...",Toast.LENGTH_SHORT).show();
            }
        }
        public void pauseDownload(){
            if (downloadTask != null){
                downloadTask.pauseDownload();
            }
        }
        public void cancelDownload(){
            if (downloadTask != null){
                downloadTask.cancelDownLoad();
            }else{
                if (downloadUrl != null){
                    //取消下载时需要将文件删除，并将通知关闭。
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory +fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownLoadService.this,"Canceled",Toast.LENGTH_SHORT).show();

                }
            }
        }

    }
    private NotificationManager getNotificationManager(){
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }
    private Notification getNotification(String title,int progress){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress>= 0){
            //当progress大于等于0时，才需要显示下载进度。
            builder.setContentText(progress +"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }
}
