package nl.knaw.huygens.security.client;

/*
 * #%L
 * Security Client
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
