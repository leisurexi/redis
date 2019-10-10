package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import io.rebloom.client.Client;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis布隆过滤器误判测试
 * 当布隆过滤器说某个值存在时，这个值可能不存在；当它说某个值不存在时，那就肯定不存在。
 * User: leisurexi
 * Date: 2019-10-09
 * Time: 11:29 下午
 */
public class BloomTest {

    private static final String KEY = "codehole";

    /**
     * 布隆过滤器对于已经见过的元素肯定不会误判，它只会误判那些没见过的元素
     */
    @Test
    public void test1() {
        Client client = RedisPool.getClientConnection();
        client.delete(KEY);
        for (int i = 0; i < 100000; i++) {
            client.add(KEY, "user" + i);
            boolean exists = client.exists(KEY, "user" + i);
            if (!exists) {
                System.out.println(i + "不存在");
                break;
            }
        }
    }

    /**
     * 输出了214，也就是到第214个元素的时候，它出现了误判
     */
    @Test
    public void test2() {
        Client client = RedisPool.getClientConnection();
        client.delete(KEY);
        for (int i = 0; i < 100000; i++) {
            client.add(KEY, "user" + i);
            //注意 i+1，这个是当前布隆过滤器没见过的
            boolean exists = client.exists(KEY, "user" + (i + 1));
            if (exists) {
                System.out.println(i);
                break;
            }
        }
    }

    private String chars;

    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            builder.append((char) ('a' + i));
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
        for (int i = 0; i < n; i++) {
            users.add(randomString(64));
        }
        return users;
    }

    /**
     * 随机生成一堆字符串，然后切分为两组，将其中一组塞入布隆过滤器，然后再判断另外一组存在与否，
     * 取误判个数和字符串总量一半的百分比作为误判率
     *
     * 误判率是1%多一点左右
     */
    @Test
    public void test3() {
        List<String> users = randomUsers(100000);
        List<String> usersTrain = users.subList(0, users.size() / 2);
        List<String> userTest = users.subList(users.size() / 2, users.size());
        Client client = RedisPool.getClientConnection();
        client.delete(KEY);
        usersTrain.forEach(s -> client.add(KEY, s));
        int falses = 0;
        for (String s : userTest) {
            boolean exists = client.exists(KEY, s);
            if (exists) {
                falses++;
            }
        }
        System.out.printf("%d %d\n", falses, userTest.size());
    }

    /**
     * 自定义参数布隆过滤器，需要在add之前使用bf.reserve指令显示创建。如果对于key已经存在，
     * bf.reserve会报错。bf.reserve有三个参数，分别是key、error_rate(错误率)和initial_size。
     * error_rate越低，需要的空间越大。
     * initial_size表示预计放入的元素数量，当实际数量超出这个数值时，误判率会上升，所以需要提前设置一个较大的
     * 数值避免误判率升高。
     * 如果不使用bf.reserve，默认的error_rate是0.01，默认的initial_size是100。
     */
    @Test
    public void test4() {
        List<String> users = randomUsers(100000);
        List<String> usersTrain = users.subList(0, users.size() / 2);
        List<String> userTest = users.subList(users.size() / 2, users.size());
        Client client = RedisPool.getClientConnection();
        client.delete(KEY);
        //对应bf.reserve指令
        client.createFilter(KEY, 50000, 0.001);
        usersTrain.forEach(s -> client.add(KEY, s));
        int falses = 0;
        for (String s : userTest) {
            boolean exists = client.exists(KEY, s);
            if (exists) {
                falses++;
            }
        }
        System.out.printf("%d %d\n", falses, userTest.size());
    }


}
