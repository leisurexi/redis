package com.leisurexi.redis.limiter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 单机版的漏斗限流示例(分布式可以使用RedisCell)
 * User: leisurexi
 * Date: 2019-10-11
 * Time: 8:26 下午
 */
public class FunnelRateLimiter {

    /**
     * makeSpace方法是漏斗算法的核心，其在每次灌水前都会被调用以触发漏水，给漏斗腾出空间来。能腾出多少空间取决于
     * 过去了多久以及流水的速率。
     */
    static class Funnel {
        //漏斗容量
        int capacity;
        //漏嘴流水速率
        float leakingRate;
        //漏斗剩余空间
        int leftQuota;
        //上一次漏水时间
        long leakingTs;

        public Funnel(int capacity, float leakingRate) {
            this.capacity = capacity;
            this.leakingRate = leakingRate;
            this.leftQuota = capacity;
            this.leakingTs = System.currentTimeMillis();
        }

        void makeSpace() {
            long nowTs = System.currentTimeMillis();
            //距离上一次漏水过去了多久
            long deltaTs = nowTs - leakingTs;
            //又可以腾出不少空间
            int deltaQuota = (int) (deltaTs * leakingRate);
            //间隔时间太长，整数数字过大溢出
            //整数类型溢出会变成负数
            if (deltaQuota < 0) {
                leftQuota = capacity;
                leakingTs = nowTs;
                return;
            }
            //腾出空间太小，最小单位是1
            if (deltaQuota < 1) {
                return;
            }
            leftQuota += deltaQuota;
            leakingTs = nowTs;
            //剩余空间不得高于容量
            if (leftQuota > capacity) {
                leftQuota = capacity;
            }
        }

        boolean watering(int quota) {
            makeSpace();
            //判断剩余空间是否足够
            if (leftQuota >= quota) {
                leftQuota -= quota;
                return true;
            }
            return false;
        }
    }

    //所有的漏斗
    private Map<String, Funnel> funnels = new HashMap<>();

    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);
        if (funnel == null) {
            funnel = new Funnel(capacity, leakingRate);
            funnels.put(key, funnel);
        }
        return funnel.watering(1); //需要1个quota
    }

    public static void main(String[] args) {
        FunnelRateLimiter limiter = new FunnelRateLimiter();
        for (int i = 0; i < 20; i++) {
            boolean result = limiter.isActionAllowed("leisurexi", "reply", 15, (float) 0.5);
            System.out.println(result);
        }
    }

}
