package com.spy.guli.service.statistics.feign;

import com.spy.guli.service.base.result.R;
import com.spy.guli.service.statistics.feign.fallback.UcenterFeignClientFallback;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author spy
 */
@FeignClient(value = "service-ucenter",fallback = UcenterFeignClientFallback.class)
public interface UcenterServiceFeignClient {
    @ApiOperation(value = "根据日期统计注册人数")
    @GetMapping("/admin/statistics/daily/count-register-num/{day}")
    public R countRegisterNum(@ApiParam(value = "日期xxxx-xx-xx",required = true)  @PathVariable("day") String day);
}
