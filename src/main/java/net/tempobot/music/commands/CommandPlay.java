package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.commands.handler.DefaultLoadResultHandler;
import net.tempobot.music.source.spotify.SpotifyAudioSourceManager;
import net.tempobot.music.util.MessageUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.concurrent.TimeUnit;

public class CommandPlay implements CommandExecutor {

    public static final UrlValidator VALIDATOR = new UrlValidator();

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getOrCreate(context.getGuild(), context.getChannel(), state.getChannel());

        if (controller.getVoiceChannelId() != -1 && controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:"));
        } else {
            final DefaultLoadResultHandler handler = new DefaultLoadResultHandler(context, controller);

            String query = String.join(" ", args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE));
            if (!VALIDATOR.isValid(query)) {
                query = "ytsearch: " + query;
            } else if (SpotifyAudioSourceManager.SPOTIFY_LINK_REGEX.matcher(query).find()) {
                MessageUtils.sendMessage(context.getGuild(), context.message("Detected a Spotify link, for large albums/playlists this might take a bit..."));
            }

            controller.setTextChannelId(context.getChannel());

            controller.getAudioPlayerManager().loadItemOrdered(context.getGuild().getIdLong(), query, handler);
        }

    }

}
