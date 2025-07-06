package id.krisnaanggara.scheduler.service;

import id.krisnaanggara.scheduler.job.HttpJob;
import id.krisnaanggara.scheduler.model.JobSchedule;
import id.krisnaanggara.scheduler.model.constant.Const;
import id.krisnaanggara.scheduler.model.constant.TaskType;
import id.krisnaanggara.scheduler.model.dto.*;
import id.krisnaanggara.scheduler.utils.JsonUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    @Inject
    Scheduler scheduler;

    public void createJob(CreateJobDto parameter) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        TriggerKey triggerKey = new TriggerKey(parameter.getName() + Const.TRIGGER_SUFFIX, parameter.getGroup());
        if (TaskType.HTTP.equals(parameter.getTaskType())) {
            setJobDataMap(jobDataMap, parameter.getHttpParameter(), parameter.getCronType());
        }
        JobDetail jobDetail = JobBuilder.newJob((Class<? extends Job>) getJobClass(parameter.getTaskType())).withIdentity(parameter.getName(), parameter.getGroup()).setJobData(jobDataMap).storeDurably(true).build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(parameter.getCronType()))
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public List<GetJobDto> getJob() throws SchedulerException {
        List<GetJobDto> results = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                GetJobDto getJobDTO = new GetJobDto();
                getJobDTO.setName(jobKey.getName());
                getJobDTO.setGroup(jobKey.getGroup());
                results.add(getJobDTO);
            }
        }
        return results;
    }

    public void updateJob(UpdateJobDto parameter) throws SchedulerException {
        TriggerKey triggerKey = new TriggerKey(parameter.getName() + Const.TRIGGER_SUFFIX, parameter.getGroup());
        JobDetail jobDetail = scheduler.getJobDetail(new JobKey(parameter.getName(), parameter.getGroup()));
        if (TaskType.HTTP.equals(parameter.getTaskType())) {
            setJobDataMap(jobDetail.getJobDataMap(), parameter.getHttpParameter(), parameter.getCronType());
            scheduler.addJob(jobDetail, true);
        }
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(parameter.getCronType()))
                .build();
        scheduler.rescheduleJob(triggerKey, trigger);
    }

    private void setJobDataMap(JobDataMap jobDataMap, HTTPJobDto httpParameter, String cronType) {
        jobDataMap.put(Const.LABEL_URL, httpParameter.getUrl());
        jobDataMap.put(Const.LABEL_BODY, httpParameter.getBody());
        jobDataMap.put(Const.LABEL_METHOD, httpParameter.getMethod());
        jobDataMap.put(Const.LABEL_CRON, cronType);
        jobDataMap.put(Const.LABEL_AUTH, httpParameter.getAuth() != null? httpParameter.getAuth().toString():"false");
        jobDataMap.put(Const.LABEL_CLIENT_ID, httpParameter.getClientId());
        jobDataMap.put(Const.LABEL_CLIENT_SECRET, httpParameter.getClientSecret());
        jobDataMap.put(Const.LABEL_USER_ID, httpParameter.getUserId());
        jobDataMap.put(Const.LABEL_USER_SECRET, httpParameter.getUserSecret());
        jobDataMap.put(Const.LABEL_KEYCLOAK_SERVER, httpParameter.getKeycloakServer());
        jobDataMap.put(Const.LABEL_RETRY_COUNT, httpParameter.getRetryCount());
        jobDataMap.put(Const.LABEL_CONTINUE_FROM_PARENT, httpParameter.getContinueFromParent() != null? httpParameter.getContinueFromParent().toString():"false");
        jobDataMap.put(Const.LABEL_REST_TIMEOUT, httpParameter.getRestTimeout());
        jobDataMap.put(Const.LABEL_SEND_JOB_SCHEDULE_HISTORY_ID, httpParameter.getSendJobScheduleHistoryId() != null? httpParameter.getSendJobScheduleHistoryId().toString():"false");
    }

    public void deleteJob(CommonJobDto commonJobDto) throws SchedulerException {
        scheduler.deleteJob(new JobKey(commonJobDto.getName(), commonJobDto.getGroup()));
        TriggerKey triggerKey = new TriggerKey(commonJobDto.getName()+ Const.TRIGGER_SUFFIX, commonJobDto.getGroup());
        scheduler.unscheduleJob(triggerKey);
    }

    public void pauseJob(CommonJobDto commonJobDTO) throws SchedulerException  {
        scheduler.pauseJob(JobKey.jobKey(commonJobDTO.getName(), commonJobDTO.getGroup()));
    }

    public void resumeJob(CommonJobDto commonJobDTO) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(commonJobDTO.getName(), commonJobDTO.getGroup()));
    }

    public void pauseJobGroup(CommonGroupJobDto commonGroupJobDTO) throws SchedulerException {
        scheduler.pauseJobs(GroupMatcher.jobGroupEquals(commonGroupJobDTO.getGroup()));
    }

    public void resumeJobGroup(CommonGroupJobDto commonGroupJobDTO) throws SchedulerException {
        scheduler.resumeJobs(GroupMatcher.jobGroupEquals(commonGroupJobDTO.getGroup()));
    }

    public Object getJobClass(TaskType taskType) {
        if (taskType.equals(TaskType.HTTP)) {
            return HttpJob.class;
        }
        return null;
    }

    public void stopAllJob() {
        try {
            scheduler.pauseAll();
        } catch (SchedulerException e) {
            logger.error("Exception while stopping all job {}", e);
        }
    }

    public void resumeAllJob() {
        try {
            scheduler.resumeAll();
        } catch (SchedulerException e) {
            logger.error("Exception while resuming all job {}", e);
        }
    }

    public CommonJobDto getCommonJobDTO(String jobName, String groupName) {
        return new CommonJobDto(jobName, groupName);
    }

    public CreateJobDto getJobDTO(JobSchedule jobSchedule) {
        return new CreateJobDto(jobSchedule.getJobName(), jobSchedule.getJobGroup(),
                TaskType.valueOf(jobSchedule.getJobType().toString()), jobSchedule.getCronJob(),
                JsonUtil.fromJson(jobSchedule.getJobParams(), HTTPJobDto.class));
    }

    public UpdateJobDto getUpdateJobDTO(JobSchedule jobSchedule) {
        return new UpdateJobDto(jobSchedule.getJobName(), jobSchedule.getJobGroup(),
                TaskType.valueOf(jobSchedule.getJobType().toString()), jobSchedule.getCronJob(),
                JsonUtil.fromJson(jobSchedule.getJobParams(), HTTPJobDto.class));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllJobs() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String jobName = jobKey.getName();
                    String jobGroup = jobKey.getGroup();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    Date scheduleTime = triggers.get(0).getStartTime() != null?triggers.get(0).getStartTime():null;
                    Date nextFireTime = triggers.get(0).getNextFireTime() != null?triggers.get(0).getNextFireTime():null;
                    Date lastFiredTime = triggers.get(0).getPreviousFireTime() != null?triggers.get(0).getPreviousFireTime():null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("jobName", jobName);
                    map.put("groupName", jobGroup);
                    map.put("scheduleTime", scheduleTime != null?sdf.format(scheduleTime):null);
                    map.put("lastFiredTime", lastFiredTime != null?sdf.format(lastFiredTime):null);
                    map.put("nextFireTime", nextFireTime != null?sdf.format(nextFireTime):null);
                    checkJobStatus(map, jobName, jobGroup);
                    list.add(map);
                    logger.debug("Job details:");
                    logger.debug("Job Name: {}, Group Name: {}, Schedule Time: {}",jobName, groupName, scheduleTime);
                }
            }
        } catch (SchedulerException e) {
            logger.error("SchedulerException while fetching all jobs. error message. error {}", e);
        }
        return list;
    }

    private void checkJobStatus(Map<String, Object> map, String jobName, String jobGroup) {
        if(isJobRunning(jobName, jobGroup)){
            map.put("jobStatus", "RUNNING");
        }else{
            String jobState = getJobState(jobName, jobGroup);
            map.put("jobStatus", jobState);
        }
    }

    public boolean isJobRunning(String jobName, String jobGroup) {
        logger.debug("Request received to check if job is running");
        logger.debug("Parameters received for checking job is running now : jobName : {} jobGroup : {}", jobName, jobGroup);
        try {
            List<JobExecutionContext> currentJobs = scheduler.getCurrentlyExecutingJobs();
            if(currentJobs!=null){
                for (JobExecutionContext jobCtx : currentJobs) {
                    String jobNameDB = jobCtx.getJobDetail().getKey().getName();
                    String groupNameDB = jobCtx.getJobDetail().getKey().getGroup();
                    if (jobName.equalsIgnoreCase(jobNameDB) && jobGroup.equalsIgnoreCase(groupNameDB)) {
                        return true;
                    }
                }
            }
        } catch (SchedulerException e) {
            logger.error("SchedulerException while checking job with key: {} {} is running. error {}", jobName, jobGroup, e);
            return false;
        }
        return false;
    }

    public String getJobState(String jobName, String jobGroup) {
        logger.debug("JobServiceImpl.getJobState()");
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
            if(!triggers.isEmpty()){
                for (Trigger trigger : triggers) {
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    if (Trigger.TriggerState.PAUSED.equals(triggerState)) {
                        return "PAUSED";
                    }else if (Trigger.TriggerState.BLOCKED.equals(triggerState)) {
                        return "BLOCKED";
                    }else if (Trigger.TriggerState.COMPLETE.equals(triggerState)) {
                        return "COMPLETE";
                    }else if (Trigger.TriggerState.ERROR.equals(triggerState)) {
                        return "ERROR";
                    }else if (Trigger.TriggerState.NONE.equals(triggerState)) {
                        return "NONE";
                    }else if (Trigger.TriggerState.NORMAL.equals(triggerState)) {
                        return "SCHEDULED";
                    }
                }
            }
        } catch (SchedulerException e) {
            logger.error("SchedulerException while checking job with name and group exist: {}", e);
        }
        return null;
    }

    public void forceJob(CommonJobDto commonJobDTO) throws SchedulerException {
        scheduler.triggerJob(new JobKey(commonJobDTO.getName(), commonJobDTO.getGroup()));
    }

}
