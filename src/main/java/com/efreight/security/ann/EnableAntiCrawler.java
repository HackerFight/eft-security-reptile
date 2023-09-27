package com.efreight.security.ann;

import com.efreight.security.autoconfigure.AntiCrawlerPolicyAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.SchedulingConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fu yuan hui
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AntiCrawlerPolicyAutoConfiguration.class)
@Documented
public @interface EnableAntiCrawler {

}