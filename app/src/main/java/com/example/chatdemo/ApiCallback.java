package com.example.chatdemo;

public interface ApiCallback<T> {
    void onSuccess(T response);
    void onError(String errorMessage);
}