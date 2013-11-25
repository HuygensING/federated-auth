package nl.knaw.huygens.security.core.model;

import java.util.UUID;

public interface SecuritySession {

  UUID getId();

  HuygensPrincipal getOwner();

}