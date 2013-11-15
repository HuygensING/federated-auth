package nl.knaw.huygens.security.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import nl.knaw.huygens.security.model.HuygensPrincipal;
import nl.knaw.huygens.security.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/auth")
public class AuthResource {
    private static final Logger log = LoggerFactory.getLogger(AuthResource.class);

    private final SessionManager sessionManager;

    @Inject
    public AuthResource(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GET
    @Path("/session/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateSession(@PathParam("id") String id) {
        final HuygensPrincipal session = sessionManager.getSession(id);

        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(session).build();
    }

}
