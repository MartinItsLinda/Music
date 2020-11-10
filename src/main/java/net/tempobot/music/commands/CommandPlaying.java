package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

import java.util.concurrent.TimeUnit;

public class CommandPlaying implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null || controller.getPlayer().getPlayingTrack().getState() == AudioTrackState.FINISHED) {
            context.message("Sorry but there's no music currently playing :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            controller.getTrackScheduler().sendCurrentSong(context.getChannel(), true);
        }
        
    }
    
}
