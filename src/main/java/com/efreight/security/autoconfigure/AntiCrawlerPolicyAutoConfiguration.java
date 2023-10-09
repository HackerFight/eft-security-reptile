package com.efreight.security.autoconfigure;

import com.efreight.security.interceptor.SecurityPolicyInterceptor;
import com.efreight.security.mail.EmailSendProcessor;
import com.efreight.security.properties.AntiCrawlerProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Role;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author fu yuan hui
 * @date 2023-09-26 11:34:40 Tuesday
 */
@PropertySource(value = "classpath:eft-private-key.properties")
@EnableConfigurationProperties(AntiCrawlerProperties.class)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AntiCrawlerPolicyAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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
}

