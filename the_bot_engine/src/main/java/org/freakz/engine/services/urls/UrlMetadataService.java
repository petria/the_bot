package org.freakz.engine.services.urls;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.data.service.EnvValuesService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
        StringBuilder sb = new StringBuilder();
        if (!urlStrings.isEmpty()) {
            SysEnvValue envValue = envValuesService.findFirstByKey("channel.do.url.topic");
            if (envValue != null) {
                String[] split = envValue.getValue().split(",");
                for (String alias : split) {
                    if (alias.equals(request.getEchoToAlias())) {
                        String title = doGetUrlTitles(request, alias, urlStrings, engine);
                        if (title != null) {
                            sb.append(title);
                        }
                    }
                    //request.getFromChannelId();
                }
            }
        }
        //return sb.toString();
    }

    private String doGetUrlTitles(EngineRequest request, String alias, List<String> urlStrings, BotEngine engine) {
        log.debug("get url metadata!");
        for (String url : urlStrings) {
            log.debug("resolve url title: {}", url);
            UrlMetadata titleText = getUrlMetadata(url);
            if (titleText != null) {
                log.debug("{} -> {}", url, titleText);
                String reply = String.format("[ %s%s%s ]", "\u0002", titleText, "\u0002");
                engine.sendReplyMessage(request, reply);
                return reply;
            } else {
                log.debug("No title found!");
            }
        }
        return null;
    }


    public UrlMetadata getUrlMetadata(String url) {
        try {
            Connection.Response execute
                    = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
                    .referrer("https://www.discogs.com/")
                    .execute();

            Document document = execute.parse();
            Element head = document.head();
            List<MetaAttribute> metaAttributes = new ArrayList<>();
            if (head != null) {
                Elements metaElements = head.getElementsByTag("meta");
                for (Element metaElement : metaElements) {
                    List<MetaAttribute> attributes = MetaAttribute.fromJsoupElements(metaElement);
                    metaAttributes.addAll(attributes);
                }
            }

            Elements title = head.select("title");
            UrlMetadata metadata = UrlMetadata.builder().url(url).title(title.text()).metaAttributes(metaAttributes).build().ok();
            metadata = checkSpecialTitles(metadata);
            return metadata;

        } catch (Exception e) {
            log.error("url title fetch failed: {}", url, e);
            UrlMetadata error = UrlMetadata.builder().url(url).build().error(e.getMessage());
            return error;
        }
    }

    private boolean isTypeOf(UrlMetadata metadata, String type) {
        String value = metadata.getMetaAttributeValue("og:site_name");
        if (value != null && value.equals(type)) {
            return true;
        }
        return false;
    }

    private UrlMetadata checkSpecialTitles(UrlMetadata metadata) {
        if (isTypeOf(metadata, "IMDb")) {
            String value = metadata.getMetaAttributeValue("og:title");
            String newTitle = String.format("[IMDB] %s", value);
            metadata.setTitle(newTitle);
        } else if (isTypeOf(metadata, "YouTube")) {
            String value = metadata.getMetaAttributeValue("og:title");
            String newTitle = String.format("[YouTube] %s", value);
            metadata.setTitle(newTitle);
        }
        return metadata;
    }

}
