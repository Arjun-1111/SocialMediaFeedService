package com.project.SocialMediaFeedService.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        //create redis template
        RedisTemplate<String,String> template = new RedisTemplate<>();

        //set connection factory
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        //Set key serializer
        template.setKeySerializer(stringRedisSerializer);

        //Set value serializer
        template.setValueSerializer(stringRedisSerializer);

        //Set hash key serializer
        template.setHashKeySerializer(stringRedisSerializer);

        //Set hash value serializer
        template.setHashValueSerializer(stringRedisSerializer);

        //Initialize the template
        template.afterPropertiesSet();

        return template;
    }


    //Spring Boot 4.x and Jackson 3, the rules for managing dates have dramatically changed
    //1. No manual JavaTimeModule: The jsr310 date-time module has been natively merged into Jackson 3's core.
    // The old JavaTimeModule class does not exist anymore; it is active automatically
    //2. Readable ISO Strings by Default: Jackson 3 officially flipped its default configurations.
    // Dates are now stored as readable ISO-8601 strings by default
    //3. Immutable Builders: You should instantiate your mapper using
    // format-specific builders (JsonMapper.builder()) to ensure complete thread-safety.
    @Bean
    public ObjectMapper objectMapper(){
        return JsonMapper.builder()
                // OPTIONAL: Explicitly forces ISO strings (though false is the default now in Jackson 3)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Note: No registerModule(new JavaTimeModule()) is required!
                // It is built directly into Jackson 3 core out-of-the-box.
                .build();
    }
}
