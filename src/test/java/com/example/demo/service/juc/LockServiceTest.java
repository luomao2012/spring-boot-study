package com.example.demo.service.juc;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marion
 * @date 2021/4/29
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LockServiceTest {

    @Autowired
    private LockService lockService;

    @Test
    void testSpinLock() {
        lockService.testSpinLock();
    }

    @Test
    void testRWLock() {
        lockService.testRWLock();
    }

    @Test
    void testCDLatch() {
        lockService.testCDLatch();
    }

    @Test
    void testCycBarrier() {
        lockService.testCycBarrier();
    }
}