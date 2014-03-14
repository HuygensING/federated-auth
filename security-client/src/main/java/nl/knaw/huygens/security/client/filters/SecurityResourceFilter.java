package nl.knaw.huygens.security.client.filters;

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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import nl.knaw.huygens.security.client.AuthorizationHandler;
import nl.knaw.huygens.security.client.SecurityContextCreator;
import nl.knaw.huygens.security.client.UnauthorizedException;
import nl.knaw.huygens.security.client.model.SecurityInformation;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

/**
 * The SecurityResourceFilter uses an AuthorizationHandler to get the mandatory information to create a SecurityContext. 
 * In order to determine the user an {@code Authorization} header is expected with an authorization token as value.
 * This SecurityContext is created by a SecurityContextCreator.
 * If the SecurityContext is null, it will not be set to the ContainerRequest. 
 *
 */
public final class SecurityResourceFilter implements ResourceFilter, ContainerRequestFilter {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityResourceFilter.class);
  protected final SecurityContextCreator securityContextCreator;
  protected final AuthorizationHandler authorizationHandler;

  protected SecurityResourceFilter(SecurityContextCreator securityContextCreator, AuthorizationHandler authorizationHandler) {
    this.securityContextCreator = securityContextCreator;
    this.authorizationHandler = authorizationHandler;
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {

    SecurityContext securityContext = createSecurityContext(request);

    if (securityContext != null) {

      /*
       * TODO: fill setSecure and setAuthenticationScheme
       * ContainerRequest.isSecure wraps SecurityContext.isSecure 
       * the same is true for 
       * ContainerRequest.getAuthenticationScheme()
       */
      request.setSecurityContext(securityContext);
    }

    return request;
  }

  protected SecurityContext createSecurityContext(ContainerRequest request) {
    SecurityInformation securityInformation;
    String token = getToken(request);

    LOG.info("token: {} length: {}", token, StringUtils.length(token));

    try {
      securityInformation = authorizationHandler.getSecurityInformation(token);
    } catch (UnauthorizedException e) {
      throw new WebApplicationException(Status.UNAUTHORIZED);
    }

    return securityContextCreator.createSecurityContext(securityInformation);
  }

  private String getToken(ContainerRequest request) {

    return request.getHeaderValue(HttpHeaders.AUTHORIZATION);
  }

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return this;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    // TODO Auto-generated method stub
    return null;
  }
}
