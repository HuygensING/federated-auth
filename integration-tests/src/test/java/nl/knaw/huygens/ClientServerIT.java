package nl.knaw.huygens;

/*
 * #%L
 * Security Integration-tests
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

import static com.sun.jersey.api.client.ClientResponse.Status.SEE_OTHER;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;
import static nl.knaw.huygens.security.server.rest.SAMLResource.SURF_IDP_SSO_URL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

import javax.ws.rs.core.Response.StatusType;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor.Builder;
import nl.knaw.huygens.security.client.HuygensAuthenticationHandler;
import nl.knaw.huygens.security.client.UnauthorizedException;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;
import nl.knaw.huygens.testing.GuiceJerseyTestBase;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientServerIT extends GuiceJerseyTestBase {
    private static final Logger log = LoggerFactory.getLogger(JerseyTest.class);

    private final Client client;

    public ClientServerIT() {
        super(createInjector());
        log.debug("Setting up Client-Server integration test");
        client = Client.create();
        client.setFollowRedirects(false);
    }

    private static Injector createInjector() {
        log.debug("Creating Injector");
        return Guice.createInjector(new ClientServerMockingModule());
    }

    //    @Test
    public void testSomething() {
        HuygensAuthenticationHandler handler = new HuygensAuthenticationHandler(client, "http://www.example.com",
                "Huygens auth");
        try {
            handler.getSecurityInformation("some session");
        } catch (UnauthorizedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLogin() {
        final String loginURL = "/saml2/login";
        final String redirectURL = "hsurl=http://www.example.com";
        final ClientResponse response = resource().path(loginURL).post(ClientResponse.class, redirectURL);
        log.debug("response.status: {}", response.getStatusInfo());
        log.debug("response: {}", response);
        assertThat(response.getStatusInfo(), Is.<StatusType>is(SEE_OTHER));
        assertThat(response.getHeaders().getFirst("Location"), startsWith(SURF_IDP_SSO_URL));

        final LoginService loginService = getInjector().getInstance(LoginService.class);
        log.debug("loginService: {}", loginService);

        final SessionService sessionService = getInjector().getInstance(SessionService.class);
        log.debug("sessionService: {}", sessionService);
    }

    @Override
    protected AppDescriptor configure() {
        return new Builder("nl.knaw.huygens") //
                .initParam(PROPERTY_CONTAINER_REQUEST_FILTERS, LoggingFilter.class.getName()) //
                .initParam(PROPERTY_CONTAINER_RESPONSE_FILTERS, LoggingFilter.class.getName()) //
                .build();
    }

}
