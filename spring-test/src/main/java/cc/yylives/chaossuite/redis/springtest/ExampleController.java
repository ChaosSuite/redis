package cc.yylives.chaossuite.redis.springtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class ExampleController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/set")
    public String setValue(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value);
        return "Value set successfully";
    }

    @GetMapping("/get")
    public String getValue(@RequestParam String key) {
        Object value = redisService.get(key);
        return value != null ? value.toString() : "Key not found";
    }
}