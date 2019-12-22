package com.leisurexi.redis.pool;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;

import java.util.*;

/**
 * @author: leisurexi
 * @date: 2019-12-22 8:29 下午
 * @description: redis stream可持久化消息队列示例
 * @since JDK 1.8
 */
@Slf4j
public class RedisStream {

    private static final String KEY = "stream";

    private List<Map<String, String>> init() {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map1 = new HashMap<>();
        map1.put("name", "leixurexi");
        map1.put("age", "30");
        list.add(map1);
        Map<String, String> map2 = new HashMap<>();
        map2.put("name", "xiaoyu");
        map2.put("age", "29");
        list.add(map2);
        Map<String, String> map3 = new HashMap<>();
        map3.put("name", "xiaoqian");
        map3.put("age", "1");
        list.add(map3);
        return list;
    }

    /**
     * 增删改查
     */
    @Test
    public void test1() {
        RedisPool.execute(jedis -> {
//            List<Map<String, String>> list = init();
//            for (Map<String, String> map : list) {
//                StreamEntryID entryID = jedis.xadd(KEY, StreamEntryID.NEW_ENTRY, map);
//                log.info("{}的消息id为: {}", map.get("name"), entryID.toString());
//            }
            log.info("Stream消息长度为: {}", jedis.xlen(KEY));
            // - 代表最小 + 代表最大，这里的start和end都给null，默认就是 -  +
            log.info("========从最小到最大的列表========");
            List<StreamEntry> streamEntryList = jedis.xrange(KEY, null, null, 3);
            for (StreamEntry streamEntry : streamEntryList) {
                log.info(streamEntry.getID() + "---" + streamEntry.getFields());
            }

            log.info("========指定最小消息ID的列表========");
            List<StreamEntry> streamEntryList1 = jedis.xrange(KEY, new StreamEntryID("1577018845757-0"), null, 3);
            for (StreamEntry streamEntry : streamEntryList1) {
                log.info(streamEntry.getID() + "---" + streamEntry.getFields());
            }

            log.info("========指定最大消息ID的列表========");
            List<StreamEntry> streamEntryList2 = jedis.xrange(KEY, null, new StreamEntryID("1577018845757-0"), 3);
            for (StreamEntry streamEntry : streamEntryList2) {
                log.info(streamEntry.getID() + "---" + streamEntry.getFields());
            }

            log.info("========从最小到最大的列表========");
            List<StreamEntry> streamEntryList3 = jedis.xrange(KEY, null, null, 3);
            for (StreamEntry streamEntry : streamEntryList3) {
                log.info(streamEntry.getID() + "---" + streamEntry.getFields());
            }

        });
    }

    /**
     * 独立消费
     */
    @Test
    public void test2() {
        RedisPool.execute(jedis -> {
            log.info("========从Stream头部读取两条信息========");
            Map.Entry<String, StreamEntryID> streamQuery1 = new AbstractMap.SimpleImmutableEntry<>(KEY, new StreamEntryID("0-0"));
            List<Map.Entry<String, List<StreamEntry>>> entryList1 = jedis.xread(2, 0, streamQuery1);
            for (Map.Entry<String, List<StreamEntry>> entry : entryList1) {
                log.info(entry.toString());
            }
        });

    }

}
