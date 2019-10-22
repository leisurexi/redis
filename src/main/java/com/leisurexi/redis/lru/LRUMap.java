package com.leisurexi.redis.lru;

import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description: 用Java的LinkedHashMap实现的简单的LRU算法
 * 实现LRU算法除了需要key/value数据结构外，还需要加一个链表，链表中的元素按照一定的顺序进行排列。
 * 当空间满的时候，会踢掉尾部的元素。当字典的某个元素被访问时，它在链表中的位置会被移动到表头，所以链表
 * 的元素排列顺序就是元素最近被访问的时间顺序。
 * User: leisurexi
 * Date: 2019-10-13
 * Time: 12:38 下午
 */
@ToString
public class LRUMap<K, V> extends LinkedHashMap<K, V> implements Map<K, V> {

    int capacity;

    /**
     * LinkedHashMap有两种迭代方式
     * 按插入顺序 - 保证迭代元素的顺序与插入顺序一致
     * 按访问顺序 - 一种特殊的迭代顺序，从最近最少访问到最多访问的元素访问顺序，非常适合构建 LRU 缓存
     */
    public LRUMap(int capacity, boolean accessOrder) {
        super(capacity, 0.75f, accessOrder);
        this.capacity = capacity;
    }

    //这边是构建访问顺序，从最少访问的元素开始，当长度大于capacity会从头部开始删除元素
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > capacity;
    }

    public static void main(String[] args) {
        LRUMap map = new LRUMap(10, Boolean.TRUE);
        for (int i = 0; i < 15; i++) {
            map.put("key" + i, "value" + i);
        }
        map.put("key10", "leisurexi");
        map.get("key11");
        for (Object entry : map.entrySet()) {
            System.out.println(entry);
        }
    }

}
