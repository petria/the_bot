package org.freakz.services;

import org.freakz.engine.data.service.EnvValuesServiceImpl;
import org.freakz.engine.services.urls.UrlMetadata;
import org.freakz.engine.services.urls.UrlMetadataService;
import org.jibble.jmegahal.JMegaHal;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UrlMetadataPOCTest {

    private EnvValuesServiceImpl envValueService = Mockito.mock(EnvValuesServiceImpl.class);

    @Test
    public void testUrlMetadataService() {
        int count = 0;
        int doMax = 7;
        List<UrlMetadata> results = new ArrayList<>();
        String[] urls = {
                "https://www.youtube.com/watch?v=cvtWHnDUTnc",
                "https://www.imdb.com/title/tt14230458/",
                "https://pbs.twimg.com/media/GJjHm1MXYAAN5Nw?format=jpg&name=medium",
                "https://www.iltalehti.fi/ulkomaat/a/16113184-7fdd-4a76-bc45-dc223919ebf3",
                "https://www.youtube.com/watch?v=fu1-wx7Wao0",
                "https://soundcloud.com/ida_radio/lets-play-house-7324",
                "https://twitter.com/ObbeVermeij/status/1764806999772975474",
/*                "https://www.iltalehti.fi/ulkomaat/a/16113184-7fdd-4a76-bc45-dc223919ebf3",
                "http://localhost:8080/test",
                "https://archive.org/search?query=Pelit%20vuosikirja",
                "https://github.com/xaos-project/XaoS"*/
        };
        UrlMetadataService sut = new UrlMetadataService(envValueService);
        for (String url : urls) {
            UrlMetadata urlMetadata = sut.getUrlMetadata(url);
            results.add(urlMetadata);
            count++;
            if (count == doMax) {
                break;
            }

        }
        Assertions.assertEquals(doMax, results.size());
        for (UrlMetadata metadata : results) {
            if (metadata.getStatus().startsWith("OK")) {
                System.out.printf("%s -> %s\n", metadata.getUrl(), metadata.getTitle());
            } else {
                System.out.printf("%s -> %s\n", metadata.getUrl(), metadata.getStatus());
            }
        }
        int foo = 0;
    }

    public Connection.Response getJsoupConnectResponse(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
//                    .referrer("https://www.discogs.com/")
                .execute();

    }

    public void fetchThreadConversations(String url, JMegaHal hal) throws IOException {
        Connection.Response response = getJsoupConnectResponse(url);
        Document document = response.parse();
        Elements elements = document.select("p");
        List<String> texts = new ArrayList<>();
        for (Element element : elements) {

            String aClass1 = element.attr("class");
            if (aClass1.matches("CommentListItem.*")) {
                String txt = element.text();
                String[] words = txt.split(" ");
                if (words.length > 5) {
                    System.out.printf("ADD: %s\n", txt);
                    hal.add(txt);

                }
//                texts.add(txt);
            }


        }
        int foo = 0;
    }

    @Test
    public void testFetchThreadConversations() throws IOException {
        String url = "https://keskustelu.suomi24.fi/t/17411323/1-poistin-evankelointini-ja-aloin-juoda";
        JMegaHal hal = new JMegaHal();
        fetchThreadConversations(url, hal);
        for (int i = 0; i < 10; i++) {
            System.out.printf("%2d: %s\n", i, hal.getSentence());
        }
    }

    @Test
    public void testFetchSuomi24() {
        String url = "https://keskustelu.suomi24.fi/yhteiskunta/uskonnot-ja-uskomukset?page=1";
        try {
            Connection.Response response = getJsoupConnectResponse(url);
            Document document = response.parse();
            Elements discussions = document.getElementsByClass("ThreadList__ListItem-hujhcy-3 hibBI");
            Map<String, String> linkMap = new HashMap<>();
            for (Element element : discussions) {
                Elements aList = element.select("a");
                for (Element aElement : aList) {
                    String href = aElement.attr("href");
                    if (href.contains("/t/")) {
                        linkMap.put(href, href);
                    }

                }

            }

            int foo = 0;


        } catch (Exception e) {
            int err = 0;
        }

    }

}
