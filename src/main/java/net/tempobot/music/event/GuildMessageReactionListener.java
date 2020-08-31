package net.tempobot.music.event;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sheepybot.api.entities.event.EventHandler;
import com.sheepybot.api.entities.event.EventListener;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.tempobot.music.emotes.Characters;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

public class GuildMessageReactionListener implements EventListener {

    @EventHandler
    public void onGuildMessageReactionAdd(final GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;

        final MessageReaction.ReactionEmote emote = event.getReactionEmote();

        if (emote.isEmote()) {

            final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());
            final GuildVoiceState state = event.getMember().getVoiceState();

            if (controller != null && event.getMessageIdLong() == controller.getTrackScheduler().getMessageId() &&
                    state != null && state.getChannel() != null && state.getChannel().getIdLong() == controller.getVoiceChannelId()) {

                final TrackScheduler scheduler = controller.getTrackScheduler();

                final TextChannel channel = event.getGuild().getTextChannelById(controller.getTextChannelId());
                if (channel != null) {

                    switch (emote.getEmote().getName()) {
                        case Characters.TRACK_PREVIOUS_NAME:

                            if (scheduler.getPreviousTrack() != null) {

                                final AudioTrack previous = scheduler.getPreviousTrack();
                                final AudioTrack current = scheduler.getCurrentTrack();

                                scheduler.queue(previous, true, previous.getUserData());
                                scheduler.next(true);
                                scheduler.queue(current, true, current.getUserData());

                            }

                            event.getChannel().retrieveMessageById(event.getMessageId())
                                    .queue(message -> message.removeReaction(emote.getEmote(), event.getUser()).queue());

                            return;
                        case Characters.PLAY_PAUSE_NAME:
                            controller.setPaused(!controller.isPaused());
                            return;
                        case Characters.TRACK_NEXT_NAME:

                            if (scheduler.isDJ(event.getMember())) {
                                channel.sendMessage("The current song has forcibly been skipped by " + event.getMember().getUser().getAsTag()).queue();
                                scheduler.next(true);
                            } else {
                                if (scheduler.voteSkip(event.getMember())) {
                                    if ((state.getChannel().getMembers().size() / 2) <= scheduler.getSkipVotes()) {
                                        channel.sendMessage("I've skipped the current song as enough people have voted").queue();
                                        scheduler.next(true);
                                    } else {
                                        channel.sendMessage(event.getMember().getAsMention() + " has voted to skip the current song.").queue();
                                    }
                                }
                            }

                            event.getChannel().retrieveMessageById(event.getMessageId())
                                    .queue(message -> message.removeReaction(emote.getEmote(), event.getUser()).queue());

                            return;
                        case Characters.LOOP_NAME:

                            scheduler.setLooping(!scheduler.isLooping());

                            if (scheduler.isLooping()) {
                                channel.sendMessage("The music queue is now looping").queue();
                            } else {
                                channel.sendMessage("The music queue is no longer looping").queue();
                            }

                            return;
                        case Characters.REPEAT_NAME:
                            scheduler.setRepeating(!scheduler.isRepeating());

                            if (scheduler.isRepeating()) {
                                channel.sendMessage("The current song is now looping").queue();
                            } else {
                                channel.sendMessage("The current song is no longer looping").queue();
                            }

                            return;
//                        case Characters.AUTOPLAY_NAME:
//
//                            scheduler.setAutoplay(!scheduler.isAutoplay());
//
//                            if (scheduler.isAutoplay()) {
//                                channel.sendMessage("When the queue finishes, I will find related videos on YouTube and auto play them.").queue();
//                            } else {
//                                channel.sendMessage("Auto play has been turned off, I will no longer search YouTube for related videos.").queue();
//                            }
//
//                            return;
                        case Characters.STOP_PLAYING_NAME:

                            controller.destroy();

                            channel.sendMessage("I've stopped playing music and left the voice channel").queue();

                            return;
                        default:
                            break;

                    }

                }

            }

        }

    }

    @EventHandler
    public void onGuildMessageReactionRemovedEvent(final GuildMessageReactionRemoveEvent event) {
        if (event.getUser() == null || event.getUser().isBot()) return;

        final MessageReaction.ReactionEmote emote = event.getReactionEmote();

        if (emote.isEmote()) {
            event.getGuild().retrieveMemberById(event.getUserIdLong()).queue(member -> {

                final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());

                final GuildVoiceState state = member.getVoiceState();
                if (controller != null && event.getMessageIdLong() == controller.getTrackScheduler().getMessageId() &&
                        state != null && state.getChannel() != null && state.getChannel().getIdLong() == controller.getVoiceChannelId()) {

                    final TrackScheduler scheduler = controller.getTrackScheduler();

                    final TextChannel channel = event.getGuild().getTextChannelById(controller.getTextChannelId());
                    if (channel != null) {

                        switch (emote.getEmote().getName()) {

                            case Characters.PLAY_PAUSE_NAME:
                                controller.setPaused(!controller.isPaused());
                                return;
                            case Characters.LOOP_NAME:

                                scheduler.setLooping(!scheduler.isLooping());

                                if (scheduler.isLooping()) {
                                    channel.sendMessage("The music queue is now looping").queue();
                                } else {
                                    channel.sendMessage("The music queue is no longer looping").queue();
                                }

                                return;
                            case Characters.REPEAT_NAME:
                                scheduler.setRepeating(!scheduler.isRepeating());

                                if (scheduler.isRepeating()) {
                                    channel.sendMessage("The current song is now looping").queue();
                                } else {
                                    channel.sendMessage("The current song is no longer looping").queue();
                                }

                                return;
//                            case Characters.AUTOPLAY_NAME:
//
//                                scheduler.setAutoplay(!scheduler.isAutoplay());
//
//                                if (scheduler.isAutoplay()) {
//                                    channel.sendMessage("When the queue finishes, I will find related videos on YouTube and auto play them.").queue();
//                                } else {
//                                    channel.sendMessage("Auto play has been turned off, I will no longer search YouTube for related videos.").queue();
//                                }
//
//                                return;
                            default:
                                break;

                        }

                    }

                }

            });
        }
        
    }
    
}
