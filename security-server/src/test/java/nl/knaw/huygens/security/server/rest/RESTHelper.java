package nl.knaw.huygens.security.server.rest;

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

import static com.google.common.collect.ImmutableList.copyOf;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class RESTHelper {
    private final Class<?> sutClass;
    private Method currentMethod;

    public RESTHelper(Object sut) {
        sutClass = sut.getClass();
    }

    public Method findMethod(final String path, Class<? extends Annotation> annotation) throws NoSuchMethodException {
        final String pathGlob = path.replaceAll("<.*>", "\\\\{.*\\\\}");
        final Path classRestAnnotation = sutClass.getAnnotation(Path.class);
        final String classRestPrefix = classRestAnnotation == null ? "" : classRestAnnotation.value();
        for (Method m : sutClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                String restPath = classRestPrefix;
                if (m.isAnnotationPresent(Path.class)) {
                    restPath += m.getAnnotation(Path.class).value();
                }
                if (restPath.matches(pathGlob)) {
                    currentMethod = m;
                    return m;
                }
            }
        }

        throw new NoSuchMethodException("No @" + annotation.getSimpleName() + " method found for REST path: " + path);
    }

    public List<String> getConsumedMediaTypes(Method m) {
        return copyOf(m.getAnnotation(Consumes.class).value());
    }

    public List<String> getConsumedMediaTypes() {
        return getConsumedMediaTypes(currentMethod);
    }

    public List<String> getProducedMediaTypes(Method m) {
        return copyOf(m.getAnnotation(Produces.class).value());
    }

    public List<String> getProducedMediaTypes() {
        return getProducedMediaTypes(currentMethod);
    }

    public List<String> getRolesAllowed(Method m) {
        return copyOf(m.getAnnotation(RolesAllowed.class).value());
    }

    public List<String> getRolesAllowed() {
        return getRolesAllowed(currentMethod);
    }
}
