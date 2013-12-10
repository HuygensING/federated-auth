package nl.knaw.huygens.security.server.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import nl.knaw.huygens.security.BaseTestCase;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class ServerSessionImplTest extends BaseTestCase {
    @Mock
    private HuygensPrincipal principal;

    @Spy
    @InjectMocks
    private ServerSessionImpl sut;

    private static void sleepMilliSecond() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorEnforcesPrincipal() throws Exception {
        new ServerSessionImpl(null);
    }

    @Test
    public void testNewSessionHasValidID() throws Exception {
        Assert.assertNotNull(sut.getId());
    }

    @Test
    public void testSessionHasCorrectOwner() throws Exception {
        assertThat(sut.getOwner(), is(principal));
    }

    @Test
    public void testNewSessionIsCurrent() throws Exception {
        assertThat(sut.isCurrent(), is(true));
    }

    @Test
    public void testNewSessionIsNotDestroyed() throws Exception {
        assertThat(sut.isDestroyed(), is(false));
    }

    @Test
    public void testRefresh() throws Exception {
        DateTime expiryBeforeRefresh = sut.getExpiresAt();
        sleepMilliSecond();
        sut.refresh();
        DateTime expiryAfterRefresh = sut.getExpiresAt();
        assertThat(expiryAfterRefresh, not(is(expiryBeforeRefresh))); // may not be equal()
        assertTrue(expiryAfterRefresh.isAfter(expiryBeforeRefresh)); // must be later
    }

    @Test
    public void testIsCurrent() throws Exception {
        when(sut.getExpiresAt()).thenReturn(DateTime.now().plusMinutes(1));
        Assert.assertThat(sut.isCurrent(), is(true));

        when(sut.getExpiresAt()).thenReturn(DateTime.now().minusMinutes(1));
        assertThat(sut.isCurrent(), is(false));
    }

    @Test
    public void testDestroy() throws Exception {
        assertThat(sut.isDestroyed(), is(false));
        sut.destroy();
        assertThat(sut.isDestroyed(), is(true));
    }
}
