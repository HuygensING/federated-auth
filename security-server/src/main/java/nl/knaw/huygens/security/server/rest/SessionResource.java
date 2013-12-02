package nl.knaw.huygens.security.server.rest;

import static nl.knaw.huygens.security.core.rest.API.ID_PARAM;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_PATH;
import static nl.knaw.huygens.security.core.rest.API.SESSION_AUTHENTICATION_URI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

import com.google.inject.Inject;
import nl.knaw.huygens.security.core.model.HuygensSession;
import nl.knaw.huygens.security.server.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed("SESSION_MANAGER")
@Path(SESSION_AUTHENTICATION_URI)
public class SessionResource {
    private static final Logger log = LoggerFactory.getLogger(SessionResource.class);

    private final SessionManager sessionManager;

    @Inject
    public SessionResource(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GET
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateSession(@PathParam(ID_PARAM) String id) {
        log.debug("Request for authentication, sessionId: [{}]", id);

        final UUID sessionId;
        try {
            sessionId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Illegal sessionId (not a UUID): [{}]", id);
            return Response.status(Status.BAD_REQUEST).build();
        }

        final HuygensSession session = sessionManager.getSession(sessionId);
        log.debug("Session for id: [{}]: [{}]", sessionId, session);

        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(session).build();
    }

    @DELETE
    @Path(SESSION_AUTHENTICATION_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSession(@PathParam(ID_PARAM) String id) {
        log.debug("Request to delete session: [{}]", id);

        final UUID sessionId;
        try {
            sessionId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Illegal sessionId (not a UUID): [{}]", id);
            return Response.status(Status.BAD_REQUEST).build();
        }

        HuygensSession removedSession = sessionManager.removeSession(sessionId);
        if (removedSession == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(removedSession).build();
    }
}
