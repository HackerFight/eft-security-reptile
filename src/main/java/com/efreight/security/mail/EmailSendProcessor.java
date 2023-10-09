package com.efreight.security.mail;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.template.TemplateException;
import com.efreight.security.properties.AntiCrawlerProperties;
import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;

/**
 * 发送邮件工具类
 *
 * @author alex
 * @date 2022/06/29
 */
@Slf4j
public class EmailSendProcessor {

    @Resource
    private JavaMailSender defaultMailSender;

    private final Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

    @Resource
    private AntiCrawlerProperties properties;

    public void send(MailContent mailContent) {
        try {
            MimeMessage message = defaultMailSender.createMimeMessage();
            // MimeMessageHelper对象，用来组装复杂邮件，其中构建方法中第二个参数为true，代表支持替代文本、内联元素和附件
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            // 发件人，不能省略
            helper.setFrom(properties.getMailConfig().getUsername(), properties.getMailConfig().getNickname());
            // 收件人，可以是多个，不能省略
            helper.setTo(ArrayUtil.toArray(properties.getMailConfig().getTo(), String.class));
            // 抄送人
            if (ArrayUtil.isNotEmpty(properties.getMailConfig().getCc())) {
                helper.setCc(ArrayUtil.toArray(properties.getMailConfig().getCc(), String.class));
            }
            // 邮件标题，可以省略，省略之后展示的是：<无标题>
            helper.setSubject(this.properties.getMailConfig().getTitle());
            if (null != mailContent.getTemplate()) {
                // 通过模板设置邮件内容
                helper.setText(FreeMarkerTemplateUtils.processTemplateIntoString(configuration.getTemplate(mailContent.getTemplate().getPath()), mailContent.getTemplate().getDataMap()), mailContent.getIsHtml());
            } else {
                // 邮件内容
                helper.setText(mailContent.getContent(), mailContent.getIsHtml());
            }

            // 附件
            if (ArrayUtil.isNotEmpty(mailContent.getAttachment())) {
                for (File file : mailContent.getAttachment()) {
                    helper.addAttachment(file.getName(), file);
                }
            }
            //发送邮件
            defaultMailSender.send(message);
            log.info("邮件发送成功");
        } catch (Exception e) {
            log.error("邮件发送失败", e);
        }
    }
}
