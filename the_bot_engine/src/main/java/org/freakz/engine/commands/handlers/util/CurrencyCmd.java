package org.freakz.engine.commands.handlers.util;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.CurrencyResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;

@HokanCommandHandler
@Slf4j
public class CurrencyCmd extends AbstractCmd {
    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {

        jsap.setHelp("Currency Convertor");

        UnflaggedOption opt = new UnflaggedOption(ARG_AMOUNT)
                .setRequired(true)
                .setDefault("100")
                .setGreedy(false);

        UnflaggedOption opt2 = new UnflaggedOption(ARG_FROM)
                .setRequired(true)
                .setGreedy(false)
                .setDefault("INR");

        UnflaggedOption opt3 = new UnflaggedOption(ARG_TO)
                .setRequired(true)
                .setGreedy(false)
                .setDefault("EUR");


        jsap.registerParameter(opt);
        jsap.registerParameter(opt2);
        jsap.registerParameter(opt3);

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        try {
            double totalAmount = Double.parseDouble(results.getString(ARG_AMOUNT));
            CurrencyResponse response = doServiceRequest(request, results, ServiceRequestType.CurrencyService);
            if (response.getStatus().startsWith("OK:")) {
                double amountForOne = response.getAmount();
                double resultAmount = totalAmount * amountForOne;
                String from = " " + response.getFrom();
                String to = " " + response.getTo();
                return totalAmount + from + " equals " + resultAmount + to;
            } else {
                return "Enter Correct Currencies";
            }
        } catch (NumberFormatException e) {
            return "Enter Correct Amount: " + results.getString(ARG_AMOUNT);
        }
    }
}
