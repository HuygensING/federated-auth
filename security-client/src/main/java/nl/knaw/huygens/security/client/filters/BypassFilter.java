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

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

/**
 * A resource filter that can be used to bypass the security for example.
 * @author martijnm
 *
 */
public final class BypassFilter implements ResourceFilter, ContainerRequestFilter {

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    request.setSecurityContext(new SecurityContext() {

      @Override
      public boolean isUserInRole(String role) {
        return true;
      }

      @Override
      public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public Principal getUserPrincipal() {
        return null;
      }

      @Override
      public String getAuthenticationScheme() {
        return null;
      }
    });

    return request;
  }

  @Override
  public ContainerRequestFilter getRequestFilter() {
    return this;
  }

  @Override
  public ContainerResponseFilter getResponseFilter() {
    return null;
  }

}