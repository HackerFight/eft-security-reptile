package com.efreight.security.interceptor;

import com.efreight.security.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fu yuan hui
 * @date 2023-09-25 10:32:15 Monday
 *
 * 反爬虫一阶段功能初步实现
 */
@Slf4j
public class SecurityPolicyInterceptor extends AbstractSecurityPolicyConfigurable {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //静态资源访问不进行检查
        if (!(handler instanceof HandlerMethod) || ignoreErrorUrl(request)) {
            return true;
        }

        String url = request.getRequestURI();
        String ip = IpUtils.getIpAddr(request);
        boolean illegality = true;
        String policyKey = request.getHeader(POLICY_KEY);
        if (StringUtils.isBlank(policyKey)) {
            log.error("无法从请求头中获取安全策略token值，疑似非法请求， ip 地址： {}, url: {}", ip, url);
            illegality = false;
        }

        SecureDecipherContext decipher = null;
        if (illegality && !(decipher = super.decipher(policyKey)).canDecipher()) {
            illegality = false;
        }

        if (illegality && !super.match(decipher.getDecipherContext())) {
            log.error("安全策略token值不符合规则，疑似非法请求， ip 地址: {}, url: {}", ip, url);
            illegality = false;
        }


        String behavior = request.getHeader(BEHAVIOR_COLLECT);
        if (illegality && StringUtils.isBlank(behavior)) {
            log.error("无法检测到用户的手动操作行为，疑似非法请求， ip 地址: {}, url: {}", ip, url);
            illegality = false;
        }

        if (illegality && enableRedis() && (ipIsLock(ip) || reachLimitRequestTimes(ip, url))) {
            log.error("当前请求的ip疑似非法请求，已经被锁定 ip 地址: {}, url: {}", ip, url);
            illegality = false;
        }


        if (!illegality) {
            log.error("准备邮件通知，非法请求地址url: {}, 非法请求ip: {}", url, ip);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法请求");
            sendWarnEmail(request);
        }

        return illegality;
    }
}
