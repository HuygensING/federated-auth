package nl.knaw.huygens.security.client;

/*
 * #%L
 * Security Client
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
import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static com.sun.jersey.api.client.ClientResponse.Status.FORBIDDEN;
import static com.sun.jersey.api.client.ClientResponse.Status.GONE;
import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static nl.knaw.huygens.security.core.rest.API.REFRESH_PATH;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_URI;

import javax.ws.rs.core.Response.StatusType;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.HuygensSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuygensAuthenticationHandler implements AuthenticationHandler {

    private Logger LOG = LoggerFactory.getLogger(HuygensAuthenticationHandler.class);

    private final Client client;
    private final String authorizationUrl;
    private final String basicCredentials;

    public HuygensAuthenticationHandler(Client client, String authorizationUrl, String basicCredentials) {
        this.client = checkNotNull(client);
        this.basicCredentials = checkNotNull(basicCredentials);
        if (StringUtils.isBlank(authorizationUrl)) {
            LOG.info("Authorization url was empty");
            throw new IllegalArgumentException("Authorization url was empty");
        }

        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public SecurityInformation getSecurityInformation(String sessionToken) throws UnauthorizedException {

        HuygensSession session = doSessionDetailsRequest(sessionToken);

        doSessionRefresh(sessionToken);

        return new HuygensSecurityInformation(session.getOwner());
    }

    private void doSessionRefresh(String sessionToken) {
        client.resource(authorizationUrl).path(SESSION_AUTHENTICATION_URI).path(sessionToken).path(REFRESH_PATH)
              .header(HttpHeaders.AUTHORIZATION, basicCredentials).put();
    }

    private HuygensSession doSessionDetailsRequest(String sessionToken) throws UnauthorizedException {
        LOG.info("sessionToken: {}", sessionToken);
        if (StringUtils.isBlank(sessionToken)) {
            LOG.info("Session token was empty");
            throw new UnauthorizedException();
        }

        LOG.info("authorization url: {}", authorizationUrl);

        Builder builder = createSessionResourceBuilder(sessionToken);

        ClientResponse response = builder.get(ClientResponse.class);

        StatusType statusType = response.getStatusInfo();
        if (statusType == OK) {
            // expected.
        }
        else if (statusType == NOT_FOUND) {
            LOG.info("Session token {} is unknown.", sessionToken);
            throw new UnauthorizedException();
        }
        else if (statusType == GONE) {
            LOG.info("Session of token {} is expired.", sessionToken);
            throw new UnauthorizedException();
        }
        else if (statusType == BAD_REQUEST) {
            LOG.error("Illegal session token {}.", sessionToken);
            throw new UnauthorizedException();
        }
        else {
            LOG.error("Unknown exception for token {}.", sessionToken);
            LOG.error("Response status is {}.", statusType);
            throw new UnauthorizedException();
        }

        LOG.info("clientResponse: " + response);

        HuygensSession session = response.getEntity(ClientSession.class);
        return session;
    }

    private Builder createSessionResourceBuilder(String sessionToken) {
        WebResource resource = client.resource(authorizationUrl).path(SESSION_AUTHENTICATION_URI).path(sessionToken);

        LOG.info("url: {}", resource.getURI());

        return resource.header(HttpHeaders.AUTHORIZATION, basicCredentials);
    }

    /**
     * Destroy the session of the user that wants to logout.
     *
     * @param sessionToken the session id that should be destroyed.
     * @return true if the session was (already) destroyed or expired.
     * @throws IllegalArgumentException if {@code sessionToken} is not a legal session id on the server.
     * @throws UnauthorizedException    when the client is unauthorized to destroy the session.
     */
    public boolean logout(String sessionToken) throws UnauthorizedException {
        Builder builder = createSessionResourceBuilder(sessionToken);

        ClientResponse response = builder.delete(ClientResponse.class);

        StatusType statusType = response.getStatusInfo();
        if (statusType == OK || statusType == NOT_FOUND || statusType == GONE) {
            return true;
        }
        else if (statusType == BAD_REQUEST) {
            throw new IllegalArgumentException("Illegal session token: " + sessionToken);
        }
        else if (statusType == FORBIDDEN) {
            throw new UnauthorizedException("Session token: " + sessionToken);
        }
        else {
            return false;
        }
    }
}
