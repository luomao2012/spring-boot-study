package com.example.demo.service.juc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 高并发锁相关练习
 * @author Marion
 * @date 2021/4/29
 */
@Slf4j
@Service
public class LockService {

    AtomicReference<Thread> threadAtomicReference = new AtomicReference<>();

    /**
     * 加锁
     */
    public void spinLock() {
        /**
         * 1. 查询JAVA并发包的手册
         */
        Thread thread = Thread.currentThread();
        log.debug("1. lock current thread={}", thread.getName());

        int i = 0;
        while (!threadAtomicReference.compareAndSet(null, thread)) {
            System.out.println("set success! " + i++ + thread.getName());
        }
    }

    /**
     * 取消锁
     */
    public void spinUnLock() {
        /**
         * 1. 判断是否是当前线程，如果是设置为null
         */
        Thread thread = Thread.currentThread();
        log.debug("1. unlock current thread={}", thread.getName());

        threadAtomicReference.compareAndSet(thread, null);
    }

    /**
     * 实现一个自旋锁
     * 1. 解锁失败导致自旋锁跑满CPU
     */
    public void testSpinLock() {
        /**
         * 1. 使用两个线程分别进行加锁解锁
         */
        new Thread(() -> {
            spinLock();
            try {
                Thread.sleep(5000L);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
            spinUnLock();
        }, "one").start();

        log.debug("sleep 1s.");

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            spinLock();
            try {
                Thread.sleep(5000L);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
            spinUnLock();
        }, "two").start();

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 2. 读写锁
     */
    public void testRWLock() {
        MyCache myCache = new MyCache();

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> {
                myCache.put(String.valueOf(finalI), finalI);
            }, String.valueOf(i)).start();
        }

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> {
                myCache.get(String.valueOf(finalI));
            }, String.valueOf(i)).start();
        }
    }

    /**
     * 自定义读写锁缓存类
     */
    public class MyCache {

        HashMap<String, Object> map = new HashMap<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public void put(String key, Object val) {
            lock.writeLock().lock();
            try {
                map.put(key, val);
                log.debug("write key={}, val={}", key, val);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public Object get(String key) {
            lock.readLock().lock();
            try {
                log.debug("read key={}", key);
                return map.get(key);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.readLock().unlock();
            }
            return null;
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                map.clear();
                log.debug("clear map");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * 3. CountDownLatch
     * 当计数减到零后再执行
     */
    @SneakyThrows
    public void testCDLatch() {

        CountDownLatch countDownLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++ ) {
            int finalI = i;
            new Thread(() -> {
                log.debug("[{}] count down", finalI);
                countDownLatch.countDown();
            }, String.valueOf(i)).start();
        }

        countDownLatch.await();
        log.debug("last execute!");

    }

    /**
     * 4. 测试CyclicBarrier
     * 当初始的计数满了之后再执行
     */
    public void testCycBarrier() {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(5, () -> log.debug("game start!"));

        for (int i = 0; i < 5; i++ ) {
            int finalI = i;
            new Thread(() -> {
                try {
                    log.debug("[{}] member join", finalI);
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }, String.valueOf(i)).start();
        }
    }

}
