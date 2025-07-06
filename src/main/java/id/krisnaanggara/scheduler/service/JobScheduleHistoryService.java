package id.krisnaanggara.scheduler.service;

import id.krisnaanggara.scheduler.model.JobScheduleHistory;
import id.krisnaanggara.scheduler.model.constant.Const;
import id.krisnaanggara.scheduler.model.constant.JobStatus;
import id.krisnaanggara.scheduler.model.dto.HTTPJobResponseDto;
import id.krisnaanggara.scheduler.repository.JobScheduleHistoryRepository;
import id.krisnaanggara.scheduler.utils.JsonUtil;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@ActivateRequestContext
public class JobScheduleHistoryService {

    private final Logger log = LoggerFactory.getLogger(JobScheduleHistoryService.class);

    @Inject
    JobScheduleHistoryRepository jobScheduleHistoryRepository;

    @Transactional
    public JobScheduleHistory save(JobScheduleHistory jobScheduleHistory) {
        boolean isUpdate = false;
        Date executeDateTime = new Date();
        if(jobScheduleHistory.getId() !=  null) {
            isUpdate = true;
        }
        log.debug("Request to save JobScheduleHistory : {}", jobScheduleHistory);
        if(isUpdate) {
            jobScheduleHistory.setCompleteTime(executeDateTime);
            jobScheduleHistory.setUpdatedDate(executeDateTime);
            jobScheduleHistoryRepository.getEntityManager().merge(jobScheduleHistory);
        } else {
            jobScheduleHistory.setCreatedDate(executeDateTime);
            jobScheduleHistory.setTriggerTime(executeDateTime);
            jobScheduleHistory.setJobStatus(JobStatus.TRIGGERED);
            jobScheduleHistoryRepository.persist(jobScheduleHistory);
        }
        return jobScheduleHistory;
    }

    @Transactional
    public void delete(Long jobScheduleId) throws SchedulerException {
        log.debug("Request to delete JobScheduleHistory by JobSchedulerId : {}", jobScheduleId);
        jobScheduleHistoryRepository.delete("jobSchedule.id", jobScheduleId);
    }

    public JobScheduleHistory findById(String id) {
        log.debug("Request to get JobScheduleHistory : {}", id);
        return jobScheduleHistoryRepository.findById(id);
    }

    public List<JobScheduleHistory> findTodayHistoryByJobScheduleIdAndStatus(Long id, JobStatus jobStatus) {
        log.debug("Request to get today JobScheduleHistory by job schedule id: {}", id);
        return jobScheduleHistoryRepository.findTodayHistoryByJobScheduleIdAndStatus(id, jobStatus);
    }

    public List<JobScheduleHistory> findTodayHistoryByJobScheduleId(Long id) {
        log.debug("Request to get today JobScheduleHistory by job schedule id: {}", id);
        return jobScheduleHistoryRepository.findTodayHistoryByJobScheduleId(id);
    }

    @SuppressWarnings("rawtypes")
    public void asyncUpdate(Map jobResponse) {
        log.debug("Async Update");
        Uni.createFrom()
                .item(jobResponse)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .subscribe()
                .with(this::updateWorker, Throwable::printStackTrace);
    }

    @SuppressWarnings("rawtypes")
    private Uni<Void> updateWorker(Map jobResponseMap) {
        HTTPJobResponseDto jobResponseDTO = null;
        String jobScheduleHistoryId = (String) jobResponseMap.get(Const.LABEL_JOB_SCHEDULE_HISTORY_ID);
        String jobResponse = null;
        String jobStatus = null;
        try {
            try {
                jobResponse = (String) jobResponseMap.get(Const.LABEL_BODY);
                jobResponseDTO = JsonUtil.fromJson(jobResponse, HTTPJobResponseDto.class);
                jobStatus = jobResponseDTO.getStatus();
            } catch (Exception e) {
                log.error("Can't parse HTTPJobResponseDTO, message: {}", e.getLocalizedMessage());
            }

            JobScheduleHistory jobScheduleHistory = findById(jobScheduleHistoryId);
            jobScheduleHistory.setJobStatus(jobStatus != null? JobStatus.valueOf(jobStatus): JobStatus.COMPLETED);
            jobScheduleHistory.setCompleteTime(new Date());
            jobScheduleHistory.setJobResponse(jobResponseDTO != null? jobResponseDTO.getResponse(): jobResponse);
            save(jobScheduleHistory);
        } catch (Exception e) {
            log.error("Error update JobScheduleHistory with id: {}, message: {}", jobResponseDTO.getJobScheduleHistoryId(), e.getLocalizedMessage());
        }
        return Uni.createFrom().voidItem();
    }

    public JobScheduleHistory notifyJobResult(HTTPJobResponseDto jobResponse) throws Exception {
        JobScheduleHistory jobScheduleHistory = findById(jobResponse.getJobScheduleHistoryId());
        if(jobScheduleHistory != null) {
            jobScheduleHistory.setJobStatus(JobStatus.valueOf(jobResponse.getStatus()));
            jobScheduleHistory.setJobResponse(jobResponse.getResponse());
            save(jobScheduleHistory);
        } else {
            throw new Exception("Job schedule history with id: "+jobResponse.getJobScheduleHistoryId()+" is not found!");
        }
        return jobScheduleHistory;
    }
}
