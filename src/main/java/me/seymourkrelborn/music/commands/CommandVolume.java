package me.seymourkrelborn.music.commands;

import com.sheepybot.Bot;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.messaging.Messaging;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.util.AudioUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import java.awt.*;

public class CommandVolume implements CommandExecutor {

    @Override
    public void execute(CommandContext context, Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.reply("You must be in the same voice channel as me to do this.");
        } else {

            final int parsedVolume = args.next(ArgumentParsers.alt(ArgumentParsers.INTEGER, controller.getPlayer().getVolume()));

            final int volume = Math.max(1, Math.min(150, parsedVolume));
            controller.getPlayer().setVolume(volume);

            final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();

            builder.setColor(new Color(61, 90, 254));
            builder.setDescription(AudioUtils.formatProgressBar(volume, 150) + " (" + controller.getPlayer().getVolume() + " / 150)");

            context.reply(builder.build());

        }
        
    }
    
}
