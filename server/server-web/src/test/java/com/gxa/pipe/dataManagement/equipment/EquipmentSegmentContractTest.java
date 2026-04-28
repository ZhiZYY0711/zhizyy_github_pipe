package com.gxa.pipe.dataManagement.equipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentSegmentContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void addRequestAcceptsPipeSegmentId() throws Exception {
        EquipmentAddRequest request = objectMapper.readValue(
                """
                {
                  "type": "压力传感器",
                  "area_id": "320100",
                  "pipe_id": "1",
                  "pipe_segment_id": "3",
                  "status": "正常",
                  "responsible": "1"
                }
                """,
                EquipmentAddRequest.class);

        assertThat(request.getPipeSegmentId()).isEqualTo("3");
    }

    @Test
    void responseSerializesPipeSegmentFields() throws Exception {
        EquipmentResponse response = new EquipmentResponse();
        response.setPipeSegmentId("3");
        response.setPipeSegmentName("南京市-苏州市段");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"pipe_segment_id\":\"3\"");
        assertThat(json).contains("\"pipe_segment_name\":\"南京市-苏州市段\"");
    }
}
