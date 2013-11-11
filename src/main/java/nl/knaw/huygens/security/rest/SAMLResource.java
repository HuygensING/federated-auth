package nl.knaw.huygens.security.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import com.google.common.base.Strings;
import nl.knaw.huygens.security.saml2.SAMLEncoder;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HTTPTransportUtils;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Path("/saml2")
public class SAMLResource {
    public static final String IDP = "https://engine.surfconext.nl/authentication/idp/single-sign-on";

    private static final String CONSUMER = "http://demo17.huygens.knaw.nl/apis-authorization-server/oauth2/authorize";

    private static final Logger log = LoggerFactory.getLogger(SAMLResource.class);

    private static final SAMLEncoder SAML_ENCODER = new SAMLEncoder();

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Response getAuthenticationRequest() throws MessageEncodingException {
        final AuthnRequest message = buildAuthnRequestObject();
        final String defB64Message = SAML_ENCODER.deflateAndBase64Encode(message);
        final String urlEncodedMsg = HTTPTransportUtils.urlEncode(defB64Message);  // URL encoding done by UriBuilder ?
        UriBuilder uriBuilder = UriBuilder.fromPath(IDP);
        uriBuilder.queryParam("SAMLRequest", defB64Message);
        uriBuilder.queryParam("RelayState", "0xDeadBeef");

        /* HTTP proxies and the user agent intermediary should not cache SAML protocol messages.
         * To ensure this, the following rules SHOULD be followed.
         * When returning SAML protocol messages using HTTP 1.1, HTTP responders SHOULD:
         * Include a Cache-Control header field set to "no-cache, no-store".
         * Include a Pragma header field set to "no-cache".
         */
        return Response.seeOther(uriBuilder.build()) //
                .header("Cache-Control", "no-cache, no-store") // See: 3.4.5.1, HTTP and Caching Considerations
                .header("Pragma", "no-cache")//
                .build();
    }

    @POST
    @Path("acs")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response assertionConsumerService(@FormParam("SAMLResponse") String base64SamlResponse,
                                             @FormParam("RelayState") String relayState) {
        log.debug("assertionConsumerService: RelayState={}, SAMLResponse={}", relayState, base64SamlResponse);

        if (Strings.isNullOrEmpty(base64SamlResponse)) {
            log.warn("Bad request: invalid SAMLResponse parameter");
            return Response.status(Status.BAD_REQUEST) //
                    .entity("Missing parameter 'SAMLResponse' (null or empty)") //
                    .build();
        }

        if (Strings.isNullOrEmpty(relayState)) {
            log.warn("Missing (null or empty) RelayState parameter");
            // let's allow this for now, until we actually need to relate information back to the original request.
        }

        String samlResponse = new String(Base64.decode(base64SamlResponse));
        log.debug("decoded samlReponse: {}", samlResponse);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        StringBuilder attrs = new StringBuilder();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(samlResponse.getBytes()));
            Element root = doc.getDocumentElement();
            UnmarshallerFactory uf = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = uf.getUnmarshaller(root);
            XMLObject responseXmlObj = unmarshaller.unmarshall(root);
            org.opensaml.saml2.core.Response resp = (org.opensaml.saml2.core.Response) responseXmlObj;

            Assertion assertion = resp.getAssertions().get(0);
            verify(assertion.getSignature());

            String subject = assertion.getSubject().getNameID().getValue();
            log.debug("subject: {}", subject);

            String issuer = assertion.getIssuer().getValue();
            log.debug("issuer: {}", issuer);

