package io.boomerang.service;

import static io.cloudevents.core.CloudEventUtils.mapData;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.client.EngineClient;
import io.boomerang.model.TaskCustom;
import io.boomerang.model.TaskRequest;
import io.boomerang.model.TaskTemplate;
import io.boomerang.model.WorkflowRequest;
import io.boomerang.model.enums.RunPhase;
import io.boomerang.model.enums.RunStatus;
import io.boomerang.model.ref.TaskRun;
import io.boomerang.model.ref.TaskType;
import io.boomerang.model.ref.WorkflowRun;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

@Service
public class EventServiceImpl implements EventService {

  @Autowired
  private WorkflowService workflowService;
  
  @Autowired
  private TaskService taskService;
  
  @Autowired
  private EngineClient engineClient;

  private static final Logger logger = LogManager.getLogger(EventServiceImpl.class);

  private static final String TYPE_PREFIX = "io.boomerang.event.status.";
  
  @Override
  public ResponseEntity<?> process(CloudEvent event) {
    // Check if event that we support and return with accepted or rejected. Processing will be done async.
    if (event.getType().startsWith(TYPE_PREFIX)) {
      processAsync(event);
    } else {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }
    logger.info("CloudEvent Processed.");
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }
  
  private Future<Boolean> processAsync(CloudEvent event) {
    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;
      try {
        String eventType = event.getType().substring(TYPE_PREFIX.length());
        logger.info("Event Type: " + eventType);
        ObjectMapper mapper = new ObjectMapper();    
        if ("workflowrun".toLowerCase().equals(eventType.toLowerCase())) {
          PojoCloudEventData<WorkflowRun> data = mapData(
              event,
              PojoCloudEventDataMapper.from(mapper,WorkflowRun.class)
          );
          WorkflowRun workflowRun = data.getValue();
          logger.info(workflowRun.toString());
          WorkflowRequest request = new WorkflowRequest();
          request.setWorkflowRef(workflowRun.getWorkflowRef());
          request.setWorkflowRunRef(workflowRun.getId());
          request.setLabels(workflowRun.getLabels());
          request.setWorkspaces(workflowRun.getWorkspaces());
          if (RunPhase.pending.equals(workflowRun.getPhase()) && RunStatus.ready.equals(workflowRun.getStatus())) {
            logger.info("Create Workflow");
            workflowService.execute(request);
            engineClient.startWorkflow(workflowRun.getId());
          } else if (RunPhase.completed.equals(workflowRun.getPhase())) {
            logger.info("Need to close out the workflow");
            workflowService.terminate(request);
            engineClient.finalizeWorkflow(workflowRun.getId());
          }
        } else if ("taskrun".toLowerCase().equals(eventType.toLowerCase())) {
          PojoCloudEventData<TaskRun> data = mapData(
              event,
              PojoCloudEventDataMapper.from(mapper,TaskRun.class)
          );
          TaskRun taskRun = data.getValue();
          if (RunPhase.pending.equals(taskRun.getPhase()) && RunStatus.ready.equals(taskRun.getStatus())) {
            logger.info("Create Task");
            if (TaskType.template.equals(taskRun.getType())) {
              TaskTemplate request = new TaskTemplate(taskRun);
              logger.info(request.toString());
            taskService.execute(request);
            engineClient.startTask(taskRun.getId());
            } else if (TaskType.custom.equals(taskRun.getType())) {
              TaskCustom request = new TaskCustom(taskRun);
              logger.info(request.toString());
            taskService.execute(request);
            engineClient.startTask(taskRun.getId());
            }
          } else if (RunPhase.completed.equals(taskRun.getPhase())) {
            logger.info("Need to close out the workflow");
            //TODO: Close out any workspaces
          }
          logger.info(taskRun.toString());
        }
        isSuccess = Boolean.TRUE;
      } catch (Exception e) {
        logger.fatal("A fatal error has occurred while processing the message!", e);
      }
      return isSuccess;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

//  @Override
//  public ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeWebhookEvent(String token,
//      String requestUri, String trigger, String workflowId, JsonNode payload,
//      String workflowActivityId, String topic, String status) {
//
//    // Validate Token and WorkflowID. Do first.
//    HttpStatus accessStatus = checkAccess(workflowId, token);
//
//    if (accessStatus != HttpStatus.OK) {
//      return ResponseEntity.status(accessStatus).build();
//    }
//
//    final String eventId = UUID.randomUUID().toString();
//    final String eventType = TYPE_PREFIX + trigger;
//    final URI uri = URI.create(requestUri);
//    String subject = "/" + workflowId;
//
//    // Validate WFE Attributes
//    if ("wfe".equals(trigger) && workflowActivityId != null) {
//      subject = subject + "/" + workflowActivityId + "/" + topic;
//    } else if ("wfe".equals(trigger)) {
//
//      // WFE requires workflowActivityId
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//    }
//
//    if (!"failure".equals(status)) {
//      status = "success";
//    }
//    CustomAttributeExtension statusCAE = new CustomAttributeExtension("status", status);
//
//    // @formatter:off
//    final CloudEventImpl<JsonNode> cloudEvent = CloudEventBuilder.<JsonNode>builder()
//        .withType(eventType)
//        .withExtension(statusCAE)
//        .withId(eventId)
//        .withSource(uri)
//        .withData(payload)
//        .withSubject(subject)
//        .withTime(ZonedDateTime.now())
//        .build();
//    // @formatter:on
//
//    logger.debug("routeWebhookEvent() - CloudEvent: " + cloudEvent);
//
//    forwardCloudEvent(cloudEvent);
//
//    return ResponseEntity.ok().body(cloudEvent);
//  }
//
//  @Override
//  public ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeCloudEvent(
//      CloudEvent<AttributesImpl, JsonNode> cloudEvent, String token, URI uri) {
//
//    // Validate Token and WorkflowID. Do first.
//    String subject = cloudEvent.getAttributes().getSubject().orElse("");
//
//    if (!subject.startsWith("/") || cloudEvent.getData().isEmpty()) {
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//    }
//
//    HttpStatus accessStatus = checkAccess(getWorkflowIdFromSubject(subject), token);
//    if (accessStatus != HttpStatus.OK) {
//      return ResponseEntity.status(accessStatus).build();
//    }
//
//    logger.debug("routeCloudEvent() - CloudEvent Attributes: " + cloudEvent.getAttributes());
//    logger.debug("routeCloudEvent() - CloudEvent Data: " + cloudEvent.getData().get());
//
//    String eventId = UUID.randomUUID().toString();
//    String eventType = TYPE_PREFIX + "custom";
//
//    String status = "success";
//    if (cloudEvent.getExtensions() != null && cloudEvent.getExtensions().containsKey("status")) {
//      String statusExtension = cloudEvent.getExtensions().get("status").toString();
//      if ("failure".equals(statusExtension)) {
//        status = statusExtension;
//      }
//    }
//    CustomAttributeExtension statusCAE = new CustomAttributeExtension("status", status);
//
//    // @formatter:off
//    final CloudEventImpl<JsonNode> forwardedCloudEvent = CloudEventBuilder.<JsonNode>builder()
//        .withType(eventType)
//        .withExtension(statusCAE)
//        .withId(eventId)
//        .withSource(uri)
//        .withData(cloudEvent.getData().get())
//        .withSubject(subject)
//        .withTime(ZonedDateTime.now())
//        .build();
//    // @formatter:on
//
//    forwardCloudEvent(forwardedCloudEvent);
//
//    return ResponseEntity.ok().body(forwardedCloudEvent);
//  }
//
//  private HttpStatus checkAccess(String workflowId, String token) {
//    if (authorizationEnabled) {
//      logger.debug("checkAccess() - Token: " + token);
//
//      if (token != null && !token.isEmpty() && workflowId != null && !workflowId.isEmpty()) {
//        return workflowClient.validateWorkflowToken(workflowId, token);
//      } else {
//        logger.error("checkAccess() - Error: no token provided.");
//        return HttpStatus.UNAUTHORIZED;
//      }
//    } else {
//      return HttpStatus.OK;
//    }
//  }
//
//  private String getWorkflowIdFromSubject(String subject) {
//    // Reference 0 will be an empty string as it is the left hand side of the split
//    String[] splitArr = subject.split("/");
//    if (splitArr.length >= 2) {
//      return splitArr[1].toString();
//    } else {
//      logger.error("processCloudEvent() - Error: No workflow ID found in event");
//      return "";
//    }
//  }
//
//  private void forwardCloudEvent(CloudEventImpl<JsonNode> cloudEvent) {
//
//    // If eventing is enabled, try to send the cloud event to it
//    try {
//      pubOnlyTunnel.orElseThrow().publish(jetstreamStreamSubject, Json.encode(cloudEvent));
//
//    } catch (Exception e) {
//
//      // The code will get to this point only if eventing is disabled or if it
//      // for some reason it failed to publish the message
//      workflowClient.executeWorkflowPut(cloudEvent);
//    }
//  }
}
