package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

import java.util.concurrent.TimeUnit;

public class CommandRestart implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.message("Sorry but there's no music currently playing :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            controller.getPlayer().getPlayingTrack().setPosition(0);

            context.message("Back to the beginning we go.").deleteAfter(10, TimeUnit.SECONDS);
        }
        
    }
    
}
