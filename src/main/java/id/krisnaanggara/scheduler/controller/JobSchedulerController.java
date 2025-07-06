package id.krisnaanggara.scheduler.controller;

import id.krisnaanggara.scheduler.model.JobSchedule;
import id.krisnaanggara.scheduler.model.JobScheduleHistory;
import id.krisnaanggara.scheduler.model.dto.CommonResponseDto;
import id.krisnaanggara.scheduler.model.dto.HTTPJobResponseDto;
import id.krisnaanggara.scheduler.service.JobScheduleHistoryService;
import id.krisnaanggara.scheduler.service.JobScheduleService;
import id.krisnaanggara.scheduler.service.JobService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api")
@ApplicationScoped
public class JobSchedulerController {
    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerController.class);

    private Response createResponse(Response.Status status, String type, Object result) {
        return Response
                .status(status)
                .type(type)
                .entity(result)
                .build();
    }

    @Inject
    JobScheduleService jobScheduleService;

    @Inject
    JobService jobService;

    @Inject
    JobScheduleHistoryService jobScheduleHistoryService;

    private CommonResponseDto createResponseMessage(String jobName, String groupName, String requestName, String responseMessage){
        return new CommonResponseDto(jobName, groupName, requestName, responseMessage);
    }

    @POST
    @Path("/job-schedules")
    public Response createJobSchedule(@Valid JobSchedule jobSchedule) throws SchedulerException {
        logger.debug("REST request to save JobSchedule : {}", jobSchedule);
        if(jobSchedule.getId() != null){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobSchedule.getJobName(), jobSchedule.getJobGroup(), "createJobSchedule", "Can't create job schedule, It's already have an ID"));
        }
        if(jobScheduleService.getByJobName(jobSchedule.getJobName())!=null){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobSchedule.getJobName(),jobSchedule.getJobGroup(), "createJobSchedule", "Job Schedule with this name, already exist"));
        }
        JobSchedule result = jobScheduleService.save(jobSchedule);
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, result);
    }

    @PUT
    @Path("/job-schedules")
    public Response updateJobSchedule(@Valid JobSchedule jobSchedule) throws SchedulerException {
        logger.debug("REST request to update JobSchedule : {}", jobSchedule);
        if(jobSchedule.getId() == null){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobSchedule.getJobName(), jobSchedule.getJobGroup(), "updateJobSchedule", "Invalid job schedules id"));
        }
        JobSchedule result = jobScheduleService.save(jobSchedule);
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, result);
    }

    @GET
    @Path("/job-schedules")
    public Response findAllJobSchedules(){
        logger.debug("REST request to get a page of JobSchedules");
        List<JobSchedule> jobScheduleList = jobScheduleService.findAll();
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, jobScheduleList);
    }

    @GET
    @Path("/job-schedules/{id}")
    public Response getJobSchedules(Long id){
        logger.debug("REST request to get JobSchedule : {}", id);
        Optional<JobSchedule> jobSchedule = jobScheduleService.findOne(id);
        if(jobSchedule.isPresent()){
            return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, jobSchedule);
        }else{
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(null, null, "getJobSchedule", "No data found!"));
        }
    }

    @DELETE
    @Path("/job-schedule/{id}")
    public Response deleteJobSchedule(Long id) throws Exception {
        logger.debug("REST request to delete JobSchedule : {}", id);
        jobScheduleService.delete(id);
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                createResponseMessage(null, null, "deleteJobSchedule", "Job schedule : "+id+" deleted!"));
    }

    @GET
    @Path("/job-schedule/findAll/{groupName}")
    public Response findJobSchedulesByGroupName(String groupName){
        logger.debug("REST request to find JobScheduler by group name {}",groupName);
        List<JobSchedule> jobScheduleList = jobScheduleService.findByGroupName(groupName);
        if(!jobScheduleList.isEmpty()){
            return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, jobScheduleList);
        }else{
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(null, groupName, "findJobSchedulesByGroupName", "No Data found!"));
        }
    }

    @GET
    @Path("/stopAllJob")
    public Response stopAllJob(){
        logger.debug("REST request to stop all job");
        jobService.stopAllJob();
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                createResponseMessage(null, null, "stopAllJob", "All job paused"));
    }

    @GET
    @Path("/resumeAllJob")
    public Response resumeAllJob(){
        logger.debug("REST request to resume all job");
        jobService.resumeAllJob();
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                createResponseMessage(null, null, "resumeAllJob", "All job resumed"));
    }

    @GET
    @Path("/resumeJob/{groupName}/{jobName}")
    public Response resumeJob(String groupName, String jobName){
        logger.debug("REST request to resume job : {}", jobName);
        try{
            jobService.resumeJob(jobService.getCommonJobDTO(jobName,groupName));
            return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "resumeJob", "Resume success"));
        }catch (Exception e){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "resumeJob", "Resume failed"));
        }
    }

    @GET
    @Path("/pauseJob/{groupName}/{jobName}")
    public Response pauseJob(String groupName, String jobName){
        logger.debug("REST request to pause job : {}", jobName);
        try{
            jobService.pauseJob(jobService.getCommonJobDTO(jobName,groupName));
            return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "pauseJob", "Pause success"));
        }catch (Exception e){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "pauseJob", "Pause failed"));
        }
    }

    @GET
    @Path("/getAllJobStatus")
    public Response getAllJobStatus(){
        logger.debug("REST request to list all job");
        List<Map<String, Object>> list = jobService.getAllJobs();
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, list);
    }

    @GET
    @Path("/getStatusJob/{groupName}/{jobName}")
    public Response getStatusJob(String groupName, String jobName){
        logger.debug("REST get status job : {}", jobName);
        String jobStatus = jobService.getJobState(jobName, groupName);
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                createResponseMessage(jobName, groupName, "getStatusjob", jobStatus));
    }

    @GET
    @Path("/forceJob/{groupName}/{jobName}")
    public Response forceJob(String groupName, String jobName){
        logger.debug("REST request to force job: {}", jobName);
        try{
            jobService.forceJob(jobService.getCommonJobDTO(jobName, groupName));
            return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "forceJob", "Force success"));
        }catch (Exception e){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(jobName, groupName, "forceJob", "Force failed"));
        }
    }

    @POST
    @Path("/notify-job-result")
    public Response notifyJobResult(@Valid HTTPJobResponseDto jobResponse) throws Exception{
        logger.debug("REST request to update job response & status : {}", jobResponse);
        if(jobResponse.getJobScheduleHistoryId() == null || jobResponse.getJobScheduleHistoryId().equals("")){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(null, null, "notifyJobResult", "Please provide job history id"));
        }
        if(jobResponse.getStatus() == null || jobResponse.getStatus().equals("")){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(null, null, "notifyJobResult", "Please provide job history status"));
        }
        if(jobResponse.getResponse() == null || jobResponse.getResponse().equals("")){
            return createResponse(Response.Status.BAD_REQUEST, MediaType.APPLICATION_JSON,
                    createResponseMessage(null, null, "notifyJobResult", "Please provide job history response result"));
        }
        JobScheduleHistory result = jobScheduleHistoryService.notifyJobResult(jobResponse);
        return createResponse(Response.Status.OK, MediaType.APPLICATION_JSON, result);
    }
}
