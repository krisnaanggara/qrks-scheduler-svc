package id.krisnaanggara.scheduler.utils;

import id.krisnaanggara.scheduler.model.constant.Const;
import id.krisnaanggara.scheduler.service.JobScheduleHistoryService;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.OAuth2WebClient;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RestUtil {

    private final Logger logger = LoggerFactory.getLogger(RestUtil.class);

    @Inject
    Vertx vertx;

    @Inject
    JobScheduleHistoryService jobSheduleHistoryService;

    public void request(String jobScheduleHistoryId, String jobName, JobDataMap dataMap) {
        HttpMethod httpMethod = HttpMethod.valueOf(dataMap.getString(Const.LABEL_METHOD));
        Boolean auth = dataMap.getBooleanFromString(Const.LABEL_AUTH) != null;
        if(auth){
            auth = dataMap.getBooleanFromString(Const.LABEL_AUTH);
        }

        try {
            if(auth) {
                String clientId = dataMap.getString(Const.LABEL_CLIENT_ID);
                String clientSecret = dataMap.getString(Const.LABEL_CLIENT_SECRET);
                String userId = dataMap.getString(Const.LABEL_USER_ID);
                String userSecret = dataMap.getString(Const.LABEL_USER_SECRET);
                String keycloakServer = dataMap.getString(Const.LABEL_KEYCLOAK_SERVER);

                OpenIDConnectAuth.discover(
                                vertx,
                                new OAuth2Options()
                                        .setClientId(clientId)
                                        .setClientSecret(clientSecret)
                                        .setSite(keycloakServer))
                        .onSuccess(oauth2 -> {
                            OAuth2WebClient client = OAuth2WebClient.create(WebClient.create(vertx), oauth2)
                                    .withCredentials(new UsernamePasswordCredentials(userId, userSecret));
                            try {
                                executeRestClient(client, dataMap, jobScheduleHistoryId, jobName, httpMethod);
                            } catch (Exception e) {
                                logger.error("Error rest call to API {}", e.getMessage());
                            }
                        })
                        .onFailure(err -> {
                            logger.info("Error Authentication {}, For Job {}, Cause by: {}", "scheduler-service", jobName,  err.getMessage());
                        });
            } else {
                WebClient client = WebClient.create(vertx);
                executeRestClient(client, dataMap, jobScheduleHistoryId, jobName, httpMethod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void executeRestClient(WebClient client, JobDataMap dataMap, String jobScheduleHistoryId, String jobName, HttpMethod httpMethod) throws Exception {
        String endpoint = dataMap.getString(Const.LABEL_URL);
        logger.info("Execute rest call: "+endpoint);
        String parameter = dataMap.getString(Const.LABEL_BODY);
        Boolean sendJobScheduleHistoryId = dataMap.getBooleanFromString(Const.LABEL_SEND_JOB_SCHEDULE_HISTORY_ID) != null? dataMap.getBooleanFromString(Const.LABEL_SEND_JOB_SCHEDULE_HISTORY_ID): false;
        String cronExp = dataMap.getString(Const.LABEL_CRON);
        Integer restTimeout = Integer.parseInt(dataMap.getString(Const.LABEL_REST_TIMEOUT) != null && !dataMap.getString(Const.LABEL_REST_TIMEOUT).equals("") ? dataMap.getString(Const.LABEL_REST_TIMEOUT): ConfigProvider.getConfig().getValue("job-schedule.timeout", String.class));
        String newEndpoint = endpoint;
        JsonObject bodyJson = null;
        if(!"".equals(parameter) && parameter != null && !"null".equals(parameter)) {
            if(parameter.contains("{")) {
                bodyJson = new JsonObject(parameter);
                if(sendJobScheduleHistoryId) {
                    bodyJson.put(Const.LABEL_JOB_SCHEDULE_HISTORY_ID, jobScheduleHistoryId);
                }
            } else {
                if(HttpMethod.GET.equals(httpMethod)) {
                    if(sendJobScheduleHistoryId) {
                        newEndpoint = endpoint+parameter+"/"+jobScheduleHistoryId;
                    } else {
                        newEndpoint = endpoint+parameter;
                    }
                }
            }
        }
        client
                .requestAbs(httpMethod, newEndpoint)
                .timeout(restTimeout)
                .sendJson(bodyJson)
                .onSuccess(ar -> {
                    HttpResponse<Buffer> response = ar;
                    Map jobResponseMap = new HashMap<>();
                    jobResponseMap.put(Const.LABEL_JOB_SCHEDULE_HISTORY_ID, jobScheduleHistoryId);
                    jobResponseMap.put(Const.LABEL_BODY, response.bodyAsString());
                    jobSheduleHistoryService.asyncUpdate(jobResponseMap);
                    logger.info("TASK {} : {},  ACCESING : {}, METHOD : {}, PARAMETERS : {}, STATUS : {}, BODY :{}", jobName, cronExp, endpoint, httpMethod, parameter, response.statusMessage(), response.bodyAsString());
                })
                .onFailure(ar -> {
                    logger.error("ERROR Execute TASK {}, CAUSE by: {}", jobName, ar.getCause().getMessage());
                });
    }
}

