package io.boomerang.model.event;

import java.io.IOException;
import java.time.ZoneOffset;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.model.ref.TaskRun;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;

public class TaskRunStatusEvent extends Event {

  private TaskRun taskRun;

  @Override
  public CloudEvent toCloudEvent() throws IOException {
    
    ObjectMapper mapper = new ObjectMapper();    
    CloudEventData data = PojoCloudEventData.wrap(taskRun, mapper::writeValueAsBytes);

    // @formatter:off
    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(getType().getCloudEventType())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(MediaType.APPLICATION_JSON.toString(), data);
    // @formatter:on

    if (Strings.isNotEmpty(super.getInitiatorContext())) {
      cloudEventBuilder =
          cloudEventBuilder.withExtension(EXTENSION_ATTRIBUTE_CONTEXT, super.getInitiatorContext());
    }

    return cloudEventBuilder.build();
  }

  public void setTaskRun(TaskRun taskRun) {
    this.taskRun = taskRun;
  }
}