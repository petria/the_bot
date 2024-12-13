package org.freakz.engine.services.topcounter;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.util.StringStuff;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.data.service.DataValuesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TopCountService {

  private final BotEngine botEngine;

  private final DataValuesService dataValuesService;

  @Autowired
  public TopCountService(BotEngine botEngine, DataValuesService dataValuesService) {
    this.botEngine = botEngine;
    this.dataValuesService = dataValuesService;
  }

  private void processReply(EngineRequest eRequest, String reply) {
    botEngine.sendReplyMessage(eRequest, reply);
  }

  public void calculateTopCounters(EngineRequest request) {
    try {
      for (TopCountsEnum countEnum : TopCountsEnum.values()) {
        if (doCalc(request, countEnum)) {
          handleLastTime(request, countEnum);
        }
      }

    } catch (DataRepositoryException e) {
      log.error("TopCount failed", e);
    }
  }

  private void handleLastTime(EngineRequest request, TopCountsEnum countEnum) {
    String nick =
        request
            .getFromSender(); // .getFromSender();
                              // //""//request.getIrcMessageEvent().getSender().toLowerCase();
    String channel =
        request
            .getReplyTo(); // request.getEngineRequest().
                           // getIrcMessageEvent().getChannel().toLowerCase();
    String network =
        request.getNetwork(); // request.getIrcMessageEvent().getNetwork().toLowerCase();
    String key = String.format(countEnum.getLastTimeKeyName(), nick.toUpperCase());
    try {
      dataValuesService.setValue(nick, channel, network, key, System.currentTimeMillis() + "");
    } catch (DataRepositoryException e) {
      log.error("Could not handleLastTime", e);
    }
  }

  //                              0     1     2     3     4     5     6
  private static int[] COUNTS = {500, 1000, 1024, 2000, 3000, 4000, 10000};
  private static Map<Integer, String[]> COUNT_MSGS = new HashMap<>();

  static {
    COUNT_MSGS.put(COUNTS[0], new String[] {"Mi Frend-läppä", "Mi Frend-läppä", "Mi Frend-läppä"});
    COUNT_MSGS.put(
        COUNTS[1], new String[] {"Yor Mi Frend-läppä", "Yor Mi Frend-läppä", "Yor Mi Frend-läppä"});
    COUNT_MSGS.put(COUNTS[2], new String[] {"2^10", "1k", "2^10"});
    COUNT_MSGS.put(COUNTS[3], new String[] {"3 msg1", "3 msg2", "3 msg3"});
    COUNT_MSGS.put(COUNTS[4], new String[] {"4 msg1", "4 msg2", "4 msg3"});
    COUNT_MSGS.put(COUNTS[5], new String[] {"5 msg1", "5 msg2", "5 msg3"});
    COUNT_MSGS.put(COUNTS[6], new String[] {"6 msg1", "6 msg2", "6 msg3"});
  }

  private boolean doCalc(EngineRequest request, TopCountsEnum countEnum)
      throws DataRepositoryException {
    String message = request.getCommand().toLowerCase();
    if (message.matches(countEnum.getRegex())) {
      log.debug("Match: {}", countEnum);
      String nick = request.getFromSender(); // ; getIrcMessageEvent().getSender().toLowerCase();
      String channel = request.getReplyTo(); // getIrcMessageEvent().getChannel().toLowerCase();
      String network = request.getNetwork();

      //            log.debug("Got  {} from: {}", key, nick);

      String keyWithTime = buildKeyWithTimeInfo(request.getTimestamp(), countEnum.getKeyName());

      String value = dataValuesService.getValue(nick, channel, network, keyWithTime);
      if (value == null) {
        value = "1";
      } else {
        int count = Integer.parseInt(value);
        count++;
        value = "" + count;

        for (int limit : COUNTS) {
          if (count == limit) {
            String rndMsg = StringStuff.getRandomString(COUNT_MSGS.get(limit));
            String positionChange =
                String.format("%s: %d. -> %s!!", countEnum.getPrettyName(), count, rndMsg);
            processReply(request, request.getFromSender() + ": " + positionChange);
          }
        }
      }

      PositionChange oldPos = getNickPosition(channel, network, countEnum.getKeyName(), nick);

      //            log.debug("{} {} count: {}", key, nick, value);
      dataValuesService.setValue(nick, channel, network, keyWithTime, value);

      PositionChange newPos = getNickPosition(channel, network, countEnum.getKeyName(), nick);

      //            log.debug("key: {} - oldPos: {} <-> newPos {}", key, oldPos, newPos);

      if (oldPos != null && newPos != null) {
        if (newPos.position < oldPos.position) {

          String posText;
          if (newPos.position == 1) {
            posText =
                String.format(
                    "*%s*  \u0002%d. %s = %s\u0002 <--> %d. %s = %s",
                    countEnum.getPrettyName(),
                    newPos.position,
                    newPos.own.getNick(),
                    newPos.own.getValue(),
                    newPos.position + 1,
                    newPos.after.getNick(),
                    newPos.after.getValue());

          } else {
            posText =
                String.format(
                    "*%s*  %d. %s = %s <--> \u0002%d. you = %s\u0002 <--> %d. %s = %s",
                    countEnum.getPrettyName(),
                    newPos.position - 1,
                    newPos.ahead.getNick(),
                    newPos.ahead.getValue(),
                    newPos.position,
                    newPos.own.getValue(),
                    newPos.position + 1,
                    newPos.after.getNick(),
                    newPos.after.getValue());
          }
          processReply(request, request.getFromSender() + ": " + posText);
        }
      }
      return true;
    }
    return false;
  }

  private String buildKeyWithTimeInfo(long timeStmap, String key) {
    //        LocalDateTime.
    LocalDateTime ts = LocalDateTime.now(); // TODO
    String keyWithTimeInfo =
        String.format(
            "%s_%d_%02d_%02d_%02d",
            key, ts.getYear(), ts.getDayOfMonth(), ts.getMonthValue(), ts.getHour());

    return keyWithTimeInfo;
  }

  public List<DataValuesModel> getDataValuesAsc(String channel, String network, String key) {
    return this.dataValuesService.getDataValuesAsc(channel, network, key);
  }

  public DataValuesService getDataValuesService() {
    return this.dataValuesService;
  }

  class PositionChange {
    DataValuesModel ahead = null;
    DataValuesModel own = null;
    int position = 0;
    DataValuesModel after = null;
  }

  private PositionChange getNickPosition(String channel, String network, String key, String nick) {
    List<DataValuesModel> dataValues = dataValuesService.getDataValuesAsc(channel, network, key);
    if (dataValues.size() > 0) {

      for (int i = 0; i < dataValues.size(); i++) {
        if (dataValues.get(i).getNick().equalsIgnoreCase(nick)) {
          PositionChange change = new PositionChange();
          change.own = dataValues.get(i);
          change.position = i + 1;
          if (i > 0) {
            change.ahead = dataValues.get(i - 1);
          }
          if (i != dataValues.size() - 1) {
            change.after = dataValues.get(i + 1);
          }
          return change;
        }
      }
    }
    return null;
  }
}
