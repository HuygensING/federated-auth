package nl.knaw.huygens.security.server;

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

import javax.servlet.ServletContextEvent;
import java.security.NoSuchAlgorithmException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContextListener extends GuiceServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ServletContextListener.class);

    private Injector injector;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.debug("contextInitialized");
        super.contextInitialized(servletContextEvent);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        log.debug("contextDestroyed");
        super.contextDestroyed(servletContextEvent);
    }

    @Override
    protected Injector getInjector() {
        injector = Guice.createInjector(new ServletConfigurationModule());

        try {
            log.info("Initializing secure random identifier generator");
            final SecureRandomIdentifierGenerator generator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            log.error("No algorithm for secure random identifier generator: {}", e.getMessage());
        }

        return injector;
    }

}
