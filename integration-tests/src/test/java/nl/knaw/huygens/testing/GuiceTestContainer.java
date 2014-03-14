package nl.knaw.huygens.testing;

/*
 * #%L
 * Security Integration-tests
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
