package nl.knaw.huygens.security.client;

import nl.knaw.huygens.security.client.model.SecurityInformation;

/**
 * Get the data from a ContainerRequest, that is required for the
 * SecurityInformation. This interface should be used to communicate with 3rd
 * party security services.
 */
public interface AuthorizationHandler {

	/**
	 * Extracts the information for needed for creating a SecurityContext from a
	 * ContainerRequest. The implementation of this interface will be dependent
	 * on the 3rd party security implementation. 
	 * @param sessionId the id of the user session.
	 * 
	 * @return the information needed to create a SecurityContext.
	 * @throws UnauthorizedException will be thrown when the @{code sessionId} is null or invalid.
	 */
	SecurityInformation getSecurityInformation(String sessionId) throws UnauthorizedException;
}
