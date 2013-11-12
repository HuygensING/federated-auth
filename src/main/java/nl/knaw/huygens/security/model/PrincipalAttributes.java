package nl.knaw.huygens.security.model;

import java.util.EnumSet;

import com.google.common.base.Objects;

public class PrincipalAttributes {
    private String commonName;

    private String displayName;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String toString() {
        return Objects.toStringHelper(this)//
                .add("persistentID", persistentID)//
                .add("commonName", commonName)//
                .add("displayName", displayName)//
                .add("surname", surname)//
                .add("emailAddress", emailAddress)//
                .add("affiliations", affiliations)//
                .add("organization", organization)//
                .toString();
    }

    public static enum Affiliation {employee, student, affiliate, staff, member}
}
