package nl.knaw.huygens.security.client;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.SecuritySession;

import org.joda.time.DateTime;

public class ClientSession implements SecuritySession {
  private UUID id;
  private DateTime expiresOn;
  private HuygensPrincipal owner;

  @Override
  public HuygensPrincipal getOwner() {
    return owner;
  }

  @Override
  public DateTime getExpiresOn() {
    return expiresOn;
  }

  @Override
  public UUID getId() {
    return id;
  }

}
