package com.example.demo.controller;

import com.example.demo.model.Val;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

/**
 * 测试多线程ThreadLocal
 * 1. ab工具命令 ab -n1000 -c 100 localhost:8080/add
 * 2. 查询结果：curl localhost:8080/get
 * @author Marion
 * @date 2021/4/24
 */
@RestController
public class ThreadLocalController {

    private static int age = 0;

    /**
     * 注意Val不能重写HashCode，会导致多线程安全问题
     */
    private static HashSet<Val<Integer>> ageContainer = new HashSet<>();

    /**
     * 加锁解决线程安全问题
     */
    private static synchronized void lockAdd() {
        age++;
    }

    /**
     * 通过ThreadLocal解决线程安全问题
     */
    private static ThreadLocal<Val<Integer>> at = ThreadLocal.withInitial(() -> {
        Val<Integer> am = new Val<>();
        am.set(0);
        // 1. 并发线程不安全
        // ageContainer.add(am);

        addAge(am);
        return am;
    });

    /**
     * 解决添加并发线程安全问题
     */
    static synchronized void addAge(Val<Integer> val) {
        ageContainer.add(val);
    }

    private static void incAgeLocal() throws InterruptedException {
        Thread.sleep(100L);
        Val<Integer> v = at.get();
        v.set(v.get() + 1);
    }

    @RequestMapping("/get")
    public int getAge() {
        return ageContainer.stream().map(Val::get).reduce((a, x) -> a + x).get();
    }

    @RequestMapping("/add")
    public int incAge() throws InterruptedException {
        // lockAdd();
        incAgeLocal();
        return 1;
    }

}
