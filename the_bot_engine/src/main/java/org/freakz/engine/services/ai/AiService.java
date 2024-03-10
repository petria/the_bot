package org.freakz.engine.services.ai;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.*;
import org.jibble.jmegahal.JMegaHal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PROMPT;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.AiService)
public class AiService extends AbstractService {


    private static ConfigService configService; // TODO FIX!

    private static JMegaHal jMegaHal = null;
    private static String update = null;

    @Override
    public void initializeService(ConfigService configService) throws Exception {
        AiService.configService = configService;
        String logDir = configService.getBotLogDir() + "oldlogs/ircnet/";
//        log.debug("scanning logs from: {}", logDir);
//        listLogFiles(logDir);
    }


    public List<String> processLogFile(Path path) throws IOException {
        List<String> list = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String stripped = processLine(line);
                if (!stripped.isEmpty()) {
                    list.add(stripped);
                }
            }
            return list;

        } catch (MalformedInputException e) {
            log.error("{}", path.toString(), e);
        }
        return list;
    }

    private String processLine(String line) {
        String[] split = line.split(" ");
        List<String> words = new ArrayList<>();
        Collections.addAll(words, split);

        words.remove(0); // timestamp
        words.remove(0); // sender nick

        if (words.size() > 0) {
            if (words.get(0).endsWith(":")) {
                words.remove(0);
            }
            if (words.size() > 3) {
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(word);
                }
                return sb.toString();
            }
        }
        return "";
    }

    public List<File> scanForFolders(String baseDir) throws IOException {
        List<File> dirs = Files.list(Path.of(baseDir))
                .map(Path::toFile)
                .filter(File::isDirectory)
                .collect(Collectors.toList());
        return dirs;
    }

    private static final String LOGS_DIR = "/Users/petria/code/github/the_bot/runtime/oldlogs/ircnet/";
    //private static final String LOGS_DIR = "/Users/petria/code/github/the_bot/pyksy_logs";

    public String[] scanForIniFiles(String baseDir) throws IOException {
        File f = new File(baseDir);
        String[] list = f.list((dir, name) -> name.endsWith(".log"));
        return list;
    }

    public JMegaHal listLogFiles() throws IOException {
        return listLogFiles(LOGS_DIR);
    }

    public JMegaHal listLogFiles(String logsBasePath) throws IOException {

        List<File> logFolder = scanForFolders(logsBasePath);
        List<String> allLogFiles = new ArrayList<>();
        for (File logDir : logFolder) {
            String path = logDir.getAbsolutePath();
            String[] logFiles = scanForIniFiles(path);
            for (String logFile : logFiles) {
                allLogFiles.add(path + "/" + logFile);
            }
        }

        int i = 0;
        JMegaHal megaHal = new JMegaHal();
        for (String logFile : allLogFiles) {
            Path p = Path.of(logFile);
            i++;
            if (i % 300 == 0) {
                update = String.format("Feed MegaHAL: %d / %d", i, allLogFiles.size());
                log.debug(update);
            }
            List<String> logLines = processLogFile(p);
            for (String logLine : logLines) {
                megaHal.add(logLine);
            }
        }
        log.debug("Feed MegaHAL: done all {}", allLogFiles.size());

        jMegaHal = megaHal;
        return jMegaHal;

    }


    @Override
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {

        AiResponse aiResponse = AiResponse.builder().build();
        if (jMegaHal == null) {
            if (update != null) {
                aiResponse.setStatus("NOK: " + update);
            } else {
                aiResponse.setStatus("NOK: feeding still in process?");
            }

        } else {
            String prompt = String.join(" ", request.getResults().getStringArray(ARG_PROMPT));
            String hal = jMegaHal.getSentence(prompt);
            aiResponse.setStatus("OK");
            aiResponse.setResult(hal);
        }
        return aiResponse;
    }
}
