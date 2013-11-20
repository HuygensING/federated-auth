package nl.knaw.huygens.security.server.service;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import nl.knaw.huygens.security.server.model.HuygensSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final Map<UUID, HuygensSession> sessions;

    public SessionManager() {
        log.debug("SessionManager created");
        sessions = Maps.newConcurrentMap(); // TODO: check MapMaker
    }

    public HuygensSession getSession(UUID sessionKey) {
        log.debug("Request for session: [{}]", sessionKey);
        return sessions.get(sessionKey);
    }

    public void addSession(HuygensSession session) {
        log.debug("Adding session: [{}]", session.getId());
        sessions.put(session.getId(), session);
    }

    public void removeSession(HuygensSession session) {
        log.debug("Removing session: [{}]", session.getId());
        sessions.remove(session.getId());
    }

}
