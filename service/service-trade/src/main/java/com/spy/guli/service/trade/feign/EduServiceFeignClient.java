package com.spy.guli.service.trade.feign;

import com.spy.guli.service.base.result.R;
import com.spy.guli.service.trade.feign.impl.CourseFeignClientFallback;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author spy
 */
@FeignClient(value = "service-edu",fallback = CourseFeignClientFallback.class)
public interface EduServiceFeignClient {
    /**
     * 调用远程服务的接口
     * @param courseId
     * @return
     */
    @ApiOperation(value = "根据课程id查询课程信息")
    @GetMapping("/api/edu/course/get-course-dto/{courseId}")
    public R getCourseDtoById(@ApiParam(value = "课程Id")@PathVariable("courseId") String courseId);
}
