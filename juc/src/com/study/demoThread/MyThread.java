package com.study.demoThread;

/**
 * @author yangz
 * @date 2021/12/15 - 9:27
 * 继承Thread的类就成为一个线程类，可以直接启动 .start();
 */
public class MyThread extends Thread{
    @Override
    public void run() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("线程启动了1111");
    }
}
class ThreadTest{
    public static void main(String[] args) {

         new MyThread().start();
    }
}
