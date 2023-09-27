package com.efreight.security.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱接收人实体类
 *
 * @author alex
 * @date 2022/06/30
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MailReceiver {
    /**
     * 收件人
     */
    private String[] to;
    /**
     * 抄送人
     */
    private String[] cc;
    /**
     * 密送人
     */
    private String[] bcc;
}
