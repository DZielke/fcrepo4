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

import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.stream.Stream.builder;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.RDF_SOURCE;

import java.util.stream.Stream;

import javax.jcr.Node;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/16/14
 */
public class LdpRdfContext extends NodeRdfContext {

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     */
    public LdpRdfContext(final FedoraResource resource,
                         final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);
    }

    @Override
    public Stream<Triple> applyThrows(final Node unused) {
        final Stream.Builder<Triple> triples = builder();

        triples.add(create(topic(), type.asNode(), RDF_SOURCE.asNode()));
        if (resource() instanceof Container) {
            triples.add(create(topic(), type.asNode(), CONTAINER.asNode()));
            if (!resource().hasType(FEDORA_CONTAINER)) {
                triples.add(create(topic(), type.asNode(), BASIC_CONTAINER.asNode()));
            }
        }
        return triples.build();
    }
}
