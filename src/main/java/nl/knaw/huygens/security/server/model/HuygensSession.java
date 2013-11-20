package nl.knaw.huygens.security.server.model;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import org.joda.time.DateTime;

public class HuygensSession {
    private final UUID id;

    private HuygensPrincipal owner;

    private DateTime expiresOn;

    public HuygensSession() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public HuygensPrincipal getOwner() {
        return owner;
    }

    public void setOwner(HuygensPrincipal owner) {
        this.owner = owner;
    }

    public DateTime getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(DateTime expiresOn) {
        this.expiresOn = expiresOn;
    }
}
