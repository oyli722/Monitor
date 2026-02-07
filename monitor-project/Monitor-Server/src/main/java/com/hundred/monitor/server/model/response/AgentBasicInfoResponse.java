package com.hundred.monitor.server.model.response;

import com.hundred.monitor.commonlibrary.monitor.response.CommonResponse;
import com.hundred.monitor.server.model.entity.Agent;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentBasicInfoResponse extends CommonResponse {
    private List<Agent>  basicInfoList;

    public static AgentBasicInfoResponse success(List<Agent> basicInfoList) {
        AgentBasicInfoResponse response = new AgentBasicInfoResponse();
        response.setSuccess(true);
        response.setBasicInfoList(basicInfoList);
        return response;
    }
    public static AgentBasicInfoResponse error(String message) {
        AgentBasicInfoResponse response = new AgentBasicInfoResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
