package nl.knaw.huygens.security.server.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import org.joda.time.DateTime;

public class ServerSessionImpl implements ServerSession {
    private final UUID id;

    private final HuygensPrincipal owner;

    private boolean destroyed;

    @JsonIgnore
    private DateTime expiresAt;

    public ServerSessionImpl(HuygensPrincipal owner) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        refresh();
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
    public void destroy() {
        destroyed = true;
    }

    @JsonIgnore
    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @JsonIgnore
    @Override
    public boolean isCurrent() {
        return expiresAt.isAfterNow();
    }

    @Override
    public void refresh() {
        expiresAt = new DateTime().plusMinutes(60);
    }
}
