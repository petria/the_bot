package org.freakz.cli.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Scanner;

@Service
@Slf4j
public class CliService implements CommandLineRunner {

    private static final Scanner scanner = new Scanner(System.in);
    private String user = "TestUser";

    private boolean doMainLoop = true;

    @Autowired
    private MessageSender sender;

    private void mainLoop(String botUser) {
//        System.out.print("\033[H\033[2J");
        System.out.print("\n\n");
        System.out.print(">>> -------------    W E L C O M E  to  The Bot     ------------- <<<\n\n");
        System.out.print("\n\n");

        String prev = "";
        String last = "";

        prompt();

        while (doMainLoop) {
            String message = scanner.nextLine();
            if (!message.isEmpty()) {
                if (message.equals("!!")) {
                    pressed = true;
                    String reply = sender.sendToServer(prev, botUser);
                    printReplyAndPrompt(reply);

                } else if (message.equals("!")) {
                    pressed = true;
                    String reply = sender.sendToServer(last, botUser);
                    printReplyAndPrompt(reply);
                } else {
                    prev = last;
                    last = message;
                    pressed = true;
                    if (doMainLoop) {
                        if (message.equals("quit")) {
                            doMainLoop = false;
                        } else {
                            String reply = sender.sendToServer(message, botUser);
                            printReplyAndPrompt(reply);
                        }
                    }
                }
            } else {
                prompt();
            }

        }

        System.out.println(">> Exit Client main loop, bye!");
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

    boolean pressed = false;


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

        if (args.length == 0) {
            log.info("ENTERING INTERACTIVE MODE");
            Thread t = new Thread(() -> mainLoop(user));
            t.setName("The Bot client: " + user);
            t.start();

        } else {
            log.info("SENDING LINE: {}", args[0]);
            String reply = sender.sendToServer(args[0], user);
            print(reply + "\n", true);

        }

    }

}
