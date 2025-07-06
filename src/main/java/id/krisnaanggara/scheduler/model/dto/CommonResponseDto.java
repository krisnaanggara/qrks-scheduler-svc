package id.krisnaanggara.scheduler.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommonResponseDto {
    private String jobName;
    private String groupName;
    private String requestName;
    private String responseMessage;
}
