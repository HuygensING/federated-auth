package nl.knaw.huygens.security.server.service;

import static nl.knaw.huygens.security.core.rest.API.REDIRECT_URL_HTTP_PARAM;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.MissingParameterException;
import nl.knaw.huygens.security.server.model.LoginRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginService {
    private static final String MSG_MISSING_HOST_IN_REDIRECT_URI = "Malformed redirect URI (no host)";

    private static final String MSG_MISSING_SCHEME_IN_REDIRECT_URI = "Redirect URI not absolute (missing scheme)";

    private static final String MSG_MALFORMED_REDIRECT_URI = "Malformed redirectURI (unknown protocol)";

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final Map<UUID, LoginRequest> loginRequestsByRelayState;

    private DateTime nextPurge;

    public LoginService() {
        log.debug("LoginService created");
        loginRequestsByRelayState = Maps.newHashMap();
        nextPurge = DateTime.now().plusMinutes(1);
    }

    public UUID createLoginRequest(final URI redirectURI) {
        if (redirectURI == null) {
            throw new MissingParameterException(REDIRECT_URL_HTTP_PARAM);
        }

        // e.g., /resource
        if (redirectURI.getScheme() == null) {
            throw new BadRequestException(MSG_MISSING_SCHEME_IN_REDIRECT_URI);
        }

        // e.g., http:/8080/resource
        if (redirectURI.getHost() == null) {
            throw new BadRequestException(MSG_MISSING_HOST_IN_REDIRECT_URI);
        }

        try {
            // e.g., ptth://www.example.com   (unknown protocol)
            new URL(redirectURI.toString());
        } catch (MalformedURLException e) {
            throw new BadRequestException(MSG_MALFORMED_REDIRECT_URI);
        }

        final LoginRequest loginRequest = new LoginRequest(redirectURI);
        addLoginRequest(loginRequest);

        return loginRequest.getRelayState();
    }

    private void addLoginRequest(LoginRequest loginRequest) {
        log.debug("Adding login request: [{}]", loginRequest);

        if (nextPurge.isBeforeNow()) {
            purgeExpiredRequests();
        }

        loginRequestsByRelayState.put(loginRequest.getRelayState(), loginRequest);
    }

    public LoginRequest removeLoginRequest(UUID relayState) {
        log.debug("Fetching and removing login request: [{}]", relayState);

        final LoginRequest loginRequest = loginRequestsByRelayState.remove(relayState);
        if (loginRequest == null || loginRequest.isExpired()) {
            return null;
        }

        log.debug("Found login request: [{}]", loginRequest);
        return loginRequest;
    }

    public Collection<LoginRequest> getPendingLoginRequests() {
        return loginRequestsByRelayState.values();
    }

    public List<LoginRequest> purgeExpiredRequests() {
        log.debug("Purging expired login requests");

        final List<LoginRequest> purged = Lists.newArrayList();

        for (final Iterator<LoginRequest> iter = getPendingLoginRequests().iterator(); iter.hasNext(); ) {
            final LoginRequest loginRequest = iter.next();
            if (loginRequest.isExpired()) {
                log.debug("Purging: [{}]", loginRequest.getRelayState());
                purged.add(loginRequest);
                iter.remove(); // iteration safe removal from underlying collection.
            }
        }

        nextPurge = DateTime.now().plusMinutes(5);

        return purged;
    }
}
