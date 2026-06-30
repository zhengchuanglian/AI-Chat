package com.example.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.domain.School;
import com.example.ai.service.SchoolService;
import com.example.ai.mapper.SchoolMapper;
import org.springframework.stereotype.Service;

/**
* @author zcl
* @description 针对表【school(校区表)】的数据库操作Service实现
* @createDate 2026-04-24 12:47:13
*/
@Service
public class SchoolServiceImpl extends ServiceImpl<SchoolMapper, School>
    implements SchoolService{

}




