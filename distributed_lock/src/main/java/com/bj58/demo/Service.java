package com.bj58.demo;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author huxuefeng
 * @create 2018-05-09 下午7:17
 **/
public class Service {
    private static JedisPool pool = null;
    static {
        JedisPoolConfig config = new JedisPoolConfig();
        //设置最大连接数
        config.setMaxTotal(500);
        //设置最大空闲数
        config.setMaxIdle(8);
        //设置最大等待时间
        config.setMaxWaitMillis(1000*100);
        // borrow一个Jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        pool = new JedisPool(config,"127.0.0.1",6002,8000);
    }

    LockFactory lockFactory = new LockFactory(pool);

    int n = 500;

    public void test(){
        String id = lockFactory.locked("res",5000,1000);
        System.out.println(Thread.currentThread().getName() + "获得了锁");
        System.out.println(--n);
        lockFactory.unLock("res",id);
    }
}
