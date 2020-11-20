package com.xf.redis.controller;

import com.xf.redis.jedis.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author xf
 * @date 2020-09-29 21:10
 * @since 1.0.0
 */
@RestController
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    private final String REDIS_LOCK="buy_goods_key";

    @RequestMapping("/buy_goods")
    public String buy_goods() throws Exception {

        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, value, 30L, TimeUnit.SECONDS);
            if (!aBoolean){
                return "抢锁失败";
            }
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber =result==null ?0:Integer.parseInt(result);
            if (goodsNumber>0){
                int realNumber=goodsNumber-1;
                stringRedisTemplate.opsForValue().set("goods:001",String.valueOf(realNumber));
                System.out.println("成功买到商品,库存还剩下:"+realNumber+"件"+"\t 服务端提供接口:"+serverPort);
                return "成功买到商品,库存还剩下:"+realNumber+"件"+"\t 服务端提供接口:"+serverPort;
            }else {
                System.out.println("商品已经售完,库存还剩下/活动结束/调用超时,欢迎下次光临"+"\t 服务端提供接口:"+serverPort);
            }
            return "商品已经售完,库存还剩下/活动结束/调用超时,欢迎下次光临"+"\t 服务端提供接口:"+serverPort;
        } finally {

            Jedis jedis = RedisUtils.getJedis();
            String script="if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            try {
                Object o = jedis.eval(script, Collections.singletonList(REDIS_LOCK), Collections.singletonList(value));
                if ("1".equals(o.toString())){
                    System.out.println("------del redis lock ok");
                }else {
                    System.out.println("------del redis lock error");

                }
            } finally {
                if (null!=jedis){
                    jedis.close();
                }
            }
        }


    }
}
