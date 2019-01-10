package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse {

	@JsonProperty("code")
	private String code;

	@JsonProperty("message")
	private String message;
	
	@JsonProperty("output")
	private Map<String, String> output = new HashMap<String, String>();

	public TaskResponse() {
	}

	public TaskResponse(String code, String desc, Map<String, String> output) {
		super();
		this.code = code;
		this.message = desc;
		this.output = output;
	}

	public String getDesc() {
		return this.message;
	}
	public String getCode() {
		return this.code;
	}

	public void setDesc(String desc) {
		this.message = desc;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<String, String> getOutput() {
		return output;
	}

	public void setOutput(Map<String, String> output) {
		this.output = output;
	}
}