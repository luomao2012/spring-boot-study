package com.example.demo.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 注意：不要使用@Data，因为里面包含了@EqualsAndHashCode
 * @author Marion
 * @date 2021/4/24
 */
@Getter
@Setter
public class Val<T> {

    private T t;

    public void set(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }
}
