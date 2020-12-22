package net.tempobot.music.event;

import com.google.common.collect.Lists;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.argument.RawArguments;
import com.sheepybot.api.entities.event.EventHandler;
import com.sheepybot.api.entities.event.EventListener;
import com.sheepybot.api.entities.language.I18n;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.guild.Preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GuildMessageListener implements EventListener {

    @EventHandler
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {

        final Guild guild = event.getGuild();
        final Message message = event.getMessage();

        if (message.getContentRaw().isEmpty()) return;

        String content = message.getContentStripped();

        final GuildSettings settings = GuildSettingsCache.get().getEntity(guild.getIdLong());

        if (content.startsWith(settings.getPrefix())) {

            content = content.substring(settings.getPrefix().length()).trim();
            if (!content.isEmpty()) {
                final List<String> split = Lists.newArrayList(content.split("\\s+")); //whitespace
                final String trigger = split.get(0);

                settings.getPresets().stream().filter(preset -> preset.getName().equalsIgnoreCase(trigger)).findFirst().ifPresent(preset -> {

                    final Command command = Main.get().getCommandRegistry().getCommandByNameOrAlias(Collections.singletonList(preset.getCommand()));
                    if (command != null) {

                        final List<String> args = preset.getArguments();
                        if (split.size() > 1) {
                            args.addAll(split);
                        }

                        final CommandContext context = new CommandContext(event.getChannel(), event.getMember(), guild, trigger, command, message, I18n.getDefaultI18n(), event.getJDA());
                        final Arguments arguments = new Arguments(context, new RawArguments(args));

                        command.handle(context, arguments);

                    } else {
                        Main.get().getDatabase().execute("DELETE FROM `guild_presets` WHERE `id` = ? AND `guild_id` = ?;", preset.getPresetId(), preset.getGuildId());
                    }

                });
            }

        }

    }

}
