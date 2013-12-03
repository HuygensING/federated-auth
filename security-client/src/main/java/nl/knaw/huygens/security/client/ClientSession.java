package nl.knaw.huygens.security.client;

import java.util.UUID;

import nl.knaw.huygens.security.core.model.HuygensPrincipal;
import nl.knaw.huygens.security.core.model.HuygensSession;

public class ClientSession implements HuygensSession {
  private UUID id;
  private HuygensPrincipal owner;

  @Override
  public HuygensPrincipal getOwner() {
    return owner;
  }

  @Override
  public UUID getId() {
    return id;
  }

}
