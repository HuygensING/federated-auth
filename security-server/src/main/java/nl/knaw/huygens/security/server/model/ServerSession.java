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

import javax.security.auth.Destroyable;
import javax.security.auth.Refreshable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.HuygensSession;
import nl.knaw.huygens.security.server.util.PropertiesHelper;
import org.joda.time.DateTime;

public class ServerSession implements HuygensSession, Destroyable, Refreshable {
    private static final int DURATION = PropertiesHelper.getIntegerProperty("session.duration");

    private final UUID id;

    private final HuygensPrincipal owner;

    private boolean destroyed;

    @JsonIgnore
    private DateTime expiresAt;

    public ServerSession(HuygensPrincipal owner) {
        this.owner = checkNotNull(owner);
        this.id = UUID.randomUUID();
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
        return getExpiresAt().isAfterNow();
    }

    @Override
    public void refresh() {
        expiresAt = new DateTime().plusMinutes(DURATION);
    }

    DateTime getExpiresAt() {
        return expiresAt;
    }
}
