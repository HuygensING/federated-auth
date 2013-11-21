package nl.knaw.huygens.security.server.model;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.SecuritySession;

import org.joda.time.DateTime;

public class HuygensSession implements SecuritySession {
    private final UUID id;

    private HuygensPrincipal owner;

    private DateTime expiresOn;

    public HuygensSession() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see nl.knaw.huygens.security.server.model.SecuritySession#getOwner()
     */
    @Override
    public HuygensPrincipal getOwner() {
        return owner;
    }

    public void setOwner(HuygensPrincipal owner) {
        this.owner = owner;
    }

    /* (non-Javadoc)
     * @see nl.knaw.huygens.security.server.model.SecuritySession#getExpiresOn()
     */
    @Override
    public DateTime getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(DateTime expiresOn) {
        this.expiresOn = expiresOn;
    }
}
