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
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.RdfLexicon.DESCRIBES;
import static org.fcrepo.kernel.RdfLexicon.DESCRIBED_BY;

import java.util.stream.Stream;

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/16/14
 */
public class ContentRdfContext extends NodeRdfContext {
    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     */
    public ContentRdfContext(final FedoraResource resource,
                             final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);
    }

    @Override
    public Stream<Triple> applyThrows(final javax.jcr.Node unused) {

        if (resource() instanceof NonRdfSourceDescription) {
            // if this resource describes a bitstream
            final FedoraResource contentNode = ((NonRdfSourceDescription) resource()).getDescribedResource();
            final Node contentSubject = translator().reverse().convert(contentNode).asNode();
            // add triples representing parent-to-content-child relationship
            return Stream.of(create(topic(), DESCRIBES.asNode(), contentSubject));
        }
        if (resource() instanceof FedoraBinary) {
            // if this resource is a bitstream
            final FedoraResource description = ((FedoraBinary) resource()).getDescription();
            final Node descriptionUri = translator().reverse().convert(description).asNode();
            return Stream.of(create(topic(), DESCRIBED_BY.asNode(), descriptionUri));
        }
        return empty();
    }

}
