package nl.knaw.huygens.security.server.rest;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class BaseTestCase {
    protected RESTHelper restHelper;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        restHelper = new RESTHelper(getSUT());
    }

    public abstract Object getSUT();
}
