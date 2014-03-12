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
