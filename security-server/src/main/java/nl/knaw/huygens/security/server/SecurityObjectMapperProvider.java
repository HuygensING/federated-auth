package nl.knaw.huygens.security.server;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class SecurityObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private static final Logger log = LoggerFactory.getLogger(SecurityObjectMapperProvider.class);

    @Override
    public ObjectMapper getContext(Class<?> type) {
        ObjectMapper objectMapper = new ObjectMapper();
        log.debug("Setting up Jackson ObjectMapper: [{}]", objectMapper);

        // These are 'dev' settings giving us human readable output.
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // JodaModule maps DateTime to a flat String (or timestamp, see above) instead of recursively yielding
        // the entire object hierarchy of DateTime which is way too verbose.
        objectMapper.registerModule(new JodaModule());

        return objectMapper;
    }
}