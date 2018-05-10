package com.bj58.demo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.UUID;

/**
 * Created by hxf on 18-5-9.
 * @author huxuefeng
 */
public class LockFactory {

    private final JedisPool jedisPool;

    public LockFactory(JedisPool jedisPool){
        this.jedisPool = jedisPool;
    }

    /**
     * 实现思想
     * 获取锁的时候，使用setnx加锁，并使用expire命令为锁添加一个超时时间，超过该时间则自动释放锁，锁的value值为一个随机生成的UUID，通过此在释放锁的时候进行判断。
     * 获取锁的时候还设置一个获取的超时时间，若超过这个时间则放弃获取锁。
     * 释放锁的时候，通过UUID判断是不是该锁，若是该锁，则执行delete进行锁释放。
     * SETNX key value
     * 将 key 的值设为 value ，当且仅当 key 不存在。
     * 若给定的 key 已经存在，则 SETNX 不做任何动作。
     * SETNX 是『SET if Not eXists』(如果不存在，则 SET)的简写。
     * 可用版本：
     * >= 1.0.0
     * 返回值：
     * 设置成功，返回 1 。
     * 设置失败，返回 0 。
     */
    /**
     * 加锁
     * @param lockName 锁的key
     * @param getTimeout 获取超时时间
     * @param timeout 锁的超时时间
     * @return 锁标识
     */
    public String locked(String lockName,long getTimeout,long timeout){
        Jedis jedis = null;
        String result = null;
        try{
            //获取连接
            jedis = jedisPool.getResource();
            //随机生成一个值
            String identifier = UUID.randomUUID().toString();
            //锁名（key的值）
            String lockKey = "lock:" + lockName;
            //超时时间,上锁后超过此时间自动释放锁
            int lockExpire = (int)(timeout / 1000);

            //请求锁的当前时间
            long currentTime = System.currentTimeMillis();

            //获取锁的超时时间，超过这个时间则放弃获取锁
            long end = currentTime + getTimeout;

            //未超过超时时间获取锁
            while ( currentTime < end ){
                if (jedis.setnx(lockKey,identifier) == 1){
                    //上锁成功
                    jedis.expire(lockKey,lockExpire);
                    result = identifier;
                    //返回值,用于释放锁时间确认
                    return result;
                }
                //返回-1代表key没有设置超时时间,为key设置一个超时时间
                if(jedis.ttl(lockKey) == -1){
                    jedis.expire(lockKey,lockExpire);
                }

                try{
                    //为了避免出现活锁，短暂暂停（活锁：是指线程1可以使用资源，但它很礼貌，让其他线程先使用资源，线程2也可以使用资源，但它很绅士，也让其他线程先使用资源。这样你让我，我让你，最后两个线程都无法使用资源。）
                    Thread.sleep(10);
                }catch (InterruptedException e){
                    //线程中断
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return result;
    }

    /**
     * 释放锁
     * @param lockName 锁的Key
     * @param identifier
     * @return
     */
    public boolean unLock(String lockName,String identifier){
        Jedis jedis = null;
        String lockKey = "lock:" + lockName;
        boolean res = false;
        try{
            jedis = jedisPool.getResource();
            while (true){
                //监视lock,准备开始事物 Redis Watch 命令用于监视一个(或多个) key ，如果在事务执行之前这个(或这些) key 被其他命令所改动，那么事务将被打断
                jedis.watch(lockKey);
                //通过上锁返回的值判断是不是该锁，若是该锁，则删除，释放锁
                if(identifier.equals(jedis.get(lockKey))){
                    Transaction transaction = jedis.multi();
                    transaction.del(lockKey);
                    List<Object> results = transaction.exec();
                    if(results == null){
                        continue;
                    }
                    res = true;
                }
                jedis.unwatch();
                break;
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(jedis != null ){
                jedis.close();
            }
        }
        return res;
    }
}
