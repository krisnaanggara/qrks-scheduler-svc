package id.krisnaanggara.scheduler.model.dto;

import id.krisnaanggara.scheduler.model.constant.TaskType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateJobDto {
    private String name;
    private String group;
    private TaskType taskType;
    private String cronType;
    private HTTPJobDto httpParameter;
}
