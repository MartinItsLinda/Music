package net.tempobot.music.commands.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.tempobot.music.audio.TrackScheduler;
import org.jetbrains.annotations.NotNull;

public class AutoQueueLoadResultHandler implements AudioLoadResultHandler {

    private final TrackScheduler scheduler;
    private final String data;
    public AutoQueueLoadResultHandler(@NotNull("scheduler cannot be null") final TrackScheduler scheduler,
                                      final String data) {
        this.scheduler = scheduler;
        this.data = data;
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        this.scheduler.queue(track, false, this.data);
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        if (!playlist.isSearchResult()) {
            if (playlist.getTracks().size() > 1) {
                playlist.getTracks().forEach(this::trackLoaded);
            } else {
                if (playlist.getSelectedTrack() != null) {
                    this.trackLoaded(playlist.getSelectedTrack());
                } else {
                    this.trackLoaded(playlist.getTracks().get(0));
                }
            }
        }
    }

    @Override
    public void noMatches() {

    }

    @Override
    public void loadFailed(final FriendlyException exception) {
    }

}
