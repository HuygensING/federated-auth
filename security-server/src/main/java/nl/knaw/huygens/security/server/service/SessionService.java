package nl.knaw.huygens.security.server.service;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.RefreshFailedException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.server.ResourceGoneException;
import nl.knaw.huygens.security.server.model.ServerSession;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final Map<UUID, ServerSession> sessions;

    private DateTime nextPurge;

    public SessionService() {
        log.debug("SessionService created");
        sessions = Maps.newHashMap();
        nextPurge = DateTime.now().plusMinutes(1);
    }

    public Collection<ServerSession> getSessions() {
        log.debug("Getting all sessions");
        return sessions.values();
    }

    public ServerSession findSession(final UUID sessionID) {
        log.debug("Getting session: [{}]", sessionID);
        final ServerSession session = getSession(sessionID);

        if (session == null) {
            throw new NotFoundException("Unknown session: " + sessionID);
        }

        if (!session.isCurrent()) {
            throw new ResourceGoneException("Expired session: " + sessionID);
        }

        if (session.isDestroyed()) {
            throw new ResourceGoneException("Destroyed session: " + sessionID);
        }

        return session;
    }

    public void addSession(final ServerSession session) {
        final UUID sessionID = session.getId();

        if (sessionID == null) {
            log.warn("Attempt to add session with null session ID");
            throw new IllegalArgumentException("Refusing to add session with null session ID");
        }

        if (!session.isCurrent()) {
            log.warn("Attempt to add expired session: {}", sessionID);
            throw new IllegalArgumentException("Refusing to add expired session: " + sessionID);
        }

        if (session.isDestroyed()) {
            log.warn("Attempt to add destroyed session: {}", sessionID);
            throw new IllegalArgumentException("Refusing to add destroyed session: " + sessionID);
        }

        if (sessions.containsKey(sessionID)) {
            log.warn("Attempt to add duplicate session: {}", sessionID);
            throw new IllegalStateException("Session already added for session ID: " + sessionID);
        }

        if (isTimeToPurge()) {
            purge();
        }

        log.debug("Adding session: [{}]", sessionID);
        sessions.put(sessionID, session);
    }

    public ServerSession refreshSession(final UUID sessionID) {
        log.debug("Refreshing session: [{}]", sessionID);

        final ServerSession session = findSession(sessionID);

        try {
            session.refresh();
        } catch (RefreshFailedException e) {
            log.warn("Failed to refresh session: {}", session);
        }

        return session;
    }

    public ServerSession destroySession(final UUID sessionID) {
        log.debug("Destroying session: [{}]", sessionID);

        final ServerSession session = findSession(sessionID);

        try {
            session.destroy();
        } catch (DestroyFailedException e) {
            log.warn("Failed to destroy session: {}", session);
        }

        return session;
    }

    public Collection<ServerSession> purge() {
        log.debug("Purging stale sessions");
        List<ServerSession> purged = Lists.newArrayList();

        for (final Iterator<ServerSession> iter = sessions.values().iterator(); iter.hasNext(); ) {
            final ServerSession session = iter.next();
            if (session.isDestroyed() || !session.isCurrent()) {
                log.debug("Purging: [{}]", session.getId());
                purged.add(session);
                iter.remove(); // iteration safe removal from underlying collection.
            }
        }

        nextPurge = DateTime.now().plusMinutes(5);
        log.debug("Next purge after: [{}]", nextPurge);

        return purged;
    }

    ServerSession getSession(final UUID sessionID) {
        return sessions.get(sessionID);
    }

    boolean isTimeToPurge() {
        return nextPurge.isBeforeNow();
    }

}
