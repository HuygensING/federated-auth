package nl.knaw.huygens.testing;

import com.google.inject.Injector;
import com.sun.jersey.test.framework.JerseyTest;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class GuiceJerseyTestBase extends JerseyTest {
    private static final Logger log = LoggerFactory.getLogger(GuiceJerseyTestBase.class);

    private final Injector injector;

    public GuiceJerseyTestBase(Injector injector) {
        super(new GuiceTestContainerFactory(injector));
        this.injector = injector;
        log.debug("GuiceJerseyTestBase created");
    }

    protected Injector getInjector() {
        return injector;
    }

    @BeforeClass
    public static void setUpLogger() {
        log.debug("Installing jul-to-slf4j diversion");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
