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


import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.UUID;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sun.jersey.guice.JerseyServletModule;
import nl.knaw.huygens.security.server.saml2.SAML2PrincipalAttributesMapper;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientServerMockingModule extends JerseyServletModule {
    private static final Logger log = LoggerFactory.getLogger(ClientServerMockingModule.class);

    private final LoginService loginService;

    public ClientServerMockingModule() {
        log.debug("ClientServerMockingModule created");
        loginService = mock(LoginService.class);
        final String a = "11111111-1111-1111-1111-111111111111";
        given(loginService.createLoginRequest(any(URI.class))).willReturn(UUID.fromString(a));
    }

    @Provides
    @Singleton
    public LoginService provideLoginService() {
        log.debug("Returning mocked LoginService: {}", loginService);
        return loginService;
    }

    @Provides
    @Singleton
    public SessionService provideSessionService() {
        SessionService sessionService = new SessionService();
        log.debug("Returning SessionService: {}", sessionService);
        return sessionService;
    }

    @Provides
    public SAML2PrincipalAttributesMapper provideSAML2PrincipalAttributesMapper() {
        log.debug("Providing SAML2PrincipalAttributesMapper");
        final SAML2PrincipalAttributesMapper mapper = new SAML2PrincipalAttributesMapper();
        return mapper;
    }
}
