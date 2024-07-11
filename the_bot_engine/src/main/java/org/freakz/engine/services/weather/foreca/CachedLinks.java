package org.freakz.engine.services.weather.foreca;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.foreca.CountryCityLink;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedLinks {
    private Map<String, CountryCityLink> toCollectLinks;
}
