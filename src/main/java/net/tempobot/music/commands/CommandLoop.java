package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;

import java.util.concurrent.TimeUnit;

public class CommandLoop implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.message("Sorry but I'm not in a voice channel so there's no music to loop :confused:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();

            scheduler.setLooping(!scheduler.isLooping());

            if (scheduler.isLooping()) {
                context.message("Keeping the tunes rolling! The current song will continue to play over and over until you run this command again.").deleteAfter(10, TimeUnit.SECONDS).send();
            } else {
                context.message("Time for a change? Once this song ends then the queue will continue as normal.").deleteAfter(10, TimeUnit.SECONDS).send();
            }

        }
        
    }
}
