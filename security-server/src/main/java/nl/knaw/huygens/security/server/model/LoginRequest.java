package nl.knaw.huygens.security.server.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.UUID;

public class LoginRequest {
    private final UUID relayState;

    private final URI redirectURI;

    public LoginRequest(final URI redirectURI) {
        this.redirectURI = checkNotNull(redirectURI);
        this.relayState = UUID.randomUUID();
    }

    public UUID getRelayState() {
        return relayState;
    }

    public URI getRedirectURI() {
        return redirectURI;
    }
}
