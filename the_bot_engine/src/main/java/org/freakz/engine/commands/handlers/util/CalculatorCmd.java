package org.freakz.engine.commands.handlers.util;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_EXPRESSION;

@HokanCommandHandler
public class CalculatorCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(CalculatorCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {

    jsap.setHelp("Simple calculator");

    UnflaggedOption opt = new UnflaggedOption(ARG_EXPRESSION).setRequired(true).setGreedy(false);

    jsap.registerParameter(opt);

  }

  @Override
  public List<HandlerAlias> getAliases(String botName) {
    List<HandlerAlias> list = new ArrayList<>();
    list.add(createWithArgsAlias("!calc", "!calculator"));
    return list;
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    String result;
    try {
      Expression expression = new ExpressionBuilder(results.getString(ARG_EXPRESSION)).build();
      result = "" + expression.evaluate();

    } catch (Exception e) {
      result = e.getMessage();
    }

    return String.format("%s = %s", results.getString(ARG_EXPRESSION), result);
  }

}
