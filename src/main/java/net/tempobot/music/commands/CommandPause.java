package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

import java.util.concurrent.TimeUnit;

public class CommandPause implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.message("Sorry but I'm not in a voice channel so there's no music to pause/resume :confused:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            final AudioPlayer player = controller.getPlayer();

            player.setPaused(!player.isPaused());

            if (player.isPaused()) {
                context.message("And then there was silence. :zipper_mouth:").deleteAfter(10, TimeUnit.SECONDS).send();
            } else {
                context.message("Bringing back the music, this might take a second...").deleteAfter(10, TimeUnit.SECONDS).send();
            }
        }
        
    }
    
}
