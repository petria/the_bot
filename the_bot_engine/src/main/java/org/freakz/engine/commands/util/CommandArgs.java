package org.freakz.engine.commands.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: petria
 * Date: 11/7/13
 * Time: 3:32 PM
 *
 * @author Petri Airio <petri.j.airio@gmail.com>
 */
public class CommandArgs implements Serializable {

    private String command;
    private final String[] args;

    public CommandArgs(String line) {
        String[] split = line.split(" ");
        List<String> words = new ArrayList<>();
        Collections.addAll(words, split);

        this.command = words.remove(0);
        this.args = new String[words.size()];
        for (int i = 0; i < words.size(); i++) {
            this.args[i] = words.get(i);
        }

    }

    public int getArgCount() {
        return args.length;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean hasArgs() {
        return args.length > 0;
    }

    public String getArg(int index) {
        if (index >= 0 && index < args.length) {
            return args[index];
        } else {
            return null;
        }
    }

    public String[] getArgs() {
        return this.args;
    }

    /*    public String getArgs() {
            return joinArgs(1);
        }

    */
    public String joinArgs(int fromArg) {
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for (int i = fromArg; i < args.length; i++) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

}
