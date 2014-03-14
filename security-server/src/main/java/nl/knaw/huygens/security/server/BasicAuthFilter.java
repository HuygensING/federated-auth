package nl.knaw.huygens.security.server;

/*
 * #%L
 * Security Server
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

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import nl.knaw.huygens.security.server.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(BasicAuthFilter.class);

    private final Map<String, Collection<String>> rolesByAuth;

    public BasicAuthFilter() {
        log.debug("BasicAuthFilter created");
        rolesByAuth = readRoles();
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        final String auth = request.getHeaderValue(AUTHORIZATION);
        if (auth != null) {
            final Collection<String> roles = rolesByAuth.get(auth);
            if (roles != null) {
                request.setSecurityContext(createSecurityContext(request, roles));
            }
        }

        return request;
    }

    private Map<String, Collection<String>> readRoles() {
        final Map<String, Collection<String>> authRoles = Maps.newHashMap();

        log.debug("Reading authorization roles");
        final Properties authProperties = PropertiesHelper.getAuthProperties();
        final Splitter commaSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        for (String profile : commaSplitter.split(authProperties.getProperty("profiles"))) {
            final String authProp = authProperties.getProperty(profile + ".auth");
            final String rolesProp = authProperties.getProperty(profile + ".roles");
            final ImmutableSet<String> roles = ImmutableSet.copyOf(commaSplitter.split(rolesProp));
            log.debug("Auth profile [{}] has roles: {}", profile, roles);
            authRoles.put(authProp, roles);
        }

        return authRoles;
    }

    private final SecurityContext createSecurityContext(final ContainerRequest request,
                                                        final Collection<String> roles) {
        log.debug("Creating security context with roles: {}", roles);

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
