package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.util.MessageUtils;

public class CommandPrefix implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());

        final String prefix = args.next(ArgumentParsers.STRING);

        if (prefix.length() > 5) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but your prefix has to be between 1 and 5 characters. :frowning:"));
        } else if (prefix.equals(settings.getPrefix())) {
            MessageUtils.sendMessage(context.getGuild(), context.message("You already have that as your prefix. :confused:"));
        } else {

            context.replace("{{prefix}}", prefix);

            final boolean updated = Main.get().getDatabase().execute("UPDATE `guilds` SET `guild_prefix` = ? WHERE `guild_id` = ?", prefix, context.getGuild().getIdLong());
            if (updated) {
                settings.setPrefix(prefix);
                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("Your prefix has been updated to `%s`.\n**WARNING:** This command is scheduled to be removed and may go at any point, please use the {{prefix}}settings command instead.", prefix)));
            } else {
                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and your prefix might not have been updated, if this keeps happening then please let us know."));
            }

        }

    }

}
