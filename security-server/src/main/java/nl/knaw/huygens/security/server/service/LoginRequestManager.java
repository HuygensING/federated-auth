package nl.knaw.huygens.security.server.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import nl.knaw.huygens.security.server.model.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginRequestManager {
    private static final Logger log = LoggerFactory.getLogger(LoginRequestManager.class);

    private final Map<UUID, LoginRequest> loginRequestsByRelayState;

    public LoginRequestManager() {
        log.debug("LoginRequestManager created");
        loginRequestsByRelayState = Maps.newHashMap();
    }

    public LoginRequest getLoginRequest(UUID relayState) {
        log.debug("Getting login request: [{}]", relayState);
        return loginRequestsByRelayState.get(relayState);
    }

    public void addLoginRequest(LoginRequest loginRequest) {
        log.debug("Adding login request: [{}]", loginRequest);
        loginRequestsByRelayState.put(loginRequest.getRelayState(), loginRequest);
    }

    public LoginRequest removeLoginRequest(UUID relayState) {
        log.debug("Fetching and removing login request: [{}]", relayState);
        return loginRequestsByRelayState.remove(relayState);
    }

    public int getPendingLoginRequestCount() {
        return loginRequestsByRelayState.size();
    }

    public List<LoginRequest> removeExpiredRequests() {
        final List<LoginRequest> removedLoginRequests = Lists.newArrayList();
        log.debug("Removing expired login requests");

        Iterator<LoginRequest> iter = loginRequestsByRelayState.values().iterator();
        while (iter.hasNext()) {
            final LoginRequest loginRequest = iter.next();
            if (loginRequest.isExpired()) {
                log.debug("Removing expired login request: [{}]", loginRequest.getRelayState());
                removedLoginRequests.add(loginRequest);
                iter.remove();
            }
        }

        return removedLoginRequests;
    }
}
