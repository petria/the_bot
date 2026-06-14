package org.freakz.cli.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.io.Console;
import java.util.Scanner;

@Service
public class CliService implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(CliService.class);

  private static final Scanner scanner = new Scanner(System.in);
  boolean pressed = false;
  private boolean doMainLoop = true;
  @Autowired
  private MessageSender sender;

  private void mainLoop(String targetHost) {
    //        System.out.print("\033[H\033[2J");
    System.out.print("\n\n");
    System.out.print(">>> -------------    W E L C O M E  to  The Bot     ------------- <<<\n\n");
    System.out.printf("Connected to %s as %s%n%n", sender.baseUrl(), sender.loggedInUsername());

    String prev = "";
    String last = "";

    prompt();

    while (doMainLoop) {
      String message = scanner.nextLine();
      if (!message.isEmpty()) {
        if (message.equals("!!")) {
          pressed = true;
          String reply = sender.sendToServer(prev);
          printReplyAndPrompt(reply);

        } else if (message.equals("!")) {
          pressed = true;
          String reply = sender.sendToServer(last);
          printReplyAndPrompt(reply);
        } else {
          prev = last;
          last = message;
          pressed = true;
          if (doMainLoop) {
            if (message.equals("quit")) {
              doMainLoop = false;
            } else {
              String reply = sender.sendToServer(message);
              printReplyAndPrompt(reply);
            }
          }
        }
      } else {
        prompt();
      }
    }

    System.out.println(">> Exit Client main loop, bye!");
    sender.logout();
    System.exit(0);
  }

  private void printReplyAndPrompt(String reply) {
    sleep(150L);

    if (reply != null) {
      if (!pressed) {
        System.out.println();
      }

      print(reply + "\n", true);
      prompt();

    } else {

      prompt();
    }
  }

  private void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      //
    }
  }

  private void prompt() {
    prompt(null);
  }

  private void prompt(String prompt) {
    if (doMainLoop) {
      pressed = false;
      if (prompt != null) {

        print(ConsoleColors.RESET + prompt, true);

      } else {
        print(ConsoleColors.RESET + "the_bot> ", true);
      }
    }
  }

  private void print(String message, boolean color) {
    /*        if (color) {
        message = doColors(message);
    } else {
        message = stripColors(message);
    }*/
    System.out.print(message);
  }

  @Override
  public void run(String... args) throws Exception {
    String targetHost = resolveTarget(args);
    String oneShotMessage = resolveOneShotMessage(args);
    LoginCredentials credentials = promptForCredentials(targetHost);
    MessageSender.LoginSession session;
    try {
      session = sender.login(targetHost, credentials.username(), credentials.password());
    } catch (MessageSender.CliClientException e) {
      print("LOGIN FAILED: " + e.getMessage() + "\n", true);
      System.exit(1);
      return;
    }

    if (oneShotMessage == null) {
      log.info("ENTERING INTERACTIVE MODE against {} as {}", session.baseUrl(), session.username());
      Thread t = new Thread(() -> mainLoop(targetHost));
      t.setName("The Bot client: " + session.username());
      t.start();

    } else {
      try {
        log.info("SENDING LINE against {} as {}: {}", session.baseUrl(), session.username(), oneShotMessage);
        String reply = sender.sendToServer(oneShotMessage);
        print("BOT REPLY: " + reply + "\n", true);
      } finally {
        sender.logout();
      }
    }
  }

  String resolveTarget(String... args) {
    if (args.length == 0 || args[0] == null || args[0].isBlank()) {
      throw new IllegalArgumentException("Usage: java -jar the_bot_cli-<version>.jar <host> [command...]");
    }
    return args[0].trim();
  }

  String resolveOneShotMessage(String... args) {
    if (args.length < 2) {
      return null;
    }
    return String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
  }

  private LoginCredentials promptForCredentials(String targetHost) {
    System.out.printf("Login to %s%n", targetHost);
    System.out.print("Username: ");
    String username = scanner.nextLine().trim();
    String password = readPassword();
    return new LoginCredentials(username, password);
  }

  private String readPassword() {
    Console console = System.console();
    if (console != null) {
      char[] password = console.readPassword("Password: ");
      return password == null ? "" : new String(password);
    }
    System.out.print("Password: ");
    return scanner.nextLine();
  }

  record LoginCredentials(String username, String password) {
  }

}
