package com.example.servicebestpractice;

public interface DownloadListener {//定义接口，用于使用JAVA回调机制
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
