package id.krisnaanggara.scheduler.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HTTPJobResponseDto {
    private String jobScheduleHistoryId;
    private String status;
    private String response;
}
