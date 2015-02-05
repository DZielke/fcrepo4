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

import static org.fcrepo.kernel.observer.FedoraEvent.PROPERTY_EVENT_TYPES;
import static org.fcrepo.kernel.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import javax.jcr.observation.Event;

import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.observer.eventmappings.InternalExternalEventMapper;

import org.slf4j.Logger;

/**
 * Maps all JCR {@link Event}s concerning one JCR node to one
 * {@link FedoraEvent}. Adds the types of those JCR events together to calculate
 * the final type of the emitted FedoraEvent. TODO stop aggregating events in
 * the heap and make this a purely lazy algorithm, if possible
 *
 * @author ajs6f
 * @since Feb 27, 2014
 */
public class AllNodeEventsOneEvent implements InternalExternalEventMapper {

    private final static Logger log = getLogger(AllNodeEventsOneEvent.class);

    @Override
    public Stream<FedoraEvent> apply(final Stream<Event> events) {
        final BinaryOperator<FedoraEvent> combine = (ev1, ev2) -> {
            final Set<Integer> oldTypes = ev1.getTypes();
            oldTypes.forEach(ev2::addType);
            if (oldTypes.stream().anyMatch(PROPERTY_EVENT_TYPES::contains)) {
                final String eventPath = ev1.getPath();
                ev2.addProperty(eventPath.substring(eventPath.lastIndexOf("/") + 1));
            } else {
                log.trace("Not adding non-event property from: {} to {}", ev1, ev2);
            }
            return ev2;
        };

        final Map<String, List<Event>> groupedEvents = events.collect(groupingBy(uncheck(Event::getIdentifier)));
        return groupedEvents.entrySet().stream().map(
                e -> e.getValue().stream().map(FedoraEvent::new).reduce(combine)).map(Optional::get);
    }
}
