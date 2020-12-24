package com.spy.guli.service.edu.controller;


import com.spy.guli.service.base.result.R;

import com.spy.guli.service.edu.entity.vo.SubjectVo;
import com.spy.guli.service.edu.service.SubjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 课程科目 前端控制器
 * </p>
 *
 * @author spy
 * @since 2020-10-14
 */
@CrossOrigin
@ApiModel(value = "课程模块")
@RestController
@RequestMapping("/edu/subject")
public class SubjectController {
    @Autowired
    SubjectService subjectService;
    @ApiOperation(value = "嵌套数据列表")
    @GetMapping("/nested-list")
    public R nestedList() {
        List<SubjectVo> subjectVoList = subjectService.nestedList();
        return R.ok().data("items",subjectVoList);
    }
}

