package com.spy.guli.service.ucenter.entity.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 注册页面的提交表单收集信息类
 * @author spy
 */
@ApiModel(value = "注册页面收集信息表单类")
@Data
public class RegisterForm {

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String mobile;

    @ApiModelProperty(value = "验证码")
    private String code;

    @ApiModelProperty(value = "密码")
    private String password;

}
