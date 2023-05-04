package com.study.foctory.simple;

/**
 * @auther shkstart
 * @create 2021-12-03-17:42
 * 工厂类
 * 静态工厂模式
 * 当增加一个新产品时，需要改动源代码，违反开闭原则
 */
public class CarFactory {

    public static Car getCar(String car){
        if (car.equals("宝马")){
            return new Bmv();
        }else if(car.equals("特斯拉")){
            return new Tesla();
        }
         return null;
    }
}
