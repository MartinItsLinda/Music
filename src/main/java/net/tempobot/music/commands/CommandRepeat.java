package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.tempobot.music.util.MessageUtils;

import java.util.concurrent.TimeUnit;

public class CommandRepeat implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            MessageUtils.sendMessage(context.getGuild(), context.message("There's no music currently playing."));
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            scheduler.setRepeating(!scheduler.isRepeating());

            if (scheduler.isRepeating()) {
                MessageUtils.sendMessage(context.getGuild(), context.message("Once a song ends it will be added again to the back of the queue."));
            } else {
                MessageUtils.sendMessage(context.getGuild(), context.message("When your song ends you will have to add it if you want to listen to it again."));
            }

        }
        
    }
    
}
