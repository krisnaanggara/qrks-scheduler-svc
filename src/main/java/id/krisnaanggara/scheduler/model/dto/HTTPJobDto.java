package id.krisnaanggara.scheduler.model.dto;

import id.krisnaanggara.scheduler.model.constant.TaskType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HTTPJobDto {
    private String url;
    private String body;
    private String method;
    private Boolean auth;
    private String clientId;
    private String clientSecret;
    private String userId;
    private String userSecret;
    private String keycloakServer;
    private String retryCount;
    private Boolean continueFromParent;
    private String restTimeout;
    private Boolean sendJobScheduleHistoryId;
}
