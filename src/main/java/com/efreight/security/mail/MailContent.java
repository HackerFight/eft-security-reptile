package com.efreight.security.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * 邮件发送内容实体类
 *
 * @author alex
 * @date 2022/06/30
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MailContent {
    /**
     * 邮件标题
     */
    private String title;
    /**
     * 邮件内容
     */
    private String content;
    /**
     * 使用模板发送邮件内容，与content二选一
     */
    private MailTemplate template;
    /**
     * 邮件内容是否为html
     */
    private Boolean isHtml = Boolean.FALSE;
    /**
     * 附件列表
     */
    private File[] attachment;
}
