package net.tempobot.music.audio;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.messaging.Messaging;
import com.sheepybot.api.entities.scheduler.ScheduledTask;
import com.sheepybot.util.Objects;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.emotes.Characters;
import net.tempobot.music.util.AudioUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrackScheduler extends AudioEventAdapterWrapped {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);

    /**
     * The pattern used to match YouTube URLs
     */
    public static final Pattern YT_PATTERN = Pattern.compile("^(https?://)?((www\\.)?youtube\\.com|youtu\\.?be)/.+$");

    /**
     * The pattern used to match SoundCloud URLs
     */
    public static final Pattern SC_PATTERN = Pattern.compile("^https?://(soundcloud\\.com|snd\\.sc)/.+$");

    /**
     * The pattern used to match Twitch URLs
     */
    public static final Pattern TWITCH_PATTERN = Pattern.compile("^(https?://)?(www\\.)?twitch\\.tv/.+$");

    /**
     * The pattern used to match Vimeo URLs
     */
    public static final Pattern VIMEO_PATTERN = Pattern.compile("^(https?://)?(www\\.)?vimeo\\.com/.+$");

    /**
     * The pattern used to match Google Translate URLs used for speech.
     */
    public static final Pattern GOOGLE_TRANSLATE_PATTERN = Pattern.compile("^(https?://)?(www\\.)?translate.google.com/translate_tts.+$");

    private final AudioController controller;
    private final AudioPlayer player;
    private final Deque<AudioTrack> queue;
    private final Deque<AudioTrack> trackHistory;
    private final Set<Long> voteSkips;
    private final JDA jda;
    private final ScheduledTask task;

    private boolean repeating;
    private boolean looping;
    private boolean autoplay;
    private boolean constantPlaying;
    private AudioTrack current;
    private AudioTrack previous;

    private final AtomicLong currentSongMessageId = new AtomicLong(-1);
    private final AtomicLong currentQueueMessageId = new AtomicLong(-1);
    private final AtomicLong currentHistoryMessageId = new AtomicLong(-1);

    public TrackScheduler(final AudioController controller, final AudioPlayer player, final JDA jda) {
        this.controller = controller;
        this.player = player;
        this.player.setVolume(80);
        this.jda = jda;
        this.queue = new LinkedList<>();
        this.trackHistory = Lists.newLinkedList();
        this.voteSkips = new HashSet<>();
        this.task = Main.get().getScheduler().runTaskRepeating(new TrackProgressUpdater(this),
                TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(30));
    }

    /**
     * @return The parent {@link AudioController} for this {@link TrackScheduler}
     */
    public AudioController getController() {
        return this.controller;
    }

    /**
     * @return The {@link AudioPlayer} for this {@link TrackScheduler}
     */
    public AudioPlayer getPlayer() {
        return this.player;
    }

    /**
     * @return A clone of the current playing {@link AudioTrack},
     * or {@code null} if there is no audio track playing
     */
    public AudioTrack getCurrentTrack() {
        return this.current;
    }

    /**
     * @return The previous {@link AudioTrack} or {@code null} if there was no previous {@link AudioTrack}
     */
    public AudioTrack getPreviousTrack() {
        return this.previous;
    }

    /**
     * @return The pending tracks for this {@link TrackScheduler}
     */
    public Deque<AudioTrack> getQueue() {
        return this.queue;
    }

    /**
     * @return {@code true} if the current {@link AudioTrack} is on repeat,
     * {@code false} otherwise
     */
    public boolean isRepeating() {
        return this.repeating;
    }

    /**
     * @param repeating Whether this current {@link AudioTrack} is repeating
     */
    public void setRepeating(final boolean repeating) {
        this.repeating = repeating;
    }

    /**
     * @return {@code true} if this {@link TrackScheduler} is looping the current playlist,
     * {@code false} otherwise.
     */
    public boolean isLooping() {
        return this.looping;
    }

    /**
     * @param looping Whether the current {@link TrackScheduler} should loop its playlist
     */
    public void setLooping(final boolean looping) {
        this.looping = looping;
    }

    /**
     * Whether this {@link TrackScheduler} will continue to remain in a {@link net.dv8tion.jda.api.entities.VoiceChannel} idling despite no {@link Member}s
     * being present in it.
     *
     * @return {@code true} if 24/7 mode is enabled, {@code false} otherwise
     */
    public boolean is247() {
        return this.constantPlaying;
    }

    /**
     * Set whether to remain in a {@link net.dv8tion.jda.api.entities.VoiceChannel} idling despite no {@link Member}s being present in it
     *
     * @param constantPlaying Whether to remain in the connected {@link net.dv8tion.jda.api.entities.VoiceChannel}
     */
    public void set247(final boolean constantPlaying) {
        this.constantPlaying = constantPlaying;
    }

    /**
     * Whether this {@link TrackScheduler} should auto play music once there are no more songs to queue
     *
     * @return {@code true} if this {@link TrackScheduler} will auto play, {@code false} otherwise.
     */
    public boolean isAutoplay() {
        return this.autoplay;
    }

    /**
     * Set whether we should retrieve a recommended track from YouTube when there are no more songs to queue
     *
     * @param autoplay The new auto play state
     */
    public void setAutoplay(final boolean autoplay) {
        this.autoplay = autoplay;
    }

    /**
     * @return The last message this {@link TrackScheduler} sent or {@code -1} if no message has been sent yet
     */
    public long getCurrentSongMessageId() {
        return this.currentSongMessageId.get();
    }

    /**
     * @return The last message this {@link TrackScheduler} sent or {@code -1} if no message has been sent yet
     */
    public long getCurrentQueueMessageId() {
        return this.currentQueueMessageId.get();
    }

    /**
     * @return The last message this {@link TrackScheduler} sent or {@code -1} if no message has been sent yet
     */
    public long getCurrentHistoryMessageId() {
        return this.currentHistoryMessageId.get();
    }

    /**
     * @return The {@link ScheduledTask} used for periodically updating currently playing embeds.
     */
    public ScheduledTask getUpdaterTask() {
        return this.task;
    }

    /**
     * @return A {@link Deque} of tracks that have been played.
     */
    public Deque<AudioTrack> getTrackHistory() {
        return this.trackHistory;
    }

    /**
     * Bulk queue songs into this {@link TrackScheduler}, updating the queue embed after all tracks have been queued.
     *
     * @param tracks The {@link AudioTrack}s to enqueue
     * @param data   The data associated with the tracks (may be null)
     */
    public synchronized void queue(@NotNull(value = "tracks cannot be null") final List<AudioTrack> tracks,
                                   final Object data) {
        tracks.forEach(track -> this.queue(track, false, data));
    }

    /**
     * @param track The {@link AudioTrack} to queue
     */
    public synchronized void queue(final AudioTrack track) {
        this.queue(track, false, "YouTube Auto Play");
    }

    /**
     * @param track  The {@link AudioTrack} to queue
     * @param first  Whether to queue the {@link AudioTrack} first or add to the end of the queue
     * @param member The {@link Member} who queued the song (can be null)
     */
    public synchronized void queue(final AudioTrack track, final boolean first, final Member member) {
        this.queue(track, first, (member == null ? "YouTube AutoPlay" : member.getUser().getAsTag()));
    }

    /**
     * @param track The {@link AudioTrack} to queue
     * @param first Whether to queue the {@link AudioTrack} first or add to the end of the queue
     * @param data  The user data attached to the track
     */
    public synchronized void queue(final AudioTrack track, final boolean first, final Object data) {

        final AudioTrack clone = track.makeClone();
        clone.setUserData(data);

        if (this.player.getPlayingTrack() != null) {

            if (first) {
                this.queue.offerFirst(clone);
            } else {
                this.queue.offer(clone);
            }

        } else {
            this.trackHistory.offerFirst(clone);
            this.player.playTrack(clone);
        }

        this.controller.setPauseTime(-1);

    }

    /**
     * Clear any pending {@link AudioTrack}s in this {@link TrackScheduler}
     *
     * <p>This will not stop the current playing {@link AudioTrack} if there is one</p>
     */
    public void clear() {

        this.queue.clear();
        this.voteSkips.clear();
        this.current = null;
        this.previous = null;
        this.looping = false;
        this.repeating = false;
        this.constantPlaying = false;

        final TextChannel channel = this.controller.getJDA().getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        if (this.getCurrentSongMessageId() != -1) channel.deleteMessageById(this.getCurrentSongMessageId()).queue(null, __ -> {});
        if (this.getCurrentQueueMessageId() != -1) channel.deleteMessageById(this.getCurrentQueueMessageId()).queue(null, __ -> {});
        if (this.getCurrentHistoryMessageId() != -1) channel.deleteMessageById(this.getCurrentHistoryMessageId()).queue(null, __ -> {});

    }

    /**
     * @param member The {@link Member} that voted skip
     * @return {@code true} if the {@link Member} hasn't already voted to skip, {@code false} otherwise
     */
    public boolean voteSkip(final Member member) {
        return this.voteSkips.add(member.getIdLong());
    }

    /**
     * @return The amount of members that voted skip
     */
    public int getSkipVotes() {
        return this.voteSkips.size();
    }

    /**
     * Attempts to play the next {@link AudioTrack} in this {@link TrackScheduler}
     * <p>
     * <p>If there are no more remaining {@link AudioTrack}s in this {@link TrackScheduler}
     * then the {@link AudioController} will begin a countdown to be destroyed after ~15 minutes of inactivity.</p>
     *
     * @param force Whether to force the next song to be played or not, if there are no more songs in the queue
     *              this will stop the current player.
     */
    public synchronized void next(final boolean force) {

        //should auto play be enabled, any cancellation logic is automatically handled
        //by the AutoPlayLoadResultHandler
        final AudioTrack track = this.getNextTrack(force);
        if (track == null) {
            if (force) {
                this.player.stopTrack();
            }

            this.controller.setPauseTime(System.currentTimeMillis());
        } else {
            this.trackHistory.offerFirst(track);
            this.player.playTrack(track);
        }

        this.voteSkips.clear();
    }

    /**
     * Attempt to retrieve the next {@link AudioTrack} from the head of and removing it from the queue.
     *
     * <p>This method will return the current track should {@link #isRepeating()} return {@code true}</p>
     *
     * <p>If {@link #isLooping()} returns {@code true} then the current track will be appended to the end of the queue.</p>
     *
     * <p>If there are no more {@link AudioTrack}s in the queue and {@link #isAutoplay()} returns {@code true}
     * then this method will attempt to query the YouTube Data V3 API to retrieve a related track if the previous
     * track was also played from YouTube and {@link AudioTrackInfo#identifier} does not return {@code null} or
     * {@link String#isEmpty()} return {@code false}.</p>
     *
     * <p>This method may return {@code null} if {@link #isAutoplay()} returns {@code true}
     * as the queueing of {@link AudioTrack}s is handled by the {@link AutoPlayLoadResultHandler}</p>
     *
     * @param force Whether to force the next track to play, this will set {@link #isRepeating()} to false
     * @return The next track or {@code null} if there are no more tracks to play.
     * This may also return {@code null} if queue is empty and {@link #isAutoplay()} returns {@code true}.
     */
    private AudioTrack getNextTrack(final boolean force) {

        LOGGER.info("Retrieving next track from audio queue...");

        AudioTrack track;
        if (force) {
            LOGGER.info("Track next has been forced, ignoring track repeat");
            track = this.queue.poll();
            this.setRepeating(false);
        } else if (this.isRepeating() && this.current != null) {
            LOGGER.info("Current track is repeating, returning a clone of the current track");
            track = this.current;
        } else {
            track = this.queue.poll();
        }

        if (this.isLooping() && this.current != null) {
            LOGGER.info("Audio queue looping is enabled, adding the last played song to the end of the queue...");
            if (track == null) {
                track = this.current;
            } else {
                this.queue.offer(this.current);
            }
        }

        if (track != null) {

            LOGGER.info("Setting track user data...");

            final Object userData = track.getUserData();

            track = track.makeClone();
            track.setUserData(userData);

        }

        return track;
    }

    /**
     * Sends/updates an existing message for a list of songs recently played by this {@link TrackScheduler}.
     *
     * @param channel   The {@link TextChannel} to send to.
     * @param page      The page to start at (pages are in multiples of 10 and start at 1).
     * @param forceSend Whether to update an existing message or send a new one updating the old message ID with the new one.
     */
    public synchronized void sendCurrentHistory(@NotNull(value = "channel cannot be null") final TextChannel channel,
                                                final int page,
                                                final boolean forceSend) {
        Objects.checkArgument(page >= 1, "page cannot be less than 1");

        //basically a copy paste method of #sendCurrentQueue but a few textual changes
        //which makes it slightly different and i can't figure out a way to replicate
        //this method atm that would take account of those textual changes.

        final List<AudioTrack> queue = this.getTrackHistory().stream().skip(((page - 1) * 10)).limit(10).collect(Collectors.toList());

        //noinspection IntegerDivisionInFloatingPointContext
        final int pageCount = (int) (Math.ceil(this.getTrackHistory().size() / 10) + 1);

        final long totalMusicLength = queue.stream().mapToLong(AudioTrack::getDuration).sum();

        final EmbedBuilder builder = new EmbedBuilder();

        String description;
        if (this.getTrackHistory().size() > 1) {
            description = "There's been " + this.getTrackHistory().size() + " songs played so far.";
        } else {
            description = "There's only been one song played so far.";
        }

        description += "\n\nYou've played " + AudioUtils.formatTrackLength(totalMusicLength, false) + " worth of music.\n\n";

        builder.setTitle(channel.getGuild().getName() + "'s Music History");
        builder.setColor(Color.MAGENTA);

        final StringJoiner joiner = new StringJoiner("\n");

        for (final AudioTrack track : queue) {

            final AudioTrackInfo info = track.getInfo();

            joiner.add(String.format("[%s](%s) requested by `%s`\n", MarkdownSanitizer.sanitize(info.title.substring(0, Math.min(info.title.length(), 20)).replaceAll("[()]", "")), info.uri, track.getUserData()));

        }

        builder.addField("Track History", joiner.toString(), true);
        builder.setFooter(String.format("Page %d/%d", page, pageCount));
        builder.setDescription(description);

        final Consumer<Message> actionConsumer = message -> {
            LOGGER.info(String.format("Updating stored queue message ID from %d to %d", this.getCurrentSongMessageId(), message.getIdLong()));
            this.currentHistoryMessageId.set(message.getIdLong());

            LOGGER.info(String.format("Updating bound text channel from %d to %d", this.controller.getTextChannelId(), channel.getIdLong()));
            this.controller.setTextChannelId(channel);
        };

        if (forceSend || this.getCurrentHistoryMessageId() == -1) {

            if (this.getCurrentHistoryMessageId() != -1) {
                final TextChannel oldChannel = this.controller.getJDA().getTextChannelById(this.controller.getTextChannelId());
                if (oldChannel != null) oldChannel.deleteMessageById(this.getCurrentHistoryMessageId()).queue();
            }

            channel.sendMessage(builder.build()).queue(actionConsumer);

        } else {
            //blame intellij for screaming at me "statement lambda can be replaced with expression lambda"
            channel.retrieveMessageById(this.getCurrentHistoryMessageId()).queue(message -> message.editMessage(builder.build()).queue(null, __ -> channel.sendMessage(builder.build()).queue(actionConsumer)), (ex) -> channel.sendMessage(builder.build()).queue(actionConsumer));
        }

    }

    /**
     * Sends/updates an existing message for the current song queue to the requested {@link TextChannel}.
     *
     * @param channel   The {@link TextChannel} to send to.
     * @param page      The page to start at (pages are in multiples of 10 and start at 1).
     * @param forceSend Whether to update an existing message or send a new one updating the old message ID with the new one.
     */
    public synchronized void sendCurrentQueue(@NotNull(value = "channel cannot be null") final TextChannel channel,
                                              final int page,
                                              final boolean forceSend) {
        Objects.checkArgument(page >= 1, "page cannot be less than 1");

        final List<AudioTrack> queue = this.getQueue().stream().skip(((page - 1) * 10)).limit(10).collect(Collectors.toList());

        //noinspection IntegerDivisionInFloatingPointContext
        final int pageCount = (int) (Math.ceil(this.getQueue().size() / 10) + 1);

        final long totalMusicLength = queue.stream().mapToLong(AudioTrack::getDuration).sum();

        final EmbedBuilder builder = new EmbedBuilder();

        String description;
        if (this.getQueue().size() > 1) {
            description = "There's " + this.getQueue().size() + " songs in the queue.";
        } else {
            if (this.getQueue().size() == 1) {
                description = "There is one song in the queue.";
            } else {
                description = "There are no songs in the queue.";
            }
        }

        description += "\n\nYou've got " + AudioUtils.formatTrackLength(totalMusicLength, false) + " worth of music to play\n\n";

        builder.setTitle(channel.getGuild().getName() + "'s Music Queue");
        builder.setColor(Color.MAGENTA);

        final StringJoiner joiner = new StringJoiner("\n");

        int position = 1;
        for (final Iterator<AudioTrack> iterator = queue.iterator(); iterator.hasNext(); position++) {

            final AudioTrack track = iterator.next();
            final AudioTrackInfo info = track.getInfo();

            joiner.add(String.format("%d - [%s](%s) requested by `%s`\n", position, MarkdownSanitizer.sanitize(info.title.substring(0, Math.min(info.title.length(), 20)).replaceAll("[()]", "")), info.uri, track.getUserData()));

        }

        builder.addField("Upcoming Songs:", joiner.toString(), true);
        builder.setFooter(String.format("Page %d/%d", page, pageCount));
        builder.setDescription(description);

        final Consumer<Message> actionConsumer = message -> {
            this.currentQueueMessageId.set(message.getIdLong());
            this.controller.setTextChannelId(channel);
        };

        if (forceSend || this.getCurrentQueueMessageId() == -1) {

            if (this.getCurrentQueueMessageId() != -1) {
                final TextChannel oldChannel = this.controller.getJDA().getTextChannelById(this.controller.getTextChannelId());
                if (oldChannel != null) oldChannel.deleteMessageById(this.getCurrentQueueMessageId()).queue();
            }

            channel.sendMessage(builder.build()).queue(actionConsumer);

        } else {

            channel.retrieveMessageById(this.getCurrentQueueMessageId()).queue(message -> message.editMessage(builder.build()).queue(null, __ -> channel.sendMessage(builder.build()).queue(actionConsumer)), (ex) -> channel.sendMessage(builder.build()).queue(actionConsumer));

        }

    }

    /**
     * Sends/updates a formatted message containing the song name, requester, song author and the volume to the requested {@link TextChannel}
     *
     * @param channel   The {@link TextChannel}
     * @param forceSend Whether to force sending a replacement message, otherwise this method will update the existing message.
     */
    public synchronized void sendCurrentSong(@NotNull(value = "channel cannot be null") final TextChannel channel,
                                             final boolean forceSend) {
        if (this.player.getPlayingTrack() == null) {
            Messaging.message(channel, "Sorry but there's no music currently playing :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {

            final AudioTrackInfo info = this.player.getPlayingTrack().getInfo();

            final EmbedBuilder builder = new EmbedBuilder();

            builder.setColor(Color.MAGENTA);

            final Object userData = this.current.getUserData();

            builder.addField("Requester", userData == null ? "YouTube Auto Play" : userData.toString(), true);
            builder.addField("Author", info.author, true);

            if (info.isStream) {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn3.iconfinder.com/data/icons/devices-and-communication-1/100/radio-128.png");
            } else if (SC_PATTERN.matcher(info.uri).matches()) {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn2.iconfinder.com/data/icons/minimalism/128/soundcloud.png");
            } else if (TWITCH_PATTERN.matcher(info.uri).matches()) {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn0.iconfinder.com/data/icons/social-network-7/50/16-128.png");
            } else if (VIMEO_PATTERN.matcher(info.uri).matches()) {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn0.iconfinder.com/data/icons/social-flat-rounded-rects/512/vinevimeo-128.png");
            } else if (GOOGLE_TRANSLATE_PATTERN.matcher(info.uri).matches()) {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn2.iconfinder.com/data/icons/funtime-objects-part-1/60/005_008_robot_baby_friend_gift_present_samodelkin-128.png");
            } else {
                builder.setAuthor("Playing: " + info.title, info.uri, "https://cdn3.iconfinder.com/data/icons/social-icons-5/607/YouTube_Play.png");
            }

            final long trackPosition = this.controller.getPlayer().getPlayingTrack().getPosition();

            final String position = AudioUtils.formatTrackLength(trackPosition);
            final String duration = AudioUtils.formatTrackLength(current.getDuration());

            final String trackProgress = info.isStream ? "STREAM" : position + " / " + duration;

            builder.addField("Volume", this.player.getVolume() + "/150", true);

            builder.setDescription(AudioUtils.formatProgressBar(trackPosition, current.getDuration()) + " " + trackProgress);

            builder.setFooter("Use the reactions below to control the music.");

            final Consumer<Message> addReactions = (message) -> {
                message.addReaction(Characters.TRACK_PREVIOUS).queue();
                message.addReaction(Characters.PLAY_PAUSE).queue();
                message.addReaction(Characters.TRACK_NEXT).queue();
                message.addReaction(Characters.STOP_PLAYING).queue();
                message.addReaction(Characters.LOOP).queue();
                message.addReaction(Characters.REPEAT).queue();
//                message.addReaction(Characters.AUTOPLAY).queue();

                this.currentSongMessageId.set(message.getIdLong());
                this.controller.setTextChannelId(channel);
            };

            if (forceSend || this.getCurrentSongMessageId() == -1) {

                if (this.getCurrentSongMessageId() != -1) {
                    final TextChannel oldChannel = this.controller.getJDA().getTextChannelById(this.controller.getTextChannelId());
                    if (oldChannel != null) oldChannel.deleteMessageById(this.getCurrentSongMessageId()).queue();
                }

                channel.sendMessage(builder.build()).queue(addReactions);

            } else {

                channel.retrieveMessageById(this.currentSongMessageId.get()).queue(message -> message.editMessage(builder.build()).queue(null, __ -> channel.sendMessage(builder.build()).queue(addReactions)), (ex) -> channel.sendMessage(builder.build()).queue(addReactions));

            }

        }
    }

    /**
     * Save all tracks played to the database.
     */
    public void saveTracks() {

        //Should probably use a synchronized lock on this maybe?

        LOGGER.info(String.format("Saving tracks to database for guild %s...", this.controller.getGuildId()));

        final Database database = Main.get().getDatabase();

        for (final AudioTrack track : this.trackHistory) {

            final AudioTrackInfo info = track.getInfo();

            if (!track.isSeekable() || info.isStream || info.author == null || info.author.isEmpty()) continue;

            database.execute("INSERT INTO `guild_track_history`(`guild_id`, `track_name`, `track_author`, `track_url`, `member`) VALUES(?, ?, ?, ?, ?);",
                    this.controller.getGuildId(), info.title, info.author, info.uri, track.getUserData().toString());
        }

    }

    /**
     * Save track queues in the event of a bot restart.
     */
    public void saveQueue() {

        LOGGER.info(String.format("Saving audio queue for Guild %d", this.controller.getGuildId()));

        final Database database = Main.get().getDatabase();

        for (final AudioTrack track : this.queue) {

            final AudioTrackInfo info = track.getInfo();

            database.execute("INSERT INTO `guild_queues`(`guild_id`, `track_url`, `member`) VALUES(?, ?, ?);", this.controller.getGuildId(), info.uri, track.getUserData());

        }

        if (this.queue.size() > 0) {
            database.execute("INSERT INTO `guild_queue_channel_data` (`guild_id`, `text_channel_id`, `voice_channel_id`) VALUES(?, ?, ?);",
                    this.controller.getGuildId(), this.controller.getTextChannelId(), this.controller.getVoiceChannelId());
        }

    }

    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason reason) {
        LOGGER.info(String.format("Finished playing song %s video url %s in guild %s", track.getInfo().title, track.getInfo().uri, this.jda.getGuildById(this.controller.getGuildId()).getName()));

        this.previous = this.current;

        if (!this.isRepeating() && !this.isAutoplay()) {
            this.current = null;
        }

        if (reason.mayStartNext) {
            this.next(false);
        } else {
            this.controller.setPauseTime(System.currentTimeMillis());
        }

    }

    @Override
    public void onTrackStart(final AudioPlayer player, final AudioTrack track) {

        this.controller.setPauseTime(-1);

        if (this.getCurrentTrack() != null && this.getCurrentTrack().getIdentifier().equals(track.getIdentifier()))
            return;

        LOGGER.info(String.format("Playing track %s with identifier %s!", track.getInfo().title, track.getIdentifier()));

        this.current = track;

        final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        this.sendCurrentSong(channel, false);

        final AudioTrackInfo info = this.current.getInfo();

        LOGGER.info(String.format("Started playing song %s video url %s in guild %s", info.title, info.uri, channel.getGuild().getName()));
    }

    @Override
    public void onTrackException(final AudioPlayer player, final AudioTrack track, final FriendlyException exception) {
        LOGGER.info("An error occurred on track '" + track.getInfo().uri + "'", exception);

//        Metrics.MUSIC.inc("trackPlayFailures");

        this.current = null;

        this.controller.setPauseTime(-1);
        this.setRepeating(false);

        final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        Messaging.message(channel, String.format("I ran into an error while playing `%s`: %s", track.getInfo().title, exception.getMessage())).deleteAfter(10, TimeUnit.SECONDS).send();
    }

    @Override
    public void onTrackStuck(final AudioPlayer player, final AudioTrack track, final long thresholdMs) {
        LOGGER.info("Player got stuck during track '" + track.getInfo().uri + "'.");

        final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        Messaging.message(channel, String.format("I got stuck while playing `%s` (%d thresholdMs)", track.getInfo().title, thresholdMs)).deleteAfter(10, TimeUnit.SECONDS).send();
    }

    /**
     * @param member The {@link Member}
     * @return {@code true} if the {@link Member} is either the owner of the {@link net.dv8tion.jda.api.entities.Guild}
     * or if they have the DJ role, {@code false} otherwise
     */
    public boolean isDJ(@NotNull(value = "member cannot be null") final Member member) {
        if (member.isOwner() || member.getVoiceState().getChannel().getMembers().stream().filter(mem -> !mem.getUser().isBot()).count() == 1) {
            return true;
        } else {
            final GuildSettings settings = GuildSettingsCache.get().getEntity(this.controller.getGuildId());
            if (!settings.getDjRoles().isEmpty()) {
                return !Collections.disjoint(member.getRoles().stream().map(Role::getIdLong).collect(Collectors.toList()), settings.getDjRoles());
            } else {
                return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) || member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("DJ"));
            }
        }
    }

    private class TrackProgressUpdater implements Runnable {

        private final TrackScheduler scheduler;

        TrackProgressUpdater(final TrackScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {

            final AudioTrack track = this.scheduler.player.getPlayingTrack();
            if (track != null && this.scheduler.getCurrentSongMessageId() != -1 && !this.scheduler.controller.isPaused()) {

                final Guild guild = this.scheduler.jda.getGuildById(this.scheduler.controller.getGuildId());
                if (guild == null) {
                    this.scheduler.controller.destroy(false);
                    return;
                }

                final TextChannel channel = guild.getTextChannelById(this.scheduler.controller.getTextChannelId());
                if (channel == null) return;

                sendCurrentSong(channel, false);

            }

        }

    }

}
