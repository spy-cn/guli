package com.spy.guli.service.ucenter.controller;

import com.google.gson.Gson;
import com.spy.guli.common.util.HttpClientUtils;
import com.spy.guli.service.base.exception.GuliException;
import com.spy.guli.service.base.helper.JwtHelper;
import com.spy.guli.service.base.helper.JwtInfo;
import com.spy.guli.service.base.result.ResultCodeEnum;
import com.spy.guli.service.ucenter.entity.Member;
import com.spy.guli.service.ucenter.service.MemberService;
import com.spy.guli.service.ucenter.util.UcenterWxProperties;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 * @author spy
 */

@Controller
@RequestMapping("/api/ucenter/wx")
@Api(tags = "微信登录模块")
@Slf4j
public class ApiWxController {
    @Autowired
    MemberService memberService;

    @Autowired
    UcenterWxProperties ucenterWxProperties;
    @Value("${server.port}")
    Integer port;

    /**
     * 微信登录
     *
     * @param session
     * @return
     */
    @GetMapping("/login")
    public String wxQrCodeLogin(HttpSession session) {
        String qrUrl = memberService.wxQrCodeLogin(session);
        //重定向
        return "redirect:" + qrUrl;
    }

    /**
     * 参数在url地址后面拼接 请求方式为Get
     */
    @GetMapping("/callback")
    public String wxCallback(String code, String state, HttpSession session) throws Exception {
        //检查state 是否正确，防止被攻击
        String wx_open_state = (String) session.getAttribute("wx_open_state");
        System.err.println(state);
        System.err.println(wx_open_state);
        System.err.println(code);
        //为空或者两个state不相等
        if (StringUtils.isEmpty(wx_open_state) || !state.equals(wx_open_state) || StringUtils.isEmpty(code)) {
            //非法回调
            throw new GuliException(ResultCodeEnum.ILLEGAL_CALLBACK_REQUEST_ERROR);
        }
        //使用code 换区accessToken
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=%s" +
                "&secret=%s" +
                "&code=%s" +
                "&grant_type=authorization_code";
        String appId = ucenterWxProperties.getAppId();
        String appSecret = ucenterWxProperties.getAppSecret();
        //进行字符串填充
        url = String.format(url, appId, appSecret, code);
        HttpClientUtils client = new HttpClientUtils(url);
        client.get();
        //获取内容
        String content = client.getContent();

        Gson gson = new Gson();
        Map map = gson.fromJson(content, Map.class);
        //响应成功的数据，是没有errcode的，所以只要有errcode 便是错误的响应
        if (map.get("errcode")!=null){
            log.error("错误的响应内容：%s",content);
            throw  new GuliException(ResultCodeEnum.ILLEGAL_CALLBACK_REQUEST_ERROR);
        }
        //获取accessToken 和openid
        String accessToken = (String) map.get("access_token");
        String openid = (String) map.get("openid");

        //根据openid查询当前用户是否已经使用过微信登录
        Member member=memberService.getByOpenId(openid);
        System.err.println(member);
        if (member==null){
            //根据accessToken获取wx用户的数据
            //https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
            url = "https://api.weixin.qq.com/sns/userinfo?" +
                    "access_token=%s" +
                    "&openid=%s";
            url = String.format(url, accessToken, openid);

            client = new HttpClientUtils(url);
            client.get();
            String userContent = client.getContent();
            Map userInfo = gson.fromJson(userContent, Map.class);
            System.err.println(userInfo);
            String nickname = (String) userInfo.get("nickname");
            Double sex = (Double) userInfo.get("sex");
            String headimgurl = (String) userInfo.get("headimgurl");
            //将上面获取到的wx用户的信息存到数据库中
            member = new Member();
            member.setOpenid(openid);
            member.setAvatar(headimgurl);
            member.setNickname(nickname);
            member.setSex(sex.intValue());
            memberService.save(member);
        }
        JwtInfo jwtInfo = new JwtInfo();
        jwtInfo.setId(member.getId());
        jwtInfo.setAvatar(member.getAvatar());
        jwtInfo.setNickname(member.getNickname());
        String token = JwtHelper.createToken(jwtInfo);
        return "redirect:http://localhost:3000?token="+token;
    }

    /**
     * 分布式Session的使用步骤，我们的目的就是将session保存 起来
     * 1、redis 用来存储session
     * 2、引入相关依赖
     * 3、配置指定session存储的技术
     */

}
