package nl.knaw.huygens.security.server;

import static com.sun.jersey.api.container.filter.LoggingFilter.FEATURE_LOGGING_DISABLE_ENTITY;
import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES;
import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static com.sun.jersey.spi.container.servlet.ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import nl.knaw.huygens.security.server.filters.CharsetResponseFilter;
import nl.knaw.huygens.security.server.util.ClassNameIterator;

public class ServletConfigurationModule extends ServletModule {
    private static final Joiner COMMA_JOINER = Joiner.on(',');

    private static String getClassNames(Class<?>... classes) {
        return COMMA_JOINER.join(new ClassNameIterator(classes));
    }

    @Override
    protected void configureServlets() {
        final Map<String, String> params = Maps.newHashMap();
        params.put(FEATURE_LOGGING_DISABLE_ENTITY, "true");
        params.put(FEATURE_POJO_MAPPING, "true");
        params.put(PROPERTY_PACKAGES, getClass().getPackage().getName());
        params.put(PROPERTY_WEB_PAGE_CONTENT_REGEX, "/static/.*");
        params.put(PROPERTY_CONTAINER_REQUEST_FILTERS, getRequestFilters());
        params.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, getResponseFilters());
        params.put(PROPERTY_RESOURCE_FILTER_FACTORIES, getFilterFactories());

        filter("/*").through(GuiceContainer.class, params);
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
