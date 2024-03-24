package org.freakz.engine.services.urls;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UrlMetadata {

    private String url;

    private String title;

    private List<MetaAttribute> metaAttributes;


}