            for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
                for (Attribute attribute : attributeStatement.getAttributes()) {
                    final String name = attribute.getName();
                    log.debug("attribute.name: {}", name);
                    for (XMLObject xmlObject : attribute.getAttributeValues()) {
                        final XSAny xsAny = (XSAny) xmlObject;
                        final String text = xsAny.getTextContent();
                        log.debug("+- attribute.value: {} (hasChildren: {})", text, xsAny.hasChildren());
                        for (XMLObject child : xsAny.getOrderedChildren()) {
                            log.debug("   +- child: {}", child);
                            NameID nameID = (NameID) child;
                            log.debug("   +- nameID.format: {}", nameID.getFormat());
                            log.debug("   +- nameID.value: {}", nameID.getValue());
                        }

                        if ("urn:mace:dir:attribute-def:mail".equals(name)) {
                            attrs.append("mail: [").append(text).append("]\n");
                        }
                        else if ("urn:mace:dir:attribute-def:displayName".equals(name)) {
                            attrs.append("displayName: [").append(text).append("]\n");
                        }
                    }
                }
            }

            String statusCode = resp.getStatus().getStatusCode().getValue();
            log.debug("statusCode: {}", statusCode);
        } catch (ParserConfigurationException e) {
            log.warn("ParserConfigurationException: {}", e.getMessage());
        } catch (SAXException e) {
            log.warn("SAXException: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("IOException: {}", e.getMessage());
        } catch (UnmarshallingException e) {
            log.warn("UnmarshallingException: {}", e.getMessage());
        }


        // todo: retrieve original URI based on relayState
        return Response.ok("Welcome!\n\n" + attrs.toString()).build();
    }

    private void verify(Signature signature) {
        log.debug("Verifying signature: {}", signature);

        try {
            new SAMLSignatureProfileValidator().validate(signature);
            log.debug("Valid signature profile");

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory
                    .generateCertificate(new FileInputStream(getCertFile()));
            PublicKey x509PublicKey = certificate.getPublicKey();
            log.debug("x509PublicKey.algorithm: {}", x509PublicKey.getAlgorithm());
            log.debug("x509PublicKey.format: {}", x509PublicKey.getFormat());

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509PublicKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance(x509PublicKey.getAlgorithm());
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            BasicX509Credential cred = new BasicX509Credential();
            cred.setPublicKey(publicKey);

            SignatureValidator signatureValidator = new SignatureValidator(cred);
            signatureValidator.validate(signature);
            log.debug("Valid signature");
        } catch (ValidationException e) {
            log.warn("Signature Validation Exception: {}", e.getMessage());
        } catch (CertificateException e) {
            log.warn("CertificateException: {}", e.getMessage());
        } catch (URISyntaxException e) {
            log.warn("URISyntaxException: {}", e.getMessage());
        } catch (FileNotFoundException e) {
            log.warn("Certificate file not found: {}", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            log.debug("No RSA KeyFactory instance to be found: {}", e.getMessage());
        } catch (InvalidKeySpecException e) {
            log.debug("Invalid Key Spec: {}", e.getMessage());
        }
    }

    private File getCertFile() throws URISyntaxException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final URL resource = classLoader.getResource("/certificates/surfconext.cert");
        log.debug("getCertFile: resource={}", resource);
        return new File(resource.toURI());
    }

    /**
     * HTTP proxies and the user agent intermediary should not cache SAML protocol messages. To ensure this,
     * the following rules SHOULD be followed.
     * <p/>
     * When returning SAML protocol messages using HTTP 1.1, HTTP responders SHOULD:
     * Include a Cache-Control header field set to "no-cache, no-store".
     * Include a Pragma header field set to "no-cache".
     * <p/>
     * There are no other restrictions on the use of HTTP headers.
     */
    private CacheControl getNoCachingControl() {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        return cc;
    }

    private AuthnRequest buildAuthnRequestObject() {
        // Issuer object
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("https://secure.huygens.knaw.nl");

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
        authnRequest.setDestination("https://engine.surfconext.nl/authentication/idp/single-sign-on");
        authnRequest.setIssueInstant(new DateTime()); // aka "now"
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
//        authnRequest.setAssertionConsumerServiceURL(CONSUMER);
        authnRequest.setIssuer(issuer);
        authnRequest.setNameIDPolicy(nameIDPolicy);
//        authnRequest.setRequestedAuthnContext(requestedAuthnContext);
        authnRequest.setID(UUID.randomUUID().toString());
        authnRequest.setVersion(SAMLVersion.VERSION_20);

        return authnRequest;
    }

}
