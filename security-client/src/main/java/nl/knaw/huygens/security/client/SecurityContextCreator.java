package nl.knaw.huygens.security.client;

import javax.ws.rs.core.SecurityContext;

import nl.knaw.huygens.security.client.model.SecurityInformation;

/**
 * Creates a SecurityContext with the information provided by the security information. 
 * If the SecurityInformation is null the returned SecurityContext will be null too.
 *
 */
public interface SecurityContextCreator {

  SecurityContext createSecurityContext(SecurityInformation securityInformation);
}
