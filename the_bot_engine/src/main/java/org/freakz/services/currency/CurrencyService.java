package org.freakz.services.currency;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.CurrencyResponse;
import org.freakz.services.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_FROM;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TO;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.CurrencyService)
public class CurrencyService extends AbstractService {
    private String urlBase = "https://www.google.com/finance/quote/";
    private String amountClass = "YMlKec fxKbKc";
    private String currencyClass = "zzDege";

    @Override
    public void initializeService(ConfigService configService) throws Exception {
    }

    @Override
    public <T extends ServiceResponse> CurrencyResponse handleServiceRequest(ServiceRequest request) {
        CurrencyResponse response = CurrencyResponse.builder().build();
        String from = request.getResults().getString(ARG_FROM);
        String to = request.getResults().getString(ARG_TO);

        try {
            Document doc = Jsoup.connect(urlBase + from + "-" + to).get();
            String amount = doc.getElementsByClass(amountClass).get(0).text();
            String[] currencies = doc.getElementsByClass(currencyClass).get(0).text().split(" to ");
            response.setTo(currencies[1]);
            response.setFrom(currencies[0]);
            response.setAmount(Double.parseDouble(amount));
            response.setStatus("OK: Results Found");
        } catch (Exception e) {
            response.setStatus("NOK: " + e.getMessage());
        }
        return response;
    }
}
