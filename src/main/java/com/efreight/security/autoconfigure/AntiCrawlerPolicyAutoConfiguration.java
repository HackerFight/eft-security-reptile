package com.efreight.security.autoconfigure;

import com.efreight.security.interceptor.SecurityPolicyInterceptor;
import com.efreight.security.mail.EmailSendProcessor;
import com.efreight.security.properties.AntiCrawlerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author fu yuan hui
 * @date 2023-09-26 11:34:40 Tuesday
 */
@ConditionalOnWebApplication
@PropertySource(value = "classpath:eft-private-key.properties")
@EnableConfigurationProperties(AntiCrawlerProperties.class)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AntiCrawlerPolicyAutoConfiguration implements WebMvcConfigurer {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Resource
    private AntiCrawlerProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AntiCrawlerProperties.MvcConfig mvcConfig = properties.getMvcConfig();
        if (mvcConfig != null) {
            List<String> pathPatterns = mvcConfig.getPathPatterns();
            List<String> excludePathPatterns = mvcConfig.getExcludePathPatterns();
            if (CollectionUtils.isEmpty(pathPatterns)) {
                pathPatterns = Collections.singletonList("/**");
            }

            registry.addInterceptor(antiCrawlerInterceptor())
                    .addPathPatterns(pathPatterns)
                    .excludePathPatterns(CollectionUtils.isEmpty(excludePathPatterns) ? Collections.emptyList() : excludePathPatterns)
                    .order(mvcConfig.getOrder());

            return;
        }

        registry.addInterceptor(antiCrawlerInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public HandlerInterceptor antiCrawlerInterceptor() {
        return new SecurityPolicyInterceptor();
    }

    @ConditionalOnProperty(prefix = "eft.security.reptile.mail-config", name = "enable", havingValue = "true")
    @Bean(name = "defaultMailSender")
    public JavaMailSender createMailSender(AntiCrawlerProperties properties) {
        JavaMailSenderImpl javaMailSender =new JavaMailSenderImpl();
        javaMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        javaMailSender.setHost(properties.getMailConfig().getHost());
        javaMailSender.setPort(properties.getMailConfig().getPort());
        javaMailSender.setProtocol(JavaMailSenderImpl.DEFAULT_PROTOCOL);
        javaMailSender.setUsername(properties.getMailConfig().getUsername());
        javaMailSender.setPassword(properties.getMailConfig().getPassword());
        Properties p = new Properties();
        p.setProperty("mail.smtp.timeout", "25000");
        if (properties.getMailConfig().isUseSsl()) {
            p.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        javaMailSender.setJavaMailProperties(p);
        return javaMailSender;
    }


    @ConditionalOnProperty(prefix = "eft.security.reptile.mail-config", name = "enable", havingValue = "true")
    @Bean
    public EmailSendProcessor emailSendProcessor(){
        return new EmailSendProcessor();
    }

    /**
     * 只要外部系统集成了spring-boot-starter-redis, 那么就会有 {@link RedisTemplate}, 所以这里的配置不会对外部系统有任何影响
     */
    @Configuration
    public static class RedisConfig {

        /**
         * 要在 application.yaml 中配置了true才会生效。
         */
        @ConditionalOnProperty(prefix = "eft.security.reptile.redis-config", name = "enable", havingValue = "true")
        @ConditionalOnMissingBean
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setHashKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
            redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

            return redisTemplate;
        }
    }
}

