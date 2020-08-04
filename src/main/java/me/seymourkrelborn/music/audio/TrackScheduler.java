package me.seymourkrelborn.music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.api.entities.messaging.Messaging;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import me.seymourkrelborn.music.emotes.Characters;
import me.seymourkrelborn.music.util.AudioUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    private final Set<Long> voteSkips;
    private final JDA jda;

    private boolean repeating;
    private boolean looping;
    private boolean autoplay;
    private boolean constantPlaying;
    private AudioTrack current;
    private AudioTrack previous;

    private final AtomicLong messageId = new AtomicLong(-1);

    public TrackScheduler(final AudioController controller, final AudioPlayer player, final JDA jda) {
        this.controller = controller;
        this.player = player;
        this.player.setVolume(80);
        this.jda = jda;
        this.queue = new LinkedList<>();
        this.voteSkips = new HashSet<>();
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
    public long getMessageId() {
        return this.messageId.get();
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
        this.looping = false;
        this.repeating = false;
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

            this.clear();

            final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
            if (channel == null) return;

            if (!this.isAutoplay()) {
                channel.sendMessage("There are no more songs left in the queue.").queue();
                this.controller.setPauseTime(System.currentTimeMillis()); //tell the AudioController that we currently aren't playing audio
            }

        } else {
            this.player.playTrack(track);
        }

        this.voteSkips.clear();
    }

    /**
     * Tries to retrieve and remove the next {@link AudioTrack} from the head of the queue.
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
     *
     * @return The next track or {@code null} if there are no more tracks to play and {@link #isRepeating()} or {@link #isLooping()} return {@code false}.
     * This may also return {@code null} if queue is empty and {@link #isAutoplay()} returns {@code true}
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

//        } else if (this.isAutoplay() && this.current != null) {
//
//            LOGGER.info("Couldn't retrieve any track from the audio queue, searching youtube for a related video instead...");
//
//            final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
//            if (channel == null) return null;
//
//            final String videoId = this.current.getInfo().identifier;
//            if (videoId != null && !videoId.isEmpty()) {
//                final List<String> relatedVideos = YouTubeUtil.getRandomYouTubeVideoFromRelatedVideo(videoId, 7);
//                if (!relatedVideos.isEmpty()) {
//                    //YouTube will often give a video that was related to the previous video, this helps to try and make sure people don't get the same video twice
//                    this.controller.getAudioPlayerManager().loadItem(relatedVideos.get(ThreadLocalRandom.current().nextInt(relatedVideos.size())),
//                            new AutoPlayLoadResultHandler(this));
//                } else {
//                    LOGGER.info(String.format("YouTube returned no results for video ID %s.", videoId));
//                    channel.sendMessage("I couldn't find any related videos from YouTube " + Emoji.SHEEPY_SAD).queue();
//                }
//            } else {
//                channel.sendMessage("I'm sorry but the last track you played isn't from YouTube so it doesn't support the Auto Play feature " + Emoji.SHEEPY_SAD).queue();
//            }
//
//        }

        return track;
    }

    /**
     * Sends/updates a formatted message containing the song name, requester, song author and the volume to the requested {@link TextChannel}
     *
     * @param channel   The {@link TextChannel}
     * @param forceSend Whether to force sending a replacement message, otherwise this method will update the existing message.
     */
    public void sendCurrentSong(@NotNull(value = "channel cannot be null") final TextChannel channel,
                                final boolean forceSend) {
        if (this.player.getPlayingTrack() == null) {
            channel.sendMessage("There's no music currently playing.").queue();
        } else {

            final AudioTrackInfo info = this.current.getInfo();

            final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();

            builder.setColor(new Color(61, 90, 254));

            builder.setAuthor(info.title, info.uri, "https://emojipedia-us.s3.amazonaws.com/thumbs/120/twitter/103/musical-note_1f3b5.png");

            final Object userData = this.current.getUserData();

            builder.addField("Requested By", userData == null ? "YouTube Auto Play" : userData.toString(), true);
            builder.addField("Song Author", info.author, true);

            if (info.isStream) {
                builder.setThumbnail("https://cdn3.iconfinder.com/data/icons/devices-and-communication-1/100/radio-128.png");
            } else if (SC_PATTERN.matcher(info.uri).matches()) {
                builder.setThumbnail("https://cdn2.iconfinder.com/data/icons/minimalism/128/soundcloud.png");
            } else if (TWITCH_PATTERN.matcher(info.uri).matches()) {
                builder.setThumbnail("https://cdn0.iconfinder.com/data/icons/social-network-7/50/16-128.png");
            } else if (VIMEO_PATTERN.matcher(info.uri).matches()) {
                builder.setThumbnail("https://cdn0.iconfinder.com/data/icons/social-flat-rounded-rects/512/vinevimeo-128.png");
            } else if (GOOGLE_TRANSLATE_PATTERN.matcher(info.uri).matches()) {
                builder.setThumbnail("https://cdn2.iconfinder.com/data/icons/funtime-objects-part-1/60/005_008_robot_baby_friend_gift_present_samodelkin-128.png");
            } else {
                builder.setThumbnail("https://cdn3.iconfinder.com/data/icons/social-icons-5/607/YouTube_Play.png");
            }

            final long trackPosition = this.controller.getPlayer().getPlayingTrack().getPosition();

            final String position = AudioUtils.formatTrackLength(trackPosition);
            final String duration = AudioUtils.formatTrackLength(current.getDuration());

            final String trackProgress = info.isStream ? "STREAM" : position + " / " + duration;

            builder.addField("Volume", this.player.getVolume() + "/150", true);

            builder.setDescription(AudioUtils.formatProgressBar(trackPosition, current.getDuration()) + " " + trackProgress);

            final Consumer<Message> addReactions = (message) -> {
                message.addReaction(Characters.TRACK_PREVIOUS).queue();
                message.addReaction(Characters.PLAY_PAUSE).queue();
                message.addReaction(Characters.TRACK_NEXT).queue();
                message.addReaction(Characters.STOP_PLAYING).queue();
                message.addReaction(Characters.LOOP).queue();
                message.addReaction(Characters.REPEAT).queue();
//                message.addReaction(Characters.AUTOPLAY).queue();

                LOGGER.info(String.format("Updating stored message ID from %d to %d", this.getMessageId(), message.getIdLong()));
                this.messageId.set(message.getIdLong());

                LOGGER.info(String.format("Updating bound text channel from %d to %d", this.controller.getTextChannelId(), channel.getIdLong()));
                this.controller.setTextChannelId(channel);
            };

            if (forceSend || this.getMessageId() == -1) {
                LOGGER.info("Sending a new message due to it either being forced or there not being any existing message to update...");
                channel.sendMessage(builder.build()).queue(addReactions);
            } else {
                LOGGER.info("Retrieving existing message to update it with the new content...");

                channel.retrieveMessageById(this.messageId.get()).queue(message -> {
                    message.editMessage(builder.build()).queue();
                }, (ex) -> {
                    LOGGER.info(String.format("Couldn't retrieve message ID %d from text channel, sending a new message instead", this.getMessageId()));
                    channel.sendMessage(builder.build()).queue(addReactions);
                });
            }

        }
    }

    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason reason) {
        LOGGER.info(String.format("Finished playing song %s video url %s in guild %s", track.getInfo().title, track.getInfo().uri, this.jda.getGuildById(this.controller.getGuildId()).getName()));

        if (!this.isRepeating() && !this.isAutoplay()) {
            this.current = null;
        }

        if (reason.mayStartNext) {
            this.next(false);
            this.previous = track;
        }

    }

    @Override
    public void onTrackStart(final AudioPlayer player, final AudioTrack track) {
        if (this.getCurrentTrack() != null && this.getCurrentTrack().getIdentifier().equals(track.getIdentifier())) return;

        LOGGER.info(String.format("Playing track %s with identifier %s!", track.getInfo().title, track.getIdentifier()));

        this.current = track;

        final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        this.controller.setPauseTime(-1);
        this.sendCurrentSong(channel, false);

        final AudioTrackInfo info = this.current.getInfo();

        LOGGER.info(String.format("Started playing song %s video url %s in guild %s", info.title, info.uri, channel.getGuild().getName()));
    }

    @Override
    public void onTrackException(final AudioPlayer player, final AudioTrack track, final FriendlyException exception) {
        LOGGER.info("An error occurred on track '" + track.getInfo().uri + "'", exception);

//        Metrics.MUSIC.inc("trackPlayFailures");

        this.current = null;
        this.setRepeating(false);

        final TextChannel channel = this.jda.getTextChannelById(this.controller.getTextChannelId());
        if (channel == null) return;

        channel.sendMessage(String.format("I ran into an error while playing `%s`", track.getInfo().title)).queue();
    }

    @Override
    public void onTrackStuck(final AudioPlayer player, final AudioTrack track, final long thresholdMs) {
        LOGGER.info("Player got stuck during track '" + track.getInfo().uri + "'.");
    }

    /**
     * @param member The {@link Member}
     * @return {@code true} if the {@link Member} is either the owner of the {@link net.dv8tion.jda.api.entities.Guild}
     * or if they have the DJ role, {@code false} otherwise
     */
    public boolean isDJ(final Member member) {
        //noinspection ConstantConditions
        return member != null && member.getVoiceState().getChannel().getIdLong() == this.getController().getVoiceChannelId() &&
                (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) ||
                        member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("DJ")) ||
                        member.getVoiceState().getChannel().getMembers().stream().filter(mem -> !mem.getUser().isBot()).count() == 1);
    }

}
