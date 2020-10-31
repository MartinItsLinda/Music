package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;

import java.util.concurrent.TimeUnit;

public class CommandRepeat implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.reply("There's no music currently playing.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            scheduler.setRepeating(!scheduler.isRepeating());

            if (scheduler.isRepeating()) {
                context.message("Once a song ends it will be added again to the back of the queue.").deleteAfter(10, TimeUnit.SECONDS);
            } else {
                context.message("When your song ends you will have to add it if you want to listen to it again.").deleteAfter(10, TimeUnit.SECONDS);
            }

        }
        
    }
    
}
