package nl.knaw.huygens.security.server.rest;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static nl.knaw.huygens.security.core.rest.API.REDIRECT_URL_HTTP_PARAM;
import static nl.knaw.huygens.security.core.rest.API.SESSION_ID_HTTP_PARAM;
import static nl.knaw.huygens.security.server.Roles.LOGIN_MANAGER;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.jersey.api.NotFoundException;
import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.server.BadRequestException;
import nl.knaw.huygens.security.server.model.LoginRequest;
import nl.knaw.huygens.security.server.model.ServerSession;
import nl.knaw.huygens.security.server.saml2.SAML2PrincipalAttributesMapper;
import nl.knaw.huygens.security.server.saml2.SAMLEncoder;
import nl.knaw.huygens.security.server.service.LoginService;
import nl.knaw.huygens.security.server.service.SessionService;
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

@Path("/saml2") // Part of the SURFconext contract -- Thou shalt not change this path!
public class SAMLResource {
    public static final String SURF_IDP_SSO_URL_PRE_HEARTBLEED //
            = "https://engine.surfconext.nl/authentication/idp/single-sign-on";

    public static final String SURF_IDP_SSO_URL_POST_HEARTBLEED //
            = "https://engine.surfconext.nl/authentication/idp/single-sign-on/key:20140505";

    public static final String SURF_IDP_SSO_URL = SURF_IDP_SSO_URL_POST_HEARTBLEED;

    public static final String HUYGENS_SECURITY_URL = "https://secure.huygens.knaw.nl";

    private static final String SAML_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

    private static final String RES_SURFCONEXT_CERT = "/certificates/surfconext.cert";

    private static final String MSG_ILLEGAL_RELAY_STATE = "Illegal RelayState (not a UUID)";

    private static final String MSG_MISSING_RELAY_STATE = "Missing parameter 'RelayState' (null or empty)";

    private static final String MSG_MISSING_SAML_RESPONSE = "Missing parameter 'SAMLResponse' (null or empty)";

    private static final String MSG_UNKNOWN_RELAY_STATE = "Login request unknown or expired. Have a nice day.";

    private static final String QUERY_PARAM_RELAY_STATE = "RelayState";

    private static final String QUERY_PARAM_SAML_REQUEST = "SAMLRequest";

    private static final String QUERY_PARAM_SAML_RESPONSE = "SAMLResponse";

    private static final Logger log = LoggerFactory.getLogger(SAMLResource.class);

    private final SessionService sessionService;

    private final LoginService loginService;

    private final SAMLEncoder samlEncoder;

    @Inject
    public SAMLResource(SessionService sessionService, LoginService loginService, SAMLEncoder samlEncoder) {
        this.sessionService = checkNotNull(sessionService);
        this.loginService = checkNotNull(loginService);
        this.samlEncoder = checkNotNull(samlEncoder);
    }

    @POST
    @Path("/login")
    @Produces(TEXT_HTML)
    public Response requestLogin(@FormParam(REDIRECT_URL_HTTP_PARAM) URI redirectURI) throws MessageEncodingException {
        log.debug("Login request, redirectURI=[{}]", redirectURI);

        final UUID relayState = loginService.createLoginRequest(redirectURI);
        log.debug("Relay state: [{}]", relayState);

        final String request = samlEncoder.deflateAndBase64Encode(buildAuthnRequestObject());
        log.debug("SAML request: [{}]", request);

        UriBuilder uriBuilder = UriBuilder.fromPath(SURF_IDP_SSO_URL_POST_HEARTBLEED);
        uriBuilder.queryParam(QUERY_PARAM_RELAY_STATE, relayState);
        uriBuilder.queryParam(QUERY_PARAM_SAML_REQUEST, request);

        /* 3.4.5.1, HTTP and Caching Considerations:
         * HTTP proxies and the user agent intermediary should not cache SAML protocol messages.
         * To ensure this, the following rules SHOULD be followed.
         * When returning SAML protocol messages using HTTP 1.1, HTTP responders SHOULD:
         * Include a Cache-Control header field set to "no-cache, no-store".
         * Include a Pragma header field set to "no-cache".
         */
        return Response.seeOther(uriBuilder.build()) //
                .header("Cache-Control", "no-cache") //
                .header("Cache-Control", "no-store") //
                .header("Pragma", "no-cache") //
                .build();
    }

