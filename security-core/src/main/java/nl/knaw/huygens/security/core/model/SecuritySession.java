package nl.knaw.huygens.security.core.model;

import org.joda.time.DateTime;

public interface SecuritySession {

  HuygensPrincipal getOwner();

  DateTime getExpiresOn();

}