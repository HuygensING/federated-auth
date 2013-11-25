package nl.knaw.huygens.security.server.model;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.HuygensSession;

public class HuygensSessionImpl implements HuygensSession {
    private final UUID id;

    private HuygensPrincipal owner;

    public HuygensSessionImpl() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public HuygensPrincipal getOwner() {
        return owner;
    }

    public void setOwner(HuygensPrincipal owner) {
        this.owner = owner;
    }

}
