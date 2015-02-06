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
package org.fcrepo.kernel.services.functions;

import static org.fcrepo.kernel.FedoraJcrTypes.FROZEN_MIXIN_TYPES;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.value2string;
import static org.fcrepo.kernel.utils.Streams.fromIterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.utils.UncheckedPredicate;

/**
 * Base class for matching sets of node types
 * @author armintor@gmail.com
 * @author ajs6f
 *
 */
public abstract class BooleanTypesPredicate implements Predicate<Node> {

    protected final Collection<String> nodeTypes;

    /**
     * Base constructor for function peforming boolean ops on matched node types.
     * @param types the types
     */
    public BooleanTypesPredicate(final String... types) {
        nodeTypes = Arrays.asList(types);
    }

    @Override
    public boolean test(final Node input) {
        try {
            if (isFrozen.test(input) && input.hasProperty(FROZEN_MIXIN_TYPES)) {
                final Stream<Value> values = fromIterator(property2values.apply(input.getProperty(FROZEN_MIXIN_TYPES)));
                return test((int) values.map(value2string).filter(nodeTypes::contains).count());
            }
            return test((int) nodeTypes.stream().filter(UncheckedPredicate.uncheck(n->input.isNodeType(n))).count());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected abstract boolean test(final int matched);

}
