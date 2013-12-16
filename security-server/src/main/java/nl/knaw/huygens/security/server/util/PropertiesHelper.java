package nl.knaw.huygens.security.server.util;

import java.io.File;
import java.io.FileInputStream;
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
        return getProperties("/build.properties");
    }

    public static Properties getAuthProperties() {
        final Properties auth = getProperties();
        return getProperties(new File(auth.getProperty("roles.file")));
    }

    public static int getIntegerProperty(String property) {
        return Integer.parseInt(getProperties().getProperty(property));
    }

    private static Properties getProperties() {
        return getProperties("/hss.properties");
    }

    private static Properties getProperties(String resourceName) {
        final Properties properties = new Properties();

        try {
            log.debug("Loading properties from classpath: {}", resourceName);
            properties.load(INSTANCE.getClass().getResourceAsStream(resourceName));
        } catch (IOException e) {
            log.warn("IOException reading {}: {}", resourceName, e);
        }

        return properties;
    }

    private static Properties getProperties(File file) {
        final Properties properties = new Properties();

        try {
            log.debug("Loading properties from file: {}", file);
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            log.warn("IOException reading {}: {}", file, e);
        }

        return properties;
    }
}
