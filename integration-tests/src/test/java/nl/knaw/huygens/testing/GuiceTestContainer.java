package nl.knaw.huygens.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;

import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;
import com.sun.jersey.test.framework.impl.container.inmemory.TestResourceClientHandler;
import com.sun.jersey.test.framework.spi.container.TestContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuiceTestContainer implements TestContainer {
    private static final Logger log = LoggerFactory.getLogger(GuiceTestContainer.class);

    private final URI baseUri;

    private final Injector injector;

    private final ResourceConfig resourceConfig;

    private final WebApplication webApplication;

    public GuiceTestContainer(URI baseUri, ResourceConfig resourceConfig, Injector injector) {
        log.debug("Creating GuiceTestContainer, baseUri={}", baseUri);
        this.baseUri = checkNotNull(baseUri);
        this.injector = checkNotNull(injector);
        this.resourceConfig = checkNotNull(resourceConfig);
        this.webApplication = WebApplicationFactory.createWebApplication();
    }

    @Override
    public Client getClient() {
        log.debug("Creating client");
        return new Client(new TestResourceClientHandler(baseUri, webApplication));
    }

    @Override
    public URI getBaseUri() {
        return baseUri;
    }

    @Override
    public void start() {
        log.debug("Starting webApplication");
        if (!webApplication.isInitiated()) {
            webApplication.initiate(resourceConfig, new GuiceComponentProviderFactory(resourceConfig, injector));
        }
    }

    @Override
    public void stop() {
        log.debug("Stopping webApplication");
        if (webApplication.isInitiated()) {
            webApplication.destroy();
        }
    }
}
