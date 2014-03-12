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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public abstract class AbstractSecurityAnnotationResourceFilterFactory implements ResourceFilterFactory {

  @Inject
  @Named("security.enabled")
  protected boolean securityEnabled;

  public AbstractSecurityAnnotationResourceFilterFactory() {
    super();
  }

  protected abstract ResourceFilter createResourceFilter(AbstractMethod am);

  /**
   * A method needed to override security.
   */
  protected abstract ResourceFilter createNoSecurityResourceFilter();

  protected boolean hasAnnotation(AbstractMethod am, Class<? extends Annotation> annotation) {
    return am.getAnnotation(annotation) != null || am.getResource().getAnnotation(annotation) != null;
  }

  protected abstract boolean hasRightAnnotations(AbstractMethod am);

  @Override
  public final List<ResourceFilter> create(AbstractMethod am) {
    if (!hasRightAnnotations(am)) {
      return null;
    }

    if (!securityEnabled) {
      return Collections.singletonList(createNoSecurityResourceFilter());
    }

    return Collections.singletonList(createResourceFilter(am));
  }

}
