package com.gxa.pipe.dataManagement.repairman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairmanQueryRequest {

    private Integer page;

    @JsonProperty("page_size")
    private Integer pageSize;

    private String name;

    private String sex;

    @JsonProperty("min_age")
    private String minAge;

    @JsonProperty("max_age")
    private String maxAge;

    @JsonProperty("area_id")
    private String areaId;

    @JsonProperty("entry_start_time")
    private String entryStartTime;

    @JsonProperty("entry_end_time")
    private String entryEndTime;

    private String phone;
}
