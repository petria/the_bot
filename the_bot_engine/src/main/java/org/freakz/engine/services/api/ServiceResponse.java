package org.freakz.engine.services.api;

public class ServiceResponse {

  private String status = "NOK: not implemented";

  public ServiceResponse() {
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {

    this.status = status;

  }


  @Override

  public boolean equals(Object o) {

    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;


    ServiceResponse that = (ServiceResponse) o;


    return status != null ? status.equals(that.status) : that.status == null;

  }


  @Override

  public int hashCode() {

    return status != null ? status.hashCode() : 0;

  }


  @Override

  public String toString() {

    return "ServiceResponse{" +

        "status='" + status + '\'' +

        '}';

  }

}

  
