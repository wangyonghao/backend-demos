package com.wyh.demos.propertyencryption;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wangyonghao
 * @date 2021/1/7
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class PropertyEncryptionDemoTest {
    @Resource
    PropertyEncryptionDemo propertyEncryptionDemo;

    @Test
    void printProperties() {
        propertyEncryptionDemo.printProperties();
    }
}
