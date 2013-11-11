package nl.knaw.huygens.security.saml2;

import static com.google.common.base.Charsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SAMLEncoder {
    private static final Logger log = LoggerFactory.getLogger(SAMLEncoder.class);

    public static String deflateAndBase64Encode(SAMLObject message) throws MessageEncodingException {
        log.debug("Deflating and Base64 encoding SAML message");
        String messageStr = XMLHelper.nodeToString(marshallMessage(message));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true); // RFC1951 compliant deflater
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);

        try {
            deflaterOutputStream.write(messageStr.getBytes(UTF_8));
            deflaterOutputStream.finish();
        } catch (IOException e) {
            throw new MessageEncodingException("Unable to DEFLATE and Base64 encode SAML message", e);
        }

        return Base64.encodeBytes(byteArrayOutputStream.toByteArray(), Base64.DONT_BREAK_LINES);
    }

    private static Element marshallMessage(XMLObject message) throws MessageEncodingException {
        log.debug("Marshalling message");
        try {
            Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(message);
            if (marshaller == null) {
                log.error("Unable to marshall message, no marshaller registered for message object: {}", message
                        .getElementQName());
                throw new MessageEncodingException("Unable to marshall message");
            }

            Element messageElement = marshaller.marshall(message);

            if (log.isTraceEnabled()) {
                log.trace("Marshalled message into DOM representation:\n{}", XMLHelper.nodeToString(messageElement));
            }

            return messageElement;
        } catch (MarshallingException e) {
            log.error("Error marshalling message to its DOM representation", e);
            throw new MessageEncodingException("Error marshalling message to its DOM representation", e);
        }
    }
}
