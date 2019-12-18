package com.leisurexi.redis.pool;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: leisurexi
 * Date: 2019-10-13
 * Time: 5:37 下午
 */
@Data
public class Holder<T> {

    private T value;

}
