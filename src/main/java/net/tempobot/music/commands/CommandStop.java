package net.tempobot.music.commands;

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

public class CommandStop implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but there's no music currently playing. :frowning:"));
        } else {
            controller.destroy(false);
            MessageUtils.sendMessage(context.getGuild(), context.message("It's sad to say goodbye, I've stopped playing music and left the voice channel. :frowning:"));
        }
        
    }
    
}
