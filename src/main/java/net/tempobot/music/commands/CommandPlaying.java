package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

public class CommandPlaying implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null || controller.getPlayer().getPlayingTrack().getState() == AudioTrackState.FINISHED) {
            context.reply("There's no music currently playing.");
        } else {
            controller.getTrackScheduler().sendCurrentSong(context.getChannel(), true);
        }
        
    }
    
}
