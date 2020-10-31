package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.StringJoiner;

public class CommandHelp implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
        builder.setColor(Color.MAGENTA);

        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("_ _");
        joiner.add("Hey there, I'm Tempo!");
        joiner.add("_ _");
        joiner.add("You can view my list of commands on my website [here](https://tempobot.net/commands)");
        joiner.add("_ _");
        joiner.add("If you need some help in getting me setup then please join my support server [here](https://discord.gg/6YKNbBt)");
        joiner.add("_ _");
        joiner.add("To get help with using a command type !help <command>");
        joiner.add("_ _");
//        joiner.add("Enjoying the bot and feel like supporting us? You can help out by subscribing to our Patreon page [here]()");
//        joiner.add("_ _ ");
//        joiner.add("");

    }

}
