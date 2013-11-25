package nl.knaw.huygens.security.core.model;

import java.security.Principal;
import java.util.EnumSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;

public class HuygensPrincipal implements Principal {
  private String commonName;

  private String displayName;

  private String givenName;

  private String surname;

  private String emailAddress;

  private EnumSet<Affiliation> affiliations = EnumSet.noneOf(Affiliation.class);

  private String organization;

  private String persistentID;

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  @JsonIgnore
  public String getName() {
    return getCommonName();
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  public void addAffiliation(Affiliation affiliation) {
    affiliations.add(affiliation);
  }

  public void addAffiliation(String affiliationString) {
    addAffiliation(Affiliation.valueOf(affiliationString));
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HuygensPrincipal)) {
      return false;
    }

    HuygensPrincipal other = (HuygensPrincipal) obj;

    boolean isEqual = Objects.equal(other.affiliations, affiliations);
    isEqual &= Objects.equal(other.commonName, commonName);
    isEqual &= Objects.equal(other.displayName, displayName);
    isEqual &= Objects.equal(other.emailAddress, emailAddress);
    isEqual &= Objects.equal(other.givenName, givenName);
    isEqual &= Objects.equal(other.organization, organization);
    isEqual &= Objects.equal(other.persistentID, persistentID);
    isEqual &= Objects.equal(other.surname, surname);

    return isEqual;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(affiliations, commonName, displayName, emailAddress, givenName, organization, persistentID, surname);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)//
        .add("persistentID", persistentID)//
        .add("commonName", commonName)//
        .add("displayName", displayName)//
        .add("givenName", givenName)//
        .add("surname", surname)//
        .add("emailAddress", emailAddress)//
        .add("affiliations", affiliations)//
        .add("organization", organization)//
        .toString();
  }
}
