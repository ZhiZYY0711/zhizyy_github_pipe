package com.gxa.pipe.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 演习和检修员连接表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnManoeuvreRepairman {
    
    /**
     * 演习和检修员连接表的唯一标识
     */
    private Long id;
    
    /**
     * 演习id
     */
    private Long manoeuvreId;
    
    /**
     * 检修员id
     */
    private Long repairmanId;
}