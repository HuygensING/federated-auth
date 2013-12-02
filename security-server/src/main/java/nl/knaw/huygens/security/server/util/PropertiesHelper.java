package nl.knaw.huygens.security.server.util;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesHelper {
    private static final Logger log = LoggerFactory.getLogger(PropertiesHelper.class);

    private static final PropertiesHelper INSTANCE = new PropertiesHelper();

    private PropertiesHelper() {
    }

    public static Properties getBuildProperties() {
        return getProperties(buildResourceName("build"));
    }

    public static Properties getAuthProperties() {
        return getProperties(buildResourceName("auth"));
    }

    private static String buildResourceName(String name) {
        return "/" + name + ".properties";
    }

    private static Properties getProperties(String resourceName) {
        final Properties properties = new Properties();

        try {
            log.debug("Loading properties: {}", resourceName);
            properties.load(INSTANCE.getClass().getResourceAsStream(resourceName));
        } catch (IOException e) {
            log.warn("IOException reading {}: {}", resourceName, e.toString());
        }

        return properties;
    }
}
