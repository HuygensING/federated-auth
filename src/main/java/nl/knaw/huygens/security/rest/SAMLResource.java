package nl.knaw.huygens.security.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@Path("/saml")
public class SAMLResource {

    public static final String CONSUMER = "http://demo17.huygens.knaw.nl/apis-authorization-server/oauth2/authorize";

    private static final Logger log = LoggerFactory.getLogger(SAMLResource.class);

    @GET
    @Path("/auth")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAuthenticationRequest() throws MarshallingException, IOException {
        final AuthnRequest authnRequest = buildAuthnRequestObject();
        final String encodedRequestMessage = encodeAuthnRequest(authnRequest);

        return encodedRequestMessage;
    }

    private AuthnRequest buildAuthnRequestObject() {
        // Issuer object
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("http://oaaas-dev");

        // NameIDPolicy
        NameIDPolicy nameIDPolicy = new NameIDPolicyBuilder().buildObject();
        nameIDPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        nameIDPolicy.setSPNameQualifier("Issuer");
        nameIDPolicy.setAllowCreate(true);

        // AuthnContextClass
        AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
        authnContextClassRef
                .setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

        // AuthnContext
        RequestedAuthnContext requestedAuthnContext = new RequestedAuthnContextBuilder().buildObject();
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

        // AuthnRequest
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setForceAuthn(false);
        authnRequest.setIsPassive(false);
        authnRequest.setDestination("http://demo17.huygens.knaw.nl/mujina-idp/SingleSignOnService");
        authnRequest.setIssueInstant(new DateTime()); // aka "now"
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        authnRequest.setAssertionConsumerServiceURL(CONSUMER);
        authnRequest.setIssuer(issuer);
        authnRequest.setNameIDPolicy(nameIDPolicy);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);
        authnRequest.setID(UUID.randomUUID().toString());
        authnRequest.setVersion(SAMLVersion.VERSION_20);

        return authnRequest;
    }

    private String encodeAuthnRequest(AuthnRequest authnRequest) throws MarshallingException, IOException {
        Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(authnRequest);
        Element authDOM = marshaller.marshall(authnRequest);

        StringWriter sw = new StringWriter();
        XMLHelper.writeNode(authDOM, sw);
        String requestMessage = sw.toString();
        log.debug("requestMessage: {}", requestMessage);

        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
        dos.write(requestMessage.getBytes());
        dos.close();

        String encodedRequestMessage = Base64.encodeBytes(baos.toByteArray(), Base64.DONT_BREAK_LINES);

        return encodedRequestMessage;
    }
}
