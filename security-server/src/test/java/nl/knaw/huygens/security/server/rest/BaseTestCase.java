package nl.knaw.huygens.security.server.rest;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class BaseTestCase {
    @Before
    public void initAnnotationBasedMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
    }
}
