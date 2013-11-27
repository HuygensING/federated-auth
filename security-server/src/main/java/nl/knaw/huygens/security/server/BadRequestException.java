package nl.knaw.huygens.security.server;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BadRequestException extends WebApplicationException {
    private static final Logger log = LoggerFactory.getLogger(BadRequestException.class);

    public BadRequestException(String message) {
        super(Response.status(BAD_REQUEST).entity(message).build());
        log.warn("Bad request: {}", message);
    }
}
