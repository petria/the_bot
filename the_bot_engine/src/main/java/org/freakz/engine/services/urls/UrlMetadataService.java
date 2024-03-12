package org.freakz.engine.services.urls;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.data.service.EnvValuesService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UrlMetadataService {

    private final EnvValuesService envValuesService;

    public UrlMetadataService(EnvValuesService envValuesService) {
        this.envValuesService = envValuesService;
    }

    @Async
    public void handleEngineRequest(EngineRequest request, BotEngine engine) {
        String regexp = "(https?://|www\\.)\\S+";

        Pattern p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(request.getCommand());
        List<String> urlStrings = new ArrayList<>();
        while (m.find()) {
            String url = m.group();
            urlStrings.add(url);
        }

        if (!urlStrings.isEmpty()) {
            SysEnvValue envValue = envValuesService.findFirstByKey("channel.do.url.topic");
            if (envValue != null) {
                String[] split = envValue.getValue().split(",");
                for (String alias : split) {
                    if (alias.equals(request.getEchoToAlias())) {
                        doGetUrlTitles(request, alias, urlStrings, engine);
                    }
                    //request.getFromChannelId();
                }
            }
        }
    }

    private void doGetUrlTitles(EngineRequest request, String alias, List<String> urlStrings, BotEngine engine) {
        log.debug("get url metadata!");
        for (String url : urlStrings) {
            log.debug("resolve url title: {}", url);
            String titleText = getUrlTitle(url);
            if (titleText != null) {
                log.debug("{} -> {}", url, titleText);
                String reply = String.format("[ %s%s%s ]", "\u0002", titleText, "\u0002");
                engine.sendReplyMessage(request, reply);
            } else {
                log.debug("No title found!");
            }
        }
    }

    private String getUrlTitle(String url) {
        try {
            Connection.Response execute
                    = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
                    .referrer("https://www.discogs.com/")
                    .execute();

            Document document = execute.parse();

            Elements head = document.getElementsByTag("head");
            if (head != null) {
                Elements title = head.select("title");
                if (title != null) {
                    String titleText = title.text();
                    return titleText;
                }
            }

        } catch (Exception e) {
            int err = 0;
        }
        return null;
    }
}
