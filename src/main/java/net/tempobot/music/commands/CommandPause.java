package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;

public class CommandPause implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.reply("There's no music currently playing.");
        } else {
            final AudioPlayer player = controller.getPlayer();

            player.setPaused(!player.isPaused());

            if (player.isPaused()) {
                context.reply("The player has been paused!");
            } else {
                context.reply("The player has been resumed!");
            }
        }
        
    }
    
}
