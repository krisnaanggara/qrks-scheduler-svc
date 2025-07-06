package id.krisnaanggara.scheduler.service;

import id.krisnaanggara.scheduler.model.JobSchedule;
import id.krisnaanggara.scheduler.repository.JobScheduleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@ActivateRequestContext
public class JobScheduleService {
    private final Logger logger = LoggerFactory.getLogger(JobScheduleService.class);

    @Inject
    JobScheduleRepository jobScheduleRepository;

    @Inject
    JobService jobService;

    @Inject
    JobScheduleHistoryService jobScheduleHistoryService;

    public List<JobSchedule> findAll(){
        logger.debug("Request to get all JobSchedule");
        return jobScheduleRepository.listAll();
    }

    public Optional<JobSchedule> findOne(Long id){
        logger.debug("Request to get JobSchedule : {}", id);
        return jobScheduleRepository.findByIdOptional(id);
    }

    public List<JobSchedule> findJobScheduleChild(Long id){
        logger.debug("Request to get JobSchedule : {}", id);
        return jobScheduleRepository.find("jobScheduleParentId",id).list();
    }

    public List<JobSchedule> findByGroupName(String jobGroup) {
        logger.debug("Request to find all JobSchedule by jobGroup", jobGroup);
        return jobScheduleRepository.findAllByGroupName(jobGroup);
    }

    public JobSchedule getByJobName(String jobName){
        logger.debug("Request to get JobSchedule by jobName : {}", jobName);
        return jobScheduleRepository.findByJobName(jobName);
    }

    @Transactional
    public JobSchedule save(JobSchedule jobSchedule) throws SchedulerException {
        boolean isUpdate = false;
        Date executeDatetime = new Date();
        if(jobSchedule.getId() != null){
            isUpdate = true;
        }
        logger.debug("Request to save Job Schedule: {}", jobSchedule);
        if(isUpdate){
            JobSchedule jobScheduleUpdate = findOne(jobSchedule.getId()).get();
            jobScheduleUpdate.setJobParams(jobSchedule.getJobParams());
            jobScheduleUpdate.setCronJob(jobSchedule.getCronJob());
            jobScheduleUpdate.setUpdatedDate(executeDatetime);
            jobScheduleRepository.persist(jobScheduleUpdate);
            jobService.updateJob(jobService.getUpdateJobDTO(jobSchedule));
        }else{
            jobSchedule.setCreatedDate(executeDatetime);
            jobScheduleRepository.persist(jobSchedule);
            jobService.createJob(jobService.getJobDTO(jobSchedule));
        }
        return jobSchedule;
    }

    @Transactional
    public void delete(Long id) throws Exception{
        logger.debug("Request to delete JobSchedule : {}", id);
        JobSchedule jobSchedule = jobScheduleRepository.findById(id);
        if(jobSchedule != null){
            List<JobSchedule> jobScheduleChilds = findJobScheduleChild(id);
            if(jobScheduleChilds != null && jobScheduleChilds.size() > 0){
                throw new Exception("Can't delete JobSchedule "+jobSchedule.getJobName()+", job child is found!");
            }
            jobScheduleHistoryService.delete(id);
            jobScheduleRepository.deleteById(id);
            jobService.deleteJob(jobService.getCommonJobDTO(jobSchedule.getJobName(), jobSchedule.getJobGroup()));
        }
    }
}
