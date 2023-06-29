package com.heardlearn.system.sysconfiguration.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 业务类
 * 调用购买方法减少redis中的库存数量
 * 1.0是在单机上,之后的是分布式微服务-->一台Nginx轮询方式代理2台服务,2台微服务高并发下访问各自的购买方法
 * @author yangz
 * @createTime 2023/6/19 - 15:25
 */
@Service
public class InventoryService {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private Lock lock;

    /**
     * 1.0版 在单机服务加锁 ,解决单机重复消费问题
     * 问题：当但是在分布式系统中因为竞争的线程可能不在同一个节点/服务器上（同一个jvm中）
     */
    public String sale1() {
        lock = new ReentrantLock();
        StringBuilder retMessage = new StringBuilder();
        //加锁
        lock.lock();
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            //解锁
            lock.unlock();
        }
        return retMessage.toString();
    }

    /**
     * 2.0版 在分布式系统中各个服务同时调用消费方法,解决重复消费问题
     * 问题：递归过多容易导致StackOverflow*
     */
    public String sale2() {

        StringBuilder retMessage = new StringBuilder();
        String key = "redisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        //setnx命令如果当前key不存在,则添加成功返回true
        boolean flag = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, uuidValue));

        if (!flag) {
            //没有抢到锁,则等待20毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //递归重试
            sale2();
        } else {
            //抢锁成功,执行正常业务逻辑
            try {
                //1.获取库存信息
                String result = (String) redisTemplate.opsForValue().get("inventory");
                //2.判断库存是否足够
                Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
                //3.扣减库存,每次减1
                if (inventoryNum > 0) {
                    redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                    retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
                } else {
                    retMessage.append("该商品已经售完");
                }
            } finally {
                redisTemplate.delete(key);
            }
        }
        return retMessage.toString();
    }

    /**
     * 2.1版 优化StackOverflow问题
     * 使用自旋代替递归重试
     * 问题：当本服务抢锁成功后,没有执行到删除key可以就挂了,就会导致死锁
     */
    public String sale2_1() {

        StringBuilder retMessage = new StringBuilder();
        String key = "redisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        //抢锁失败重复抢 自旋
        while (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, uuidValue))) {
            //没有抢到锁,则等待20毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //抢锁成功,执行正常业务逻辑
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            redisTemplate.delete(key);
        }

        return retMessage.toString();
    }

    /**
     * 2.2版 优化死锁问题
     * 添加key的过期时间
     * 问题：当A线程的业务处理超过key的过期时间,其所占的锁就自动被释放掉了,然后B线程抢到锁开始干活
     * 在B线程进行业务处理时,此时A线程完成业务处理就会删除B添加的锁
     */
    public String sale2_2() {

        StringBuilder retMessage = new StringBuilder();
        String key = "redisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        //抢锁失败重复抢 自旋
        while (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, uuidValue, 10, TimeUnit.SECONDS))) {
            //没有抢到锁,则等待20毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //当设置key及抢锁成功,立刻设置过期时间10秒--不应该分开添加和设置过期时间需要保证多条命令的原子性
        // redisTemplate.expire(key,10,TimeUnit.SECONDS);

        //抢锁成功,执行正常业务逻辑
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            redisTemplate.delete(key);
        }
        return retMessage.toString();
    }

    /**
     * 3.0 修复误删,只能自己删除自己的锁
     * 问题: 判断误删和删除的两条命令不具有原子性
     */
    public String sale3() {

        StringBuilder retMessage = new StringBuilder();
        String key = "redisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        //抢锁失败重复抢 自旋
        while (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, uuidValue, 10, TimeUnit.SECONDS))) {
            //没有抢到锁,则等待20毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //抢锁成功,执行正常业务逻辑
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            //改进,只能删除自己的key,不能删除别人的
            String value = (String) redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotEmpty(value) && Objects.equals(value, uuidValue)) {
                redisTemplate.delete(key);
            }
        }
        return retMessage.toString();
    }

    /**
     * 4.0 通过lua脚本发送多条命令执行保证原子性
     * 问题: 不满足重入性
     */
    public String sale4() {
        StringBuilder retMessage = new StringBuilder();
        String key = "redisLock";
        String uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();

        //抢锁失败重复抢 自旋
        while (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, uuidValue, 10, TimeUnit.SECONDS))) {
            //没有抢到锁,则等待20毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //抢锁成功,执行正常业务逻辑
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            //改进,修改为lua脚本的redis分布式锁调用,必须保证原子性
            String luaScript = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            //DefaultRedisScript 参数1:lua脚本 参数2:返回值类型--key的集合--参数数组
            redisTemplate.execute(new DefaultRedisScript<>(luaScript, Integer.class), Arrays.asList(key), uuidValue);
        }
        return retMessage.toString();
    }

    /**
     * 5.0 在1.0版上使用自定义的锁
     *
     */
    public String sale5() {
        lock = new MyRedisLock(redisTemplate);
        StringBuilder retMessage = new StringBuilder();
        //加锁
        lock.lock();
        try {
            //1.获取库存信息
            String result = (String) redisTemplate.opsForValue().get("inventory");
            //2.判断库存是否足够
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);
            //3.扣减库存,每次减1
            if (inventoryNum > 0) {
                redisTemplate.opsForValue().set("inventory", String.valueOf(--inventoryNum));
                retMessage.append("成功卖出一商品,库存剩余: ").append(inventoryNum);
            } else {
                retMessage.append("该商品已经售完");
            }
        } finally {
            //解锁
            lock.unlock();
        }
        return retMessage.toString();

    }
}
