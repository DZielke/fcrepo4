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
package org.fcrepo.kernel.impl.services.functions;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Get the internal Modeshape BinaryKey for a binary property
 *
 * @author awoods
 * @author ajs6f
 */
public class GetBinaryKey implements Function<Property, BinaryKey> {

    @Override
    public BinaryKey apply(final Property input) {
        requireNonNull(input, "null cannot have a Binarykey!");
        try {
            return ((BinaryValue) input.getBinary()).getKey();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

}
