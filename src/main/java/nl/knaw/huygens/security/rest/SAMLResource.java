package nl.knaw.huygens.security.rest;

import static com.google.common.base.Charsets.UTF_8;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

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
        return encodeAuthnRequest(buildAuthnRequestObject());
    }

    @POST
    @Path("acs")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response assertionConsumerService(@FormParam("SAMLResponse") String base64SamlResponse,
                                             @FormParam("RelayState") String relayState) {
        log.debug("assertionConsumerService: RelayState={}, SAMLResponse={}", relayState, base64SamlResponse);

        try {
            Inflater inflater = new Inflater(true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InflaterOutputStream ios = new InflaterOutputStream(baos, inflater);
            ios.write(Base64.decode(base64SamlResponse));
            ios.close();
            final String response = new String(baos.toByteArray());
            log.debug("decoded response: {}", response);
        } catch (UnsupportedEncodingException e) {
            log.warn("Unsupported encoding: {}", UTF_8);
        } catch (IOException e) {
            log.warn("IOException: {}", e.getMessage());
        }
        // todo: retrieve original URI based on relayState
        return Response.seeOther(URI.create("/redirect")).build();
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
//        authnRequest.setDestination("http://demo17.huygens.knaw.nl/mujina-idp/SingleSignOnService");
        authnRequest.setDestination("https://engine.surfconext.nl/authentication/idp/single-sign-on");
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

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
        dos.write(requestMessage.getBytes());
        dos.close();
        final byte[] source = baos.toByteArray();
        String base64Message = Base64.encodeBytes(source, Base64.DONT_BREAK_LINES);
        log.debug("base64Message: {}", base64Message);

        final byte[] decoded = Base64.decode(base64Message);
        log.debug("source.len: {}, decoded.len: {}", source.length, decoded.length);
        for (int i = 0; i < source.length; i++) {
            if (source[i] != decoded[i]) {
                log.warn("byte[{}] mismatch", i);
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        Inflater inflater = new Inflater(true);
        InflaterInputStream iis = new InflaterInputStream(bais, inflater);
        baos = new ByteArrayOutputStream();
        byte[] space = new byte[1024];
        int count = iis.read(space);
        while (count != -1) {
            log.debug("Read {} bytes", count);
            baos.write(space, baos.size(), count);
            count = iis.read(space);
        }
        inflater.end();
        iis.close();

        log.debug("inflated: {}", baos.toByteArray());

        String encodedRequestMessage = URLEncoder.encode(base64Message, UTF_8.name());
        log.debug("urlEncodedMessage: {}", encodedRequestMessage);

        return encodedRequestMessage;
    }
}
