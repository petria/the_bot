package org.freakz.engine.commands.handlers.ai;

import com.martiansoftware.jsap.*;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;

//@HokanDEVCommand
@HokanCommandHandler
@Slf4j
public class HokanCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Ask something from Hokan 'AI'.");
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
        jsap.registerParameter(flg);

        FlaggedOption id
            = new FlaggedOption(ARG_ID)
            .setLongFlag("id");
        jsap.registerParameter(id);

        UnflaggedOption opt = new UnflaggedOption(ARG_PROMPT)
                .setList(true)
                .setRequired(true)
                .setGreedy(true);

        jsap.registerParameter(opt);

    }

    @Override
    public List<HandlerAlias> getAliases(String botName) {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createToBotAliasWithArgs(botName, "!hokan"));
        return list;
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        String prefix = results.getString(ARG_PREFIX);
        String id = results.getString(ARG_ID);

        AiResponse aiResponse = doServiceRequestMethods(request, results, ServiceRequestType.AiService);
        if (aiResponse.getStatus().startsWith("NOK")) {
            return "Something Went Wrong: " + aiResponse.getStatus();
        }
        if (prefix != null) {
            String[] split = aiResponse.getResult().split("\n");
            StringBuilder sb = new StringBuilder();
            sb.append(prefix).append(" START\n");
            for (String s : split) {
                sb.append(prefix).append(" ");
                if (id != null) {
                    sb.append(" --id ").append(id).append(" ");
                }
                sb.append(s).append("\n");
            }
            sb.append(prefix).append(" END");
            return sb.toString();
        } else {
            return aiResponse.getResult();
        }
    }
}
