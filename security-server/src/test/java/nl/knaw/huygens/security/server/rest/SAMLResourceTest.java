package nl.knaw.huygens.security.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static nl.knaw.huygens.security.server.Roles.LOGIN_MANAGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.core.rest.API;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.model.LoginRequest;
import nl.knaw.huygens.security.server.saml2.SAMLEncoder;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.DefaultBootstrap;
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

    private LoginService loginService;

    private SAMLEncoder samlEncoder;

    private SessionService sessionService;

    public SAMLResourceTest() {
        testURI = URI.create("www.example.com");
        testRelayState = UUID.fromString("12345678-abcd-1234-abcd-1234567890ab");
        testSAMLRequest = "%3Csaml%20request%3E";
        testLoginRequest = new LoginRequest(testURI);
    }

    @BeforeClass
    public static void bootstrapDependencies() throws Exception {
        DefaultBootstrap.bootstrap();
    }

    @Before
    public void setup() throws Exception {
        loginService = mock(LoginService.class);
        samlEncoder = mock(SAMLEncoder.class);
        sessionService = mock(SessionService.class);
        sut = new SAMLResource(sessionService, loginService, samlEncoder);
    }

    @Test
    public void testLoginMustRedirect() throws Exception {
        when(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).thenReturn(testSAMLRequest);
        when(loginService.createLoginRequest(testURI)).thenReturn(testRelayState);
        assertEquals(303, sut.requestLogin(testURI).getStatus());
    }

    @Test
    public void testLoginRedirectLocationSAML2Compliance() throws Exception {
        when(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).thenReturn(testSAMLRequest);
        when(loginService.createLoginRequest(testURI)).thenReturn(testRelayState);

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
        when(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).thenReturn(testSAMLRequest);
        when(loginService.createLoginRequest(testURI)).thenReturn(testRelayState);

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

    @Test(expected = BadRequestException.class)
    public void testACSNoSAMLResponseParam() throws Exception {
        sut.consumeAssertion(null, testRelayState.toString());
    }

    @Test(expected = BadRequestException.class)
    public void testACSEmptySAMLResponseParam() throws Exception {
        sut.consumeAssertion("", testRelayState.toString());
    }

    @Test(expected = BadRequestException.class)
    public void testACSNoRelayStateParam() throws Exception {
        sut.consumeAssertion(testSAMLRequest, null);
    }

    @Test(expected = BadRequestException.class)
    public void testACSEmptyRelayStateParam() throws Exception {
        sut.consumeAssertion(testSAMLRequest, "");
    }

    @Test(expected = BadRequestException.class)
    public void testACSIllegalRelayStateParam() throws Exception {
        sut.consumeAssertion(testSAMLRequest, "illegal-relaystate");
    }

    @Test(expected = NotFoundException.class)
    public void testACSInvalidRelayStateParam() throws Exception {
        sut.consumeAssertion(testSAMLRequest, testRelayState.toString());
    }

    @Test
    public void testConsumeAssertion() throws Exception {
        final UUID relayState = testLoginRequest.getRelayState();
        when(loginService.removeLoginRequest(relayState)).thenReturn(testLoginRequest);
        final Response response = sut.consumeAssertion(getSAMLResponse(), relayState.toString());
        assertEquals(303, response.getStatus());
        final URI loactionURI = (URI) response.getMetadata().getFirst("Location");
        log.debug("Location: {}", loactionURI);
        final String location = loactionURI.toString();
        assertTrue(location.startsWith(testLoginRequest.getRedirectURI().toString()));
        assertTrue(location.contains(API.SESSION_ID_HTTP_PARAM + '='));
    }

    @Test
    public void testPurgeExpiredLoginRequests() throws Exception {
        when(loginService.purgeExpiredRequests()).thenReturn(Lists.newArrayList(testLoginRequest));
        final Response response = sut.purgeExpiredLoginRequests();
        Collection purged = (Collection) response.getEntity();
        assertEquals(1, purged.size());
        assertTrue(purged.contains(testLoginRequest));
    }

    @Test
    public void testGetLoginRequests() throws Exception {
        when(loginService.getPendingLoginRequests()).thenReturn(Lists.newArrayList(testLoginRequest));
        final Collection<LoginRequest> loginRequests = sut.getLoginRequests();
        assertEquals(1, loginRequests.size());
        assertTrue(loginRequests.contains(testLoginRequest));
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
        when(loginService.removeLoginRequest(testRelayState)).thenReturn(testLoginRequest);
        final Response response = sut.removeLoginRequest(testRelayState.toString());
        assertEquals(testLoginRequest, response.getEntity());
    }

    @Test
    public void testRESTLoginRequest() throws Exception {
        Method m = findRESTMethod("/saml2/login", POST.class);

        assertNull(m.getAnnotation(RolesAllowed.class));

        assertTrue(getProducedMediaTypes(m).contains(TEXT_HTML));
    }

    @Test
    public void testRESTAssertionConsumerService() throws Exception {
        Method m = findRESTMethod("/saml2/acs", POST.class);
        assertNull(m.getAnnotation(RolesAllowed.class));

        assertTrue(getProducedMediaTypes(m).contains(TEXT_HTML));
        assertTrue(getConsumedMediaTypes(m).contains(APPLICATION_FORM_URLENCODED));
    }

    @Test
    public void testRESTRemoveLoginRequest() throws Exception {
        Method m = findRESTMethod("/saml2/requests/", DELETE.class);

        final List<String> rolesAllowed = getRolesAllowed(m);
        assertTrue(rolesAllowed.contains(LOGIN_MANAGER));
        assertEquals(1, rolesAllowed.size());

        assertTrue(getProducedMediaTypes(m).contains(APPLICATION_JSON));
    }

    @Test
    public void testRESTPurge() throws Exception {
        final Method m = findRESTMethod("/saml2/purge", POST.class);

        final List<String> rolesAllowed = getRolesAllowed(m);
        assertEquals(1, rolesAllowed.size());
        assertTrue(rolesAllowed.contains(LOGIN_MANAGER));

        assertTrue(getProducedMediaTypes(m).contains(APPLICATION_JSON));
    }

    private String getSAMLResponse() throws IOException {
        final URL url = getClass().getResource("/saml-response.txt");
        final File file = new File(url.getFile());
        String samlResponse = Files.toString(file, Charsets.UTF_8);
        return URLDecoder.decode(samlResponse, Charsets.UTF_8.name());
    }

    private List<String> getConsumedMediaTypes(Method m) {
        return ImmutableList.copyOf(m.getAnnotation(Consumes.class).value());
    }

    private List<String> getProducedMediaTypes(Method m) {
        return ImmutableList.copyOf(m.getAnnotation(Produces.class).value());
    }

    private List<String> getRolesAllowed(Method m) {
        return ImmutableList.copyOf(m.getAnnotation(RolesAllowed.class).value());
    }

    private Method findRESTMethod(String path, Class annotation) throws NoSuchMethodException {
        final Path classRestAnnotation = sut.getClass().getAnnotation(Path.class);
        final String classRestPrefix = classRestAnnotation == null ? "" : classRestAnnotation.value();
        for (Method m : sut.getClass().getDeclaredMethods()) {
            if (m.getAnnotation(annotation) != null) {
                final Path methodRestAnnotation = m.getAnnotation(Path.class);
                final String restPath = classRestPrefix + methodRestAnnotation.value();
                if (methodRestAnnotation != null && restPath.startsWith(path)) {
                    return m;
                }
            }
        }

        throw new NoSuchMethodException("No @" + annotation.getSimpleName() + " method found for REST path: " + path);
    }
}
