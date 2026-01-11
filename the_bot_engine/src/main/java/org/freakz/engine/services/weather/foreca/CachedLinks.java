package org.freakz.engine.services.weather.foreca;

import org.freakz.common.model.foreca.CountryCityLink;

import java.util.Map;

public class CachedLinks {
  private Map<String, CountryCityLink> toCollectLinks;

  public CachedLinks() {
  }

  public CachedLinks(Map<String, CountryCityLink> toCollectLinks) {
    this.toCollectLinks = toCollectLinks;
  }

  public Map<String, CountryCityLink> getToCollectLinks() {
    return toCollectLinks;
  }

  public void setToCollectLinks(Map<String, CountryCityLink> toCollectLinks) {
    this.toCollectLinks = toCollectLinks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CachedLinks that = (CachedLinks) o;

    return toCollectLinks != null ? toCollectLinks.equals(that.toCollectLinks) : that.toCollectLinks == null;
  }

  @Override
  public int hashCode() {
    return toCollectLinks != null ? toCollectLinks.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "CachedLinks{" +
        "toCollectLinks=" + toCollectLinks +
        '}';
  }
}
