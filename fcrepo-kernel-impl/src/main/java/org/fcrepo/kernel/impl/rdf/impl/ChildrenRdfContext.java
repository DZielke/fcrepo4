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

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.utils.Streams.fromIterator;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/16/14
 */
public class ChildrenRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public ChildrenRdfContext(final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        if (resource.getNode().hasNodes()) {
            LOGGER.trace("Found children of this resource.");
            final Stream<FedoraResource> children = fromIterator(resource().getChildren());
            concat(children.map(child2triples));
        }
    }

    private final Function<FedoraResource, Triple> child2triples = child -> {
        final FedoraResource describedThing =
                child instanceof NonRdfSourceDescription ? ((NonRdfSourceDescription) child).getDescribedResource()
                        : child;
        final com.hp.hpl.jena.graph.Node childSubject = translator().reverse().convert(describedThing).asNode();
        LOGGER.trace("Creating triples for child node: {}", child);
        return create(subject(), CONTAINS.asNode(), childSubject);
    };
}
