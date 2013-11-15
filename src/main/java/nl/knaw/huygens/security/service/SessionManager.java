package nl.knaw.huygens.security.service;

import java.util.EnumSet;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import nl.knaw.huygens.security.model.HuygensPrincipal;
import nl.knaw.huygens.security.model.HuygensPrincipal.Affiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, HuygensPrincipal> sessions;

    public SessionManager() {
        log.debug("SessionManager created");
        sessions = Maps.newConcurrentMap(); // TODO: check MapMaker
        addSampleSession();
    }

    public HuygensPrincipal getSession(String sessionKey) {
        log.debug("Request for session: [{}]", sessionKey);
        return sessions.get(sessionKey);
    }

    public void addSession(String sessionKey, HuygensPrincipal huygensPrincipal) {
        log.debug("Adding session: [{}] -> [{}]", sessionKey, huygensPrincipal.getPersistentID());
        sessions.put(sessionKey, huygensPrincipal);
    }

    public void removeSession(String sessionKey) {
        log.debug("Removing session: [{}]", sessionKey);
        sessions.remove(sessionKey);
    }

    private void addSampleSession() {
        HuygensPrincipal pa = new HuygensPrincipal();
        pa.setGivenName("Hayco");
        pa.setPersistentID("28f4a6e30d2ef7728fb4b3233cc6313e316cd992");
        pa.setOrganization("huygens.knaw.nl");
        pa.setCommonName("Hayco de Jong");
        pa.setDisplayName("Hayco de Jong");
        pa.setSurname("Jong, de");
        pa.setEmailAddress("hayco.de.jong@huygens.knaw.nl");
        pa.setAffiliations(EnumSet.of(Affiliation.employee));
        sessions.put("deadbeef", pa);
    }
}
