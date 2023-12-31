package com.efreight.security.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author fu yuan hui
 * @date 2023-09-26 13:47:28 Tuesday
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "eft.security.reptile")
public class AntiCrawlerProperties {

    /**
     * redis 配置，连接就用spring配置即可
     */
    private RedisConfig redisConfig;

    /**
     * 邮箱配置
     */
    private MailConfig mailConfig;

    /**
     * mvc 拦截配置
     */
    private MvcConfig mvcConfig;

    /**
     * 私钥，用于解密
     */
    private String privateKey;

    @Data
    public static class RedisConfig {

        private boolean enable = true;

        private Integer limit;

        private Long lockTime;

        private String lockIpPrefix;

        private String lockRequestPrefix;
    }

    @Getter
    @Setter
    public static class MailConfig {

        private boolean enable;

        private String host;

        private String username;

        private String password;

        private String nickname;

        private Integer port = 465;

        private boolean useSsl = true;

        private List<String> to;

        private List<String> cc;

        private String title;

        private boolean enableAsync = false;
    }

    @Getter
    @Setter
    public static class MvcConfig {

        /**
         * 需要拦截的url,默认是拦截所有
         */
        private List<String> pathPatterns;

        /**
         * 不需要拦截的url
         */
        private List<String> excludePathPatterns;

        /**
         * 执行拦截器顺序
         */
        private int order;
    }
}
