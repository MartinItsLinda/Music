package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.util.MessageUtils;

import java.awt.*;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class CommandHelp implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());

        context.replace("{{prefix}}", settings.getPrefix());

        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
        builder.setColor(Color.MAGENTA);

        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Hey there, I'm Tempo!");
        joiner.add("_ _");
        joiner.add("You can start streaming music straight away without any setup, just run the `{{prefix}}play` command, give it something to search for and you're good to go!");
        joiner.add("_ _");
        joiner.add("Tempo supports a range of audio sources from YouTube, Soundcloud, Spotify, Twitch and many more!");
        joiner.add("_ _");
        joiner.add("To change Tempos behaviour, use the `{{prefix}}settings` command.");
        joiner.add("_ _");
        joiner.add("You can view a list of commands on my website [here](https://tempobot.net/commands).");
        joiner.add("_ _");
        joiner.add("Struggling to figure it out? Checkout our FAQ page [here](https://tempobot.net/faq).");
        joiner.add("_ _");
        joiner.add("If you're still confused or just want to chat then join our support server [here](https://tempobot.net/support).");
        joiner.add("_ _");
        joiner.add("Loving Tempo and want to use him in your own server? Invite him [here](https://tempobot.net/invite)");
        joiner.add("_ _");

        builder.setDescription(joiner.toString());

        MessageUtils.sendMessage(context.getGuild(), context.message(builder.build()));

    }

}
