package nl.knaw.huygens.security.client.filters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.Test;

public class SecurityResourceFilterFactoryTest {
    @Test public void testNoSecurityResourceFilter() {
        SecurityResourceFilterFactory sut = new SecurityResourceFilterFactory(null, null);
        assertThat(sut.createNoSecurityResourceFilter(), instanceOf(BypassFilter.class));
    }
}
