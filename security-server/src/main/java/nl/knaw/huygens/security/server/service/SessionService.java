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
import nl.knaw.huygens.security.core.model.HuygensSession;
import nl.knaw.huygens.security.server.ResourceGoneException;
import nl.knaw.huygens.security.server.model.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final Map<UUID, ServerSession> sessions;

    public SessionService() {
        log.debug("SessionService created");
        sessions = Maps.newHashMap();
    }

    public Collection<ServerSession> getSessions() {
        log.debug("Getting all sessions");
        return sessions.values();
    }

    public HuygensSession getSession(UUID sessionId) {
        log.debug("Getting session: [{}]", sessionId);

        return findSession(sessionId);
    }

    public void addSession(ServerSession session) {
        log.debug("Adding session: [{}]", session.getId());
        sessions.put(session.getId(), session);
    }

    public HuygensSession refreshSession(UUID sessionId) {
        log.debug("Refreshing session: [{}]", sessionId);
        final ServerSession session = findSession(sessionId);

        try {
            session.refresh();
        } catch (RefreshFailedException e) {
            log.warn("Failed to refresh session: {}", session);
        }

        return session;
    }

    public HuygensSession destroySession(UUID sessionId) {
        log.debug("Destroying session: [{}]", sessionId);
        final ServerSession session = findSession(sessionId);

        try {
            session.destroy();
        } catch (DestroyFailedException e) {
            log.warn("Failed to destroy session: {}", session);
        }

        return session;
    }

    public Collection<ServerSession> purge() {
        log.debug("Purging expired and destroyed sessions");
        List<ServerSession> purged = Lists.newArrayList();

        for (final Iterator<ServerSession> iter = sessions.values().iterator(); iter.hasNext(); ) {
            final ServerSession session = iter.next();
            if (session.isDestroyed() || !session.isCurrent()) {
                log.debug("Purging: [{}]", session.getId());
                purged.add(session);
                iter.remove(); // iteration safe removal from underlying collection.
            }
        }

        return purged;
    }

    private ServerSession findSession(UUID sessionId) {
        final ServerSession session = sessions.get(sessionId);

        if (session == null) {
            throw new NotFoundException("Unknown session: " + sessionId);
        }

        if (session.isDestroyed()) {
            throw new ResourceGoneException("Destroyed session: " + sessionId);
        }

        if (!session.isCurrent()) {
            throw new ResourceGoneException("Expired session: " + sessionId);
        }

        return session;
    }

}
