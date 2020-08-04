package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.audio.TrackScheduler;

public class CommandRepeat implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.reply("There's no music currently playing.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            scheduler.setRepeating(!scheduler.isRepeating());

            if (scheduler.isRepeating()) {
                context.reply("The current song will now loop");
            } else {
                context.reply("The current song is no longer looping");
            }
        }
        
    }
    
}
