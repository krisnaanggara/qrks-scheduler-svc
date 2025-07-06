package id.krisnaanggara.scheduler.repository;

import id.krisnaanggara.scheduler.model.JobSchedule;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class JobScheduleRepository implements PanacheRepository<JobSchedule> {
    public List<JobSchedule> findAllByGroupName(String jobGroup) {
        return list("jobGroup", jobGroup);
    }

    public JobSchedule findByJobName(String jobName){
        return find("jobName", jobName).firstResult();
    }
}
