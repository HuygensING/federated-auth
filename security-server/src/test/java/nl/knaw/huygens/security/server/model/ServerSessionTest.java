package nl.knaw.huygens.security.server.model;

/*
 * #%L
 * Security Server
 * =======
 * Copyright (C) 2013 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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

public class ServerSessionTest extends BaseTestCase {
    @Mock
    private HuygensPrincipal principal;

    @Spy
    @InjectMocks
    private ServerSession sut;

    private static void sleepMilliSecond() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorEnforcesPrincipal() throws Exception {
        new ServerSession(null);
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
