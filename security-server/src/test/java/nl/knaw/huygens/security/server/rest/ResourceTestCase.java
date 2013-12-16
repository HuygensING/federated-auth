package nl.knaw.huygens.security.server.rest;

import nl.knaw.huygens.security.BaseTestCase;
import org.junit.Before;

public abstract class ResourceTestCase extends BaseTestCase {
    protected RESTHelper restHelper;

    @Before
    public void initRESTHelper() {
        restHelper = new RESTHelper(getSUT());
    }

    public abstract Object getSUT();
}
