package nl.knaw.huygens.security.client.model;

import java.security.Principal;
import java.util.EnumSet;

import nl.knaw.huygens.security.core.model.Affiliation;

public interface SecurityInformation {

  public abstract String getDisplayName();

  public abstract Principal getPrincipal();

  public abstract String getCommonName();

  public abstract String getGivenName();

  public abstract String getSurname();

  public abstract String getEmailAddress();

  public abstract EnumSet<Affiliation> getAffiliations();

  public abstract String getOrganization();

  public abstract String getPersistentID();

}