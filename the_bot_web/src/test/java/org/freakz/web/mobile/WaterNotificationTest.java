package org.freakz.web.mobile;

import org.freakz.web.config.TheBotWebProperties;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class WaterNotificationTest {
  @TempDir Path directory;
  @Test void keepsLatestWaterStatePerUserAcrossReloadWithoutRemovingChat() {
    var properties=new TheBotWebProperties();
    properties.setMobileNotificationsFile(directory.resolve("mobile.json").toString());
    var mapper=JsonMapper.builder().findAndAddModules().build();
    var service=new MobileNotificationService(mapper,properties);
    service.accept(event("1","alice","WATER_WATCH","first"));
    service.accept(event("2","alice","BOT_REPLY","chat"));
    service.accept(event("3","bob","WATER_WATCH","other user"));
    service.accept(event("4","alice","WATER_WATCH","latest"));
    var reloaded=new MobileNotificationService(mapper,properties);
    assertThat(reloaded.list("alice")).extracting(MobileNotificationService.NotificationRecord::body).containsExactlyInAnyOrder("chat","latest");
    assertThat(reloaded.list("bob")).hasSize(1);
  }
  private MobileNotificationEvent event(String id,String user,String type,String body) {
    return new MobileNotificationEvent(id,user,type,"Water",body,null,null,null,Instant.now());
  }
}
