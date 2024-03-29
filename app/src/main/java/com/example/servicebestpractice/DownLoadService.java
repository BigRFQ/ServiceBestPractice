package com.example.servicebestpractice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
//保证下载功能可以一直在后台运行
public class DownLoadService extends Service {
    private DownloadTask downloadTask;//下载对象
    private String downloadUrl;
//定义接口对象，并实现接口方法。
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
    class DownloadBinder extends Binder{//Binder实现服务和活动通讯
        public void startDownload(String url){
            if (downloadTask == null){//当多线程活动没有开启
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);//传入接口函数，利用回调机制活动和服务间传递消息。
                downloadTask.execute(downloadUrl);//启动后台任务。
                //添加自己编写的前台服务代码
                startForeground(1,getNotification("Downloading...",0));//前台服务显示
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

//    //在该函数中测试前台服务代码
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        String CHANNEL_ONE_ID = "com.primedu.cn";
//        String CHANNEL_ONE_NAME = "Channel One";
//
//        NotificationChannel notificationChannel = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
//                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.setShowBadge(true);
//            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            manager.createNotificationChannel(notificationChannel);
//        }
//
//        Log.d("测试前台服务","显示前台服务");
//        Intent intent = new Intent(this,MainActivity.class);
//        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
//        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ONE_ID)
//                .setContentTitle("题目")
//                .setContentText("内容")
//                .setWhen(System.currentTimeMillis())
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
//                .setContentIntent(pi)
//                .build();
//        startForeground(1,notification);
//
//    }

    //构建通知类型，包括图像、文字、标题等。
    private Notification getNotification(String title,int progress){
        String CHANNEL_ONE_ID = "com.primedu.cn";
        String CHANNEL_ONE_NAME = "Channel One";
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ONE_ID);
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
