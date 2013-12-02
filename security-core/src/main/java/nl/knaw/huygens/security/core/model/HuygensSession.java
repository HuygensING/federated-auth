package nl.knaw.huygens.security.core.model;

import java.util.UUID;

import org.joda.time.DateTime;

public interface HuygensSession {
    public UUID getId();
    public HuygensPrincipal getOwner();
    public DateTime getExpiresAt();
}