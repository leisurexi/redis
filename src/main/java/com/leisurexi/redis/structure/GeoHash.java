package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;

import java.util.List;

/**
 * @author: leisurexi
 * @date: 2019-12-21 8:50 下午
 * @description: redis 地理位置Geo模块示例
 * @since JDK 1.8
 */
@Slf4j
public class GeoHash {

    private static final String KEY = "company";

    /**
     * 增加
     */
    @Test
    public void add() {
        RedisPool.execute(jedis -> {
            jedis.geoadd(KEY, 116.48105, 39.996794, "掘金");
            jedis.geoadd(KEY, 116.514203, 39.905409, "掌阅");
            jedis.geoadd(KEY, 116.489033, 40.007669, "美团");
            jedis.geoadd(KEY, 116.562108, 39.787602, "京东");
            jedis.geoadd(KEY, 116.334255, 40.027400, "小米");
        });
    }

    /**
     * 距离
     */
    @Test
    public void distance() {
        RedisPool.execute(jedis -> {
            Double distance1 = jedis.geodist(KEY, "掘金", "掌阅", GeoUnit.KM);
            log.info("掘金和掌阅相距: {} km", distance1);
            Double distance2 = jedis.geodist(KEY, "掘金", "美团", GeoUnit.KM);
            log.info("掘金和美团相距: {} km", distance2);
            Double distance3 = jedis.geodist(KEY, "掘金", "京东", GeoUnit.KM);
            log.info("掘金和京东相距: {} km", distance3);
            Double distance4 = jedis.geodist(KEY, "掘金", "小米", GeoUnit.KM);
            log.info("掘金和小米相距: {} km", distance4);
            Double distance5 = jedis.geodist(KEY, "掘金", "掘金", GeoUnit.KM);
            log.info("掘金和掘金相距: {} km", distance5);
        });
    }

    /**
     * 获取元素位置
     */
    @Test
    public void get() {
        RedisPool.execute(jedis -> {
            String[] companies = {"掘金", "掌阅", "美团", "京东", "小米"};
            List<GeoCoordinate> coordinateList = jedis.geopos(KEY, companies);
            for (int i = 0; i < companies.length; i++) {
                log.info("{}公司的经度是: {}，纬度是: {}", companies[i], coordinateList.get(i).getLongitude(),
                        coordinateList.get(i).getLatitude());
            }
        });
    }

    /**
     * 获取元素的hash值，可以用这个编码值直接去 http://geohash.org/${hash}
     */
    @Test
    public void getHash() {
        RedisPool.execute(jedis -> {
            String[] companies = {"掘金", "掌阅", "美团", "京东", "小米"};
            List<String> geohash = jedis.geohash(KEY, companies);
            for (int i = 0; i < companies.length; i++) {
                log.info("{}公司的hash值是: {}", companies[i], geohash.get(i));
            }
        });
    }

    /**
     * 附近的公司
     */
    @Test
    public void nearby() {
        RedisPool.execute(jedis -> {
            GeoRadiusParam param = GeoRadiusParam.geoRadiusParam()
                    .count(3)
                    .sortDescending();
            //范围20km以内最多三个元素按距离正排序，它不会排除自身
            List<GeoRadiusResponse> responseList = jedis.georadiusByMember(KEY, "掌阅", 20, GeoUnit.KM, param);
            for (GeoRadiusResponse response : responseList) {
                log.info(response.getMemberByString());
            }

            GeoRadiusParam radiusParam = GeoRadiusParam.geoRadiusParam()
                    .count(3)
                    .withDist()
                    .sortAscending();
            //跟上面的命令类似，只不过是把元素名换成了经纬度
            List<GeoRadiusResponse> radiusResponseList = jedis.georadius(KEY, 116.514202, 39.905409, 20, GeoUnit.KM, radiusParam);
            for (GeoRadiusResponse response : radiusResponseList) {
                log.info(response.getMemberByString());
                log.info(String.valueOf(response.getDistance()));
            }
        });
    }

}