    @POST
    @Path("/acs")  // Part of the SURFconext contract -- Thou shalt not change this path!
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_HTML)
    public Response consumeAssertion(@FormParam(QUERY_PARAM_SAML_RESPONSE) String samlResponseParam,
                                     @FormParam(QUERY_PARAM_RELAY_STATE) String relayStateParam) {
        log.trace("consumeAssertion: RelayState={}, SAMLResponse={}", relayStateParam, samlResponseParam);

        if (Strings.isNullOrEmpty(samlResponseParam)) {
            log.warn(MSG_MISSING_SAML_RESPONSE);
            throw new BadRequestException(MSG_MISSING_SAML_RESPONSE);
        }

        if (Strings.isNullOrEmpty(relayStateParam)) {
            log.warn(MSG_MISSING_RELAY_STATE);
            throw new BadRequestException(MSG_MISSING_RELAY_STATE);
        }

        final UUID relayState;
        try {
            relayState = UUID.fromString(relayStateParam);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(MSG_ILLEGAL_RELAY_STATE);
        }

        final LoginRequest loginRequest = loginService.removeLoginRequest(relayState);
        if (loginRequest == null) {
            throw new NotFoundException(MSG_UNKNOWN_RELAY_STATE);
        }

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
            final ServerSession session = new ServerSession(huygensPrincipal);
            try {
                uriBuilder.queryParam(SESSION_ID_HTTP_PARAM, session.getId());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unable to append query parameter to redirectURI");
            }
            sessionService.addSession(session);
        }
        else {
            log.warn("Login failed: [{}] ([{}])", huygensPrincipal, statusCode);
        }

        final URI uri;
        try {
            uri = uriBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("RedirectURI does not allow session as query parameter");
        } catch (UriBuilderException e) {
            throw new BadRequestException("Failed to build redirect URL from redirectURI and sessionId");
        }

        log.debug("Redirecting to: [{}]", uri);
        return Response.seeOther(uri).build();
    }

    @POST
    @RolesAllowed(LOGIN_MANAGER)
    @Path("/purge")
    @Produces(APPLICATION_JSON)
    public Response purgeExpiredLoginRequests() {
        return Response.ok(loginService.purgeExpiredRequests()).build();
    }

    @GET
    @RolesAllowed(LOGIN_MANAGER)
    @Path("/requests")
    @Produces(APPLICATION_JSON)
    public Collection<LoginRequest> getLoginRequests() {
        return loginService.getPendingLoginRequests();
    }

    @DELETE
    @RolesAllowed(LOGIN_MANAGER)
    @Path("/requests/{id}")
    @Produces(APPLICATION_JSON)
    public Response removeLoginRequest(@PathParam("id") String id) {
        final UUID relayState;
        try {
            relayState = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Illegal relayState (not a UUID): " + id);
        }

        final LoginRequest request = loginService.removeLoginRequest(relayState);
        if (request == null) {
            throw new NotFoundException("Login request not found: " + relayState);
        }

        return Response.ok(request).build();
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
        } catch (FileNotFoundException e) {
            log.warn("Certificate file not found: {}", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            log.debug("No RSA KeyFactory instance to be found: {}", e.getMessage());
        } catch (InvalidKeySpecException e) {
            log.debug("Invalid Key Spec: {}", e.getMessage());
        }
    }

    private File getCertFile() {
//        final ClassLoader classLoader = getClass().getClassLoader();
//        final URL resource = classLoader.getResource(RES_SURFCONEXT_CERT);
//        return new File(resource.toURI());
        final URL url = getClass().getResource(RES_SURFCONEXT_CERT);
        return new File(url.getFile());
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
