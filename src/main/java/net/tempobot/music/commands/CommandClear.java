package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

import java.util.concurrent.TimeUnit;

public class CommandClear implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.message("Sorry but I'm not in a voice channel so there's no music queue to clear :confused:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            controller.getTrackScheduler().clear();
            controller.getPlayer().stopTrack();

            context.message("There you go, I've cleared the music queue for you.").deleteAfter(10, TimeUnit.SECONDS).send();
        }

    }

}
