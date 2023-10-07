package com.efreight.security.mail;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fu yuan hui
 * @date 2023-09-26 16:41:56 Tuesday
 */
public interface MailTemplatePostProcessor {

    /**
     * 创建email发送的模板内容
     * @param request
     * @return
     */
    String createMailContent(HttpServletRequest request);
}
