package org.freakz.engine.services.ai;

import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.CommandHandlerLoader;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.common.model.engine.EngineRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Iterator;

/**
 * Spring AI tool bridge that exposes classic Hokan commands as a single generic tool.
 *
 * The LLM can call this to execute an existing !command.
 */
public class CommandToolCallBackFunctions {

  private static final Logger log = LoggerFactory.getLogger(CommandToolCallBackFunctions.class);

  private final BotEngine botEngine;
  private final CommandHandlerLoader commandHandlerLoader;

  public CommandToolCallBackFunctions(BotEngine botEngine, CommandHandlerLoader commandHandlerLoader) {
    this.botEngine = botEngine;
    this.commandHandlerLoader = commandHandlerLoader;
  }

  @Tool(
      description = "Execute a classic bot command as if the user had typed !<commandName> <args>. " +
                    "Command name must be without leading '!'."
  )
  public String executeCommand(
      @ToolParam(description = "Command name without leading '!'. Example: 'help', 'weather', 'quiz'.")
      String commandName,
      @ToolParam(description = "Raw argument string exactly as the user would type after the command. Can be empty.", required = false)
      String args,
      @ToolParam(description = "Chat network name (e.g. IRCNet, Discord, Telegram).", required = false)
      String network,
      @ToolParam(description = "Channel or conversation identifier.", required = false)
      String channel,
      @ToolParam(description = "Nickname of the user who triggered the command.", required = false)
      String sentByNick,
      @ToolParam(description = "Real name of the user who triggered the command.", required = false)
      String sentByRealName
  ) {
    try {
      if (commandName == null || commandName.isBlank()) {
        return "ERROR: commandName must not be empty.";
      }

      String trigger = "!" + commandName.toLowerCase();

      // Reuse existing lookup logic as much as possible
      HokanCmd hokanCmd = commandHandlerLoader.getMatchingCommandHandlers(botEngine, trigger);
      if (hokanCmd == null) {
        return "ERROR: unknown command '" + commandName + "'.";
      }

      if (!(hokanCmd instanceof AbstractCmd abstractCmd)) {
        return "ERROR: internal command implementation does not extend AbstractCmd: "
            + hokanCmd.getClass().getSimpleName();
      }

      // Initialize JSAP options for this command
      try {
        abstractCmd.abstractInitCommandOptions();
      } catch (Exception e) {
        log.error("Failed to init JSAP options for command {}", commandName, e);
        return "ERROR: failed to initialize options for command '" + commandName + "'.";
      }

      String argString = (args == null) ? "" : args.trim();

      JSAPResult results;
      IDMap map = abstractCmd.getJsap().getIDMap();
      Iterator<?> iterator = map.idIterator();
      if (iterator.hasNext()) {
        results = abstractCmd.getJsap().parse(argString);
        if (!results.success()) {
          return String.format(
              "ERROR: invalid arguments for '%s'. Usage: !%s %s",
              commandName,
              abstractCmd.getCommandName(),
              abstractCmd.getJsap().getUsage()
          );
        }
      } else {
        // No defined options/flags, just create an empty result
        results = abstractCmd.getJsap().parse("");
      }

      // Build a minimal EngineRequest context. Most fields are left as defaults; commands
      // typically care about network / replyTo / fromSender.
      EngineRequest engineRequest = EngineRequest.builder()
          .timestamp(System.currentTimeMillis())
          .command("!" + commandName + (argString.isEmpty() ? "" : " " + argString))
          .replyTo(channel != null ? channel : "UNKNOWN")
          .fromConnectionId(0)
          .isPrivateChannel(false)
          .fromChannelId(null)
          .fromSenderId(null)
          .fromSender(sentByNick != null ? sentByNick : "LLM")
          .isFromAdmin(false)
          .network(network != null ? network : "UNKNOWN")
          .echoToAlias(null)
          .user(null)
          .botConfig(null)
          .build();

      String output = hokanCmd.executeCommand(engineRequest, results);
      return (output == null || output.isBlank())
          ? "(no output from command '" + commandName + "')"
          : output;

    } catch (Exception e) {
      log.error("Error executing command '{}' via tool callback", commandName, e);
      return "ERROR: exception while executing command '" + commandName + "': " + e.getMessage();
    }
  }

}
