package com.bj58.demo;/**
 * Created by root on 18-5-9.
 */

/**
 * 线程
 *
 * @author huxuefeng
 * @create 2018-05-09 下午7:30
 **/
public class ThreadModel extends Thread{
    private Service servive;

    public ThreadModel(Service servive){
        this.servive = servive;
    }

    @Override
    public void run(){
        servive.test();
    }
}
