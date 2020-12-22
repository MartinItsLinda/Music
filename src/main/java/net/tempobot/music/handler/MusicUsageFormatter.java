package net.tempobot.music.handler;

import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.command.UsageFormatter;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.StringJoiner;

public class MusicUsageFormatter implements UsageFormatter {

    @Override
    public Message format(@NotNull("context be null") final Command command) {

        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
        builder.setColor(Color.MAGENTA);
        builder.setTitle("Command Usage Error");

        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("**Correct Usage:**");
        joiner.add(String.format("%s %s", command.getName(), command.getUsage()));
        joiner.add("_ _");

        if (command.getUsageExamples().size() > 0) {
            joiner.add("**Example Usage:**");
        }

        for (final String usageExample : command.getUsageExamples()) {
            joiner.add(String.format("%s %s", command.getName(), usageExample));
        }

        builder.setDescription(joiner.toString());

        return new MessageBuilder().setEmbed(builder.build()).build();
    }

}
