package nl.knaw.huygens.security.client;

import static com.google.common.base.Preconditions.checkNotNull;

import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.HuygensSession;
import static nl.knaw.huygens.security.core.rest.API.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class HuygensAuthorizationHandler implements AuthorizationHandler {

  private Logger LOG = LoggerFactory.getLogger(HuygensAuthorizationHandler.class);

  private final Client client;
  private final String authorizationUrl;
  private final String basicCredentials;

  public HuygensAuthorizationHandler(Client client, String authorizationUrl, String basicCredentials) {
    this.client = checkNotNull(client);
    this.basicCredentials = checkNotNull(basicCredentials);
    if (StringUtils.isBlank(authorizationUrl)) {
      LOG.info("Authorization url was empty");
      throw new IllegalArgumentException("Authorization url was empty");
    }

    this.authorizationUrl = authorizationUrl;
  }

  @Override
  public SecurityInformation getSecurityInformation(String sessionToken) throws UnauthorizedException {

    HuygensSession session = doSessionDetailsRequest(sessionToken);
    
    doSessionRefresh(sessionToken);

    return new HuygensSecurityInformation(session.getOwner());
  }
  
  private void doSessionRefresh(String sessionToken){
    client.resource(authorizationUrl).path(SESSION_AUTHENTICATION_URI).path(sessionToken).path(REFRESH_PATH).header(HttpHeaders.AUTHORIZATION, basicCredentials).put();
  }

  private HuygensSession doSessionDetailsRequest(String sessionToken) throws UnauthorizedException {
    LOG.info("sessionToken: {}", sessionToken);
    if (StringUtils.isBlank(sessionToken)) {
      LOG.info("Session token was empty");
      throw new UnauthorizedException();
    }

    LOG.info("authorization url: {}", authorizationUrl);

    Builder builder = createSessionResourceBuilder(sessionToken);
    
    ClientResponse response = builder.get(ClientResponse.class);

    switch (response.getClientResponseStatus()) {
    case OK:
      break;
    case NOT_FOUND:
      LOG.info("Session token {} is unknown.", sessionToken);
      throw new UnauthorizedException();
    case GONE:
      LOG.info("Session of token {} is expired.", sessionToken);
      throw new UnauthorizedException();
    case BAD_REQUEST:
      LOG.error("Illegal session token {}.", sessionToken);
      throw new UnauthorizedException();
    default:
      LOG.error("Unknown exception for token {}.", sessionToken);
      LOG.error("Response code is {}.", response.getClientResponseStatus());
      throw new UnauthorizedException();
    }

    LOG.info("clientResponse: " + response);

    HuygensSession session = response.getEntity(ClientSession.class);
    return session;
  }

  private Builder createSessionResourceBuilder(String sessionToken) {
    WebResource resource = client.resource(authorizationUrl).path(SESSION_AUTHENTICATION_URI).path(sessionToken);

    LOG.info("url: {}", resource.getURI());

    return resource.header(HttpHeaders.AUTHORIZATION, basicCredentials);
  }
  
  /**
   * Destroy the session of the user that wants to logout.
   * @param sessionToken the session id that should be destroyed.
   * @return {@code true} if the session was (already) destroyed or expired.
   * @throws {@link IllegalArgumentException} if {@code sessionToken} is not a legal session id on the server.
   * @throws {@link UnauthorizedException} when the client is unauthorized to destroy the session.
   */
  public boolean logout(String sessionToken) throws UnauthorizedException{
    Builder builder = createSessionResourceBuilder(sessionToken);
    
    ClientResponse response = builder.delete(ClientResponse.class);
    
    switch (response.getClientResponseStatus()) {
    case OK:
    case NOT_FOUND:
    case GONE:
      return true;
    case BAD_REQUEST:
      throw new IllegalArgumentException("Illegal session token: " + sessionToken);
    case FORBIDDEN:
      throw new UnauthorizedException("Session token: " + sessionToken);
    default:
      return false;
    }   
  }
}
