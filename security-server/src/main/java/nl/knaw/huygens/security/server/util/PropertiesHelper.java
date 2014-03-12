package nl.knaw.huygens.security.server.util;

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
