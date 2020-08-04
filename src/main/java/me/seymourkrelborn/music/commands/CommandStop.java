package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;

public class CommandStop implements CommandExecutor {

    @Override
    public void execute(CommandContext context, Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.reply("There's no music currently playing.");
        } else {
            controller.getTrackScheduler().clear();
            controller.getPlayer().stopTrack();
            controller.disconnect();
            controller.destroy();

            context.reply("I've stopped playing music and left the voice channel");
        }
        
    }
    
}
