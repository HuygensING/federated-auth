package nl.knaw.huygens.security.client.model;

import java.security.Principal;
import java.util.EnumSet;

import nl.knaw.huygens.security.core.model.HuygensPrincipal.Affiliation;

/**
 * A class that contains the mandatory information, that is needed to create a SecurityContext.
 *
 */

// TODO create interface
public class SecurityInformation {
  private String commonName;
  private String displayName;
  private String givenName;
  private String surname;
  private String emailAddress;
  private EnumSet<Affiliation> affiliations = EnumSet.noneOf(Affiliation.class);
  private String organization;
  private String persistentID;
  private Principal principal;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Principal getPrincipal() {
    return principal;
  }

  public void setPrincipal(Principal principal) {
    this.principal = principal;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public EnumSet<Affiliation> getAffiliations() {
    return affiliations;
  }

  public void setAffiliations(EnumSet<Affiliation> affiliations) {
    this.affiliations = affiliations;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getPersistentID() {
    return persistentID;
  }

  public void setPersistentID(String persistentID) {
    this.persistentID = persistentID;
  }

}
