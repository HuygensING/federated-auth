package nl.knaw.huygens.security.server;

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
