package me.seymourkrelborn.music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * An {@link AudioLoadResultHandler} used to handle loading of auto play requested songs.
 */
public class AutoPlayLoadResultHandler implements AudioLoadResultHandler {

    private final TrackScheduler scheduler;
    AutoPlayLoadResultHandler(final TrackScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        this.scheduler.queue(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {
            if (playlist.getSelectedTrack() != null) {
                this.trackLoaded(playlist.getSelectedTrack());
            }
        }
    }

    @Override
    public void noMatches() {}

    @Override
    public void loadFailed(FriendlyException exception) {

        final AudioController controller = this.scheduler.getController();

        this.scheduler.clear();
        this.scheduler.setAutoplay(false);
        controller.setPauseTime(System.currentTimeMillis());

        final Guild guild = controller.getJDA().getGuildById(controller.getGuildId());

        if (guild != null) {
            final TextChannel channel = guild.getTextChannelById(controller.getTextChannelId());
            if (channel != null) {
                channel.sendMessage("I couldn't find any recommended videos from YouTube, auto play has been disabled.").queue();
            }
        }

    }

}
