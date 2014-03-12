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

import nl.knaw.huygens.security.client.filters.AbstractRolesAllowedResourceFilterFactory;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class TestAbstractRolesAllowedResourceFilterFactorySubclass extends AbstractRolesAllowedResourceFilterFactory {

  public TestAbstractRolesAllowedResourceFilterFactorySubclass(boolean securityEnabled) {
    super(securityEnabled);
  }

  @Override
  protected ResourceFilter createResourceFilter(AbstractMethod am) {
    return new SecurityFilter();
  }

  @Override
  protected ResourceFilter createNoSecurityResourceFilter() {
    return new NoSecurityFilter();
  }

  static final class NoSecurityFilter implements ResourceFilter {

    @Override
    public ContainerRequestFilter getRequestFilter() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      // TODO Auto-generated method stub
      return null;
    }

  }

  static final class SecurityFilter implements ResourceFilter {

    @Override
    public ContainerRequestFilter getRequestFilter() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      // TODO Auto-generated method stub
      return null;
    }

  }

}
