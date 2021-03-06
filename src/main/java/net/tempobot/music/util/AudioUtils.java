package net.tempobot.music.util;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.concurrent.TimeUnit;

public class AudioUtils {

    /**
     * Generate a progress bar
     *
     * @param progress The current progress
     * @param length   The total length
     *
     * @return The progress bar
     */
    public static String formatProgressBar(final double progress,
                                           final double length) {

        //formula = ((progress / length) * 100) / (iterations in for loop / 100) (6.66 15's in 100)
        final double percent = ((progress / length) * 100 / 6.66);

        final StringBuilder bar = new StringBuilder();

        for (int i = 0; i < 15; i++) {
            if (progress == length || percent > 95 || percent >= 3 && i <= percent) {
                bar.append("[▬](https://tempobot.net)");
            } else {
                bar.append("▬");
            }
        }

        return bar.toString();
    }

    /**
     * Format the provided {@code millis} to either "hh:mm:ss" or "hh hours, mm minutes and ss seconds" format
     *
     * @param track The {@link AudioTrack}
     *
     * @return The formatted {@link String}
     *
     * @throws IllegalArgumentException If the provided {@code millis} is negative
     */
    public static String formatTrackLength(final AudioTrack track) throws IllegalArgumentException {
        return track.getInfo().isStream ? "STREAM" : formatTrackLength(track.getPosition(), true);
    }

    /**
     * Format the provided {@code millis} to either "hh:mm:ss" or "hh hours, mm minutes and ss seconds" format
     *
     * @param track     The {@link AudioTrack}
     * @param shortened Whether the formatted result should be in long or short
     *
     * @return The formatted {@link String}
     *
     * @throws IllegalArgumentException If the provided {@code millis} is negative
     */
    public static String formatTrackLength(final AudioTrack track, final boolean shortened) throws IllegalArgumentException {
        return track.getInfo().isStream ? "STREAM" : formatTrackLength(track.getPosition(), shortened);
    }

    /**
     * Format the provided {@code millis} to either "hh:mm:ss" or "hh hours, mm minutes and ss seconds" format
     *
     * @param millis    The amount of milliseconds
     *
     * @return The formatted {@link String}
     *
     * @throws IllegalArgumentException If the provided {@code millis} is negative
     */
    public static String formatTrackLength(final long millis) throws IllegalArgumentException  {
        return formatTrackLength(millis, true);
    }

    /**
     * Format the provided {@code millis} to either "hh:mm:ss" or "hh hours, mm minutes and ss seconds" format
     *
     * @param millis    The amount of milliseconds
     * @param shortened Whether the formatted result should be in long or short
     *
     * @return The formatted {@link String}
     *
     * @throws IllegalArgumentException If the provided {@code millis} is negative
     */
    public static String formatTrackLength(final long millis,
                                           final boolean shortened) throws IllegalArgumentException {

        if (millis == 0) {
            if (shortened) {
                return "00:00";
            } else {
                return "0 seconds";
            }
        }

        final long years = millis / 31104000000L;
        final long months = millis / 2592000000L % 12;
        final long days = millis / 86400000L % 30;
        final long hours = millis / 3600000L % 24;
        final long mins = millis / 60000L % 60;
        final long secs = millis / 1000L % 60;

        String formatted = "";
        if (shortened) {

            //short format = e.g. 01:02:03 (hours, minutes and seconds)
            //we append 0 before the number if its less than 10 so it looks nicer, 01:02:03 looks nicer than 1:2:3
            if (hours > 0) {
                formatted += (hours < 10 ? "0" : "") + hours + (mins > 0 ? "" : ":00") + (secs > 0 ? "" : ":00");
            }

            if (mins > 0) {
                formatted += (hours > 0 ? ":" : "") + (mins < 10 ? "0" : "") + mins + (secs > 0 ? "" : ":00");
            }

            formatted += (mins > 0 ? ":" : "00:") + (secs < 10 ? "0" : "") + secs;

        } else {

            //long format = e.g. 1 day, 4 hours, 2 minutes and 3 seconds (or second if it's 1...just an aesthetics change)

            if (years > 0) {
                formatted += years + " year" + (years > 1 ? "s" : "");
            }

            if (months > 0) {
                formatted += (years > 0 ? ", " : "") + months + " month" + (months > 1 ? "s" : "");
            }

            if (days > 0) {
                formatted += (years > 0 || months > 0 ? ", " : "") + days + " day" + (days > 1 ? "s" : "");
            }

            if (hours > 0) {
                formatted += (years > 0 || months > 0 || days > 0 ? ", " : "") + hours + " hour" + (hours > 1 ? "s" : "");
            }

            if (mins > 0) {
                formatted += (years > 0 || months > 0 || days > 0 || hours > 0 ? ", " : "") + mins + " minute" + (mins > 1 ? "s" : "");
            }

            if (secs > 0) {
                formatted += (years > 0 || months > 0 || days > 0 || hours > 0 || mins > 0 ? " and " : "") + secs + " second" + (secs > 1 ? "s" : "");
            }

        }

        return formatted;

    }

}
