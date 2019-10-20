package top.vchar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>  TODO 功能描述 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/7 19:07
 */
@RestController
public class IndexController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RequestMapping("/home")
    public String home(){
        return "ok";
    }

    @GetMapping("/redis_test")
    public String redisTest() {
        String val = redisTemplate.opsForValue().get("test:demo");
        redisTemplate.opsForValue().set("test:demo", "this is test");
        return val;
    }

}
