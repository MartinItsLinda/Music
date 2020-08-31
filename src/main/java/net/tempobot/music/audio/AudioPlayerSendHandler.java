package net.tempobot.music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler {

    private final AudioPlayer player;

    private AudioFrame lastFrame;

    AudioPlayerSendHandler(final AudioPlayer player) {
        this.player = player;
    }

    @Override
    public boolean canProvide() {
        return (this.lastFrame = this.player.provide()) != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(this.lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }

}
