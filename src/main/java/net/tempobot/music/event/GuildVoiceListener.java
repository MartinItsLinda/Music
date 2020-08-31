package net.tempobot.music.event;

import com.sheepybot.api.entities.event.EventHandler;
import com.sheepybot.api.entities.event.EventListener;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

public class GuildVoiceListener implements EventListener {

    @EventHandler
    public void onGuildVoiceJoin(final GuildVoiceJoinEvent event) {
        final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());
        if (controller != null && controller.getVoiceChannelId() == event.getChannelJoined().getIdLong()) {
            if (controller.isPaused()) {
                controller.setPaused(false);
            }
        }
    }

    @EventHandler
    public void onGuildVoiceLeave(final GuildVoiceLeaveEvent event) {

        final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());

        if (controller != null && controller.getVoiceChannelId() == event.getChannelLeft().getIdLong()) {
            if (event.getMember().getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
                controller.destroy();
            } else if (event.getChannelLeft().getMembers().stream().allMatch(member -> member.getUser().isBot())) {
                controller.setPaused(true);
            }
        }

    }

    @EventHandler
    public void onGuildVoiceMove(final GuildVoiceMoveEvent event) {

        final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());
        final GuildVoiceState state = event.getGuild().getSelfMember().getVoiceState();
        if (controller != null && state != null && state.getChannel() != null && event.getMember().getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
            controller.setVoiceChannelId(event.getChannelJoined());
            if (event.getMember().getIdLong() == event.getGuild().getSelfMember().getIdLong() && event.getChannelJoined().getMembers().stream().allMatch(member -> member.getUser().isBot())) {
                controller.setPaused(true);
            }
        }

    }

}
