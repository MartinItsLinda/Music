package net.tempobot.music.handler;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandHandler;
import com.sheepybot.util.BotUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import org.jetbrains.annotations.NotNull;

public class MusicCommandHandler implements CommandHandler {

    @Override
    public void handle(@NotNull("context cannot be null") final CommandContext context,
                       @NotNull("args cannot be null") final Arguments args) {

        final Command command = context.getCommand();
        final Member member = context.getMember();

        if (command.isOwnerOnly() && !BotUtils.isBotAdmin(member)) {
            context.reply(context.i18n("ownerOnlyCommand"));
        } else {

            final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());

            if (!settings.getBlockedTextChannels().contains(context.getChannel().getIdLong())) {

                for (final Permission permission : command.getBotPermissions()) {

                    if (permission.name().startsWith("VOICE_")) {

                        final GuildVoiceState state = member.getVoiceState();
                        if (state == null || state.getChannel() == null) {
                            context.reply(context.i18n("voiceNoActivity"));
                            return;
                        } else if (!context.getSelfMember().hasPermission(state.getChannel(), permission)) {
                            context.reply(String.format(":no_entry_sign: I don't have permission **%s** for channel **:speaker:%s**", permission.getName(), state.getChannel().getName()));
                            return;
                        }

                    } else if (!member.hasPermission(context.getChannel(), permission)) {
                        context.reply(String.format(":no_entry_sign: I don't have permission **%s** for channel **#%s**", permission.getName(), context.getChannel().getName()));
                        return;
                    }

                }

                for (final Permission permission : command.getUserPermissions()) {

                    if (!member.hasPermission(permission)) {
                        context.reply(context.i18n("memberMissingPermission", permission.getName()));
                        return;
                    }

                }

                if (context.getCommand().getPreExecutor().apply(context)) {
                    command.getExecutor().execute(context, args);
                }

            }

        }

    }

}
