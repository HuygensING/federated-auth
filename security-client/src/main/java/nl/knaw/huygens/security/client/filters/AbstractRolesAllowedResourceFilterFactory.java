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

import javax.annotation.security.RolesAllowed;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;

/**
 * Base class for RolesAllowedFilterFactories like SecurityFilterFactory. 
 * @author martijnm
 *
 */
public abstract class AbstractRolesAllowedResourceFilterFactory extends AbstractSecurityAnnotationResourceFilterFactory {

  public AbstractRolesAllowedResourceFilterFactory() {
    super();
  }

  // Used for unit tests.
  AbstractRolesAllowedResourceFilterFactory(boolean securityEnabled) {
    this.securityEnabled = securityEnabled;
  }

  @Override
  protected abstract ResourceFilter createResourceFilter(AbstractMethod am);

  /**
   * A method needed to override security.
   */
  @Override
  protected abstract ResourceFilter createNoSecurityResourceFilter();

  @Override
  protected boolean hasRightAnnotations(AbstractMethod am) {
    return hasAnnotation(am, RolesAllowed.class);
  }
}
