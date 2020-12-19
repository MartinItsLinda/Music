package net.tempobot.music.source.spotify;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.IPlaylistItem;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyAudioSourceManager implements AudioSourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

    //regex from https://github.com/DuncteBot/SkyBot/blob/master/src/main/java/ml/duncte123/skybot/audio/sourcemanagers/spotify/SpotifyAudioSourceManager.java
    //this source manager wouldn't have been possible without it, albeit it now uses a different method for retrieving audio tracks
    //credit should still go to duncte for the regex.

    private static final String PROTOCOL_REGEX = "?:spotify:(track:)|(?:http://|https://)[a-z]+\\.";
    private static final String DOMAIN_REGEX = "spotify\\.com/";
    private static final String TRACK_REGEX = "track/([a-zA-z0-9]+)";
    private static final String ALBUM_REGEX = "album/([a-zA-z0-9]+)";
    private static final String USER_PART = "user/(?:.*)/";
    private static final String PLAYLIST_REGEX = "playlist/([a-zA-z0-9]+)";
    private static final String REST_REGEX = "(?:.*)";
    private static final String SPOTIFY_BASE_REGEX = PROTOCOL_REGEX + DOMAIN_REGEX;

    private static final Pattern SPOTIFY_TRACK_REGEX = Pattern.compile("^(" + SPOTIFY_BASE_REGEX + TRACK_REGEX + ")" + REST_REGEX + "$");
    private static final Pattern SPOTIFY_ALBUM_REGEX = Pattern.compile("^(" + SPOTIFY_BASE_REGEX + ALBUM_REGEX + ")" + REST_REGEX + "$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX = Pattern.compile("^(" + SPOTIFY_BASE_REGEX + ")" + PLAYLIST_REGEX + REST_REGEX + "$");
    private static final Pattern SPOTIFY_PLAYLIST_REGEX_USER = Pattern.compile("^(" + SPOTIFY_BASE_REGEX + ")" + USER_PART + PLAYLIST_REGEX + REST_REGEX + "$");
    private static final Pattern SPOTIFY_SECOND_PLAYLIST_REGEX = Pattern.compile("^(?:spotify:user:)(?:.*)(?::playlist:)(.*)$");

    private final SpotifyApi spotifyApi;
    private final YoutubeAudioSourceManager youtubeAudioSourceManager;
    private final ScheduledExecutorService service;

    public SpotifyAudioSourceManager(@NotNull("source manager cannot be null") final YoutubeAudioSourceManager youtubeAudioSourceManager,
                                     @NotNull("client id cannot be null") final String clientId,
                                     @NotNull("client secret cannot be null") final String clientSecret) {
        this.youtubeAudioSourceManager = youtubeAudioSourceManager;
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();

        this.service = Executors.newScheduledThreadPool(2, (r) -> new Thread(r, "Spotify-Token-Update-Thread"));
        this.service.scheduleAtFixedRate(this::updateAccessToken, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(final DefaultAudioPlayerManager manager,
                              final AudioReference reference) {
        return this.loadItem0(manager, reference);
    }

    private AudioItem loadItem0(final DefaultAudioPlayerManager manager,
                                final AudioReference reference) {

        AudioItem item = this.getSpotifyAlbum(manager, reference);

        if (item == null) {
            item = this.getSpotifyPlaylist(manager, reference);
        }

        if (item == null) {
            item = this.getSpotifyTrack(manager, reference);
        }

        return item;
    }

    private AudioItem getSpotifyAlbum(final DefaultAudioPlayerManager manager,
                                      final AudioReference reference) {
        final Matcher res = SPOTIFY_ALBUM_REGEX.matcher(reference.identifier);

        if (!res.matches()) {
            return null;
        }

        try {
            final Album album = this.spotifyApi.getAlbum(res.group(res.groupCount())).build().executeAsync().get();
            final List<AudioTrack> playlist = new ArrayList<>(album.getTracks().getTotal());

            for (final TrackSimplified track : album.getTracks().getItems()) {
                final AudioItem item = this.youtubeAudioSourceManager.loadItem(manager, new AudioReference(String.format("ytsearch: %s %s", track.getName(), track.getArtists()[0].getName()), null));
                if (item == null) continue;

                final BasicAudioPlaylist plist = (BasicAudioPlaylist) item;
                if (plist.getSelectedTrack() != null) {
                    playlist.add(plist.getSelectedTrack());
                } else if (plist.getTracks().size() > 0) {
                    playlist.add(plist.getTracks().get(0));
                }

            }

            //just a failsafe
            if (playlist.size() == 0) {
                return null;
            }

            return new BasicAudioPlaylist(album.getName(), playlist, playlist.get(0), false);
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), Severity.FAULT, e);
        }
    }

    private AudioItem getSpotifyPlaylist(final DefaultAudioPlayerManager manager,
                                         final AudioReference reference) {
        final Matcher res = getSpotifyPlaylistFromString(reference.identifier);

        if (!res.matches()) {
            return null;
        }

        final String playListId = res.group(res.groupCount());

        try {

            final Playlist spotifyPlaylist = this.spotifyApi.getPlaylist(playListId).build().execute();
            final PlaylistTrack[] playlistTracks = spotifyPlaylist.getTracks().getItems();

            if (playlistTracks.length == 0) {
                return null;
            }

            final List<AudioTrack> playlist = new ArrayList<>(playlistTracks.length);

            for (final PlaylistTrack playlistTrack : playlistTracks) {
                if (playlistTrack.getIsLocal()) {
                    continue;
                }

                final IPlaylistItem item = playlistTrack.getTrack();

                if (!(item instanceof Track)) {
                    continue;
                }

                final Track track = (Track) item;
                final AudioItem audioItem = this.youtubeAudioSourceManager.loadItem(manager, new AudioReference(String.format("ytsearch: %s %s", track.getName(), track.getArtists()[0].getName()), null));
                if (audioItem == null) continue;

                final BasicAudioPlaylist plist = (BasicAudioPlaylist) item;
                if (plist.getSelectedTrack() != null) {
                    playlist.add(plist.getSelectedTrack());
                } else if (plist.getTracks().size() > 0) {
                    playlist.add(plist.getTracks().get(0));
                }

            }

            //just a failsafe
            if (playlist.size() == 0) {
                return null;
            }

            return new BasicAudioPlaylist(spotifyPlaylist.getName(), playlist, playlist.get(0), false);
        } catch (IllegalArgumentException ex) {
            throw new FriendlyException("This playlist could not be loaded, make sure that it's public", Severity.COMMON, ex);
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e.getMessage(), Severity.FAULT, e);
        }

    }

    private AudioItem getSpotifyTrack(final DefaultAudioPlayerManager manager,
                                      final AudioReference reference) {
        final Matcher res = SPOTIFY_TRACK_REGEX.matcher(reference.identifier);

        if (!res.matches()) {
            return null;
        }

        try {
            final Track track = this.spotifyApi.getTrack(res.group(res.groupCount())).build().execute();

            final AudioItem item = this.youtubeAudioSourceManager.loadItem(manager, new AudioReference(String.format("ytsearch: %s %s", track.getName(), track.getArtists()[0].getName()), null));
            if (item == null) return null;

            final BasicAudioPlaylist plist = (BasicAudioPlaylist) item;
            if (plist.getSelectedTrack() != null) {
                return plist.getSelectedTrack();
            } else if (plist.getTracks().size() > 0) {
                return plist.getTracks().get(0);
            }

            return null;
        } catch (Exception e) {
            throw new FriendlyException(e.getMessage(), Severity.FAULT, e);
        }
    }

    @Override
    public boolean isTrackEncodable(final AudioTrack track) {
        return this.youtubeAudioSourceManager.isTrackEncodable(track);
    }

    @Override
    public void encodeTrack(final AudioTrack track,
                            final DataOutput output) {
        this.youtubeAudioSourceManager.encodeTrack(track, output);
    }

    @Override
    public AudioTrack decodeTrack(final AudioTrackInfo trackInfo,
                                  final DataInput input) {
        return this.youtubeAudioSourceManager.decodeTrack(trackInfo, input);
    }

    @Override
    public void shutdown() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    private void updateAccessToken() {
        try {
            final ClientCredentialsRequest request = this.spotifyApi.clientCredentials().build();
            final ClientCredentials clientCredentials = request.execute();

            // Set access token for further "spotifyApi" object usage
            this.spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            LOGGER.debug("Successfully retrieved an access token! " + clientCredentials.getAccessToken());
            LOGGER.debug("The access token expires in " + clientCredentials.getExpiresIn() + " seconds");
        } catch (final IOException | SpotifyWebApiException | ParseException ex) {
            LOGGER.error("Encountered an error whilst attempting to fetch a new access token from Spotify", ex);
            this.service.schedule(this::updateAccessToken, 10L, TimeUnit.SECONDS);
        }
    }

    private Matcher getSpotifyPlaylistFromString(final String input) {
        final Matcher match = SPOTIFY_PLAYLIST_REGEX.matcher(input);

        if (match.matches()) {
            return match;
        }

        final Matcher withUser = SPOTIFY_PLAYLIST_REGEX_USER.matcher(input);

        if (withUser.matches()) {
            return withUser;
        }

        return SPOTIFY_SECOND_PLAYLIST_REGEX.matcher(input);
    }

}