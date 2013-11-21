package nl.knaw.huygens.security.client;

import static com.google.common.base.Preconditions.checkNotNull;
import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.SecuritySession;
import nl.knaw.huygens.security.core.rest.API;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class HuygensAuthorizationHandler implements AuthorizationHandler {
  private Logger LOG = LoggerFactory.getLogger(HuygensAuthorizationHandler.class);

  private final Client client;

  @Inject
  public HuygensAuthorizationHandler(Client client) {
    checkNotNull(client);
    this.client = client;
  }

  @Override
  public SecurityInformation getSecurityInformation(String sessionToken) throws UnauthorizedException {
    if (StringUtils.isBlank(sessionToken)) {
      LOG.info("sessionId was empty");
      throw new UnauthorizedException();
    }

    ClientResponse response = client.resource(API.SESSION_AUTHENTICATION_URI).path(sessionToken).get(ClientResponse.class);

    switch (response.getClientResponseStatus()) {
    case NOT_FOUND:
      LOG.info("Session token {} is unknown.", sessionToken);
      throw new UnauthorizedException();
    case OK:
      break;
    case BAD_REQUEST:
      LOG.error("Illegal session token {} is unknown.", sessionToken);
      throw new UnauthorizedException();
    default:
      LOG.error("Unknown execption for token {}.", sessionToken);
      throw new UnauthorizedException();
    }

    SecuritySession session = response.getEntity(SecuritySession.class);

    return new HuygensSecurityInformation(session.getOwner());
  }
}
