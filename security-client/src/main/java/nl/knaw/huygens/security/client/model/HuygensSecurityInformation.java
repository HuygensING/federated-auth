package nl.knaw.huygens.security.client.model;

import java.security.Principal;
import java.util.EnumSet;

import nl.knaw.huygens.security.core.model.Affiliation;

/**
 * A class that contains the mandatory information, that is needed to create a SecurityContext.
 *
 */

public class HuygensSecurityInformation implements SecurityInformation {
  private String commonName;
  private String displayName;
  private String givenName;
  private String surname;
  private String emailAddress;
  private EnumSet<Affiliation> affiliations = EnumSet.noneOf(Affiliation.class);
  private String organization;
  private String persistentID;
  private Principal principal;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  public void setPrincipal(Principal principal) {
    this.principal = principal;
  }

  @Override
  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  @Override
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Override
  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  @Override
  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  @Override
  public EnumSet<Affiliation> getAffiliations() {
    return affiliations;
  }

  public void setAffiliations(EnumSet<Affiliation> affiliations) {
    this.affiliations = affiliations;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Override
  public String getPersistentID() {
    return persistentID;
  }

  public void setPersistentID(String persistentID) {
    this.persistentID = persistentID;
  }

}
