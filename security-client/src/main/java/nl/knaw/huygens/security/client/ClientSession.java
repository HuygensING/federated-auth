package nl.knaw.huygens.security.client;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.HuygensSession;
import org.joda.time.DateTime;

public class ClientSession implements HuygensSession {
  private UUID id;
  private HuygensPrincipal owner;
  private DateTime expiresAt;

  @Override
  public HuygensPrincipal getOwner() {
    return owner;
  }

    @Override
    public DateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
  public UUID getId() {
    return id;
  }

}
