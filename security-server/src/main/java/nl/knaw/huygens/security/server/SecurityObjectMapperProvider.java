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
        log.debug("Configuring ObjectMapper (adding JodaTime support)");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
}