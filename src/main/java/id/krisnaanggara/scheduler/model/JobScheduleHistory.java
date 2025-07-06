package id.krisnaanggara.scheduler.model;

import id.krisnaanggara.scheduler.model.constant.JobStatus;
import id.krisnaanggara.scheduler.model.constant.TaskType;
import io.smallrye.common.constraint.NotNull;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "job_schedule_history")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
public class JobScheduleHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    //upgrade hibernate
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false)
    private JobSchedule jobSchedule;

    @NotNull
    @Column(name = "job_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    @Column(name = "jobResponse", columnDefinition = "TEXT")
    private String jobResponse;

    @Column(name = "trigger_time")
    private Date triggerTime;

    @Column(name = "complete_time")
    private Date completeTime;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "update_date")
    private Date updatedDate;

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof JobScheduleHistory)){
            return false;
        }
        return id != null && id.equals(((JobScheduleHistory) o).id);
    }

    @Override
    public String toString() {
        return "JobScheduleHistory{" +
                "id=" + id +
                ", jobSchedule=" + jobSchedule +
                ", jobStatus=" + jobStatus +
                ", jobResponse='" + jobResponse + '\'' +
                ", triggerTime=" + triggerTime +
                ", completeTime=" + completeTime +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                '}';
    }
}
