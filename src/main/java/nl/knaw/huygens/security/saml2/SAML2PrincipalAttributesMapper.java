package nl.knaw.huygens.security.saml2;

import java.util.List;

import nl.knaw.huygens.security.model.PrincipalAttributes;
import org.opensaml.saml2.core.Attribute;
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

    private final PrincipalAttributes principalAttributes = new PrincipalAttributes();

    public PrincipalAttributes map(List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            log.debug("mapping attribute: {}", attribute.getDOM().toString());

            final String name = attribute.getName();
            if (name.startsWith("urn:oid")) {
                mapOIDAttribute(attribute);
            }
            else if (name.startsWith("urn:mace")) {
                mapMACEAttribute(attribute);
            }
            else {
                log.warn("Unrecognized attribute: {}", attribute.getDOM().toString());
            }
        }

        return principalAttributes;
    }

    public PrincipalAttributes getPrincipalAttributes() {
        return principalAttributes;
    }

    private void mapCommonName(Attribute attribute) {
        final String commonName = getFirstAttributeValueString(attribute);
        principalAttributes.setCommonName(commonName);
    }

    private void mapDisplayName(Attribute attribute) {
        final String displayName = getFirstAttributeValueString(attribute);
        principalAttributes.setDisplayName(displayName);
    }

    private void mapSurname(Attribute attribute) {
        final String surname = getFirstAttributeValueString(attribute);
        principalAttributes.setSurname(surname);
    }

    private void mapEmailAddress(Attribute attribute) {
        final String emailAddress = getFirstAttributeValueString(attribute);
        principalAttributes.setEmailAddress(emailAddress);
    }

    private void mapAffiliation(Attribute attribute) {
        for (XMLObject value : attribute.getAttributeValues()) {
            final String affiliation = ((XSAny) value).getTextContent();
            principalAttributes.addAffiliation(affiliation);
        }
    }

    private void mapOrganization(Attribute attribute) {
        final String organization = getFirstAttributeValueString(attribute);
        principalAttributes.setOrganization(organization);
    }

    private void mapPersistentID(Attribute attribute) {
        final XSAny xsAny = (XSAny) getFirstAttributeValue(attribute);
        for (XMLObject child : xsAny.getUnknownXMLObjects()) {
            log.debug("   +- child: {}", child);
            NameID nameID = (NameID) child;
            if (!FORMAT_PERSISTENT.equals(nameID.getFormat())) {
                log.warn("Incorrect format for persistent id: {}", nameID);
            }
            else {
                principalAttributes.setPersistentID(nameID.getValue());
            }
        }
    }

    private XMLObject getFirstAttributeValue(Attribute attribute) {
        if (attribute == null) {
            log.warn("Got null attribute");
            return null;
        }

        final List<XMLObject> attributeValues = attribute.getAttributeValues();
        if (attributeValues.isEmpty()) {
            log.warn("Empty attribute values list for attribute: {}", attribute.getName());
            return null;
        }

        final XMLObject value = attributeValues.get(0);
        log.debug("first attribute value for attribute {} is: {}", attribute.getName(), value);

        return value;
    }

    private String getFirstAttributeValueString(Attribute attribute) {
        XMLObject xmlObj = getFirstAttributeValue(attribute);
        if (xmlObj instanceof XSString) {
            return ((XSString) xmlObj).getValue();
        }
        else if (xmlObj instanceof XSAny) {
            return ((XSAny) xmlObj).getTextContent();
        }
        return null;
    }

    private void mapMACEAttribute(Attribute attribute) {
        final String name = attribute.getName();
        if (URN_MACE_CN.equals(name)) {
            mapCommonName(attribute);
        }
        else if (URN_MACE_DISPLAY_NAME.equals(name)) {
            mapDisplayName(attribute);
        }
        else if (URN_MACE_SURNAME.equals(name)) {
            mapSurname(attribute);
        }
        else if (URN_MACE_EMAIL.equals(name)) {
            mapEmailAddress(attribute);
        }
        else if (URN_MACE_AFFILIATION.equals(name)) {
            mapAffiliation(attribute);
        }
        else if (URN_MACE_ORGANIZATION.equals(name)) {
            mapOrganization(attribute);
        }
        else if (URN_MACE_PERSISTENT_ID.equals(name)) {
            mapPersistentID(attribute);
        }
    }

    private void mapOIDAttribute(Attribute attribute) {
        final String name = attribute.getName();
        if (URN_OID_CN.equals(name)) {
            mapCommonName(attribute);
        }
        else if (URN_OID_DISPLAY_NAME.equals(name)) {
            mapDisplayName(attribute);
        }
        else if (URN_OID_SURNAME.equals(name)) {
            mapSurname(attribute);
        }
        else if (URN_OID_EMAIL.equals(name)) {
            mapEmailAddress(attribute);
        }
        else if (URN_OID_AFFILIATION.equals(name)) {
            mapAffiliation(attribute);
        }
        else if (URN_OID_ORGANIZATION.equals(name)) {
            mapOrganization(attribute);
        }
        else if (URN_OID_LEGACY_ORGA.equals(name)) {
            log.info("Ignoring legacy home organisation attribute");
        }
        else if (URN_OID_PERSISTENT_ID.equals(name)) {
            mapPersistentID(attribute);
        }
    }
}
