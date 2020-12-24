package com.spy.guli.service.ucenter.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取yml配置文件中wx配置属性的工具类
 * @author spy
 */
@Data
@Component
@ConfigurationProperties(prefix="wx.open")
public class UcenterWxProperties {
    private String appId;
    private String appSecret;
    private String redirectUri;
}
