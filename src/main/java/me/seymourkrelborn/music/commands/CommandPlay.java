package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.commands.handler.DefaultLoadResultHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import org.apache.commons.validator.routines.UrlValidator;

public class CommandPlay implements CommandExecutor {

    public static final UrlValidator VALIDATOR = new UrlValidator();
    
    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        if (state == null || state.getChannel() == null) {
            context.reply("I'm sorry but I couldn't find a voice channel with you in it ");
        } else if (!context.getGuild().getSelfMember().hasPermission(state.getChannel(), Permission.VOICE_CONNECT)) {
            context.reply("I'm sorry but I don't have permission to connect to that channel ");
        } else {
            final AudioController controller = Main.get().getAudioLoader().getOrCreate(context.getGuild(), context.getChannel(), state.getChannel());

            if (controller.getVoiceChannelId() != -1 && controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
                context.reply("I'm sorry but you must be in the same voice channel as me to do that ");
            } else {
                final DefaultLoadResultHandler handler = new DefaultLoadResultHandler(context, controller);

                String query = String.join(" ", args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE));
                if (!VALIDATOR.isValid(query)) {
                    query = "ytsearch: " + query;
                }

                controller.setTextChannelId(context.getChannel());

                controller.getAudioPlayerManager().loadItem(query, handler);
            }
        }
        
    }
    
}
