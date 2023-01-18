package io.boomerang.client;

public interface EngineClient {

  void finalizeWorkflow(String wfRunId);

  void startWorkflow(String wfRunId);

  void startTask(String taskRunId);

  void endTask(String taskRunId);
  
}
