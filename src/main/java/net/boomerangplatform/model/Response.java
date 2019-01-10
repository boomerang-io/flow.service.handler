package net.boomerangplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {

	@JsonProperty("code")
	private String code;

	@JsonProperty("message")
	private String message;

	public Response() {
	}

	public Response(String code, String desc) {
		super();
		this.code = code;
		this.message = desc;
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
}