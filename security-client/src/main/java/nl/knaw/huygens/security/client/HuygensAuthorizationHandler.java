package nl.knaw.huygens.security.client;

import static com.google.common.base.Preconditions.checkNotNull;
import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.SecuritySession;
import nl.knaw.huygens.security.core.rest.API;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class HuygensAuthorizationHandler implements AuthorizationHandler {
  private Logger LOG = LoggerFactory.getLogger(HuygensAuthorizationHandler.class);

  private final Client client;
  private final String authorizationUrl;

  public HuygensAuthorizationHandler(Client client, String authorizationUrl) {
    checkNotNull(client);
    if (StringUtils.isBlank(authorizationUrl)) {
      LOG.info("Authorization url was empty");
      throw new IllegalArgumentException("Authorization url was empty");
    }

    checkNotNull(authorizationUrl);
    this.authorizationUrl = authorizationUrl;
    this.client = client;
  }

  @Override
  public SecurityInformation getSecurityInformation(String sessionToken) throws UnauthorizedException {

    LOG.info("sessionToken: {}", sessionToken);
    if (StringUtils.isBlank(sessionToken)) {
      LOG.info("Session token was empty");
      throw new UnauthorizedException();
    }

    LOG.info("authorization url: {}", authorizationUrl);

    WebResource resource = client.resource(authorizationUrl).path(API.SESSION_AUTHENTICATION_URI).path(sessionToken);

    LOG.info("url: {}", resource.getURI());

    ClientResponse response = resource.get(ClientResponse.class);

    switch (response.getClientResponseStatus()) {
    case NOT_FOUND:
      LOG.info("Session token {} is unknown.", sessionToken);
      throw new UnauthorizedException();
    case OK:
      break;
    case BAD_REQUEST:
      LOG.error("Illegal session token {}.", sessionToken);
      throw new UnauthorizedException();
    default:
      LOG.error("Unknown execption for token {}.", sessionToken);
      throw new UnauthorizedException();
    }

    LOG.info("clientResponse: " + response);

    SecuritySession session = response.getEntity(ClientSession.class);
    //HuygensPrincipal principal = response.getEntity(HuygensPrincipal.class);

    //return new HuygensSecurityInformation(principal);

    return new HuygensSecurityInformation(session.getOwner());
  }
}
