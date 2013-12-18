package nl.knaw.huygens;


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
