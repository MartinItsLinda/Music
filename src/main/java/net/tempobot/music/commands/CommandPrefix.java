package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;

import java.util.concurrent.TimeUnit;

public class CommandPrefix implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        context.getMessage().delete().queue();

        final String prefix = args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE);
        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());

        if (prefix.length() > 5) {
            context.reply("Sorry but your prefix has to be between 1 and 5 characters. :frowning:");
        } else if (prefix.equalsIgnoreCase(settings.getPrefix())) {
            context.reply("You already have that as your prefix. :confused:");
        } else {

            final boolean updated = Main.get().getDatabase().execute("UPDATE `guilds` SET `guild_prefix` = ? WHERE `guild_id` = ?", prefix, context.getGuild().getIdLong());
            if (updated) {
                settings.setPrefix(prefix);
                context.message(String.format("Your prefix has been updated to `%s`.", prefix)).deleteAfter(10, TimeUnit.SECONDS).send();
            } else {
                context.reply("Something went wrong and your prefix might not have been updated, if this keeps happening then please let us know.");
            }

        }

    }

}
