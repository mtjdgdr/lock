package com.bj58.demo;

/**
 * 服务
 *
 * @author huxuefeng
 * @create 2018-05-09 下午7:17
 **/
public class Demo {
    public static void main(String[] args) {
        Service service = new Service();
        for (int i=0;i<300;i++){
            ThreadModel tha = new ThreadModel(service);
            tha.start();
        }
    }
}
