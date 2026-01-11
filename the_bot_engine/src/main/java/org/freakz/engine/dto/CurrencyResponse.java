package org.freakz.engine.dto;

import org.freakz.engine.services.api.ServiceResponse;

public class CurrencyResponse extends ServiceResponse {
  private double amount;
  private String from;
  private String to;

  public CurrencyResponse() {
  }

  public CurrencyResponse(double amount, String from, String to) {
    this.amount = amount;
    this.from = from;
    this.to = to;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CurrencyResponse that = (CurrencyResponse) o;

    if (Double.compare(that.amount, amount) != 0) return false;
    if (from != null ? !from.equals(that.from) : that.from != null) return false;
    return to != null ? to.equals(that.to) : that.to == null;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(amount);
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + (from != null ? from.hashCode() : 0);
    result = 31 * result + (to != null ? to.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CurrencyResponse{" +
        "amount=" + amount +
        ", from='" + from + '\'' +
        ", to='" + to + '\'' +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private double amount;
    private String from;
    private String to;

    Builder() {
    }

    public Builder amount(double amount) {
      this.amount = amount;
      return this;
    }

    public Builder from(String from) {
      this.from = from;
      return this;
    }

    public Builder to(String to) {
      this.to = to;
      return this;
    }

    public CurrencyResponse build() {
      return new CurrencyResponse(amount, from, to);
    }
  }
}
