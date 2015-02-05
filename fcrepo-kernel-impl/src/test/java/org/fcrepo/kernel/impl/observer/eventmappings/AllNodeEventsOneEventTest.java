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
package org.fcrepo.kernel.impl.observer.eventmappings;

import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static org.jgroups.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.observer.FedoraEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.stream.Stream;

/**
 * <p>AllNodeEventsOneEventTest class.</p>
 *
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class AllNodeEventsOneEventTest {

    private static final String TEST_IDENTIFIER1 = randomUUID().toString();

    private static final String TEST_PATH1 = "/test/node1";

    private static final String TEST_IDENTIFIER2 = TEST_IDENTIFIER1;

    private static final String TEST_PATH2 = TEST_PATH1 + "/property";

    private static final String TEST_IDENTIFIER3 = randomUUID().toString();

    private static final String TEST_PATH3 = "/test/node2";

    private final AllNodeEventsOneEvent testMapping = new AllNodeEventsOneEvent();

    @Mock
    private Event mockEvent1, mockEvent2, mockEvent3;

    private  Stream<Event> testStream;

    @Before
    public void setUp() throws RepositoryException {
        when(mockEvent1.getIdentifier()).thenReturn(TEST_IDENTIFIER1);
        when(mockEvent1.getPath()).thenReturn(TEST_PATH1);
        when(mockEvent2.getIdentifier()).thenReturn(TEST_IDENTIFIER2);
        when(mockEvent2.getPath()).thenReturn(TEST_PATH2);
        when(mockEvent3.getIdentifier()).thenReturn(TEST_IDENTIFIER3);
        when(mockEvent3.getPath()).thenReturn(TEST_PATH3);
        when(mockEvent3.getType()).thenReturn(PROPERTY_CHANGED);
        testStream = Stream.of(mockEvent1, mockEvent2, mockEvent3);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get 2 FedoraEvents for 3 input JCR Events, two of which were on the same node!", 2,
                testMapping.apply(testStream).count());
    }

    @Test(expected = RuntimeException.class)
    public void testBadEvent() throws RepositoryException {
        reset(mockEvent1);
        when(mockEvent1.getIdentifier()).thenThrow(new RepositoryException("Expected."));
        testMapping.apply(testStream);
    }

    @Test
    public void testPropertyEvents() {
        final Stream<FedoraEvent> result = testMapping.apply(testStream);
        assertNotNull(result);
        assertTrue("Result is empty!", result.findAny().isPresent());
    }

    public void testProperty() {
        final Stream<FedoraEvent> result = testMapping.apply(testStream);
        assertTrue("Third mock event was not found!", result
                .anyMatch(e -> TEST_IDENTIFIER3.equals(e.getIdentifier()) && e.getProperties().size() == 1));
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testError() throws RepositoryException {
        when(mockEvent3.getPath()).thenThrow(new RepositoryException("expected"));

        final Stream<FedoraEvent> result = testMapping.apply(testStream);
        assertNotNull(result);
        result.count();
    }

}
