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

import static com.google.common.base.Charsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import com.google.inject.Singleton;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@Singleton
public class SAMLEncoder {
    private static final Logger log = LoggerFactory.getLogger(SAMLEncoder.class);

    public SAMLEncoder() {
        log.debug("SAMLEncoder created");
        try {
            log.info("Bootstrapping OpenSAML library");
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("Unable to bootstrap OpenSAML library");
        }
    }

    public String deflateAndBase64Encode(SAMLObject message) throws MessageEncodingException {
        log.trace("Deflating and Base64 encoding SAML message");
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

    private Element marshallMessage(XMLObject message) throws MessageEncodingException {
        log.trace("Marshalling message");
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
