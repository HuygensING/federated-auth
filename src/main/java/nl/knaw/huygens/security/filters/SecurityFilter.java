package nl.knaw.huygens.security.filters;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class SecurityFilter implements ContainerRequestFilter {
    private static final String AUTH_HEADER = "Authorization";

    @Override
    public ContainerRequest filter(final ContainerRequest req) {
        final String auth = req.getHeaderValue(AUTH_HEADER);
//        System.err.println("SecurityFilter: auth=" + auth);
//        System.err.println("security context: " + req.getSecurityContext());
        return req;
    }

}