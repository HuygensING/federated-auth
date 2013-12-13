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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

    @Mock
    private SessionService sessionService;

    @Mock
    private ServerSession testSession;

    @InjectMocks
    private SessionResource sut;

    @Override
    public Object getSUT() {
        return sut;
    }

    @Test
    public void testListSessions() throws Exception {
        given(sessionService.getSessions()).willReturn(testSessionList);

        //when
        final Response response = sut.listSessions();
        @SuppressWarnings("unchecked")
        final Collection<ServerSession> sessions = (Collection<ServerSession>) response.getEntity();

        //then
        verify(sessionService).getSessions();
        assertThat(response.getStatus(), is(200));
        assertThat(sessions, is(testSessionList));
    }

    @Test
    public void testGetSessionSuccess() throws Exception {
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(testSession.isCurrent()).willReturn(true);
        given(testSession.isDestroyed()).willReturn(false);

        //when
        final Response response = sut.readSession(testSessionID.toString());

        //then
        verify(sessionService).findSession(fromString(testSessionID.toString()));
        assertThat(response.getStatus(), is(200));
        assertThat((ServerSession) response.getEntity(), is(testSession));
    }

    @Test(expected = NotFoundException.class)
    public void testGetSessionNotFound() throws Exception {
        //when
        sut.readSession(testSessionID.toString());
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionExpired() throws Exception {
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(testSession.isCurrent()).willReturn(false);
        given(testSession.isDestroyed()).willReturn(false);

        //when
        sut.readSession(testSessionID.toString());
    }

    @Test(expected = ResourceGoneException.class)
    public void testGetSessionPurged() throws Exception {
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(testSession.isCurrent()).willReturn(true);
        given(testSession.isDestroyed()).willReturn(true);

        //when
        sut.readSession(testSessionID.toString());
    }

    @Test
    public void testSessionRefresh() throws Exception {
        given(testSession.isCurrent()).willReturn(true);
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(sessionService.refreshSession(testSessionID)).willReturn(testSession);

        //when
        final Response response = sut.refreshSession(testSessionID.toString());

        //then
        verify(sessionService).refreshSession(testSessionID);
        assertThat(response.getStatus(), is(200));
        assertThat((ServerSession) response.getEntity(), is(testSession));
    }

    @Test
    public void testSessionDestroy() throws Exception {
        given(testSession.isCurrent()).willReturn(true);
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(sessionService.destroySession(testSessionID)).willReturn(testSession);

        //when
        final Response response = sut.expireSession(testSessionID.toString());

        //then
        verify(sessionService).destroySession(testSessionID);
        assertThat(response.getStatus(), is(200));
        assertThat((ServerSession) response.getEntity(), is(testSession));
    }

    @Test
    public void testSessionPurge() throws Exception {
        given(testSession.isCurrent()).willReturn(false);
        given(sessionService.findSession(testSessionID)).willReturn(testSession);
        given(sessionService.purge()).willReturn(testSessionList);

        //when
        final Response response = sut.purge();
        @SuppressWarnings("unchecked")
        final Collection<ServerSession> sessions = (Collection<ServerSession>) response.getEntity();

        //then
        verify(sessionService).purge();
        assertThat(response.getStatus(), is(200));
        assertThat(sessions, is(testSessionList));
    }

    @Test
    public void testRESTGetSession() throws Exception {
        //given
        restHelper.findMethod("/sessions/<id>", GET.class);

        //then
        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(2));
        assertThat(rolesAllowed, containsInAnyOrder(SESSION_VIEWER, SESSION_MANAGER));
        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTDeleteSession() throws Exception {
        //given
        restHelper.findMethod("/sessions/<id>", DELETE.class);

        //then
        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));
        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTRefreshSession() throws Exception {
        restHelper.findMethod("/sessions/<id>/refresh", PUT.class);
        //given
        //then
        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_MANAGER));
        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTPurge() throws Exception {
        //given
        restHelper.findMethod("/sessions/purge", POST.class);

        //then
        final List<String> rolesAllowed = restHelper.getRolesAllowed();
        assertThat(rolesAllowed, hasSize(1));
        assertThat(rolesAllowed, contains(SESSION_JANITOR));
        assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
    }
}
