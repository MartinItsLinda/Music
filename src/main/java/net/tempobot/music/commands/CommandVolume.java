package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.util.AudioUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class CommandVolume implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {

            final int parsedVolume = args.next(ArgumentParsers.alt(ArgumentParsers.INTEGER, controller.getPlayer().getVolume()));

            final int volume = Math.max(1, Math.min(150, parsedVolume));
            controller.getPlayer().setVolume(volume);

            final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();

            builder.setColor(Color.MAGENTA);
            builder.setDescription(AudioUtils.formatProgressBar(volume, 150) + " (" + controller.getPlayer().getVolume() + " / 150)");

            context.reply(builder.build());

        }
        
    }
    
}
