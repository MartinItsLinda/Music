package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.util.MessageUtils;

import java.util.concurrent.TimeUnit;

public class CommandPause implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but I'm not in a voice channel so there's no music to pause/resume :confused:"));
        } else {
            final AudioPlayer player = controller.getPlayer();

            player.setPaused(!player.isPaused());

            if (player.isPaused()) {
                MessageUtils.sendMessage(context.getGuild(), context.message("And then there was silence. :zipper_mouth:"));
            } else {
                MessageUtils.sendMessage(context.getGuild(), context.message("Bringing back the music, this might take a second..."));
            }
        }
        
    }
    
}
