package com.wyh.demos.propertyencryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 属性文件加密
 * @author wangyonghao
 * @date 2021/1/7
 */
@Component
public class PropertyEncryptionDemo {
    @Value("${property-encryption.normal-property}")
    private String normalProperty;
    @Value("${property-encryption.secret-property}")
    private String secretProperty;


    public void printProperties(){
        System.out.println("normalProperty="+normalProperty);
        System.out.println("secretProperty="+secretProperty);
    }
}
