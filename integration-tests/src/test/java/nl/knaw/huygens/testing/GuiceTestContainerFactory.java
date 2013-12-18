package nl.knaw.huygens.testing;

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
