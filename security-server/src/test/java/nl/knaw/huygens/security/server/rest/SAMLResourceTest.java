package nl.knaw.huygens.security.server.rest;

import static nl.knaw.huygens.security.server.Roles.LOGIN_MANAGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.model.LoginRequest;
import nl.knaw.huygens.security.server.saml2.SAMLEncoder;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;
import org.junit.Test;
import org.opensaml.common.SAMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAMLResourceTest {
    private static final Logger log = LoggerFactory.getLogger(SAMLResourceTest.class);

    private final URI testURI;

    private final UUID testRelayState;

    private final String testSAMLRequest;

    private final LoginRequest testLoginRequest;

    private SAMLResource sut;

    public SAMLResourceTest() throws Exception {
        testURI = new URI("www.example.com");
        testSAMLRequest = "%3Csaml%20request%3E";
        testRelayState = UUID.fromString("12345678-abcd-1234-abcd-1234567890ab");
        testLoginRequest = mock(LoginRequest.class);

        LoginService loginService = mock(LoginService.class);
        when(loginService.createLoginRequest(testURI)).thenReturn(testRelayState);
        when(loginService.removeLoginRequest(testRelayState)).thenReturn(testLoginRequest);

        SAMLEncoder samlEncoder = mock(SAMLEncoder.class);
        when(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).thenReturn(testSAMLRequest);

        SessionService sessionService = mock(SessionService.class);

        sut = new SAMLResource(sessionService, loginService, samlEncoder);
    }

    @Test
    public void testLoginMustRedirect() throws Exception {
        final Response response = sut.requestLogin(testURI);
        assertEquals(Status.SEE_OTHER.getStatusCode(), response.getStatus());
    }

    @Test
    public void testLoginRedirectLocationSAML2Compliance() throws Exception {
        final Response response = sut.requestLogin(testURI);

        final URI locationURI = (URI) response.getMetadata().getFirst("Location");
        assertEquals("https", locationURI.getScheme()); // transport must be secure

        final String location = locationURI.toString();
        assertTrue(location.startsWith(SAMLResource.SURF_IDP_SSO_URL));
        assertTrue(location.contains("RelayState=" + testRelayState));
        assertTrue(location.contains("SAMLRequest=" + testSAMLRequest));
    }

    @Test
    public void testLoginCachingHeadersSAML2Compliance() throws Exception {
        final Response response = sut.requestLogin(testURI);
        final List<Object> cacheControl = response.getMetadata().get("Cache-Control");
        final List<Object> pragma = response.getMetadata().get("pragma");

        /* 3.4.5.1, HTTP and Caching Considerations:
         * HTTP proxies and the user agent intermediary should not cache SAML protocol messages.
         * To ensure this, the following rules SHOULD be followed.
         * When returning SAML protocol messages using HTTP 1.1, HTTP responders SHOULD:
         * Include a Cache-Control header field set to "no-cache, no-store".
         * Include a Pragma header field set to "no-cache".
         */
        assertTrue(cacheControl.contains("no-cache"));
        assertTrue(cacheControl.contains("no-store"));
        assertTrue(pragma.contains("no-cache"));
    }

    @Test
    public void testConsumeAssertion() throws Exception {

    }

    @Test
    public void testPurgeExpiredLoginRequests() throws Exception {

    }

    @Test
    public void testGetLoginRequests() throws Exception {

    }

    @Test(expected = BadRequestException.class)
    public void testRemoveLoginRequestForIllegalUUID() throws Exception {
        sut.removeLoginRequest("illegal-uuid");
    }

    @Test(expected = NotFoundException.class)
    public void testRemoveLoginRequestForInvalidUUID() throws Exception {
        sut.removeLoginRequest("11111111-1111-1111-1111-111111111111");
    }

    @Test
    public void testRemoveLoginRequestForValidUUID() throws Exception {
        final Response response = sut.removeLoginRequest(testRelayState.toString());
        assertEquals(testLoginRequest, response.getEntity());
    }

    @Test
    public void testRemoveLoginRequestREST() throws Exception {
        Method m = findRESTMethod("/requests/", DELETE.class);
        final List<String> rolesAllowed = getRolesAllowed(m);
        assertTrue(rolesAllowed.contains(LOGIN_MANAGER));
        assertEquals(1, rolesAllowed.size());
    }

    private List<String> getRolesAllowed(Method m) {
        return ImmutableList.copyOf(m.getAnnotation(RolesAllowed.class).value());
    }

    private Method findRESTMethod(String path, Class withAnnotation) throws NoSuchMethodException {
        for (Method m : sut.getClass().getDeclaredMethods()) {
            if (m.getAnnotation(withAnnotation) != null) {
                final Path pathAnnotation = m.getAnnotation(Path.class);
                if (pathAnnotation != null && pathAnnotation.value().startsWith(path)) {
                    return m;
                }
            }
        }

        throw new NoSuchMethodException("No @" + withAnnotation
                .getSimpleName() + " method found for REST path: " + path);
    }
}
