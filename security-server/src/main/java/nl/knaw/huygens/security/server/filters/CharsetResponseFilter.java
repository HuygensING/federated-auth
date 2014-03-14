package nl.knaw.huygens.security.server.filters;

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

import static com.google.common.base.Charsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharsetResponseFilter implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(CharsetResponseFilter.class);

    private static final String CHARSET_KEY = "charset";

    private static final String CHARSET_UTF8_PARAM = ";" + CHARSET_KEY + "=" + UTF_8;

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        MediaType mediaType = response.getMediaType();

        if (mediaType == null) {
            log.warn("Unspecified MediaType (missing @Produces?), defaulting to: text/plain");
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }

        final String type = mediaType.toString();
        if (!type.contains(CHARSET_KEY)) {
            response.getHttpHeaders().putSingle(CONTENT_TYPE, type + CHARSET_UTF8_PARAM);
        }

        return response;
    }
}
