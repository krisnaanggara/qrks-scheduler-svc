package id.krisnaanggara.scheduler.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GetJobDto {
    private String name;
    private String group;
}
