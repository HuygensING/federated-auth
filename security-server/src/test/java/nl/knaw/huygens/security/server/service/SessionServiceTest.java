package nl.knaw.huygens.security.server.service;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.UUID;

import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.BaseTestCase;
import nl.knaw.huygens.security.server.ResourceGoneException;
import nl.knaw.huygens.security.server.model.ServerSession;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class SessionServiceTest extends BaseTestCase {
    private final UUID testSessionID = fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ServerSession testSession;

    @Spy
    @InjectMocks
    private SessionService sut;

    @Test
    public void testGetSessions() throws Exception {
        assertThat(sut.getSessions(), hasSize(0));
    }

    @Test
    public void testAddSessionOK() throws Exception {
        setupTestSession(true, false);

        sut.addSession(testSession);

        verify(testSession).getId();
        assertThat(sut.getSessions(), contains(testSession));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddIllegalSession() throws Exception {
        sut.addSession(testSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExpiredSession() throws Exception {
        setupTestSession(false, false);
        sut.addSession(testSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDestroyedSession() throws Exception {
        setupTestSession(true, true);
        sut.addSession(testSession);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddDuplicateSession() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        sut.addSession(testSession);
    }

    @Test
    public void testAddSessionPurgesStaleSessions() throws Exception {
        final ServerSession staleSession = mock(ServerSession.class);
        when(staleSession.getId()).thenReturn(fromString("22222222-2222-2222-2222-222222222222"));
        when(staleSession.isCurrent()).thenReturn(true); // start 'fresh' to allow adding

        sut.addSession(staleSession);
        assertThat(sut.getSessions(), contains(staleSession));

        setupTestSession(true, false);
        when(staleSession.isCurrent()).thenReturn(false); // now make it 'stale' to force purging
        when(sut.isTimeToPurge()).thenReturn(true);

        sut.addSession(testSession);
        assertThat(sut.getSessions(), not(contains(staleSession)));
        assertThat(sut.getSessions(), contains(testSession));
    }

    @Test
    public void testFindSessionOK() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        assertThat(sut.findSession(testSessionID), is(testSession));
    }

    @Test(expected = NotFoundException.class)
    public void testFindNonExistentSession() throws Exception {
        sut.findSession(testSessionID);
    }

    @Test(expected = ResourceGoneException.class)
    public void testFindExpiredSession() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        when(testSession.isCurrent()).thenReturn(false);
        sut.findSession(testSessionID);
    }

    @Test(expected = ResourceGoneException.class)
    public void testFindDestroyedSession() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        when(testSession.isDestroyed()).thenReturn(true);
        sut.findSession(testSessionID);
    }

    @Test
    public void testRefreshSession() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        sut.refreshSession(testSessionID);
        verify(testSession).refresh();
    }

    @Test
    public void testDestroySession() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);
        sut.destroySession(testSessionID);
        verify(testSession).destroy();
    }

    @Test
    public void testPurge() throws Exception {
        setupTestSession(true, false);
        sut.addSession(testSession);

        final ServerSession expiredSession = mock(ServerSession.class);
        when(expiredSession.getId()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(expiredSession.isCurrent()).thenReturn(true);
        sut.addSession(expiredSession);
        when(expiredSession.isCurrent()).thenReturn(false);
        when(expiredSession.isDestroyed()).thenReturn(false);

        final ServerSession destroyedSession = mock(ServerSession.class);
        when(destroyedSession.getId()).thenReturn(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(destroyedSession.isCurrent()).thenReturn(true);
        sut.addSession(destroyedSession);
        when(destroyedSession.isDestroyed()).thenReturn(true);

        final ServerSession destroyedAndExpiredSession = mock(ServerSession.class);
        when(destroyedAndExpiredSession.getId()).thenReturn(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        when(destroyedAndExpiredSession.isCurrent()).thenReturn(true);
        sut.addSession(destroyedAndExpiredSession);
        when(destroyedAndExpiredSession.isCurrent()).thenReturn(false);
        when(destroyedAndExpiredSession.isDestroyed()).thenReturn(true);

        final Collection<ServerSession> purgedSessions = sut.purge();
        assertThat(purgedSessions, not(contains(testSession)));
        assertThat(purgedSessions, containsInAnyOrder(expiredSession, destroyedSession, destroyedAndExpiredSession));

        final Collection<ServerSession> remainingSessionsAfterPurge = sut.getSessions();
        assertThat(remainingSessionsAfterPurge, hasSize(1));
        assertThat(remainingSessionsAfterPurge, contains(testSession));
    }

    private void setupTestSession(boolean current, boolean destroyed) {
        when(testSession.getId()).thenReturn(testSessionID);
        when(testSession.isCurrent()).thenReturn(current);
        when(testSession.isDestroyed()).thenReturn(destroyed);
    }
}
