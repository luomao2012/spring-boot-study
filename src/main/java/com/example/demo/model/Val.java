package com.example.demo.model;

/**
 * @author Marion
 * @date 2021/4/24
 */
public class Val<T> {

    private T t;

    public void set(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }
}
