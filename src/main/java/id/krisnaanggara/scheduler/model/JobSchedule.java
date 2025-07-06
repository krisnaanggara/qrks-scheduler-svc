package id.krisnaanggara.scheduler.model;

import id.krisnaanggara.scheduler.model.constant.TaskType;
import io.smallrye.common.constraint.NotNull;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "job_schedule")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
public class JobSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_schedule_seq")
    @SequenceGenerator(name = "job_schedule_seq", sequenceName = "job_schedule_seq")
    private Long id;

    @NotNull
    @Column(name = "job_name", unique = true, nullable = false)
    private String jobName;

    @NotNull
    @Column(name = "job_group", nullable = false)
    private String jobGroup;

    @NotNull
    @Column(name = "job_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskType jobType;

    @NotNull
    @Column(name = "cron_job", nullable = false)
    private String cronJob;

    @Column(name = "job_params", columnDefinition = "TEXT")
    private String jobParams;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "update_date")
    private Date updatedDate;

    @Column(name = "jobScheduleParentId")
    private Long jobScheduleParentId;

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof JobSchedule)){
            return false;
        }
        return id != null && id.equals(((JobSchedule) o).id);
    }

    @Override
    public String toString() {
        return "JobSchedule{" +
                "id=" + id +
                ", jobName='" + jobName + '\'' +
                ", jobGroup='" + jobGroup + '\'' +
                ", jobType=" + jobType +
                ", cronJob='" + cronJob + '\'' +
                ", jobParams='" + jobParams + '\'' +
                ", updatedDate=" + updatedDate +
                ", jobScheduleParentId=" + jobScheduleParentId +
                '}';
    }
}
