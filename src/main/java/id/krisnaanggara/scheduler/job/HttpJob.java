package id.krisnaanggara.scheduler.job;

import id.krisnaanggara.scheduler.model.JobSchedule;
import id.krisnaanggara.scheduler.model.JobScheduleHistory;
import id.krisnaanggara.scheduler.model.constant.Const;
import id.krisnaanggara.scheduler.model.constant.JobStatus;
import id.krisnaanggara.scheduler.service.JobScheduleHistoryService;
import id.krisnaanggara.scheduler.service.JobScheduleService;
import id.krisnaanggara.scheduler.utils.RestUtil;
import jakarta.inject.Inject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DisallowConcurrentExecution
public class HttpJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(HttpJob.class);

    @Inject
    RestUtil restUtil;

    @Inject
    JobScheduleService jobSheduleService;

    @Inject
    JobScheduleHistoryService jobSheduleHistoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        logger.info("Execute job: "+jobName);
        Boolean continueFromParent = dataMap.getBooleanFromString(Const.LABEL_CONTINUE_FROM_PARENT) != null;
        if(continueFromParent){
            continueFromParent = dataMap.getBooleanFromString(Const.LABEL_CONTINUE_FROM_PARENT);
        }
        Boolean triggerAction = true;
        JobSchedule jobSchedule = jobSheduleService.getByJobName(jobName);
        if(jobSchedule.getJobScheduleParentId() != null) {
            logger.debug("Check today job parent history for job {}", jobName);
            List<JobScheduleHistory> parentHistoryList = jobSheduleHistoryService.findTodayHistoryByJobScheduleIdAndStatus(jobSchedule.getJobScheduleParentId(), JobStatus.COMPLETED);
            if(parentHistoryList != null & parentHistoryList.size() > 0) {
                logger.info("Today job parent, from job {} has been completed", jobName);
                triggerAction = false;
                if(continueFromParent) {
                    logger.info("Continue job {}, after job parent completed", jobName);
                    triggerAction = true;
                }
            } else {
                if(continueFromParent) {
                    logger.info("Parent job from job {}, hasn't been completed. Can't continue the job!", jobName);
                    triggerAction = false;
                } else {
                    logger.info("Continue job {} anyway", jobName);
                    triggerAction = true;
                }
            }
        }
        Integer retryCount = Integer.parseInt(!dataMap.getString(Const.LABEL_RETRY_COUNT).equals("")? dataMap.getString(Const.LABEL_RETRY_COUNT): "0");
        if(triggerAction && retryCount > 0) {
            logger.debug("Check today job history for {}", jobName);
            List<JobScheduleHistory> jobHistoryList = jobSheduleHistoryService.findTodayHistoryByJobScheduleId(jobSchedule.getId());
            if(jobHistoryList.size() >= retryCount) {
                logger.info("Job {}, run out of retry({}) for today!", jobName, retryCount);
                triggerAction = false;
            } else {
                for(JobScheduleHistory jobScheduleHistory: jobHistoryList) {
                    if(JobStatus.COMPLETED.equals(jobScheduleHistory.getJobStatus()) || JobStatus.INPROGRESS.equals(jobScheduleHistory.getJobStatus())) {
                        logger.info("Retry Job {} failed, previous job with {} status for today is found!", jobName, jobScheduleHistory.getJobStatus());
                        triggerAction = false;
                        break;
                    }
                }
            }
        }
        if(triggerAction) {
            JobScheduleHistory jobScheduleHistory = new JobScheduleHistory();
            jobScheduleHistory.setJobSchedule(jobSchedule);
            jobScheduleHistory = jobSheduleHistoryService.save(jobScheduleHistory);
            logger.info("Assign JobScheduleHistory Id:{} to job {}",jobScheduleHistory.getId(), jobName);
            restUtil.request(jobScheduleHistory.getId().toString(), jobName, dataMap);
        }
    }
}

