package org.freakz.engine.commands.handlers.ai;


import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PREFIX;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

@HokanCommandHandler
@Slf4j
public class ConversationCmd extends AbstractCmd {

  private final ConversationService conversationService;

  public ConversationCmd() {
    this.conversationService = new ConversationService();
  }

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {

    jsap.setHelp("Setup Conversation with target.");
/*

    FlaggedOption flg =
        new FlaggedOption(ARG_COUNT)
            .setStringParser(JSAP.INTEGER_PARSER)
            .setDefault("5")
            .setLongFlag("count")
            .setShortFlag('c');
    jsap.registerParameter(flg);
 */
    FlaggedOption flg
        = new FlaggedOption(ARG_PREFIX)
        .setLongFlag("prefix");
//            .setDefault("none");
    jsap.registerParameter(flg);

    UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
        .setList(true)
        .setRequired(true)
        .setGreedy(true);

    jsap.registerParameter(opt);

  }

  @Override
  public List<HandlerAlias> getAliases(String botName) {
    List<HandlerAlias> list = new ArrayList<>();
    list.add(createWithArgsAlias("!creply", "!conversation"));
    return list;
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    if (request.getCommand().startsWith("!creply")) {
      log.debug("handle !creply: {}", request.getMessage());
      return null;
    }
    return "!hokan --prefix !creply you are having conversation with another bot? Tell me how you do today.";
  }

}
