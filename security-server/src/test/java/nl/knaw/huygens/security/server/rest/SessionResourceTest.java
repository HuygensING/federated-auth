package nl.knaw.huygens.security.server.rest;

import static java.util.UUID.fromString;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.server.ResourceGoneException;
import nl.knaw.huygens.security.server.model.ServerSession;
import nl.knaw.huygens.security.server.service.SessionService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SessionResourceTest extends BaseTestCase {
    public static final String ARBITRARY_VALID_UUID = "11111111-1111-1111-1111-111111111111";

    private final String testSessionId = ARBITRARY_VALID_UUID;

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
    public void testGetSessionSuccess() throws Exception {
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        when(testSession.isCurrent()).thenReturn(true);
        when(testSession.isDestroyed()).thenReturn(false);

        final Response response = sut.getSession(testSessionId);
        verify(sessionService).getSession(fromString(testSessionId));
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is((Object) testSession));

    }

    @Test(expected = NotFoundException.class)
    public void testGetSessionNotFound() throws Exception {
        sut.getSession(testSessionId);
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionExpired() throws Exception {
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        when(testSession.isCurrent()).thenReturn(false);
        when(testSession.isDestroyed()).thenReturn(false);

        sut.getSession(testSessionId);
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionPurged() throws Exception {
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        when(testSession.isCurrent()).thenReturn(true);
        when(testSession.isDestroyed()).thenReturn(true);

        sut.getSession(testSessionId);
    }

    @Test
    public void testSessionRefresh() throws Exception {
        when(testSession.isCurrent()).thenReturn(true);
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        when(sessionService.refreshSession(testSession)).thenReturn(testSession);

        final Response response = sut.refreshSession(testSessionId);
        verify(sessionService).refreshSession(testSession);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is((Object) testSession));
    }

    @Test
    public void testSessionDestroy() throws Exception {
        when(testSession.isCurrent()).thenReturn(true);
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        when(sessionService.destroySession(testSession)).thenReturn(testSession);

        final Response response = sut.destroySession(testSessionId);
        verify(sessionService).destroySession(testSession);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is((Object) testSession));
    }

    @Test
    public void testSessionPurge() throws Exception {
        when(testSession.isCurrent()).thenReturn(false);
        when(sessionService.getSession(fromString(testSessionId))).thenReturn(testSession);
        final Collection<ServerSession> purged = Collections.singleton(testSession);
        when(sessionService.purge()).thenReturn(purged);

        final Response response = sut.purge();
        verify(sessionService).purge();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getEntity(), is((Object) purged));
    }

    @Test
    public void testRESTGetSession() throws Exception {
        final Method m = restHelper.findMethod("/sessions/<id>", GET.class);
        final List<String> rolesAllowed = restHelper.getRolesAllowed(m);

        assertThat(rolesAllowed, hasSize(2));
        assertThat(rolesAllowed, containsInAnyOrder(SESSION_VIEWER, SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(m), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTDeleteSession() throws Exception {
        final Method m = restHelper.findMethod("/sessions/<id>", DELETE.class);
        final List<String> rolesAllowed = restHelper.getRolesAllowed(m);

        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(m), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTRefreshSession() throws Exception {
        final Method m = restHelper.findMethod("/sessions/<id>/refresh", POST.class);
        final List<String> rolesAllowed = restHelper.getRolesAllowed(m);

        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));

        assertThat(restHelper.getProducedMediaTypes(m), contains(APPLICATION_JSON));
    }
}
