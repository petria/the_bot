package org.freakz.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.AiResponse;
import org.freakz.services.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.AiService)
public class AiService extends AbstractService {

    ConfigService configService = new ConfigService();
    private String key;

    {
        try {
            key = configService.readBotConfig().getBotConfig().getApiKey();
        } catch (IOException e) {
            log.debug(e.getMessage());
        }
    }

    private String url = "https://api.openai.com/v1/chat/completions";


    private String createJSONRequest(String input) {
        String modelName = "text-davinci-003";
        return String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, {\"role\": \"user\", \"content\": \"%s\"}]}",
                modelName, input
        );
    }

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {

        AiResponse aiResponse = AiResponse.builder().build();
        ObjectMapper mapper = new ObjectMapper();


        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + key);

            String userInput = String.join(" ", request.getResults().getString(ARG_PROMPT));
            log.debug("User Input: {}", userInput);

//            String userInputJson = mapper.writeValueAsString(userInput);

            HttpEntity<String> requestEntity = new HttpEntity<>(createJSONRequest(userInput), headers);
            log.debug("URL: {}", url);


            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    String.class
            );

            String result = responseEntity.getBody();
            aiResponse.setResult(result);
            aiResponse.setStatus("OK: Found the result");
        } catch (Exception e) {
            e.printStackTrace();
            aiResponse.setStatus("NOK: " + e.getMessage());
        }
        return aiResponse;
    }
}
