package nl.knaw.huygens.testing;

/*
 * #%L
 * Security Integration-tests
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

import java.net.URI;

import com.google.inject.Injector;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.LowLevelAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainer;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

public class GuiceTestContainerFactory implements TestContainerFactory {
    private final Injector injector;

    public GuiceTestContainerFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Class<? extends AppDescriptor> supports() {
        return LowLevelAppDescriptor.class;
    }

    @Override
    public TestContainer create(URI baseURI, AppDescriptor appDescriptor) throws IllegalArgumentException {
        if (!(appDescriptor instanceof LowLevelAppDescriptor)) {
            throw new IllegalArgumentException("The application descriptor must be an instance of " +
                    "LowLevelAppDescriptor");
        }

        final LowLevelAppDescriptor lowLevelAppDescriptor = (LowLevelAppDescriptor) appDescriptor;
        return new GuiceTestContainer(baseURI, lowLevelAppDescriptor.getResourceConfig(), injector);
    }

}
