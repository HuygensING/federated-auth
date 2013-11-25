package nl.knaw.huygens.security.client.model;

import java.security.Principal;
import java.util.EnumSet;

import nl.knaw.huygens.security.core.model.Affiliation;

/**
 * A model that contains the information, provided by the Identity provider.
 */
public interface SecurityInformation {

  String getDisplayName();

  Principal getPrincipal();

  String getCommonName();

  String getGivenName();

  String getSurname();

  String getEmailAddress();

  EnumSet<Affiliation> getAffiliations();

  String getOrganization();

  String getPersistentID();

}