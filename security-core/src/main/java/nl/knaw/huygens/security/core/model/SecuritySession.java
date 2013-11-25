package nl.knaw.huygens.security.core.model;

import java.util.UUID;

import org.joda.time.DateTime;

public interface SecuritySession {

  UUID getId();

  HuygensPrincipal getOwner();

  DateTime getExpiresOn();

}