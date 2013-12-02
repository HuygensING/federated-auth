package nl.knaw.huygens.security.core.model;

import java.util.UUID;

public interface HuygensSession {

  UUID getId();

  HuygensPrincipal getOwner();

}