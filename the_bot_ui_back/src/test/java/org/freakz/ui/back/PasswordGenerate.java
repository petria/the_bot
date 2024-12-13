package org.freakz.ui.back;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordGenerate {

  @Test
  public void generatePassword() {
    String password = "1234";
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    String encode = encoder.encode(password);

    System.out.printf("%s = %s\n", password, encode);
  }
}
