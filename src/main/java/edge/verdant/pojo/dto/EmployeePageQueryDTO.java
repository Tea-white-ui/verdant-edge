package edge.verdant.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {
    // 搜索关键词
    private String keyword;
    //页码
    private int page;

    //每页显示记录数
    private int pageSize;

}
