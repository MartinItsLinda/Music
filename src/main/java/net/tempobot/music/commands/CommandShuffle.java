package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandShuffle implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        context.getMessage().delete().queue();

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else if (!controller.getTrackScheduler().isDJ(context.getMember())) {
            context.message("You must be either alone, have a role called DJ or be an admin to do this.").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            Collections.shuffle((List<?>) controller.getTrackScheduler().getQueue());
            context.message("I've mixed things up a bit with the music queue, hope you like it.").deleteAfter(10, TimeUnit.SECONDS).send();
        }
        
    }
    
}
