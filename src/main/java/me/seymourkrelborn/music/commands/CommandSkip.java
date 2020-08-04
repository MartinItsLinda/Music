package me.seymourkrelborn.music.commands;

import com.sheepybot.Bot;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.audio.TrackScheduler;
import net.dv8tion.jda.api.entities.GuildVoiceState;

public class CommandSkip implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.reply("You must be in the same voice channel as me to do this.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.isDJ(context.getMember())) {
                context.reply("The current song has forcibly been skipped by " + context.getUser().getAsTag());
                scheduler.next(true);
            } else {
                if (scheduler.voteSkip(context.getMember())) {
                    if ((state.getChannel().getMembers().stream().filter(member -> !member.getUser().isBot()).count() / 2) <= scheduler.getSkipVotes()) {
                        context.reply("I've skipped the current song as enough people have voted");
                        scheduler.next(true);
                    } else {
                        context.reply(context.getMember().getAsMention() + " has voted to skip the current song.");
                    }
                } else {
                    context.reply("You have already voted to skip");
                }
            }
        }
        
    }
    
}
