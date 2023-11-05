package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.TranslateData;
import org.freakz.dto.TranslateResponse;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TEXT;


@HokanCommandHandler
public class TranslateCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Translate FIN-ENG-FIN");

        UnflaggedOption flg = new UnflaggedOption(ARG_TEXT)
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(flg);

    }

    @Override
    public List<HandlerAlias> getAliases() {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createWithArgsAlias("!trans", "!translate"));
        return list;
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        TranslateResponse translateResponse = doServiceRequest(request, results, ServiceRequestType.TranslateService);
        String responseText = "";
        for (Map.Entry<String, List<TranslateData>> entry : translateResponse.getWordMap().entrySet()) {

            String translations = "";
            for (TranslateData translateData : entry.getValue()) {
                if (translations.length() > 0) {
                    translations += ", ";
                }
                translations += translateData.getTranslation();
            }
            if (translations.length() > 0) {
                if (responseText.length() > 0) {
                    responseText += " || ";
                }
                responseText += String.format("%s :: %s", entry.getKey(), translations);
            }
        }
        if (responseText.length() == 0) {
            return String.format("%s :: n/a", results.getString(ARG_TEXT));
        } else {
            return String.format("%s", responseText);
        }

    }
}
