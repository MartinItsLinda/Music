package net.tempobot.guild;

import com.sheepybot.util.Objects;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuildSettings {

    private final long guildId;
    private boolean premium;
    private final List<Long> djRoles;
    private final List<Long> blockedTextChannels;
    private final long addedAt;

    private String prefix;
    private boolean checkingDuplicates;
    private boolean autoAnnounce;
    private boolean autoDelete;
    private boolean autoSearch;
    private boolean autoJoin;
    private int volume;

    public GuildSettings(final long guildId,
                         @NotNull("prefix cannot be null") final String prefix,
                         final boolean premium,
                         final boolean checkingDuplicates,
                         final boolean autoAnnounce,
                         final boolean autoDelete,
                         final boolean autoSearch,
                         final boolean autoJoin,
                         final int volume,
                         @NotNull("dj roles cannot be null") final List<Long> djRoles,
                         @NotNull("dj roles cannot be null") final List<Long> blockedTextChannels,
                         final long addedAt) {
        Objects.checkArgument(prefix.length() >= 1 && prefix.length() <= 5, "prefix must be between 1 and 5 in length");
        this.guildId = guildId;
        this.djRoles = djRoles;
        this.blockedTextChannels = blockedTextChannels;
        this.addedAt = addedAt;

        this.prefix = prefix;
        this.premium = premium;
        this.checkingDuplicates = checkingDuplicates;
        this.autoAnnounce = autoAnnounce;
        this.autoDelete = autoDelete;
        this.autoSearch = autoSearch;
        this.autoJoin = autoJoin;
        this.volume = Math.max(1, Math.min(150, volume));
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@NotNull("prefix cannot be null") final String prefix) {
        Objects.checkArgument(prefix.length() >= 1 && prefix.length() <= 5, "prefix must be between 1 and 5 in length");
        this.prefix = prefix;
    }

    public boolean isCheckingDuplicates() {
        return this.checkingDuplicates;
    }

    public void setCheckingDuplicates(final boolean checkingDuplicates) {
        this.checkingDuplicates = checkingDuplicates;
    }

    public boolean isAutoAnnounce() {
        return this.autoAnnounce;
    }

    public void setAutoAnnounce(final boolean autoAnnounce) {
        this.autoAnnounce = autoAnnounce;
    }

    public boolean isAutoDelete() {
        return this.autoDelete;
    }

    public void setAutoDelete(final boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public boolean isAutoSearch() {
        return this.autoSearch;
    }

    public void setAutoSearch(final boolean autoSearch) {
        this.autoSearch = autoSearch;
    }

    public boolean isAutoJoin() {
        return this.autoJoin;
    }

    public void setAutoJoin(final boolean autoJoin) {
        this.autoJoin = autoJoin;
    }

    public int getVolume() {
        return this.volume;
    }

    public void setVolume(final int volume) {
        this.volume = Math.max(1, Math.min(150, volume));
    }

    public List<Long> getDjRoles() {
        return this.djRoles;
    }

    public List<Long> getBlockedTextChannels() {
        return this.blockedTextChannels;
    }

    public boolean isPremium() {
        return this.premium;
    }

    public void setPremium(final boolean premium) {
        this.premium = premium;
    }

    public long getAddedAt() {
        return this.addedAt;
    }

}
