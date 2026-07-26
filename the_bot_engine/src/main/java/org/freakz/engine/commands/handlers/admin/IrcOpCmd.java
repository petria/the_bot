package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.common.users.UserPermissions;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.util.Arrays;
import java.util.List;

@HokanCommandHandler
public class IrcOpCmd extends AbstractCmd {

  private static final String ARG_ARGS = "args";
  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Reconcile IRC operator status for the current or named channel.");
    jsap.registerParameter(new UnflaggedOption(ARG_ARGS).setRequired(false).setGreedy(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    List<String> args = Arrays.stream(results.getStringArray(ARG_ARGS))
        .filter(value -> value != null && !value.isBlank())
        .toList();
    if (args.size() > 2 || (args.size() == 1 && !"reconcile".equalsIgnoreCase(args.get(0)))) {
      return usage();
    }
    String alias = args.size() == 2 ? args.get(1) : request.getEchoToAlias();
    if (alias == null || alias.isBlank()) {
      return "An IRC channel alias is required.";
    }
    if (!"IRCNet".equalsIgnoreCase(request.getNetwork())) {
      return "This command can only be used from IRC.";
    }
    if (!UserPermissions.has(request.getUser(), ChannelPermissionUtil.modePermission("IRC_CONNECTION", alias))) {
      return "You do not have IRC operator management permission for " + alias + ".";
    }
    try {
      var response = getBotEngine().getIrcOperatorManagementService().reconcile(alias);
      if (response.error() != null) {
        return "IRC operator reconciliation skipped: " + response.error();
      }
      return "IRC operators reconciled for " + alias + ": granted="
          + response.granted().size() + ", already-op=" + response.skipped().size();
    } catch (RuntimeException e) {
      return "IRC operator reconciliation failed: " + e.getMessage();
    }
  }

  private String usage() {
    return "Usage: !ircop reconcile [irc-channel-alias]";
  }
}
