package net.boomerangplatform.error;

import org.springframework.http.HttpStatus;

public class BoomerangException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int code;
	private final String description;
	private final HttpStatus httpStatus;
	private final String[] args;

	public BoomerangException(int code, String description, HttpStatus httpStatus, String... args) {
		super();
		this.code = code;
		this.description = description;
		this.httpStatus = httpStatus;
		this.args = args;
	}
	
	public BoomerangException(Throwable ex, int code, String description, HttpStatus httpStatus, String... args) {
		super(ex);
		this.code = code;
		this.description = description;
		this.httpStatus = httpStatus;
		this.args = args;
	}

	public BoomerangException(BoomerangError error, String... args) {
		super();
		this.code = error.getCode();
		this.description = error.getDescription();
		this.httpStatus = error.getHttpStatus();
		this.args = args;
	}

	public BoomerangException(Throwable ex, BoomerangError error, String... args) {
		super(ex);
		this.code = error.getCode();
		this.description = error.getDescription();
		this.httpStatus = error.getHttpStatus();
		this.args = args;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String[] getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "[" + code + "]: " + description;
	}

}
