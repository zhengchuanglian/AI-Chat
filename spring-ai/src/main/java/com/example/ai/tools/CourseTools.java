package com.example.ai.tools;


import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.example.ai.entity.Course;
import com.example.ai.entity.CourseReservation;
import com.example.ai.entity.School;
import com.example.ai.entity.query.CourseQuery;
import com.example.ai.service.CourseReservationService;
import com.example.ai.service.CourseService;
import com.example.ai.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;


@RequiredArgsConstructor
@Component
public class CourseTools {

    private final CourseService courseService;
    private final CourseReservationService courseReservationService;
    private final SchoolService schoolService;

    @Tool(description = "根据条件查询课程")
    public List<Course> queryCourse(@ToolParam(description = "查询条件") CourseQuery query){
        if (query == null){
            return courseService.list();
        }
        QueryChainWrapper<Course> wrapper = courseService.query()
                .eq(query.getType() != null, "type", query.getType())
                .le(query.getEdu() != null, "edu", query.getEdu());

        if (query.getSorts()!=null && !query.getSorts().isEmpty()){
            for (CourseQuery.Sort sort: query.getSorts()){
                wrapper.orderBy(true,sort.getAsc(),sort.getField());
            }
        }

        return wrapper.list();
    }


    @Tool(description = "查询所有校区信息")
    public List<School> querySchool(){
        return schoolService.list();
    }
    
    @Tool(description = "根据条件查询校区信息")
    public List<School> querySchoolByCondition(@ToolParam(description = "查询条件") CourseQuery query){
        if (query == null){
            return schoolService.list();
        }
        QueryChainWrapper<School> wrapper = schoolService.query()
                .eq(query.getType() != null, "type", query.getType())
                .le(query.getEdu() != null, "edu", query.getEdu());

        if (query.getSorts()!=null && !query.getSorts().isEmpty()){
            for (CourseQuery.Sort sort: query.getSorts()){
                wrapper.orderBy(true,sort.getAsc(),sort.getField());
            }
        }
        return wrapper.list();
    }

    @Tool(description = "生成预约单号")
    public Integer createCourseReservation(@ToolParam(description = "预约课程") String course,
                                          @ToolParam(description = "预约校区") String school,
                                           @ToolParam(description = "学生姓名") String studentNam,
                                           @ToolParam(description = "联系电话") String  contactInfo,
                                           @ToolParam(description = "备注",required = false) String remark
                                           ){
        CourseReservation courseReservation = new CourseReservation();
        courseReservation.setCourse(course);
        courseReservation.setSchool(school);
        courseReservation.setStudentName(studentNam);
        courseReservation.setContactInfo(contactInfo);
        courseReservation.setRemark(remark);
        courseReservationService.save(courseReservation);
        return courseReservation.getId();
    }


}
