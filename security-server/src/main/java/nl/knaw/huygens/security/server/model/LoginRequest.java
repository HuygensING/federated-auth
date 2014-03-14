package nl.knaw.huygens.security.server.model;

/*
 * #%L
 * Security Server
 * =======
 * Copyright (C) 2013 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
        return Objects.toStringHelper(this) //
                .add("relayState", relayState) //
                .add("redirectURI", redirectURI) //
                .add("expiresAt", expiresAt) //
                .add("isExpired", isExpired()) //
                .toString();
    }
}
