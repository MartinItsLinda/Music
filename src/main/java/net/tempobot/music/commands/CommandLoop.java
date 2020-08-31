package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;

public class CommandLoop implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.reply("There's no music currently playing.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();

            scheduler.setLooping(!scheduler.isLooping());

            if (scheduler.isLooping()) {
                context.reply("The music queue is now looping");
            } else {
                context.reply("The music queue is no longer looping");
            }

        }
        
    }
}
