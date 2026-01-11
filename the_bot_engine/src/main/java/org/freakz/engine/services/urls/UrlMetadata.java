package org.freakz.engine.services.urls;

import java.util.List;

public class UrlMetadata {

  private String url;
  private String title;
  private String status;
  private List<MetaAttribute> metaAttributes;

  public UrlMetadata(String url, String title, List<MetaAttribute> metaAttributes) {
    this.url = url;
    this.title = title;
    this.metaAttributes = metaAttributes;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<MetaAttribute> getMetaAttributes() {
    return metaAttributes;
  }

  public void setMetaAttributes(List<MetaAttribute> metaAttributes) {
    this.metaAttributes = metaAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UrlMetadata that = (UrlMetadata) o;

    if (url != null ? !url.equals(that.url) : that.url != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    return metaAttributes != null ? metaAttributes.equals(that.metaAttributes) : that.metaAttributes == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (metaAttributes != null ? metaAttributes.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UrlMetadata{" +
        "url='" + url + '\'' +
        ", title='" + title + '\'' +
        ", status='" + status + '\'' +
        ", metaAttributes=" + metaAttributes +
        '}';
  }


  public String getMetaAttributeValue(String key) {
    if (metaAttributes == null) {
      return null;
    }
    for (MetaAttribute attribute : this.metaAttributes) {
      if (attribute.getName().equals(key)) {
        return attribute.getValue();
      }
    }
    return null;
  }

  public UrlMetadata error(String msg) {
    this.status = "NOK: " + msg;
    return this;

  }

  public UrlMetadata ok(String msg) {
    this.status = "OK: " + msg;
    return this;
  }

  public UrlMetadata ok() {

    this.status = "OK";

    return this;

  }


  public static Builder builder() {

    return new Builder();

  }


  public static class Builder {

    private String url;

    private String title;

    private String status;

    private List<MetaAttribute> metaAttributes;


    Builder() {

    }


    public Builder url(String url) {

      this.url = url;

      return this;

    }


    public Builder title(String title) {

      this.title = title;

      return this;

    }


    public Builder status(String status) {

      this.status = status;

      return this;

    }


    public Builder metaAttributes(List<MetaAttribute> metaAttributes) {

      this.metaAttributes = metaAttributes;

      return this;

    }


    public UrlMetadata build() {

      UrlMetadata urlMetadata = new UrlMetadata(url, title, metaAttributes);

      urlMetadata.setStatus(status); // Set status via setter as it's not in the constructor

      return urlMetadata;

    }


    @Override

    public String toString() {

      return "Builder{" +

          "url='" + url + '\'' +

          ", title='" + title + '\'' +

          ", status='" + status + '\'' +

          ", metaAttributes=" + metaAttributes +

          '}';

    }

  }


}

  
