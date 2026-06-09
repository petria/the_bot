package org.freakz.engine.services.ai.claw;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.logs.ChatLogAccessService;
import org.springframework.stereotype.Service;

@Service
public class OpenClawLogAccessService extends ChatLogAccessService {

  public OpenClawLogAccessService(ConfigService configService, HokanNodeContextTokenService tokenService) {
    super(configService, tokenService);
  }
}
