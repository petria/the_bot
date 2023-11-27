package org.freakz.cli.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Scanner;

@Service
public class CliService implements CommandLineRunner {

    private static final Scanner scanner = new Scanner(System.in);
    private String user = "TestUser";

    private boolean doMainLoop = true;

    @Autowired
    private MessageSender sender;

    private void mainLoop(String botUser) {
        System.out.print("\033[H\033[2J");
        System.out.print("\n\n\n");

        System.out.print(">>> -------------    W E L C O M E  to  The Bot     ------------- <<<\n\n");
//        System.out.print(">>>                                                               <<<\n\n");
//        System.out.print(">>> --------------------                  ----------------------- <<<\n\n");

        String prev = "";
        String last = "";

        prompt(null);

        while (doMainLoop) {
            String message = scanner.nextLine();
            if (!message.isEmpty()) {
                if (message.equals("!!")) {
                    pressed = true;
                    sender.sendToServer(prev, botUser);
                } else if (message.equals("!")) {
                    pressed = true;
                    sender.sendToServer(last, botUser);
                    sleep(150L);
                } else {
                    prev = last;
                    last = message;
                    pressed = true;
                    if (doMainLoop) {
                        sender.sendToServer(message, botUser);
                        if (message.equals("quit")) {
                            doMainLoop = false;
                        } else {
                            sleep(150L);
                        }
                    }
                }
            } else {
                prompt(null);
            }

        }
//        doKill();
        System.out.println(">> Exit Client main loop, bye!");
        System.exit(0);
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            //
        }
    }

    boolean pressed = false;

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
//        mainLoop(user);
        Thread t = new Thread(() -> mainLoop(user));
        t.setName("The Bot client: " + user);
        t.start();
    }
}
