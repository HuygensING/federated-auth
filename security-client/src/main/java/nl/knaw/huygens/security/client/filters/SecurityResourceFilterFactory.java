package nl.knaw.huygens.security.client.filters;

import com.google.inject.Inject;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import nl.knaw.huygens.security.client.AuthorizationHandler;
import nl.knaw.huygens.security.client.SecurityContextCreator;

public class SecurityResourceFilterFactory extends AbstractRolesAllowedResourceFilterFactory {
  private final SecurityContextCreator securityContextCreator;
  private final AuthorizationHandler authorizationHandler;

  @Inject
  public SecurityResourceFilterFactory(SecurityContextCreator securityContextCreator, AuthorizationHandler authorizationHandler) {
    this.securityContextCreator = securityContextCreator;
    this.authorizationHandler = authorizationHandler;
  }

  @Override
  protected ResourceFilter createResourceFilter(AbstractMethod am) {

    return new SecurityResourceFilter(securityContextCreator, authorizationHandler);
  }

  @Override
  protected ResourceFilter createNoSecurityResourceFilter() {
    return new BypassFilter();
  }

}