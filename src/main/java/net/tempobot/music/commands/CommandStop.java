package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

import java.util.concurrent.TimeUnit;

public class CommandStop implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.message("Sorry but there's no music currently playing. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            controller.getTrackScheduler().clear();
            controller.getPlayer().stopTrack();
            controller.disconnect();
            controller.destroy(false);

            context.message("It's sad to say goodbye, I've stopped playing music and left the voice channel. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        }
        
    }
    
}
