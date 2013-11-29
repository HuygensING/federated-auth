package nl.knaw.huygens.security.server;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(BasicAuthFilter.class);

    private final Map<String, Collection<String>> rolesByAuth;

    public BasicAuthFilter() {
        log.debug("BasicAuthFilter created");
        rolesByAuth = Maps.newHashMap();
        rolesByAuth.put("Basic Ym9sa2U6YmVlcg==", ImmutableSet.of("LOGIN_MANAGER")); // "bolke:beer"
        rolesByAuth.put("Basic em9lZjpoYWFz", ImmutableSet.of("SESSION_MANAGER")); // "zoef:haas"
        rolesByAuth.put("Basic bWVqOm1pZXI=", ImmutableSet.of("LOGIN_MANAGER", "SESSION_MANAGER")); // "mej:mier"
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        final String auth = request.getHeaderValue(AUTHORIZATION);
        log.debug("auth: [{}]", auth);
        final Collection<String> roles = rolesByAuth.get(auth);
        if (roles != null) {
            request.setSecurityContext(createSecurityContext(request, roles));
        }
        return request;
    }

    private SecurityContext createSecurityContext(final ContainerRequest request, final Collection<String> roles) {
        log.debug("creating LOGIN_MANAGER security context");

        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public boolean isUserInRole(String role) {
                log.debug("Checking authorization role: [{}] in {}", role, roles);
                return roles.contains(role);
            }

            @Override
            public boolean isSecure() {
                log.debug("Checking if request was made over a secure transport");
                return request.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                log.debug("request for authentication scheme");
                return request.getAuthenticationScheme();
            }
        };
    }
}
