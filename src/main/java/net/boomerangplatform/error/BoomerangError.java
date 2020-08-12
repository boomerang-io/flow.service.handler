package net.boomerangplatform.error;

import org.springframework.http.HttpStatus;

public enum BoomerangError {
  
  /** Add reusable error list here. */
  TEAM_NAME_ALREADY_EXISTS(100, "TEAM_NAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);

  private final int code;
  private final String description;
  private final HttpStatus httpStatus;


  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }


  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  private BoomerangError(int code, String description, HttpStatus httpStatus) {
    this.code = code;
    this.description = description;
    this.httpStatus = httpStatus;
  }


}
