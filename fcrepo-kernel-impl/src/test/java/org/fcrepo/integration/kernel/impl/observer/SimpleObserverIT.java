/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.impl.observer;

import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.services.ContainerService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Set;

/**
 * <p>SimpleObserverIT class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/eventing.xml", "/spring-test/repo.xml"})
public class SimpleObserverIT extends AbstractIT {

    private static final Logger log = getLogger(SimpleObserverIT.class);


    private Integer eventBusMessageCount;

    @Inject
    private Repository repository;

    @Inject
    private ContainerService containerService;

    @Inject
    private EventBus eventBus;

    @Test
    public void TestEventBusPublishing() throws RepositoryException {
        final Session se = repository.login();
        try {
            containerService.findOrCreate(se, "/object1");
            se.save();
            containerService.findOrCreate(se, "/object2");
            se.save();
        } finally {
            se.logout();
        }

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Should be two messages, for each time
        // each node becomes a Fedora object

        assertEquals("Where are my messages!?", (Integer) 2,
                eventBusMessageCount);

    }

    @Subscribe
    public void countMessages(final FedoraEvent e) {
        eventBusMessageCount++;
        log.info("Received event from path: {}", e.getPath());
        final Set<String> properties = e.getProperties();
        assertNotNull(properties);
        log.info("Discovered properties: {}", properties);

        final String expected = JCR_NAMESPACE + "mixinTypes";
        assertTrue("Should contain: " + expected + " in " + properties, properties.contains(expected));
    }

    @Before
    public void acquireConnections() {
        eventBusMessageCount = 0;
        eventBus.register(this);
    }

    @After
    public void releaseConnections() {
        eventBus.unregister(this);
    }
}
