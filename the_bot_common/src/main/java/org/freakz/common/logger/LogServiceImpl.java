package org.freakz.common.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.feed.MessageSource;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LogServiceImpl implements LogService {

  private final String logDir;

  private Map<String, Logger> channelLoggers = new HashMap<>();

  public LogServiceImpl(String logDir) {
    this.logDir = logDir;
    log.debug("Initializing message logger, log dir: {}", this.logDir);
  }

  @Override
  public void logChannelMessage(
      LocalDateTime localDateTime,
      MessageSource messageSource,
      String network,
      String channel,
      String message) {

    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    String day = formatter.format(localDateTime);
    String key = String.format("%s%s%s", network, channel, day).toLowerCase();
    Logger logger = channelLoggers.get(key);
    if (logger == null) {
      String path = createPath(network, channel, day);
      logger = createLoggerFor(key, path);
      channelLoggers.put(key, logger);
    }
    logger.info(message);
  }

  private String createPath(String network, String channel, String day) {
    String path = this.logDir + network + "/" + channel + "/";
    path = path.toLowerCase();
    File file = new File(path);
    if (!file.exists()) {
      boolean ok = file.mkdirs();
      log.debug("Created log dir: {} -- {}", file.getAbsolutePath(), ok);
    }

    return path + day + ".log";
  }

  private Logger createLoggerFor(String string, String file) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();

    ple.setPattern("%msg%n");
    ple.setContext(lc);
    ple.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
    fileAppender.setFile(file);
    fileAppender.setEncoder(ple);
    fileAppender.setContext(lc);
    fileAppender.start();

    Logger logger = (Logger) LoggerFactory.getLogger(string);
    logger.addAppender(fileAppender);
    logger.setLevel(Level.DEBUG);
    logger.setAdditive(true); /* set to true if root should log too */

    return logger;
  }
}
