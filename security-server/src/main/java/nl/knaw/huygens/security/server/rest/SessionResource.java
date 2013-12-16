package nl.knaw.huygens.security.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.huygens.security.core.rest.API.ID_PARAM;
import static nl.knaw.huygens.security.core.rest.API.REFRESH_PATH;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_PATH;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_URI;
import static nl.knaw.huygens.security.server.Roles.SESSION_JANITOR;
import static nl.knaw.huygens.security.server.Roles.SESSION_MANAGER;
import static nl.knaw.huygens.security.server.Roles.SESSION_VIEWER;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.UUID;

import com.google.inject.Inject;
import com.sun.jersey.api.NotFoundException;

import nl.knaw.huygens.security.core.rest.API;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.ResourceGoneException;
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

    private static Response ok(Object entity) {
        return Response.ok(entity).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public Response listSessions() {
        log.debug("LIST sessions");
        return ok(sessionService.getSessions());
    }

    @GET
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({SESSION_MANAGER, SESSION_VIEWER})
    public Response readSession(@PathParam(ID_PARAM) String input) {
        log.debug("READ session: [{}]", input);
        final UUID sessionId = sanitizeUUID(input);

        final ServerSession session = sessionService.findSession(sessionId);

        if (session == null) {
            throw new NotFoundException("Unknown session: " + sessionId);
        }

        if (session.isDestroyed()) {
            throw new ResourceGoneException("Destroyed session: " + sessionId);
        }

        if (!session.isCurrent()) {
            throw new ResourceGoneException("Expired session: " + sessionId);
        }

        return ok(session);
    }

    @PUT
    @Path(SESSION_AUTHENTICATION_PATH + REFRESH_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public Response refreshSession(@PathParam(ID_PARAM) String input) {
        log.debug("REFRESH session: [{}]", input);
        return ok(sessionService.refreshSession(sanitizeUUID(input)));
    }

    @DELETE
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_MANAGER)
    public Response expireSession(@PathParam(ID_PARAM) String input) {
        log.debug("EXPIRE session: [{}]", input);
        return ok(sessionService.destroySession(sanitizeUUID(input)));
    }

    @POST
    @Path(API.PURGE_PATH)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(SESSION_JANITOR)
    public Response purge() {
        log.debug("PURGE stale sessions");
        return ok(sessionService.purge());
    }

    private UUID sanitizeUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid '" + ID_PARAM + "' session parameter: " + input);
        }
    }

}
