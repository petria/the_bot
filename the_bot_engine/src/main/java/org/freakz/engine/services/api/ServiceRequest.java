package org.freakz.engine.services.api;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.engine.EngineRequest;
import org.springframework.context.ApplicationContext;

public class ServiceRequest {

  private ApplicationContext applicationContext;
  private EngineRequest engineRequest;

  private JSAPResult results;

  public ServiceRequest() {
  }

  public ServiceRequest(ApplicationContext applicationContext, EngineRequest engineRequest, JSAPResult results) {
    this.applicationContext = applicationContext;
    this.engineRequest = engineRequest;
    this.results = results;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public EngineRequest getEngineRequest() {
    return engineRequest;
  }

  public void setEngineRequest(EngineRequest engineRequest) {
    this.engineRequest = engineRequest;
  }

  public JSAPResult getResults() {
    return results;
  }

  public void setResults(JSAPResult results) {
    this.results = results;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceRequest that = (ServiceRequest) o;

    if (applicationContext != null ? !applicationContext.equals(that.applicationContext) : that.applicationContext != null)
      return false;
    if (engineRequest != null ? !engineRequest.equals(that.engineRequest) : that.engineRequest != null) return false;
    return results != null ? results.equals(that.results) : that.results == null;
  }

  @Override
  public int hashCode() {
    int result = applicationContext != null ? applicationContext.hashCode() : 0;
    result = 31 * result + (engineRequest != null ? engineRequest.hashCode() : 0);
    result = 31 * result + (results != null ? results.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ServiceRequest{" +
        "applicationContext=" + applicationContext +
        ", engineRequest=" + engineRequest +
        ", results=" + results +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ApplicationContext applicationContext;
    private EngineRequest engineRequest;
    private JSAPResult results;

    Builder() {
    }

    public Builder applicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
      return this;
    }

    public Builder engineRequest(EngineRequest engineRequest) {
      this.engineRequest = engineRequest;
      return this;
    }

    public Builder results(JSAPResult results) {
      this.results = results;
      return this;
    }

    public ServiceRequest build() {
      return new ServiceRequest(applicationContext, engineRequest, results);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "applicationContext=" + applicationContext +
          ", engineRequest=" + engineRequest +
          ", results=" + results +
          '}';
    }
  }
}

