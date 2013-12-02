package nl.knaw.huygens.security.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.huygens.security.core.rest.API.ID_PARAM;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_PATH;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_URI;
import static nl.knaw.huygens.security.server.Roles.SESSION_MANAGER;
import static nl.knaw.huygens.security.server.Roles.SESSION_VIEWER;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.UUID;

import com.google.inject.Inject;
import nl.knaw.huygens.security.core.model.HuygensSession;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.model.ServerSession;
import nl.knaw.huygens.security.server.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(SESSION_AUTHENTICATION_URI)
public class SessionResource {
    private static final Logger log = LoggerFactory.getLogger(SessionResource.class);

    private final SessionService sessionService;

    @Inject
    public SessionResource(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    private static UUID parseSessionID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Session id '" + ID_PARAM + "' is not a valid UUID: " + input);
        }
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_VIEWER)
    public Collection<ServerSession> getSessions() {
        log.debug("LIST sessions");
        return sessionService.getSessions();
    }

    @GET
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_VIEWER)
    public HuygensSession getSession(@PathParam(ID_PARAM) String id) {
        log.debug("READ session: [{}]", id);
        return sessionService.getSession(parseSessionID(id));
    }

    @POST
    @Path(SESSION_AUTHENTICATION_PATH + "/refresh")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public HuygensSession refreshSession(@PathParam(ID_PARAM) String id) {
        log.debug("REFRESH session: [{}]", id);
        final UUID sessionId = parseSessionID(id);
        return sessionService.refreshSession(sessionId);
    }

    @DELETE
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public HuygensSession destroySession(@PathParam(ID_PARAM) String id) {
        log.debug("DESTROY session: [{}]", id);
        return sessionService.destroySession(parseSessionID(id));
    }

    @POST
    @Path("/purge")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public Collection<ServerSession> purge() {
        log.debug("Purging sessions");
        return sessionService.purge();
    }
}
