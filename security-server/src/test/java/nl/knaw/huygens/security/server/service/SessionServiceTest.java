package nl.knaw.huygens.security.server.service;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.RefreshFailedException;
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
    public void testGetSessions() {
        assertThat(sut.getSessions(), hasSize(0));
    }

    @Test
    public void testAddSessionOK() {
        setupTestSession(true, false);

        sut.addSession(testSession);

        verify(testSession).getId();
        assertThat(sut.getSessions(), contains(testSession));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddIllegalSession() {
        sut.addSession(testSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExpiredSession() {
        setupTestSession(false, false);
        sut.addSession(testSession);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDestroyedSession() {
        setupTestSession(true, true);
        sut.addSession(testSession);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddDuplicateSession() {
        setupTestSession(true, false);
        sut.addSession(testSession);
        sut.addSession(testSession);
    }

    @Test
    public void testAddSessionPurgesStaleSessions() {
        //given
        final ServerSession staleSession = mock(ServerSession.class);
        given(staleSession.getId()).willReturn(fromString("22222222-2222-2222-2222-222222222222"));
        given(staleSession.isCurrent()).willReturn(true); // start 'fresh' to allow adding

        //when
        sut.addSession(staleSession);

        //then
        assertThat(sut.getSessions(), contains(staleSession));

        //given
        setupTestSession(true, false);
        given(staleSession.isCurrent()).willReturn(false); // now make it 'stale' to force purging
        given(sut.isTimeToPurge()).willReturn(true);

        //when
        sut.addSession(testSession);

        //then
        assertThat(sut.getSessions(), not(contains(staleSession)));
        assertThat(sut.getSessions(), contains(testSession));
    }

    @Test
    public void testFindSessionOK() {
        //given
        setupTestSession(true, false);

        //when
        sut.addSession(testSession);

        //then
        assertThat(sut.findSession(testSessionID), is(testSession));
    }

    @Test(expected = NotFoundException.class)
    public void testFindNonExistentSession() {
        sut.findSession(testSessionID);
    }

    @Test(expected = ResourceGoneException.class)
    public void testFindExpiredSession() {
        setupTestSession(true, false);
        sut.addSession(testSession);
        given(testSession.isCurrent()).willReturn(false);
        sut.findSession(testSessionID);
    }

    @Test(expected = ResourceGoneException.class)
    public void testFindDestroyedSession() {
        //given
        setupTestSession(true, false);
        sut.addSession(testSession);
        given(testSession.isDestroyed()).willReturn(true);
        sut.findSession(testSessionID);
    }

    @Test
    public void testRefreshSession() throws RefreshFailedException {
        //given
        setupTestSession(true, false);
        sut.addSession(testSession);

        //when
        sut.refreshSession(testSessionID);

        //then
        verify(testSession).refresh();
    }

    @Test
    public void testDestroySession() throws DestroyFailedException {
        //given
        setupTestSession(true, false);
        sut.addSession(testSession);

        //when
        sut.destroySession(testSessionID);

        //then
        verify(testSession).destroy();
    }

    @Test
    public void testPurge() {
        //given
        final ServerSession expiredSession = createExpiredSession();
        final ServerSession destroyedSession = createDestroyedSession();
        final ServerSession destroyedAndExpiredSession = createDestroyedAndExpiredSession();
        setupTestSession(true, false);
        sut.addSession(testSession);

        //when
        final Collection<ServerSession> purgedSessions = sut.purge();

        //then
        assertThat(purgedSessions, not(contains(testSession)));
        assertThat(purgedSessions, containsInAnyOrder(expiredSession, destroyedSession, destroyedAndExpiredSession));
        final Collection<ServerSession> remainingSessionsAfterPurge = sut.getSessions();
        assertThat(remainingSessionsAfterPurge, hasSize(1));
        assertThat(remainingSessionsAfterPurge, contains(testSession));
    }

    ServerSession createExpiredSession() {
        final ServerSession session = mock(ServerSession.class);
        when(session.getId()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(session.isCurrent()).thenReturn(true);
        sut.addSession(session);
        when(session.isCurrent()).thenReturn(false);
        when(session.isDestroyed()).thenReturn(false);
        return session;
    }

    ServerSession createDestroyedSession() {
        final ServerSession session = mock(ServerSession.class);
        when(session.getId()).thenReturn(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(session.isCurrent()).thenReturn(true);
        sut.addSession(session);
        when(session.isDestroyed()).thenReturn(true);
        return session;
    }

    ServerSession createDestroyedAndExpiredSession() {
        final ServerSession session = mock(ServerSession.class);
        when(session.getId()).thenReturn(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        when(session.isCurrent()).thenReturn(true);
        sut.addSession(session);
        when(session.isCurrent()).thenReturn(false);
        when(session.isDestroyed()).thenReturn(true);
        return session;
    }

    private void setupTestSession(boolean current, boolean destroyed) {
        when(testSession.getId()).thenReturn(testSessionID);
        when(testSession.isCurrent()).thenReturn(current);
        when(testSession.isDestroyed()).thenReturn(destroyed);
    }
}
