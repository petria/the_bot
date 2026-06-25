package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.howto.HowtoIndexService;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;

@HokanCommandHandler
public class HowtoCmd extends AbstractCmd {

  public HowtoCmd() {
    setRequiredPermission(BotPermission.HOWTO_USE);
  }

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Searches built-in help about bot web UI and configuration.");

    UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
        .setList(true)
        .setRequired(true)
        .setGreedy(true);

    jsap.registerParameter(opt);
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    HowtoIndexService howtoIndexService =
        getBotEngine().getHokanServices().getApplicationContext().getBean(HowtoIndexService.class);
    return howtoIndexService.formatChatAnswer(String.join(" ", results.getStringArray(ARG_PROMPT)), 4);
  }
}
