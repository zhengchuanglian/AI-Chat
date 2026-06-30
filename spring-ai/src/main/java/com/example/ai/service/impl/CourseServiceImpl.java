package com.example.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.entity.Course;
import com.example.ai.service.CourseService;
import com.example.ai.mapper.CourseMapper;
import org.springframework.stereotype.Service;

/**
* @author zcl
* @description 针对表【course(学科表)】的数据库操作Service实现
* @createDate 2026-04-24 12:48:32
*/
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>
    implements CourseService{

}




