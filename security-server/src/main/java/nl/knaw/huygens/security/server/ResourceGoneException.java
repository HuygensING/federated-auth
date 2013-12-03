package nl.knaw.huygens.security.server;

import static javax.ws.rs.core.Response.Status.GONE;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceGoneException extends WebApplicationException {
    private static final Logger log = LoggerFactory.getLogger(ResourceGoneException.class);

    public ResourceGoneException(String message) {
        super(Response.status(GONE).entity(message).build());
        log.warn("Resource gone: {}", message);
    }
}
