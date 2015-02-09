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

package org.fcrepo.kernel.impl.rdf.impl;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.utils.UncheckedPredicate;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isBlankNode;
import static org.fcrepo.kernel.utils.Streams.fromIterator;
import static org.fcrepo.kernel.utils.UncheckedFunction.uncheck;

/**
 * Embed all blank nodes in the RDF stream
 *
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class BlankNodeRdfContext extends NodeRdfContext {

    private static final List<Integer> referencePropertyTypes = asList(PATH, REFERENCE, WEAKREFERENCE);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public BlankNodeRdfContext(final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(getBlankNodesIterator().flatMap(node -> {
            final FedoraResource res = nodeConverter.convert(node);

            return res.getTriples(idTranslator, of(TypeRdfContext.class,
                    PropertiesRdfContext.class,
                    BlankNodeRdfContext.class));
        }
                ));
    }

    private Stream<Node> getBlankNodesIterator() throws RepositoryException {
        final Iterator<Property> propertiesIterator = resource().getNode().getProperties();
        final Stream<Property> references = fromIterator(propertiesIterator).filter(filterReferenceProperties);
        return new PropertyValueStream(references).map(getNodesForValue).filter(isBlankNode);
    }

    private static final Predicate<Property> filterReferenceProperties = UncheckedPredicate
            .uncheck(p -> referencePropertyTypes.contains(p.getType()));

    private final Function<Value, Node> getNodesForValue = uncheck(v ->
            v.getType() == PATH ? session().getNode(v.getString()) : session().getNodeByIdentifier(v.getString()));
}