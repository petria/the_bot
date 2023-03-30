package org.freakz.engine.commands;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class HandlerAlias {
    String alias;
    String target;

    Class clazz;
}
