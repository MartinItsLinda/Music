package net.tempobot.guild;

import com.sheepybot.util.Objects;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuildSettings {

    private final long guildId;
    private String prefix;
    private boolean premium;
    private final List<Long> djRoles;
    private final long addedAt;

    public GuildSettings(final long guildId,
                         @NotNull(value = "prefix cannot be null") final String prefix,
                         final boolean premium,
                         @NotNull(value = "dj roles cannot be null") final List<Long> djRoles,
                         final long addedAt) {
        Objects.checkArgument(prefix.length() >= 1 && prefix.length() <= 5, "prefix must be between 1 and 5 in length");
        this.guildId = guildId;
        this.prefix = prefix;
        this.premium = premium;
        this.djRoles = djRoles;
        this.addedAt = addedAt;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@NotNull(value = "prefix cannot be null") final String prefix) {
        Objects.checkArgument(prefix.length() >= 1 && prefix.length() <= 5, "prefix must be between 1 and 5 in length");
        this.prefix = prefix;
    }

    public List<Long> getDjRoles() {
        return this.djRoles;
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
