package nl.knaw.huygens.security.server;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalAccessFilter implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(LocalAccessFilter.class);

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        log.debug("securityContext: ", request.getSecurityContext());
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
