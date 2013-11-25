package nl.knaw.huygens.security.client;

import java.security.Principal;
import java.util.EnumSet;

import nl.knaw.huygens.security.client.model.HuygensSecurityInformation;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.Affiliation;

/**
 * A mock implementation for the {@code AuthorizationHandler} interface.
 * @author martijnm
 *
 */
public class MockAuthorizationHandler implements AuthorizationHandler {

  @Override
  public SecurityInformation getSecurityInformation(String sessionId) throws UnauthorizedException {
    HuygensSecurityInformation securityInformation = new HuygensSecurityInformation();
    securityInformation.setAffiliations(EnumSet.of(Affiliation.employee));
    securityInformation.setCommonName("John Doe");
    securityInformation.setDisplayName("John Doe");
    securityInformation.setEmailAddress("john@doe.com");
    securityInformation.setGivenName("John");
    securityInformation.setSurname("Doe");
    securityInformation.setOrganization("Doe inc.");
    securityInformation.setPersistentID("111111333");
    securityInformation.setPrincipal(new Principal() {

      @Override
      public String getName() {
        return "John Doe";
      }
    });

    return securityInformation;
  }
}
