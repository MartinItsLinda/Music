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
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.tempobot.music.util.MessageUtils;

import java.util.concurrent.TimeUnit;

public class CommandSkip implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:"));
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.isDJ(context.getMember())) {
                MessageUtils.sendMessage(context.getGuild(), context.message("The song was force skipped by DJ " + context.getUser().getAsTag() + "."));
                scheduler.next(true);
            } else {
                if (scheduler.voteSkip(context.getMember())) {
                    if ((state.getChannel().getMembers().stream().filter(member -> !member.getUser().isBot()).count() / 2) <= scheduler.getSkipVotes()) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("Skipping the current song as at least half the voice channel has voted."));
                        scheduler.next(true);
                    } else {
                        MessageUtils.sendMessage(context.getGuild(), context.message(context.getMember().getAsMention() + " voted to skip this song."));
                    }
                } else {
                    MessageUtils.sendMessage(context.getGuild(), context.message("You already voted skip :confused:"));
                }
            }
        }
        
    }
    
}
