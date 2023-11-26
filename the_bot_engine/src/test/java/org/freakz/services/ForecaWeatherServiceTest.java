package org.freakz.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.foreca.CountryCityLink;
import org.freakz.services.foreca.CachedLinks;
import org.freakz.services.foreca.ForecaWeatherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ForecaWeatherServiceTest {


    @Test
    public void test_initialize_service() throws Exception {

        ForecaWeatherService sut = new ForecaWeatherService();
        sut.initWithToCollectLinks(loadTestData("foreca_test_data_same_name_cities.json"));

        // Test with city name only, should get both Europe/USA rotterdam
        List<CountryCityLink> matching = sut.getMatchingCountryCityLinks("rotterdam");
        Assertions.assertEquals(2, matching.size());


        // Test with region/city
        matching = sut.getMatchingCountryCityLinks("europe/rotterdam");
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("Netherlands", matching.get(0).getCountry());

        // Test with partial region/city
        matching = sut.getMatchingCountryCityLinks("rope/rotterdam");
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("Netherlands", matching.get(0).getCountry());

        // Test with country/city
        matching = sut.getMatchingCountryCityLinks("Netherlands/rotterdam");
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("Netherlands", matching.get(0).getCountry());

        // Test with partial country/city
        matching = sut.getMatchingCountryCityLinks("states/rotterdam");
        Assertions.assertEquals(1, matching.size());
        Assertions.assertEquals("United_States", matching.get(0).getCountry());
    }

    private CachedLinks loadTestData(String jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = readJsonFromResource(jsonFile);
        CachedLinks cachedLinks = mapper.readValue(jsonData, CachedLinks.class);

        return cachedLinks;
    }

    private String readJsonFromResource(String resourcePath) throws IOException {
        // Use the class loader to load the resource as an InputStream
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // Check if the resource was found
        if (inputStream == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Read the content of the InputStream into a String
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Close the InputStream
        inputStream.close();
        return content;
    }


}
