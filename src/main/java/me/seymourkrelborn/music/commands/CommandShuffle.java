package me.seymourkrelborn.music.commands;

import com.sheepybot.Bot;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import java.util.Collections;
import java.util.List;

public class CommandShuffle implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.reply("You must be in the same voice channel as me to do that.");
        } else if (!controller.getTrackScheduler().isDJ(context.getMember())) {
            context.reply("You must be either alone, have the DJ role or an admin to do this.");
        } else {
            Collections.shuffle((List<?>) controller.getTrackScheduler().getQueue());
            context.reply("I've shuffled the music queue.");
        }
        
    }
    
}
