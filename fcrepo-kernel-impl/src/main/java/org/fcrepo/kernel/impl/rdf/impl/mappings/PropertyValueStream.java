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
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import static org.fcrepo.kernel.utils.Streams.fromIterator;

import org.fcrepo.kernel.utils.UncheckedFunction;
import org.fcrepo.kernel.utils.iterators.SpliteratorStream;

import javax.jcr.Property;
import javax.jcr.Value;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Iterate over all the values in a property or list of properties
 *
 * @author cabeer
 * @author ajs6f
 */
public class PropertyValueStream extends SpliteratorStream<Value, PropertyValueStream> {

    /**
     * Iterate through multiple properties' values
     * @param properties
     */
    public PropertyValueStream(final Iterator<Property> properties) {
        super(fromIterator(properties).flatMap(fanout));
    }

    /**
     * Iterate through multiple properties' values
     * @param properties
     */
    public PropertyValueStream(final Stream<Property> properties) {
        super(properties.flatMap(fanout));
    }

    /**
     * Iterate through a property's values
     * @param properties
     */
    public PropertyValueStream(final Property property) {
        super(fanout.apply(property));
    }

    public PropertyValueStream(final Spliterator<? extends Value> elements) {
        super(elements);
    }

    private static Function<Property, Stream<Value>> fanout = UncheckedFunction.uncheck(p ->
            p.isMultiple() ? Arrays.stream(p.getValues()) : Stream.of(p.getValue()));

    @Override
    public PropertyValueStream withThisContext(final Spliterator<? extends Value> elements) {
        return new PropertyValueStream(elements);
    }
}
