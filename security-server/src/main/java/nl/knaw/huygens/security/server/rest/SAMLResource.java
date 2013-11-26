package nl.knaw.huygens.security.server.rest;

import static nl.knaw.huygens.security.core.rest.API.REDIRECT_URL_HTTP_PARAM;
import static nl.knaw.huygens.security.core.rest.API.SESSION_ID_HTTP_PARAM;
import static nl.knaw.huygens.security.server.saml2.SAMLEncoder.deflateAndBase64Encode;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import java.net.URI;
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
import com.google.inject.Inject;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.HuygensSession;
import nl.knaw.huygens.security.server.model.LoginRequest;
import nl.knaw.huygens.security.server.saml2.SAML2PrincipalAttributesMapper;
import nl.knaw.huygens.security.server.service.LoginRequestManager;
import nl.knaw.huygens.security.server.service.SessionManager;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
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
    public static final String SURF_IDP_SSO_URL = "https://engine.surfconext.nl/authentication/idp/single-sign-on";

    public static final String HUYGENS_SECURITY_URL = "https://secure.huygens.knaw.nl";

    public static final String SAML_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

    private static final String RES_SURFCONEXT_CERT = "/certificates/surfconext.cert";

    private static final String MSG_MISSING_REDIRECT_URI = "Missing parameter '" + REDIRECT_URL_HTTP_PARAM + "'";

    private static final String MSG_MISSING_SAML_RESPONSE = "Missing parameter 'SAMLResponse' (null or empty)";

    private static final String MSG_MISSING_RELAY_STATE = "Missing parameter 'RelayState' (null or empty)";

    private static final String MSG_ILLEGAL_RELAY_STATE = "Illegal RelayState (not a UUID)";

    private static final String MSG_UNKNOWN_RELAY_STATE = "Unknown RelayState";

    private static final Logger log = LoggerFactory.getLogger(SAMLResource.class);

    private final SessionManager sessionManager;
    private final LoginRequestManager loginManager;

    @Inject
    public SAMLResource(SessionManager sessionManager, LoginRequestManager loginManager) {
        this.sessionManager = sessionManager;
        this.loginManager = loginManager;
        log.debug("pending login request count: {}", loginManager.getPendingLoginRequestCount());
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Response getAuthenticationRequest(@QueryParam(REDIRECT_URL_HTTP_PARAM) URI redirectURI)
            throws MessageEncodingException {
        if (redirectURI == null) {
            log.warn(MSG_MISSING_REDIRECT_URI);
            return Response.status(Status.BAD_REQUEST).entity(MSG_MISSING_REDIRECT_URI).build();
        }

        final LoginRequest loginRequest = new LoginRequest(redirectURI);
        loginManager.addLoginRequest(loginRequest);

        UriBuilder uriBuilder = UriBuilder.fromPath(SURF_IDP_SSO_URL);
        uriBuilder.queryParam("RelayState", loginRequest.getRelayState());
        uriBuilder.queryParam("SAMLRequest", deflateAndBase64Encode(buildAuthnRequestObject()));

        /* 3.4.5.1, HTTP and Caching Considerations:
         * HTTP proxies and the user agent intermediary should not cache SAML protocol messages.
         * To ensure this, the following rules SHOULD be followed.
         * When returning SAML protocol messages using HTTP 1.1, HTTP responders SHOULD:
         * Include a Cache-Control header field set to "no-cache, no-store".
         * Include a Pragma header field set to "no-cache".
         */
        return Response.seeOther(uriBuilder.build()) //
                .header("Cache-Control", "no-cache, no-store") //
                .header("Pragma", "no-cache") //
                .build();
    }

    @POST
    @Path("acs")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response assertionConsumerService(@FormParam("SAMLResponse") String samlResponseParam,
                                             @FormParam("RelayState") String relayStateParam) {
        log.trace("assertionConsumerService: RelayState={}, SAMLResponse={}", relayStateParam, samlResponseParam);

        if (Strings.isNullOrEmpty(samlResponseParam)) {
            log.warn(MSG_MISSING_SAML_RESPONSE);
            return Response.status(Status.BAD_REQUEST).entity(MSG_MISSING_SAML_RESPONSE).build();
        }

        if (Strings.isNullOrEmpty(relayStateParam)) {
            log.warn(MSG_MISSING_RELAY_STATE);
            return Response.status(Status.BAD_REQUEST).entity(MSG_MISSING_RELAY_STATE).build();
        }

        final UUID relayState;
        try {
            relayState = UUID.fromString(relayStateParam);
        } catch (IllegalArgumentException e) {
            log.warn(MSG_ILLEGAL_RELAY_STATE);
            return Response.status(Status.BAD_REQUEST).entity(MSG_ILLEGAL_RELAY_STATE).build();
        }

        final LoginRequest loginRequest = loginManager.removeLoginRequest(relayState);
        if (loginRequest == null) {
            log.warn(MSG_UNKNOWN_RELAY_STATE);
            return Response.status(Status.BAD_REQUEST).entity(MSG_UNKNOWN_RELAY_STATE).build();
        }
        log.debug("Found pending login request: [{}]", loginRequest);

        String samlResponse = new String(Base64.decode(samlResponseParam));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        Assertion assertion = null;
        String statusCode = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(samlResponse.getBytes()));
            Element root = doc.getDocumentElement();
            UnmarshallerFactory uf = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = uf.getUnmarshaller(root);
            XMLObject responseXmlObj = unmarshaller.unmarshall(root);
            org.opensaml.saml2.core.Response resp = (org.opensaml.saml2.core.Response) responseXmlObj;

            assertion = resp.getAssertions().get(0);
            verify(assertion.getSignature());

            statusCode = resp.getStatus().getStatusCode().getValue();
            log.trace("statusCode: {}", statusCode);
        } catch (ParserConfigurationException e) {
            log.warn("ParserConfigurationException: {}", e.getMessage());
        } catch (SAXException e) {
            log.warn("SAXException: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("IOException: {}", e.getMessage());
        } catch (UnmarshallingException e) {
            log.warn("UnmarshallingException: {}", e.getMessage());
        }

        final SAML2PrincipalAttributesMapper mapper = new SAML2PrincipalAttributesMapper();
        final HuygensPrincipal huygensPrincipal = mapper.map(assertion).getHuygensPrincipal();

        final UriBuilder uriBuilder = UriBuilder.fromUri(loginRequest.getRedirectURI());
        if (SAML_SUCCESS.equals(statusCode)) {
            log.debug("Login successful: [{}]", huygensPrincipal);
            final HuygensSession session = createSession(huygensPrincipal);
            sessionManager.addSession(session);
            uriBuilder.queryParam(SESSION_ID_HTTP_PARAM, session.getId());
        } else {
            log.warn("Login failed: [{}] ([{}])", huygensPrincipal, statusCode);
        }

        final URI uri = uriBuilder.build();
        log.debug("Redirecting to: [{}]", uri);
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("/expire")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeExpiredLoginRequests() {
        return Response.ok(loginManager.removeExpiredRequests()).build();
    }

    private HuygensSession createSession(final HuygensPrincipal huygensPrincipal) {
        return new HuygensSession() {
            private final UUID hsid = UUID.randomUUID();
            @Override
            public UUID getId() {
                return hsid;
            }

            @Override
            public HuygensPrincipal getOwner() {
                return huygensPrincipal;
            }
        };
    }

    private void verify(Signature signature) {
        log.trace("Verifying signature: {}", signature);

        try {
            new SAMLSignatureProfileValidator().validate(signature);
            log.trace("Valid signature profile");

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory
                    .generateCertificate(new FileInputStream(getCertFile()));
            PublicKey x509PublicKey = certificate.getPublicKey();
            log.trace("x509PublicKey.algorithm: {}", x509PublicKey.getAlgorithm());
            log.trace("x509PublicKey.format: {}", x509PublicKey.getFormat());

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509PublicKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance(x509PublicKey.getAlgorithm());
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            BasicX509Credential cred = new BasicX509Credential();
            cred.setPublicKey(publicKey);

            SignatureValidator signatureValidator = new SignatureValidator(cred);
            signatureValidator.validate(signature);
            log.trace("Valid signature");
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
        final URL resource = classLoader.getResource(RES_SURFCONEXT_CERT);
        log.trace("getCertFile: resource={}", resource);
        return new File(resource.toURI());
    }

    private AuthnRequest buildAuthnRequestObject() {
        // Issuer object
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(HUYGENS_SECURITY_URL);

        // NameIDPolicy
        NameIDPolicy nameIDPolicy = new NameIDPolicyBuilder().buildObject();
        nameIDPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        nameIDPolicy.setSPNameQualifier("Issuer");
        nameIDPolicy.setAllowCreate(true);

        // AuthnRequest
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setForceAuthn(false);
        authnRequest.setIsPassive(false);
        authnRequest.setDestination(SURF_IDP_SSO_URL);
        authnRequest.setIssueInstant(new DateTime()); // aka "now"
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        authnRequest.setIssuer(issuer);
        authnRequest.setNameIDPolicy(nameIDPolicy);
        authnRequest.setID(UUID.randomUUID().toString());
        authnRequest.setVersion(SAMLVersion.VERSION_20);

        return authnRequest;
    }

}
