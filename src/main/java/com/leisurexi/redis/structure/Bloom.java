package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import io.rebloom.client.Client;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: leisurexi
 * @date: 2019-12-21 1:07 下午
 * @description: redis bloom数据结构示例
 * @since JDK 1.8
 */
@Slf4j
public class Bloom {

    /**
     * 布隆过滤器的原理:
     * 每个布隆过滤器对应到redis的数据结构里面就是一个大型的位数组和几个不一样的
     * 无偏hash函数。所谓无偏就是能够把元素的hash值算得比较均匀，让元素被hash
     * 映射到位数组中的位置比较随机。
     * 向布隆过滤器中添加key时，会使用多个hash函数对key进行hash，算得一个整数
     * 索引值，然后对位数组长度进行取模运算得到一个位置，每个hash函数都会算得一个
     * 不同的位置。再把数组的这几个位置都设置为1，就完成了add操作。
     * 向布隆过滤器询问key是否存在时，跟add一样，也会把hash的几个位置都算出来，
     * 看看位数组中这几个位置是否都为1，只要有一个位为0，那么说明布隆过滤器中这个
     * key不存在。如果这几个位置都是1，并不能说明这个key就一定存在，只是极有可能
     * 存在，因为这些位被置为1可能是因为其他的key存在所致。如果这个位数组比较稀疏，
     * 判断正确的概率就会很大，如果这个位数组比较拥挤，判断正确的概率就会降低。
     * 使用时不要让实际元素数量远大于初始化数量，当实际元素数量开始超出初始化数量时，
     * 应该对布隆过滤器进行重建，重新分配一个size更大的过滤器，在将所有的历史元素
     * 批量add进去（这就要求我们在其他的存储器中记录所有的历史元素）。因为error_rate
     * 不会因为数量刚一超出就急剧增加，这就给我们重建过滤器提供了较为宽松的时间。
     */

    private static final String KEY = "bloom";
    private static final Client CLIENT = RedisPool.getClientConnection();

    /**
     * 塞进去100000个元素，但是没有误判，原因在与布隆过滤器对于已经
     * 见过的元素肯定不会误判，它只会误判没有见过的元素
     */
    @Test
    public void test1() {
        CLIENT.delete(KEY);
        for (int i = 0; i < 100000; i++) {
            String value = "user-" + i;
            CLIENT.add(KEY, value);
//            boolean result = CLIENT.exists(KEY, value);
            //这边稍作修改，去查找没见过的元素
            boolean result = CLIENT.exists(KEY, "user-" + (i + 1));
            if (!result) {
                log.info(String.valueOf(i));
                break;
            }
        }
    }

    private String chars;

    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            builder.append(('a' + i));
        }
        chars = builder.toString();
    }

    private String randomString(int n) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int idx = ThreadLocalRandom.current().nextInt(chars.length());
            builder.append(chars.charAt(idx));
        }
        return builder.toString();
    }

    private List<String> randomUsers(int n) {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            users.add(randomString(64));
        }
        return users;
    }

    /**
     * 为自定义布隆过滤器参数之前误判率在1%多一点
     */
    @Test
    public void test2() {
        List<String> users = randomUsers(100000);
        List<String> usersTrain = users.subList(0, users.size() / 2);
        List<String> usersTest = users.subList(users.size() / 2, users.size());
        CLIENT.delete(KEY);
        //对应bf.reserve指令
        /**
         * 自定义参数布隆过滤器(默认的initial_size是100, error_rate是0.01)
         * @param key 如果对应的key已经存在，bf.reserve命令会报错
         * @param initCapacity 表示预计放入的元素数量，当实际数量超出这个数值时，误判率会上升，
         *                     所以需要提前设置一个较大的数值避免超出导致误判率升高
         * @param errorRate 错误率，该值越低，需要的空间越大
         * 自定义布隆过滤器参数后，误判率在0.012%左右
         */
        CLIENT.createFilter(KEY, 50000, 0.001);
        for (String user : usersTrain) {
            CLIENT.add(KEY, user);
        }
        int falses = 0;
        for (String user : usersTest) {
            boolean result = CLIENT.exists(KEY, user);
            if (result) {
                falses++;
            }
        }
        log.info("bloom判断存在个数: {}, 总个数: {}", falses, usersTest.size());
    }

}
