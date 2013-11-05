package nl.knaw.huygens.security.rest;

import static com.google.common.base.Charsets.UTF_8;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.UUID;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import com.google.common.base.Strings;
import nl.knaw.huygens.security.saml2.SAMLEncoder;
import org.joda.time.DateTime;
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
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HTTPTransportUtils;
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/saml2")
public class SAMLResource {
    public static final String IDP = "https://engine.surfconext.nl/authentication/idp/single-sign-on";
//            = "https://idp.diy.surfconext.nl/simplesaml/module.php/core/authenticate.php?as=example-userpass";

    public static final String CONSUMER = "http://demo17.huygens.knaw.nl/apis-authorization-server/oauth2/authorize";

    private static final Logger log = LoggerFactory.getLogger(SAMLResource.class);

    private static final SAMLEncoder SAML_ENCODER = new SAMLEncoder();

    @GET
    @Path("/auth")
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

        try {
            Inflater inflater = new Inflater(true); // Use RFC 1951 compliant inflater
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
        authnRequest.setAssertionConsumerServiceURL(CONSUMER);
        authnRequest.setIssuer(issuer);
        authnRequest.setNameIDPolicy(nameIDPolicy);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);
        authnRequest.setID(UUID.randomUUID().toString());
        authnRequest.setVersion(SAMLVersion.VERSION_20);

        return authnRequest;
    }

}
