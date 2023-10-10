package com.efreight.security.interceptor;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.efreight.security.mail.EmailSendProcessor;
import com.efreight.security.mail.MailContent;
import com.efreight.security.mail.MailTemplatePostProcessor;
import com.efreight.security.properties.AntiCrawlerProperties;
import com.efreight.security.utils.IpUtils;
import lombok.Builder;;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author fu yuan hui
 * @date 2023-09-25 11:40:48 Monday
 */
@Slf4j
public abstract class AbstractSecurityPolicyConfigurable implements HandlerInterceptor, InitializingBean, BeanFactoryAware {

    protected static final String POLICY_KEY = "by-security-policy-key";
    protected static final String BEHAVIOR_COLLECT = "behavior-list";

    protected static final String IGNORE_ERROR_URL = "/error";

    protected Integer limit = 5;

    protected Long lockTime = 3600L;

    protected static final String DEFAULT_LOCK_IP_KEY = "default-lock-ip:";

    protected static final String DEFAULT_URL_REQUEST_TIMES = "default-ip-url-times:";

    private String defaultLockIp = DEFAULT_LOCK_IP_KEY;
    private String defaultRequestTimes = DEFAULT_URL_REQUEST_TIMES;

    protected static final String PATTERN_RULE_PART1 = "^[a-z\\d]{7}(3)[a-z\\d]{6}(3)[a-z\\d]{6}(3)[a-z\\d]{6}(3)[a-z\\d]{34}\\d$";
    protected static final String PATTERN_RULE_PART2 = "^[a-z\\d]{7}[6-9][a-z\\d]{6}[6-9][a-z\\d]{6}[6-9][a-z\\d]{6}[6-9][a-z\\d]{34}[a-z]$";

    protected static final Pattern PATTERN_FIRST = Pattern.compile(PATTERN_RULE_PART1);
    protected static final Pattern PATTERN_LAST = Pattern.compile(PATTERN_RULE_PART2);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Resource
    private AntiCrawlerProperties antiCrawlerProperties;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private EmailSendProcessor emailSendProcessor;

    @Value("${eft.security.private.key}")
    private String privateKey;

    protected BeanFactory beanFactory;

    protected boolean ignoreErrorUrl(HttpServletRequest request) {
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        if(StringUtils.isNotEmpty(contextPath)) {
            String substring = url.substring(contextPath.length());
            return IGNORE_ERROR_URL.equals(substring);
        }

        return IGNORE_ERROR_URL.equals(url);
    }

    /**
     * 私钥--解密
     * @param policyContext
     * @return
     */
    protected SecureDecipherContext decipher(String policyContext) {
        try {
            String priKey = this.antiCrawlerProperties.getPrivateKey();
            if (StringUtils.isBlank(priKey)) {
                priKey = this.privateKey;
            }
            RSA rsa = SecureUtil.rsa(priKey, null);
            String context = rsa.decryptStr(policyContext, KeyType.PrivateKey, StandardCharsets.UTF_8);
            return SecureDecipherContext.builder().canDecipher(true).decipherContext(context).build();
        } catch (Exception e) {
            log.error("解密失败，请检查密钥对是否正确", e);
        }

        return SecureDecipherContext.builder().canDecipher(false).build();
    }

    /**
     * <pre>
     * 解密后的值要符合一定规则
     *  1.要求字符串长度一共是64个字符，必须等于64，其中包含字符和数字，且字符都是小写字符，
     *  2.如果最后一位是数字，那么第7,14,21,28位置的字符要求必须是数字3
     *  3.如果最后一位是字符，则要求第7,14,21,28位置的元素为6,7,8,9这四个数字中的任意一个，
     *  4.除了特定位置上必须是数字外，其他位置的元素不做要求，可以是数字也可以是字符。
     *
     * </pre>
     * @param context
     * @return
     */
    protected boolean match(String context) {
        return PATTERN_FIRST.matcher(context).matches() || PATTERN_LAST.matcher(context).matches();
    }

    protected boolean enableRedis() {
        AntiCrawlerProperties.RedisConfig redisConfig = this.antiCrawlerProperties.getRedisConfig();
        /**
         * redisConfig == null 表示没有配置redis,那么默认就开启
         */
        return redisConfig == null || redisConfig.isEnable();
    }

    protected boolean ipIsLock(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(defaultLockIp + ip));
    }

    /**
     * 1S内访问5次接口则禁用当前ip一小时
     * @param ip
     */
    protected boolean reachLimitRequestTimes(String ip, String url) {
        final String key = defaultRequestTimes + ip + url;
        Boolean hasTimes = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(hasTimes)) {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count >= limit) {
                redisTemplate.opsForValue().set(defaultLockIp + ip, ip, lockTime, TimeUnit.SECONDS);
                return true;
            }
        } else {
            redisTemplate.opsForValue().set(key, 1,1, TimeUnit.SECONDS);
        }

        return false;
    }


    protected void sendWarnEmail(HttpServletRequest request) {
        AntiCrawlerProperties.MailConfig mailConfig = this.antiCrawlerProperties.getMailConfig();
        if (mailConfig == null || !mailConfig.isEnable()) {
            log.warn("没有发现email相关的配置属性，所以无法进行邮件的发送");
            return;
        }

        MailContent mailContent = new MailContent();
        try {
            MailTemplatePostProcessor bean = this.beanFactory.getBean(MailTemplatePostProcessor.class);
            mailContent.setContent(bean.createMailContent(request));
        } catch (BeansException e) {
            mailContent = createMailContext(request);
        }

        MailContent copy = mailContent;
        if (this.antiCrawlerProperties.getMailConfig().isEnableAsync()) {
            this.executor.execute(() -> emailSendProcessor.send(copy));
        } else {
            emailSendProcessor.send(mailContent);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    protected MailContent createMailContext(HttpServletRequest request) {
        MailContent mailContent = new MailContent();
        String template = "[{0}] 在IP为 [{1}] 的机器上存在不正常操作，怀疑正在尝试爬虫穿透系统予以报警，请及时核实检查！";
        String context = MessageFormat.format(template, formatter.format(LocalDateTime.now()), IpUtils.getIpAddr(request));
        mailContent.setContent(context);
        mailContent.setIsHtml(false);

        return mailContent;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AntiCrawlerProperties.RedisConfig config = this.antiCrawlerProperties.getRedisConfig();
        if (config == null) {
            return;
        }
        if (config.getLockIpPrefix() != null && config.getLockIpPrefix().trim().length() > 0) {
            this.defaultLockIp = config.getLockIpPrefix();
        }

        if (config.getLockRequestPrefix() != null && config.getLockRequestPrefix().trim().length() > 0) {
            this.defaultRequestTimes = config.getLockRequestPrefix();
        }

        if (config.getLimit() != null && config.getLimit() > 0) {
            this.limit = config.getLimit();
        }

        if (config.getLockTime() != null && config.getLockTime() > 0) {
            this.lockTime = config.getLockTime();
        }

    }

    @Builder
    static final class SecureDecipherContext {

        private boolean canDecipher;

        private String decipherContext;

        public SecureDecipherContext(boolean canDecipher, String decipherContext) {
            this.canDecipher = canDecipher;
            this.decipherContext = decipherContext;
        }

        public SecureDecipherContext() {
        }

        public boolean canDecipher() {
            return canDecipher;
        }

        public void setCanDecipher(boolean canDecipher) {
            this.canDecipher = canDecipher;
        }

        public String getDecipherContext() {
            return decipherContext;
        }

        public void setDecipherContext(String decipherContext) {
            this.decipherContext = decipherContext;
        }
    }
}
