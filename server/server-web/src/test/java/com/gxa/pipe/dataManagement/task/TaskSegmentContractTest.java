package com.gxa.pipe.dataManagement.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSegmentContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void addRequestAcceptsPipeSegmentId() throws Exception {
        TaskAddRequest request = objectMapper.readValue(
                """
                {
                  "repairman_id": 1,
                  "area_id": 320100,
                  "pipe_id": 1,
                  "pipe_segment_id": 3,
                  "task_name": "南京市-苏州市段巡检",
                  "type": 1,
                  "priority": 2
                }
                """,
                TaskAddRequest.class);

        assertThat(request.getPipeSegmentId()).isEqualTo(3L);
    }

    @Test
    void queryRequestAcceptsPipeSegmentId() throws Exception {
        TaskQueryRequest request = objectMapper.readValue(
                """
                {
                  "pipe_id": 1,
                  "pipe_segment_id": 3
                }
                """,
                TaskQueryRequest.class);

        assertThat(request.getPipeSegmentId()).isEqualTo(3L);
    }

    @Test
    void responseSerializesPipeSegmentFields() throws Exception {
        TaskResponse response = new TaskResponse();
        response.setPipeSegmentId("3");
        response.setPipeSegmentName("南京市-苏州市段");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"pipe_segment_id\":\"3\"");
        assertThat(json).contains("\"pipe_segment_name\":\"南京市-苏州市段\"");
    }
}
