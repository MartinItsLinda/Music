package net.tempobot.music.event;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sheepybot.api.entities.event.EventHandler;
import com.sheepybot.api.entities.event.EventListener;
import com.sheepybot.api.entities.messaging.Messaging;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.tempobot.music.emotes.Characters;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

import java.util.concurrent.TimeUnit;

public class GuildMessageReactionListener implements EventListener {

    @EventHandler
    public void onGuildMessageReactionAdd(final GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;

        final MessageReaction.ReactionEmote emote = event.getReactionEmote();

        if (emote.isEmote()) {

            final AudioController controller = Main.get().getAudioLoader().getController(event.getGuild());
            final GuildVoiceState state = event.getMember().getVoiceState();

            if (controller != null && event.getMessageIdLong() == controller.getTrackScheduler().getCurrentSongMessageId() &&
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
                                Messaging.message(channel, "The song was force skipped by a DJ.").deleteAfter(10, TimeUnit.SECONDS).send();
                                scheduler.next(true);
                            } else {
                                if (scheduler.voteSkip(event.getMember())) {
                                    if ((state.getChannel().getMembers().size() / 2) <= scheduler.getSkipVotes()) {
                                        Messaging.message(channel, "Skipping the current song as at least half the voice channel has voted.").deleteAfter(10, TimeUnit.SECONDS).send();
                                        scheduler.next(true);
                                    } else {
                                        Messaging.message(channel, "You voted to skip this song.").deleteAfter(10, TimeUnit.SECONDS).send();
                                    }
                                }
                            }

                            event.getChannel().retrieveMessageById(event.getMessageId())
                                    .queue(message -> message.removeReaction(emote.getEmote(), event.getUser()).queue());

                            return;
                        case Characters.LOOP_NAME:

                            scheduler.setLooping(!scheduler.isLooping());

                            if (scheduler.isLooping()) {
                                Messaging.message(channel, "Keeping the tunes rolling! The current song will continue to play over and over until you run this command again.").deleteAfter(10, TimeUnit.SECONDS).send();
                            } else {
                                Messaging.message(channel, "Time for a change? Once this song ends then the queue will continue as normal.").deleteAfter(10, TimeUnit.SECONDS).send();
                            }

                            return;
                        case Characters.REPEAT_NAME:
                            scheduler.setRepeating(!scheduler.isRepeating());

                            if (scheduler.isRepeating()) {
                                Messaging.message(channel, "Once a song ends it will be added again to the back of the queue.").deleteAfter(10, TimeUnit.SECONDS);
                            } else {
                                Messaging.message(channel, "When your song ends you will have to add it if you want to listen to it again.").deleteAfter(10, TimeUnit.SECONDS);
                            }

                            return;
                        case Characters.STOP_PLAYING_NAME:

                            controller.destroy(false);

                            Messaging.message(channel, "It's sad to say goodbye, I've stopped playing music and left the voice channel. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();

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
                if (controller != null && event.getMessageIdLong() == controller.getTrackScheduler().getCurrentSongMessageId() &&
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
