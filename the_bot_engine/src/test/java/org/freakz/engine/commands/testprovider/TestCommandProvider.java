package org.freakz.engine.commands.testprovider;

import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.providers.CommandProvider;

import java.util.List;

public class TestCommandProvider implements CommandProvider {

  @Override
  public String namespace() {
    return "test";
  }

  @Override
  public List<Class<? extends AbstractCmd>> commands() {
    return List.of(SampleCmd.class);
  }
}
