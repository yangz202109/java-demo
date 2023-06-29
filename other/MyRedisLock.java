package com.heardlearn.system.sysconfiguration.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author yangz
 * @createTime 2023/6/28 - 14:43
 * 自研的分布式锁,实现lock接口
 * redis的hash储存  HSET key(锁名称) field(占用该锁的线程标识) value(加锁次数)
 */
public class MyRedisLock implements Lock {
    private final RedisTemplate<String, Object> redisTemplate;
    private final String lockName;
    private final String threadUUId;
    private final long expireTime;

    public MyRedisLock(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.lockName = "redisLock";
        this.threadUUId = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
        this.expireTime = 30L;
    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public void unlock() {
        //解锁脚本
        String script = "if redis.call('hexists',KEYS[1],ARGV[1]) == 0 then return nil elseif redis.call('hincrby',KEYS[1],ARGV[1],-1) == 0 then redis.call('del',KEYS[1]) return 1 else return 0 end";
        Integer execute = redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Collections.singletonList(lockName), threadUUId);
        if (Objects.isNull(execute)){
            throw new RuntimeException("this lock doesn't exists!");
        }
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(-1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (time == -1 && unit.equals(TimeUnit.SECONDS)) {
            //加锁脚本
            String script = "if redis.call('exists',KEYS[1]) == 0 or redis.call('exists',KEYS[1]) == 1 then " +
                    "redis.call('hincrby',KEYS[1],ARGV[1],1) redis.call('expire',KEYS[1],ARGV[2]) " +
                    "return 1 else return 0 end";
            Integer execute = redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Collections.singletonList(lockName), threadUUId, StrUtil.toString(expireTime));
            if (Objects.nonNull(execute)) {
                while (execute == 1) {
                    //没有抢到锁,则等待20毫秒
                    try {
                        TimeUnit.MILLISECONDS.sleep(15);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    execute = redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Collections.singletonList(lockName), threadUUId, StrUtil.toString(expireTime));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void lockInterruptibly() {
        //用不到
    }

    @Override
    public Condition newCondition() {
        //用不到
        return null;
    }
}
