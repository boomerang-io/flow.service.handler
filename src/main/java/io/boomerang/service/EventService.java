package io.boomerang.service;

import org.springframework.http.ResponseEntity;
import io.cloudevents.CloudEvent;

public interface EventService {

  ResponseEntity<?> process(CloudEvent cloudEvent);

//  ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeCloudEvent(CloudEvent<AttributesImpl, JsonNode> cloudEvent, String token, URI uri);
//
//  ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeWebhookEvent(String token, String requestUri, String trigger, String workflowId,
//      JsonNode payload, String workflowActivityId, String topic, String status);
}
