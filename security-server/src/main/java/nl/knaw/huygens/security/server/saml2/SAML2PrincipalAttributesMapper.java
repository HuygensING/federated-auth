package nl.knaw.huygens.security.server.saml2;

/*
 * #%L
 * Security Server
 * =======
 * Copyright (C) 2013 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.List;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.NameID;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAML2PrincipalAttributesMapper {
    public static final String FORMAT_PERSISTENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    public static final String URN_MACE_CN = "urn:mace:dir:attribute-def:cn";

    public static final String URN_OID_CN = "urn:oid:2.5.4.3";

    public static final String URN_MACE_DISPLAY_NAME = "urn:mace:dir:attribute-def:displayName";

    public static final String URN_OID_DISPLAY_NAME = "urn:oid:2.16.840.1.113730.3.1.241";

    public static final String URN_MACE_GIVEN_NAME = "urn:mace:dir:attribute-def:givenName";

    public static final String URN_OID_GIVEN_NAME = "urn:oid:2.5.4.42";

    public static final String URN_MACE_SURNAME = "urn:mace:dir:attribute-def:sn";

    public static final String URN_OID_SURNAME = "urn:oid:2.5.4.4";

    public static final String URN_MACE_EMAIL = "urn:mace:dir:attribute-def:mail";

    public static final String URN_OID_EMAIL = "urn:oid:0.9.2342.19200300.100.1.3";

    public static final String URN_MACE_AFFILIATION = "urn:mace:dir:attribute-def:eduPersonAffiliation";

    public static final String URN_OID_AFFILIATION = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";

    public static final String URN_MACE_ORGANIZATION = "urn:mace:terena.org:attribute-def:schacHomeOrganization";

    public static final String URN_OID_ORGANIZATION = "urn:oid:1.3.6.1.4.1.25178.1.2.9";

    public static final String URN_MACE_PERSISTENT_ID = "urn:mace:dir:attribute-def:eduPersonTargetedID";

    public static final String URN_OID_PERSISTENT_ID = "urn:oid:1.3.6.1.4.1.5923.1.1.1.10";

    // In the past, SURFconext used to send the home organisation in the attribute
    // urn:oid:1.3.6.1.4.1.1466.115.121.1.15, which was incorrect.  Since 2013, the correct oid
    // urn:oid:1.3.6.1.4.1.25178.1.2.9 is in use.  For reasons of compatibility, the old (wrong)
    // key is also still sent. It should not be used in new implementations.
    public static final String URN_OID_LEGACY_ORGA = "urn:oid:1.3.6.1.4.1.1466.115.121.1.15";

    private static final Logger log = LoggerFactory.getLogger(SAML2PrincipalAttributesMapper.class);

    private final HuygensPrincipal huygensPrincipal;

    public SAML2PrincipalAttributesMapper() {
        this.huygensPrincipal = new HuygensPrincipal();
    }

    public HuygensPrincipal getHuygensPrincipal() {
        return huygensPrincipal;
    }

    public SAML2PrincipalAttributesMapper map(Assertion assertion) {
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            map(attributeStatement);
        }

        return this;
    }

    public SAML2PrincipalAttributesMapper map(AttributeStatement attributeStatement) {
        for (Attribute attribute : attributeStatement.getAttributes()) {
            map(attribute);
        }

        return this;
    }

    public SAML2PrincipalAttributesMapper map(Attribute attribute) {
        final String name = attribute.getName();

        if (name.startsWith("urn:mace")) {
            mapMACEAttribute(attribute);
        }
        else if (name.startsWith("urn:oid")) {
            mapOIDAttribute(attribute);
        }
        else {
            log.warn("Unknown attribute schema (neither urn:mace, nor urn:oid): [{}]", name);
        }

        return this;
    }

    private void mapCommonName(Attribute attribute) {
        final String commonName = getFirstAttributeValueString(attribute);
        huygensPrincipal.setCommonName(commonName);
    }

    private void mapDisplayName(Attribute attribute) {
        final String displayName = getFirstAttributeValueString(attribute);
        huygensPrincipal.setDisplayName(displayName);
    }

    private void mapGivenName(Attribute attribute) {
        final String givenName = getFirstAttributeValueString(attribute);
        huygensPrincipal.setGivenName(givenName);
    }

    private void mapSurname(Attribute attribute) {
        final String surname = getFirstAttributeValueString(attribute);
        huygensPrincipal.setSurname(surname);
    }

    private void mapEmailAddress(Attribute attribute) {
        final String emailAddress = getFirstAttributeValueString(attribute);
        huygensPrincipal.setEmailAddress(emailAddress);
    }

  private void mapAffiliation(Attribute attribute) {
    for (XMLObject value : attribute.getAttributeValues()) {
      final String affiliation = getValueAsString(value);
      huygensPrincipal.addAffiliation(affiliation);
    }
  }

  private void mapOrganization(Attribute attribute) {
    final String organization = getFirstAttributeValueString(attribute);
    huygensPrincipal.setOrganization(organization);
  }

  private void mapPersistentID(Attribute attribute) {
    final XSAny xsAny = (XSAny) getFirstAttributeValue(attribute);
    for (XMLObject child : xsAny.getUnknownXMLObjects()) {
      NameID nameID = (NameID) child;
      final String format = nameID.getFormat();
      final String value = nameID.getValue();
      if (!FORMAT_PERSISTENT.equals(format)) {
        log.warn("Incorrect format {} for persistent id, expected: {}", format, FORMAT_PERSISTENT);
      } else {
        huygensPrincipal.setPersistentID(value);
      }
    }
  }

  private XMLObject getFirstAttributeValue(Attribute attribute) {
    final List<XMLObject> attributeValues = attribute.getAttributeValues();

    if (attributeValues.isEmpty()) {
      log.warn("Empty attribute values list for attribute: {}", attribute.getName());
      return null;
    }

    return attributeValues.get(0);
  }

  private String getFirstAttributeValueString(Attribute attribute) {
    XMLObject xmlObj = getFirstAttributeValue(attribute);

    String value = getValueAsString(xmlObj);

    log.trace("first attribute value for [{}] is [{}]", attribute.getName(), value);
    return value;
  }

  private String getValueAsString(XMLObject xmlObj) {
    String value = null;
    if (xmlObj instanceof XSString) {
      value = ((XSString) xmlObj).getValue();
    } else if (xmlObj instanceof XSAny) {
      value = ((XSAny) xmlObj).getTextContent();
    }
    return value;
  }

  private void mapMACEAttribute(Attribute attribute) {
    final String name = attribute.getName();
    if (URN_MACE_CN.equals(name)) {
      mapCommonName(attribute);
    } else if (URN_MACE_DISPLAY_NAME.equals(name)) {
      mapDisplayName(attribute);
    } else if (URN_MACE_GIVEN_NAME.equals(name)) {
      mapGivenName(attribute);
    } else if (URN_MACE_SURNAME.equals(name)) {
      mapSurname(attribute);
    } else if (URN_MACE_EMAIL.equals(name)) {
      mapEmailAddress(attribute);
    } else if (URN_MACE_AFFILIATION.equals(name)) {
      mapAffiliation(attribute);
    } else if (URN_MACE_ORGANIZATION.equals(name)) {
      mapOrganization(attribute);
    } else if (URN_MACE_PERSISTENT_ID.equals(name)) {
      mapPersistentID(attribute);
    } else {
      final String value = getFirstAttributeValueString(attribute);
      log.trace("Unmapped urn:mace attribute: [{}] -> [{}]", name, value);
    }
  }

  private void mapOIDAttribute(Attribute attribute) {
    final String name = attribute.getName();
    if (URN_OID_CN.equals(name)) {
      mapCommonName(attribute);
    } else if (URN_OID_DISPLAY_NAME.equals(name)) {
      mapDisplayName(attribute);
    } else if (URN_OID_GIVEN_NAME.equals(name)) {
      mapGivenName(attribute);
    } else if (URN_OID_SURNAME.equals(name)) {
      mapSurname(attribute);
    } else if (URN_OID_EMAIL.equals(name)) {
      mapEmailAddress(attribute);
    } else if (URN_OID_AFFILIATION.equals(name)) {
      mapAffiliation(attribute);
    } else if (URN_OID_ORGANIZATION.equals(name)) {
      mapOrganization(attribute);
    } else if (URN_OID_PERSISTENT_ID.equals(name)) {
      mapPersistentID(attribute);
    } else if (URN_OID_LEGACY_ORGA.equals(name)) {
      log.trace("Ignoring legacy home organization attribute");
    } else {
      final String value = getFirstAttributeValueString(attribute);
      log.trace("Unmapped urn:oid attribute: [{}] -> [{}]", name, value);
    }
  }
}
