package nl.knaw.huygens.security.client;

import java.security.Principal;
import java.util.EnumSet;
import java.util.UUID;

import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.security.core.model.HuygensPrincipal.Affiliation;

import com.sun.jersey.spi.container.ContainerRequest;

public class MockAuthorizationHandler implements AuthorizationHandler {

  @Override
  public SecurityInformation getSecurityInformation(ContainerRequest request) throws UnauthorizedException {
    SecurityInformation securityInformation = new SecurityInformation();
    securityInformation.setAffiliations(EnumSet.of(Affiliation.employee));
    securityInformation.setCommonName("John Doe");
    securityInformation.setDisplayName("John Doe");
    securityInformation.setEmailAddress("john@doe.com");
    securityInformation.setGivenName("John");
    securityInformation.setSurname("Doe");
    securityInformation.setOrganization("Doe inc.");
    securityInformation.setPersistentID(UUID.randomUUID().toString());
    securityInformation.setPrincipal(new Principal() {

      @Override
      public String getName() {
        return "John Doe";
      }
    });

    return securityInformation;
  }
}
