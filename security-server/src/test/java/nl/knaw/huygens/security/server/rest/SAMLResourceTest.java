package nl.knaw.huygens.security.server.rest;

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

import static com.google.common.base.Charsets.UTF_8;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static nl.knaw.huygens.security.server.Roles.LOGIN_MANAGER;
import static nl.knaw.huygens.security.server.rest.SAMLResource.SURF_IDP_SSO_URL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import nl.knaw.huygens.security.core.rest.API;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.model.LoginRequest;
import nl.knaw.huygens.security.server.saml2.SAMLEncoder;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.sun.jersey.api.NotFoundException;

public class SAMLResourceTest extends ResourceTestCase {
  private final URI testURI = URI.create("www.example.com");

  private final UUID testRelayState = UUID.fromString("12345678-abcd-1234-abcd-1234567890ab");

  private final String testSAMLRequest = "%3Csaml%20request%3E";

  private final LoginRequest testLoginRequest = new LoginRequest(testURI);

  @Mock
  private LoginService loginService;

  @Mock
  private SAMLEncoder samlEncoder;

  @Mock
  private SessionService sessionService;

  @InjectMocks
  private SAMLResource sut;

  @BeforeClass
  public static void bootstrapDependencies() throws Exception {
    DefaultBootstrap.bootstrap(); // expensive, only needed once for the entire test suite
  }

  @Override
  public Object getSUT() {
    return sut;
  }

  @Test
  public void testLoginMustRedirect() throws Exception {
    given(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).willReturn(testSAMLRequest);
    given(loginService.createLoginRequest(testURI)).willReturn(testRelayState);

    //when
    final Response response = sut.requestLogin(testURI);

    //then
    assertThat(response.getStatus(), is(303));
  }

  @Test
  public void testLoginRedirectLocationSAML2Compliance() throws Exception {
    given(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).willReturn(testSAMLRequest);
    given(loginService.createLoginRequest(testURI)).willReturn(testRelayState);

    //when
    final Response response = sut.requestLogin(testURI);

    //then
    final URI locationURI = (URI) response.getMetadata().getFirst("Location");
    assertThat(locationURI.getScheme(), is("https")); // transport must be secure
    final String location = locationURI.toString();
    assertThat(location, startsWith(SURF_IDP_SSO_URL));
    assertThat(location, containsString("RelayState=" + testRelayState));
    assertThat(location, containsString("SAMLRequest=" + testSAMLRequest));
  }

  @Test
  public void testLoginCachingHeadersSAML2Compliance() throws Exception {
    given(samlEncoder.deflateAndBase64Encode(any(SAMLObject.class))).willReturn(testSAMLRequest);
    given(loginService.createLoginRequest(testURI)).willReturn(testRelayState);

    //when
    final Response response = sut.requestLogin(testURI);

    //then
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

  @Ignore("Find a more permanent solution to parse the saml and it's properties.")
  @Test
  public void testConsumeAssertion() throws Exception {
    //given
    final UUID relayState = testLoginRequest.getRelayState();
    given(loginService.removeLoginRequest(relayState)).willReturn(testLoginRequest);

    //when
    final Response response = sut.consumeAssertion(getSAMLResponse("/saml-response.txt"), relayState.toString());

    //then
    assertEquals(303, response.getStatus());
    final URI locationURI = (URI) response.getMetadata().getFirst("Location");
    final String location = locationURI.toString();
    assertThat(location, startsWith(testLoginRequest.getRedirectURI().toString()));
    assertThat(location, containsString(API.SESSION_ID_HTTP_PARAM + '='));
  }

  @Test
  public void testConsumeAssertionNewFile() throws Exception {
    //given
    final UUID relayState = testLoginRequest.getRelayState();
    given(loginService.removeLoginRequest(relayState)).willReturn(testLoginRequest);

    //when
    final Response response = sut.consumeAssertion(getSAMLResponse("/new-saml-response.txt"), relayState.toString());

    //then
    assertEquals(303, response.getStatus());
    final URI locationURI = (URI) response.getMetadata().getFirst("Location");
    final String location = locationURI.toString();
    assertThat(location, startsWith(testLoginRequest.getRedirectURI().toString()));
    assertThat(location, containsString(API.SESSION_ID_HTTP_PARAM + '='));
  }

  @Test
  public void testPurgeExpiredLoginRequests() throws Exception {
    //given
    given(loginService.purgeExpiredRequests()).willReturn(Lists.newArrayList(testLoginRequest));

    //when
    Response response = sut.purgeExpiredLoginRequests();
    @SuppressWarnings("unchecked")
    Collection<LoginRequest> purged = (Collection<LoginRequest>) response.getEntity();

    //then
    assertThat(purged, hasSize(1));
    assertThat(purged, contains(testLoginRequest));
  }

  @Test
  public void testGetLoginRequests() throws Exception {
    given(loginService.getPendingLoginRequests()).willReturn(Lists.newArrayList(testLoginRequest));

    //when
    final Collection<LoginRequest> loginRequests = sut.getLoginRequests();

    //then
    assertThat(loginRequests, hasSize(1));
    assertThat(loginRequests, contains(testLoginRequest));
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
    given(loginService.removeLoginRequest(testRelayState)).willReturn(testLoginRequest);

    //when
    final Response response = sut.removeLoginRequest(testRelayState.toString());

    //then
    assertThat(testLoginRequest, is(response.getEntity()));
  }

  @Test
  public void testRESTLoginRequest() throws Exception {
    Method m = restHelper.findMethod("/saml2/login", POST.class);

    assertNull(m.getAnnotation(RolesAllowed.class));
    assertThat(restHelper.getProducedMediaTypes(m), contains(TEXT_HTML));
  }

  @Test
  public void testRESTAssertionConsumerService() throws Exception {
    Method m = restHelper.findMethod("/saml2/acs", POST.class);

    assertNull(m.getAnnotation(RolesAllowed.class));
    assertThat(restHelper.getProducedMediaTypes(), contains(TEXT_HTML));
    assertThat(restHelper.getConsumedMediaTypes(), contains(APPLICATION_FORM_URLENCODED));
  }

  @Test
  public void testRESTRemoveLoginRequest() throws Exception {
    restHelper.findMethod("/saml2/requests/<sessionId>", DELETE.class);

    final List<String> rolesAllowed = restHelper.getRolesAllowed();
    assertThat(rolesAllowed, hasSize(1));
    assertThat(rolesAllowed, contains(LOGIN_MANAGER));

    assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
  }

  @Test
  public void testRESTPurge() throws Exception {
    restHelper.findMethod("/saml2/purge", POST.class);

    final List<String> rolesAllowed = restHelper.getRolesAllowed();
    assertThat(rolesAllowed, hasSize(1));
    assertThat(rolesAllowed, contains(LOGIN_MANAGER));

    assertThat(restHelper.getProducedMediaTypes(), contains(APPLICATION_JSON));
  }

  private String getSAMLResponse(String fileName) throws IOException {
    final URL url = getClass().getResource(fileName);
    final File file = new File(url.getFile());
    String samlResponse = Files.toString(file, UTF_8);
    return URLDecoder.decode(samlResponse, UTF_8.name());
  }
}
