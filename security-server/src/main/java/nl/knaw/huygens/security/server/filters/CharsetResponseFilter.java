package nl.knaw.huygens.security.server.filters;

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