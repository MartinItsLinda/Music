package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;

public class CommandClear implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.reply("There's no music currently playing.");
        } else {
            controller.getTrackScheduler().clear();
            controller.getPlayer().stopTrack();

            context.reply("I've cleared the music queue");
        }

    }

}
