package nl.knaw.huygens.security.server.model;

import javax.security.auth.Destroyable;
import javax.security.auth.Refreshable;

import nl.knaw.huygens.security.core.model.HuygensSession;

public interface ServerSession extends HuygensSession, Refreshable, Destroyable {
}
