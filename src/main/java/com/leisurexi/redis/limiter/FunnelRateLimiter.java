package com.leisurexi.redis.limiter;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: leisurexi
 * @date: 2019-12-21 8:15 下午
 * @description: 单机版的漏斗限流示例(分布式可以使用RedisCell)
 * @since JDK 1.8
 */
@Slf4j
public class FunnelRateLimiter {

    /**
     * makeSpace方法是漏斗算法的核心，其在每次灌水前都会被调用以触发漏水，给漏斗腾出空间来。能腾出多少空间取决于
     * 过去了多久以及流水的速率。
     */
    static class Funnel {
        int capacity;
        float leakingRate;
        int leftQuota;
        long leakingTs;

        public Funnel(int capacity, float leakingRate) {
            this.capacity = capacity;
            this.leakingRate = leakingRate;
            this.leftQuota = capacity;
            this.leakingTs = System.currentTimeMillis();
        }

        void makeSpace() {
            long nowTs = System.currentTimeMillis();
            long deltaTs = nowTs - leakingTs;
            int deltaQuota = (int) (deltaTs * leakingRate);
            //间隔时间太长，整数数字过大溢出
            if (deltaQuota < 0) {
                this.leftQuota = capacity;
                this.leakingTs = nowTs;
                return;
            }
            //腾出空间太小，最小单位是1
            if (deltaQuota < 1) {
                return;
            }
            this.leftQuota += deltaQuota;
            this.leakingTs = nowTs;
            if (this.leftQuota > this.capacity) {
                this.leftQuota = this.capacity;
            }
        }

        boolean watering(int quota) {
            makeSpace();
            if (this.leftQuota >= quota) {
                this.leftQuota -= quota;
                return true;
            }
            return false;
        }
    }

    private Map<String, Funnel> funnels = new HashMap<>();

    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);
        if (funnel == null) {
            funnel = new Funnel(capacity, leakingRate);
            funnels.put(key, funnel);
        }
        //需要1个quota
        return funnel.watering(1);
    }

    public static void main(String[] args) {
        FunnelRateLimiter limiter = new FunnelRateLimiter();
        for (int i = 0; i < 20; i++) {
            boolean result = limiter.isActionAllowed("leisurexi", "reply", 15, 0.5F);
            log.info(String.valueOf(result));
            if (!result) {
                log.info("休息1s后再继续");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
