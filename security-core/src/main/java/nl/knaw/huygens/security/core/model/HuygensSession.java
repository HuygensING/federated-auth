package nl.knaw.huygens.security.core.model;

import java.util.UUID;

public interface HuygensSession {
    public UUID getId();
    public HuygensPrincipal getOwner();
}