package org.freakz.services.kelikamerat;

import java.util.concurrent.Executor;

public class AbstractService {

    protected Executor executor;

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
