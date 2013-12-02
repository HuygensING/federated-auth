package nl.knaw.huygens.security.server.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import org.joda.time.DateTime;

public class ServerSessionImpl implements ServerSession {
    private final UUID id;

    private final HuygensPrincipal owner;

    private DateTime expiresAt;

    private boolean destroyed;

    public ServerSessionImpl(HuygensPrincipal owner) {
        this.id = UUID.randomUUID();
        this.expiresAt = createExpiry();
        this.owner = owner;
    }

    private static DateTime createExpiry() {
        return new DateTime().plusMinutes(60);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public HuygensPrincipal getOwner() {
        return owner;
    }

    @Override
    public DateTime getExpiresAt() {
        return expiresAt;
    }

    @JsonIgnore
    @Override
    public boolean isCurrent() {
        return expiresAt.isAfterNow();
    }

    @Override
    public void refresh() {
        expiresAt = createExpiry();
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @JsonIgnore
    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
}
