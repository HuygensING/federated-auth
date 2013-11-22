package nl.knaw.huygens.security.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.Affiliation;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.SecuritySession;
import nl.knaw.huygens.security.core.rest.API;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;

public class HuygensAuthorizationHandlerTest {
  private static final String AUTHORIZATION_URL = "http://localhost:9000";
  private static final String DEFAULT_SESSION_ID = "test";
  private static final String PERSISTENT_ID = "111111333";
  private static final String ORGANISATION = "Doe inc.";
  private static final String SURNAME = "Doe";
  private static final String GIVEN_NAME = "John";
  private static final String EMAIL = "john@doe.com";
  private static final String NAME = "John Doe";
  private static final EnumSet<Affiliation> AFFILIATIONS = EnumSet.of(Affiliation.employee);
  private Client client;
  private HuygensAuthorizationHandler instance;

  @Before
  public void setUp() {
    client = mock(Client.class);
    instance = new HuygensAuthorizationHandler(client, AUTHORIZATION_URL);
  }

  @After
  public void tearDown() {
    client = null;
    instance = null;
  }

  @Test
  public void testGetSecurityInformationExistingUser() throws UnauthorizedException {
    ClientResponse response = mock(ClientResponse.class);
    when(response.getClientResponseStatus()).thenReturn(Status.OK);
    when(response.getEntity(SecuritySession.class)).thenReturn(createSecuritySession());

    setUpClient(response);

    SecurityInformation expected = createSecurityInformation();

    SecurityInformation actual = instance.getSecurityInformation(DEFAULT_SESSION_ID);
    assertEquals(expected, actual);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSecurityInformationTokenNull() throws UnauthorizedException {
    instance.getSecurityInformation(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSecurityInformationTokenEmpty() throws UnauthorizedException {
    instance.getSecurityInformation("");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSecurityInformationSessionNotFound() throws UnauthorizedException {
    ClientResponse response = mock(ClientResponse.class);
    when(response.getClientResponseStatus()).thenReturn(Status.NOT_FOUND);

    setUpClient(response);

    instance.getSecurityInformation(DEFAULT_SESSION_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSecurityInformationIllegalSessionToken() throws UnauthorizedException {
    ClientResponse response = mock(ClientResponse.class);
    when(response.getClientResponseStatus()).thenReturn(Status.BAD_REQUEST);

    setUpClient(response);

    instance.getSecurityInformation(DEFAULT_SESSION_ID);
  }

  private void setUpClient(ClientResponse response) {
    WebResource resource = mock(WebResource.class);
    when(resource.path(DEFAULT_SESSION_ID)).thenReturn(resource);
    when(resource.path(API.SESSION_AUTHENTICATION_URI)).thenReturn(resource);
    when(client.resource(AUTHORIZATION_URL)).thenReturn(resource);
    when(resource.get(ClientResponse.class)).thenReturn(response);
  }

  private SecuritySession createSecuritySession() {

    final HuygensPrincipal principal = createSecurityPrincipal();

    SecuritySession session = new SecuritySession() {

      @Override
      public HuygensPrincipal getOwner() {
        return principal;
      }

      @Override
      public DateTime getExpiresOn() {
        return new DateTime();
      }
    };

    return session;
  }

  private HuygensPrincipal createSecurityPrincipal() {
    HuygensPrincipal principal = new HuygensPrincipal();
    principal.setAffiliations(AFFILIATIONS);
    principal.setCommonName(NAME);
    principal.setDisplayName(NAME);
    principal.setEmailAddress(EMAIL);
    principal.setGivenName(GIVEN_NAME);
    principal.setSurname(SURNAME);
    principal.setOrganization(ORGANISATION);
    principal.setPersistentID(PERSISTENT_ID);
    return principal;
  }

  private SecurityInformation createSecurityInformation() {
    HuygensSecurityInformation principal = new HuygensSecurityInformation(createSecurityPrincipal());

    return principal;
  }
}
