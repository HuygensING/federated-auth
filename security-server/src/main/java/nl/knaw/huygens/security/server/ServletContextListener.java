package nl.knaw.huygens.security.server;

import static com.sun.jersey.api.container.filter.LoggingFilter.FEATURE_LOGGING_DISABLE_ENTITY;
import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES;
import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static com.sun.jersey.spi.container.servlet.ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX;

import javax.servlet.ServletContextEvent;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import nl.knaw.huygens.security.server.filters.CharsetResponseFilter;
import nl.knaw.huygens.security.server.util.ClassNameIterator;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.xml.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContextListener extends GuiceServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ServletContextListener.class);

    private static final Joiner COMMA_JOINER = Joiner.on(',');

    private static String getClassNames(Class<?>... classes) {
        return COMMA_JOINER.join(new ClassNameIterator(classes));
    }

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
        final Map<String, String> params = Maps.newHashMap();
        params.put(FEATURE_LOGGING_DISABLE_ENTITY, "true");
        params.put(FEATURE_POJO_MAPPING, "true");

        params.put(PROPERTY_PACKAGES, getClass().getPackage().getName());

        params.put(PROPERTY_WEB_PAGE_CONTENT_REGEX, "/static/.*");

        params.put(PROPERTY_CONTAINER_REQUEST_FILTERS, getRequestFilters());
        params.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, getResponseFilters());
        params.put(PROPERTY_RESOURCE_FILTER_FACTORIES, getFilterFactories());

        try {
            log.info("Bootstrapping OpenSAML library");
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("Unable to bootstrap OpenSAML library");
        }

        try {
            log.info("Initializing secure random identifier generator");
            final SecureRandomIdentifierGenerator generator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            log.error("No algorithm for secure random identifier generator: {}", e.getMessage());
        }

        return Guice.createInjector(new JerseyServletModule() {
            @Override
            protected void configureServlets() {
                filter("/*").through(GuiceContainer.class, params);
            }
        });
    }

    private String getRequestFilters() {
        return getClassNames(LoggingFilter.class, BasicAuthFilter.class);
    }

    private String getResponseFilters() {
        return getClassNames(LoggingFilter.class, CharsetResponseFilter.class);
    }

    private String getFilterFactories() {
        return getClassNames(RolesAllowedResourceFilterFactory.class);
    }
}