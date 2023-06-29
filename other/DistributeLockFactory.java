package com.heardlearn.system.sysconfiguration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.locks.Lock;

/**
 * @author yangz
 * @createTime 2023/6/29 - 14:24
 * 分布式锁工厂
 */
@Component
public class DistributeLockFactory {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据指定类型获取锁 --工厂模式
     *
     * @param lockType 锁的种类
     * @return 锁对象
     */
    public Lock getDistributeLock(DistributeLockTypeMenu lockType) {
        switch (lockType) {
            case MYSQL:
                //TODO  MYSQL版的分布式锁
                return null;
            case REDID:
                return new MyRedisLock(redisTemplate);
            case ZOOKEEPER:
                //TODO  ZOOKEEPER版的分布式锁
                return null;
        }
        return null;
    }

}
