package id.krisnaanggara.scheduler.repository;

import id.krisnaanggara.scheduler.model.JobScheduleHistory;
import id.krisnaanggara.scheduler.model.constant.JobStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class JobScheduleHistoryRepository implements PanacheRepository<JobScheduleHistory> {
    public JobScheduleHistory findById(String id){
        return find("id", UUID.fromString(id)).firstResult();
    }

    public List<JobScheduleHistory> findTodayHistoryByJobScheduleIdAndStatus(Long id, JobStatus jobStatus){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Map queryParams = new HashMap<>();
        queryParams.put("jobScheduleId", id);
        queryParams.put("jobStatus", jobStatus);
        queryParams.put("todayDate", sdf.format(new Date()));
        return list("from JobScheduleHistory where jobSchedule.id = :jobScheduleId and jobStatus = :jobStatus and to_char(triggerTime, 'dd-MM-yyyy') = :todayDate", queryParams);
    }

    public List<JobScheduleHistory> findTodayHistoryByJobScheduleId(Long id){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Map queryParams = new HashMap<>();
        queryParams.put("jobScheduleId", id);
        queryParams.put("todayDate", sdf.format(new Date()));
        return list("from JobScheduleHistory where jobSchedule.id = :jobScheduleId and to_char(triggerTime, 'dd-MM-yyyy') = :todayDate", queryParams);
    }
}
