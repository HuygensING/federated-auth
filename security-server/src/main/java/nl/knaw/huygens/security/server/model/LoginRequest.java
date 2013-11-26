package nl.knaw.huygens.security.server.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.UUID;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class LoginRequest {
    private final UUID relayState;

    private final URI redirectURI;

    private final DateTime expiresAt;

    public LoginRequest(final URI redirectURI) {
        this.redirectURI = checkNotNull(redirectURI);
        this.relayState = UUID.randomUUID();
        this.expiresAt = new DateTime().plusMinutes(5);
    }

    public UUID getRelayState() {
        return relayState;
    }

    public URI getRedirectURI() {
        return redirectURI;
    }

    public boolean isExpired() {
        return expiresAt.isBeforeNow();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)//
                .add("relayState", relayState)//
                .add("redirectURI", redirectURI)//
                .add("expiresAt", expiresAt)
                .toString();
    }
}
