package com.efreight.security.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author alex
 * @date 2022/07/01
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MailTemplate {

    private String path;

    private Map<String, Object> dataMap;
}
