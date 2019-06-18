package com.example.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer,Integer> {//实现多线程功能，实现异步消息处理
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;//表示上一次刷新时的进度
    public DownloadTask(DownloadListener listener){
        this.listener = listener;
    }//构造函数

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is =null;
        RandomAccessFile saveFile = null;
        File file= null;
        try{
            long downloadedLength = 0;//记录已下载文件的长度。
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()){
                downloadedLength= file.length();
            }
            long contentLenght = getContentLength(downloadUrl);
            if (contentLenght==0){
                return TYPE_FAILED;
            }else if (contentLenght == downloadedLength){
                //已下载字节和文件总字节相等，说明已经下载完成
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadedLength + "-")//从已下载处开始下载，实现断点下载的功能。
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null){
                is = response.body().byteStream();
                saveFile = new RandomAccessFile(file,"rw");
                saveFile.seek(downloadedLength);//跳过已下载字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len = is.read(b) )!=-1){
                    if (isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return  TYPE_PAUSED;
                    }else {
                        total +=len;
                        saveFile.write(b,0,len);
                        //计算已下载的百分比
                        int progress = (int) ((total +downloadedLength)*100/contentLenght);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (is != null){
                    is.close();
                }
                if (saveFile != null){
                    saveFile.close();
                }
                if (isCanceled &&file !=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }
    public void pauseDownload(){
        isPaused = true;
    }
    public void cancelDownLoad(){
        isCanceled = true;
    }
    private long getContentLength(String downloadUrl)throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();

        Response response = client.newCall(request).execute();
        if (response != null&& response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return  0;
    }
}
