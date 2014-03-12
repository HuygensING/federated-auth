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

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private static final Logger log = LoggerFactory.getLogger(ObjectMapperProvider.class);

    @Override
    public ObjectMapper getContext(Class<?> type) {
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("Setting up Jackson ObjectMapper: [{}]", objectMapper);

        // These are 'dev' settings giving us human readable output.
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // JodaModule maps DateTime to a flat String (or timestamp, see above) instead of recursively yielding
        // the entire object hierarchy of DateTime which is way too verbose.
        objectMapper.registerModule(new JodaModule());

        return objectMapper;
    }
}