package me.seymourkrelborn.music.commands.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.messaging.Messaging;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.audio.TrackScheduler;
import me.seymourkrelborn.music.util.AudioUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class DefaultLoadResultHandler implements AudioLoadResultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    private final CommandContext context;
    private final TrackScheduler scheduler;

    private boolean first;

    public DefaultLoadResultHandler(final CommandContext context,
                                    final AudioController controller) {
        this.context = context;
        this.scheduler = controller.getTrackScheduler();
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        final AudioTrackInfo info = track.getInfo();

        this.context.reply(String.format("`%s` has added `%s` to the queue", this.context.getUser().getAsTag(), info.title));

        this.scheduler.queue(track, false, this.context.getMember());
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        LOGGER.info("Loaded playlist from search results");
        if (!playlist.isSearchResult()) {
            if (playlist.getTracks().size() > 1) {

                this.context.reply(String.format("%s has added %d songs to the queue",
                        this.context.getUser().getAsTag(), playlist.getTracks().size()));

                playlist.getTracks().forEach(track -> this.scheduler.queue(track, false, this.context.getMember()));
            } else if (playlist.getSelectedTrack() != null) {
                this.trackLoaded(playlist.getSelectedTrack());
            }
        } else {
            this.createChoiceMenu(playlist.getTracks().subList(0, Math.min(playlist.getTracks().size(), 10) + 1));
        }
    }

    @Override
    public void noMatches() {
        this.context.reply("I'm sorry but I couldn't find any matches for that, try being more specific or use a URL");
    }

    @Override
    public void loadFailed(final FriendlyException ex) {
        this.context.reply(String.format("I'm sorry but I ran into an error trying to load that song: %s", ex.getMessage()));
    }

    /**
     * Creates a choice menu wherein users can either respond with the track id themselves
     * or have the track id as part of a reaction
     *
     * @param choices The {@link List} of {@link AudioTrack}s to choose from
     */
    private void createChoiceMenu(@NotNull(value = "choices cannot be null") final List<AudioTrack> choices) {

        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();

        final StringJoiner joiner = new StringJoiner("\n");

        joiner.add("Respond with the track ID to select the desired song");
        joiner.add("_ _");

        for (int i = 1; i < choices.size(); i++) {

            final AudioTrack track = choices.get(i - 1);
            final AudioTrackInfo info = track.getInfo();

            final String song = String.format("%d) - [%s](%s) [%s]",
                    i,
                    MarkdownSanitizer.sanitize(info.title),
                    info.uri,
                    AudioUtils.formatTrackLength(track.getDuration()));

            joiner.add(song);

        }

        builder.setTitle("Music Choice Menu - " + this.context.getUser().getAsTag());
        builder.setDescription(joiner.toString());
        builder.setColor(new Color(61, 90, 254));

        this.context.getChannel().sendMessage(builder.build()).queue(message -> {

            Main.get().getEventWaiter().newWaiter(GuildMessageReceivedEvent.class, event -> event.getGuild().getIdLong() == this.context.getGuild().getIdLong()
                    && event.getChannel().getIdLong() == this.context.getChannel().getIdLong()
                    && event.getMember().getIdLong() == this.context.getMember().getIdLong(), event -> {

                int choice;
                try {
                    choice = Integer.parseInt(event.getMessage().getContentStripped());
                } catch (final NumberFormatException ignored) {
                    context.reply("The music choice has been cancelled");
                    return true;
                }

                //if someone is trying to be clever and put 5 when there's only 4 elements, then this prevents that
                if (choice >= choices.size()) {
                    choice = choices.size();
                } else if (choice < 1) {
                    choice = 1;
                }

                this.trackLoaded(choices.get(--choice));

                return true;
            }, 1, TimeUnit.MINUTES, () -> context.reply("Your choice has timed out."));

        });

    }

}
