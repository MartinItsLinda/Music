package net.tempobot.music.commands.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.tempobot.music.util.AudioUtils;
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

    public DefaultLoadResultHandler(final CommandContext context,
                                    final AudioController controller) {
        this.context = context;
        this.scheduler = controller.getTrackScheduler();
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        final AudioTrackInfo info = track.getInfo();

        this.context.message(String.format("`%s` added `%s` to the song queue", this.context.getUser().getAsTag(), info.title)).deleteAfter(10, TimeUnit.SECONDS).send();

        this.scheduler.queue(track, false, this.context.getMember());

        if (this.scheduler.getCurrentQueueMessageId() != -1) this.scheduler.sendCurrentQueue(this.context.getChannel(), 1, false);
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        LOGGER.info("Loaded playlist from search results");
        if (!playlist.isSearchResult()) {
            if (playlist.getTracks().size() > 1) {

                this.context.message(String.format("`%s` has added %d songs to the song queue",
                        this.context.getUser().getAsTag(), playlist.getTracks().size())).deleteAfter(10, TimeUnit.SECONDS).send();

                this.scheduler.queue(playlist.getTracks(), this.context.getMember().getUser().getAsTag());

                if (this.context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) this.context.getMessage().delete().queue();
            } else {
                if (playlist.getSelectedTrack() != null) {
                    this.trackLoaded(playlist.getSelectedTrack());
                } else {
                    this.trackLoaded(playlist.getTracks().get(0));
                }
            }
        } else {
            this.createChoiceMenu(playlist.getTracks().subList(0, Math.min(playlist.getTracks().size(), 10)));
        }
    }

    @Override
    public void noMatches() {
        this.context.message("Sorry but I couldn't find anything for that, try being more specific (e.g. giving a song author) or using a URL.").deleteAfter(10, TimeUnit.SECONDS).send();
    }

    @Override
    public void loadFailed(final FriendlyException ex) {
        this.context.message(String.format("Sorry but I ran into an error trying to load that song: %s", ex.getMessage())).deleteAfter(10, TimeUnit.SECONDS).send();
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

        joiner.add("Just type the number next to a track to add it to the queue");
        joiner.add("_ _");
        joiner.add("Example: Type `1` for the first track, `2` for the second etc...");

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
        builder.setColor(Color.MAGENTA);

        this.context.getChannel().sendMessage(builder.build()).queue(message -> {

            Main.get().getEventWaiter().newWaiter(GuildMessageReceivedEvent.class, event -> event.getGuild().getIdLong() == this.context.getGuild().getIdLong()
                    && event.getChannel().getIdLong() == this.context.getChannel().getIdLong()
                    && event.getMember().getIdLong() == this.context.getMember().getIdLong(), event -> {

                if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) event.getMessage().delete().queue(null, __ -> {});
                message.delete().queue(null, __ -> {});

                int choice;
                try {
                    choice = Integer.parseInt(event.getMessage().getContentStripped());
                } catch (final NumberFormatException ignored) {
                    this.context.message("The music choice has been cancelled").deleteAfter(10, TimeUnit.SECONDS).send();
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
            }, 1, TimeUnit.MINUTES, () -> Messaging.message(this.context.getChannel(), "Your music choice has timed out").deleteAfter(10, TimeUnit.SECONDS).send());

            message.delete().queueAfter(1, TimeUnit.MINUTES, __ -> {},  __ -> {});

        });

    }

}
