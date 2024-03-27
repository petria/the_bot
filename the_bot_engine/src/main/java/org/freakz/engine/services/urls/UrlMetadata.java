package org.freakz.engine.services.urls;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UrlMetadata {

    private String url;

    private String title;

    private String status;

    private List<MetaAttribute> metaAttributes;


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
        this.status = "MOK: " + msg;
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

}
