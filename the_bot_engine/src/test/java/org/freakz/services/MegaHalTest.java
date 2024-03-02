package org.freakz.services;

import org.freakz.engine.services.ai.AiService;
import org.jibble.jmegahal.JMegaHal;

import java.io.IOException;

public class MegaHalTest {


    //    @Test
    public void testMegaHal() throws IOException {
        AiService aiService = new AiService();
        JMegaHal jMegaHal = aiService.listLogFiles();
        for (int i = 0; i < 10; i++) {
            System.out.printf("%d -> %s\n", i, jMegaHal.getSentence());
        }
    }

}
