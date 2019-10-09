package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import io.rebloom.client.Client;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis布隆过滤器误判测试
 * 当布隆过滤器说某个值存在时，这个值可能不存在；当它说某个值不存在时，那就肯定不存在。
 * User: leisurexi
 * Date: 2019-10-09
 * Time: 11:29 下午
 */
public class BloomTest {

    public static void main(String[] args) {
        Client client = RedisPool.getClientConnection();
        for (int i = 0; i < 100000; i++) {
            client.add("codehole", "user" + i);
            boolean exists = client.exists("codehole", "user" + i);
            if(!exists){
                System.out.println(i + "不存在");
                break;
            }
        }
    }

}
