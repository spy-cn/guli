package com.spy.guli.service.trade.feign.impl;

import com.spy.guli.service.base.result.R;

import com.spy.guli.service.trade.feign.EduServiceFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author spy
 */
@Component
@Slf4j
public class CourseFeignClientFallback implements EduServiceFeignClient{
    @Override
    public R getCourseDtoById(String courseId) {
        log.warn("熔断保护");
        return R.error();
    }
}
