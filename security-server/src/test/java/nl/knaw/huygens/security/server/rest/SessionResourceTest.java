package nl.knaw.huygens.security.server.rest;

import static java.util.UUID.fromString;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.huygens.security.server.Roles.SESSION_JANITOR;
import static nl.knaw.huygens.security.server.Roles.SESSION_MANAGER;
import static nl.knaw.huygens.security.server.Roles.SESSION_VIEWER;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.server.ResourceGoneException;
import nl.knaw.huygens.security.server.model.ServerSession;
import nl.knaw.huygens.security.server.service.SessionService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SessionResourceTest extends ResourceTestCase {
    private final UUID testSessionID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final Collection<ServerSession> testSessionList = Collections.emptyList();

    @InjectMocks
    private SessionResource sut;

    @Mock
    private SessionService sessionService;

    @Mock
    private ServerSession testSession;

    @Override
    public Object getSUT() {
        return sut;
    }

    @Test
    public void testListSessions() throws Exception {
        when(sessionService.getSessions()).thenReturn(testSessionList);
        final Object expectedEntity = testSessionList;

        final Response response = sut.listSessions();

        verify(sessionService).getSessions();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is(expectedEntity));
    }

    @Test
    public void testGetSessionSuccess() throws Exception {
        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        final Object expectedEntity = testSession;
        when(testSession.isCurrent()).thenReturn(true);
        when(testSession.isDestroyed()).thenReturn(false);

        final Response response = sut.readSession(testSessionID.toString());

        verify(sessionService).findSession(fromString(testSessionID.toString()));
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is(expectedEntity));
    }

    @Test(expected = NotFoundException.class)
    public void testGetSessionNotFound() throws Exception {
        sut.readSession(testSessionID.toString());
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionExpired() throws Exception {
        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        when(testSession.isCurrent()).thenReturn(false);
        when(testSession.isDestroyed()).thenReturn(false);

        sut.readSession(testSessionID.toString());
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionPurged() throws Exception {
        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        when(testSession.isCurrent()).thenReturn(true);
        when(testSession.isDestroyed()).thenReturn(true);

        sut.readSession(testSessionID.toString());
    }

    @Test
    public void testSessionRefresh() throws Exception {
        when(testSession.isCurrent()).thenReturn(true);
        final Object expectedEntity = testSession;

        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        when(sessionService.refreshSession(testSessionID)).thenReturn(testSession);

        final Response response = sut.refreshSession(testSessionID.toString());

        verify(sessionService).refreshSession(testSessionID);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is(expectedEntity));
    }

    @Test
    public void testSessionDestroy() throws Exception {
        when(testSession.isCurrent()).thenReturn(true);
        final Object expectedEntity = testSession;

        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        when(sessionService.destroySession(testSessionID)).thenReturn(testSession);

        final Response response = sut.expireSession(testSessionID.toString());

        verify(sessionService).destroySession(testSessionID);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is(expectedEntity));
    }

    @Test
    public void testSessionPurge() throws Exception {
        when(testSession.isCurrent()).thenReturn(false);
        when(sessionService.findSession(testSessionID)).thenReturn(testSession);
        final Object expectedEntity = testSessionList;
        when(sessionService.purge()).thenReturn(testSessionList);

        final Response response = sut.purge();

        verify(sessionService).purge();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is(expectedEntity));
    }

    @Test
    public void testRESTGetSession() throws Exception {
        restHelper.findMethod("/sessions/<id>", GET.class);

        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(2));
        assertThat(rolesAllowed, containsInAnyOrder(SESSION_VIEWER, SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTDeleteSession() throws Exception {
        restHelper.findMethod("/sessions/<id>", DELETE.class);

        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTRefreshSession() throws Exception {
        restHelper.findMethod("/sessions/<id>/refresh", POST.class);

        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTPurge() throws Exception {
        restHelper.findMethod("/sessions/purge", POST.class);

        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_JANITOR));

        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }
}
