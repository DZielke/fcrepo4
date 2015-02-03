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

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_URI;
import static org.fcrepo.kernel.RdfLexicon.VOAF_VOCABULARY;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.impl.utils.UncheckedFunction.uncheck;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.utils.iterators.RdfStream;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * An {@link RdfStream} with prefix-namespace mappings as well as {@link Triple}s describing those namespaces.
 *
 * @author ajs6f
 * @since Oct 9, 2013
 */
public class NamespaceRdfContext extends RdfStream {

    /**
     * Default constructor. Loads context with RDF describing namespaces in scope in the repository.
     *
     * @param session the session
     * @throws RepositoryException if repository exception occurred
     */
    public NamespaceRdfContext(final Session session) throws RepositoryException {
        super();
        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

        namespaces(stream(namespaceRegistry.getPrefixes()).filter(p -> !p.isEmpty() && !p.equals("jcr")).collect(
                        toMap(p -> p, uncheck(p -> getRDFNamespaceForJcrNamespace(namespaceRegistry.getURI(p))))));

        concat(namespaces().entrySet().stream().<Triple>flatMap(
                e -> {
                    final Node nsSubject = createURI(e.getValue());
                    return asList(create(nsSubject, type.asNode(), VOAF_VOCABULARY.asNode()),
                            create(nsSubject, HAS_NAMESPACE_PREFIX.asNode(), createLiteral(e.getKey())),
                            create(nsSubject, HAS_NAMESPACE_URI.asNode(), createLiteral(e.getValue()))).stream();
                }).iterator());
    }
}
