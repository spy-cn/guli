package com.spy.guli.service.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wxpay.sdk.WXPayUtil;
import com.google.gson.Gson;
import com.spy.guli.common.util.HttpClientUtils;
import com.spy.guli.service.base.dto.CourseDto;
import com.spy.guli.service.base.dto.MemberDto;
import com.spy.guli.service.base.exception.GuliException;
import com.spy.guli.service.base.result.R;
import com.spy.guli.service.base.result.ResultCodeEnum;
import com.spy.guli.service.trade.entity.Order;
import com.spy.guli.service.trade.feign.EduServiceFeignClient;
import com.spy.guli.service.trade.feign.UcenterServiceFeignClient;
import com.spy.guli.service.trade.mapper.OrderMapper;
import com.spy.guli.service.trade.service.OrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spy.guli.service.trade.util.OrderNoUtils;
import com.spy.guli.service.trade.util.WeiXinPayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.management.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单 服务实现类
 * </p>
 *
 * @author spy
 * @since 2020-11-04
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    EduServiceFeignClient eduServiceFeignClient;
    @Autowired
    UcenterServiceFeignClient ucenterServiceFeignClient;

    @Autowired
    WeiXinPayProperties weiXinPayProperties;

    @Override
    public String createOrder(String courseId, String memberId) {
        System.err.println("课程Id——courseId:"+courseId);
        System.err.println("会员Id-memberId:"+memberId);

        /**
         * 创建订单的业务流程
         * 首先判断用户是否购买过此产品，即数据库中是否有对应的订单信息，
         * 根据用户id和课程id 可以查找数据库中的订单信息；
         *   如果购买过此产品将产品的订单Id返回；
         *   如果未购买过此产品，则将订单信息插入到数据库中，返回订单id；
         * 订单详情页所需要的数据信息
         *   讲师：姓名
         *   课程：封面、课程id、课程的价格、课程的标题
         *   订单：订单id 、订单的总金额
         */
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("member_id",memberId);
        queryWrapper.eq("course_id",courseId);
        Order order = baseMapper.selectOne(queryWrapper);
        if (order!=null){
            return order.getId();
        }
        //如果在数据库中没有查询到记录，则创建订单，

        //调用远程的微服务，获取参数
        //R对象返回的是一个json字符串，我们需要将json字符串转为普通的Java对象
        //查询课程详情
        R rCourseDto = eduServiceFeignClient.getCourseDtoById(courseId);
        System.err.println("rCourseDto"+rCourseDto);
        //查询会员详情
        R rMemberDto = ucenterServiceFeignClient.getMemberDtoInfoById(memberId);
        System.err.println("rMember:"+rMemberDto);
        if (rCourseDto.getCode()!=20000){
            log.warn("失败");
            return "请求失败";
        }
        Map<String, Object> rCourseDtoData = rCourseDto.getData();
        System.err.println("rCourseDtoData:>>>>>"+rCourseDtoData);

        //这样无法进行强制类型转化。报错：【LinkedHashMap无法进行强制类型转化】
        //String courseDtoJson = (String) rCourseDtoData.get("courseDto");
        //Gson gson = new Gson();
        //CourseDto courseDto = gson.fromJson(courseDtoJson, CourseDto.class);

        //jackson的工具类可以将json的响应数据转化为指定类型的对象
        ObjectMapper mapper = new ObjectMapper();
        CourseDto courseDto = mapper.convertValue(rCourseDtoData.get("courseDto"), CourseDto.class);

        //远程调用用户中心的微服务，获取用户信息
        // R rMemberDto = ucenterServiceFeignClient.getMemberDtoInfoById(memberId);
        if (rMemberDto.getCode()!=20000){
            log.warn("失败");
            return "请求失败";
        }
        Map<String, Object> rMemberDtoData = rMemberDto.getData();
        //String  memberDtoJson = (String) rMemberDtoData.get("memberDto");
        //MemberDto memberDto = gson.fromJson(memberDtoJson, MemberDto.class);

        MemberDto memberDto = mapper.convertValue(rMemberDtoData.get("memberDto"), MemberDto.class);


        //创建订单
        order = new Order();
        //订单编号
        order.setOrderNo(OrderNoUtils.getOrderNo());
        //订单状态 0 未支付 1 已支付
        order.setStatus(0);
        //订单中的课程id
        order.setCourseId(courseId);
        //会员id
        order.setMemberId(memberId);
        //讲师
        order.setTeacherName(memberDto.getNickname());
        //课程封面
        order.setCourseCover(courseDto.getCover());
        //价格
        long price = (long) (courseDto.getPrice().doubleValue()*100);
        order.setTotalFee(price);
        //支付类型
        order.setPayType(1);
        //讲师姓名
        order.setTeacherName(courseDto.getTeacherName());
        baseMapper.insert(order);
        return order.getId();
    }

    @Override
    public Order getOrderInfoByOidAndMId(String orderId, String memberId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",orderId);
        queryWrapper.eq("member_id",memberId);
        Order order = baseMapper.selectOne(queryWrapper);
        return order;
    }

    @Override
    public boolean isBuyCourse(String courseId, String memberId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id",courseId);
        queryWrapper.eq("member_id",memberId);
        queryWrapper.eq("status",1);
        Order order = baseMapper.selectOne(queryWrapper);
        if (order!=null){
            return true;
        }
        return false;
    }

    @Override
    public List<Order> selectOrderList(String memberId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("member_id",memberId);
        List<Order> orders = baseMapper.selectList(queryWrapper);
        return orders;
    }

    @Override
    public boolean deleteOrderById(String orderId, String memberId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",orderId);
        queryWrapper.eq("member_id",memberId);
        int delete = baseMapper.delete(queryWrapper);
        if (delete!=0){
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> createQrToPay(String orderNo,String remoteAddr) {

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);
        Order order = baseMapper.selectOne(queryWrapper);
        if (order==null){
            log.error("创建支付二维码时，订单信息查询错误");
            throw  new GuliException(ResultCodeEnum.PAY_ORDERQUERY_ERROR);
        }
        //封装请求的参数：请求参数为xml形式的，
        // 但是微信给我们提供了工具类 WXPayUtil.mapToXml() 可以将map类型的数据转换为xml形式的数据
        Map<String, String> map = new HashMap<>();
        //公众账号ID
        map.put("appid",weiXinPayProperties.getAppId());
        //商户号
        map.put("mch_id",weiXinPayProperties.getPartner());
        //随机字符串
        map.put("nonce_str",WXPayUtil.generateNonceStr());
        //签名是根据key和参数列表生成的加密字符串
        //map.put("sign","");
        //商品描述：我们需要根据订单号查询数据库得到课程的标题,
        map.put("body",order.getCourseTitle());
        //商户订单号
        map.put("out_trade_no",orderNo);
        //标价金额
        map.put("total_fee",order.getTotalFee()+"");
        //终端Ip，也就是用户的ip地址,ip地址我们可以在request中获取，当做参数传进来
        map.put("spbill_create_ip",remoteAddr);
        //通知地址，微信支付结果的回调地址
        map.put("notify_url",weiXinPayProperties.getNotifyUrl());
        //交易类型 NATIVE -Native支付
        map.put("trade_type","NATIVE");
        //生成签名：签名是根据key和参数列表生成的加密字符串
        String signedXml = WXPayUtil.generateSignedXml(map, weiXinPayProperties.getPartnerKey());
        log.info(signedXml);

        //调用微信的api接口，统一下单，发送请求
        HttpClientUtils client = new HttpClientUtils("https://api.mch.weixin.qq.com/pay/unifiedorder");
        //将参数放入请求对象的方法体中
        client.setXmlParam(signedXml);
        //使用https的形式发送
        client.setHttps(true);
        //发送请求
        client.post();
        //得到响应结果
        String content = client.getContent();
        log.info(content);
        //响应结果为xml形式的数据，我们将其转为map格式的数据
        Map<String, String> resultMap = WXPayUtil.xmlToMap(content);
        //响应结果中有两个key ，可以判断成功或者失败  r
        //封装响应结果

        return null;
    }
}
