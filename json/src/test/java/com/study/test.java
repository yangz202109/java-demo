package com.study;

import org.junit.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author yangz
 * @date 2022/4/13 - 16:04
 */
public class test {
    @Test
    public void t1(){
        LocalDate now = LocalDate.now();
       String batch = DateTimeFormatter.ofPattern("yyyyMMdd").format(now).substring(2) + "01";
        System.out.printf(batch);

    }
}
