/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.connector.file;

import org.junit.AfterClass;
import org.infinispan.schematic.document.Document;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.ExtraPropertiesStore;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_DIGEST;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Andrew Woods
 *         Date: 2/3/14
 */
public class FedoraFileSystemConnectorTest {

    private FedoraFileSystemConnector connector;

    private static Path directoryPath;

    private static File tmpFile;
    private static File tmpFile2;

    @Mock
    private NamespaceRegistry mockRegistry;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private DocumentTranslator mockTranslator;

    @Mock
    private NameFactory mockNameFactory;

    @Mock
    private ValueFactories mockValueFactories;

    @Mock
    private ExtraPropertiesStore mockExtraPropertiesStore;

    @Mock
    private Property binaryProperty;

    @Mock
    private BinaryValue binaryValue;

    private ExecutionContext mockContext = new ExecutionContext();

    private static final Logger logger =
        getLogger(FedoraFileSystemConnectorTest.class);

    @BeforeClass
    public static void beforeClass() throws IOException {
        directoryPath = createTempDirectory("fedora-filesystemtest");
        tmpFile =
            createTempFile(directoryPath, "fedora-filesystemtestfile",
                    "txt").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
            outputStream.write("hello".getBytes());
        } catch (final IOException e) {
            logger.error("Error creating: {} - {}", tmpFile.getAbsolutePath(),
                    e.getMessage());
        }

        tmpFile2 =
            createTempFile(directoryPath, "fedora-filesystemtestfile",
                    "txt").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(tmpFile2)) {
            outputStream.write("goodbye".getBytes());
        } catch (final IOException e) {
            logger.error("Error creating: {} - {}", tmpFile2.getAbsolutePath(),
                    e.getMessage());
        }
    }

    @AfterClass
    public static void afterClass() {
        try {
            if ( tmpFile.exists() ) {
                tmpFile.delete();
            }
            if ( tmpFile2.exists() ) {
                tmpFile2.delete();
            }
        } catch (final Exception e) {
            logger.error("Error deleting: " + tmpFile.getAbsolutePath()
                    + " - " + e.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        connector = new FedoraFileSystemConnector();
        setField(connector, "directoryPath", directoryPath.toString());
        setField(connector, "translator", mockTranslator);
        setField(connector, "context", mockContext);
        setField(connector, "extraPropertiesStore", mockExtraPropertiesStore);
        setField(mockTranslator, "names", mockNameFactory);
        setField(connector, "readonly", true);
        connector.initialize(mockRegistry, mockNodeTypeManager);
        mockContext.getNamespaceRegistry().register("fedora", "http://fedora.info/definitions/v4/repository#");
        mockContext.getNamespaceRegistry().register("premis", "http://www.loc.gov/premis/rdf/v1#");
    }

    @Test(expected = RepositoryException.class)
    public void testThatReadOnlyIsRequired() throws IOException, RepositoryException {
        final FedoraFileSystemConnector c = new FedoraFileSystemConnector();
        setField(c, "directoryPath", directoryPath.toString());
        setField(c, "translator", mockTranslator);
        setField(c, "context", mockContext);
        setField(c, "extraPropertiesStore", mockExtraPropertiesStore);
        setField(mockTranslator, "names", mockNameFactory);
        c.initialize(mockRegistry, mockNodeTypeManager);
    }

    @Test
    public void testGetDocumentByIdNull() throws Exception {
        final Document doc = connector.getDocumentById(null);
        assertNull(doc);
    }

    @Test
    public void testGetDocumentByIdDatastream() throws Exception {
        when(mockTranslator.getPrimaryTypeName(any(Document.class)))
                .thenReturn(NT_FILE);
        when(mockNameFactory.create(anyString())).thenReturn(
                new BasicName("", tmpFile.getName()));

        final Document doc = connector.getDocumentById("/" + tmpFile.getName());
        assertNotNull(doc);
    }

    @Test
    public void testGetDocumentByIdContent() throws Exception {
        when(mockTranslator.getPrimaryTypeName(any(Document.class)))
                .thenReturn(NT_RESOURCE);
        when(mockNameFactory.create(anyString())).thenReturn(
                new BasicName("", tmpFile.getName()));

        when(binaryProperty.getFirstValue()).thenReturn(binaryValue);
        when(mockTranslator.getProperty(any(Document.class), eq(JCR_DATA)))
                .thenReturn(binaryProperty);

        final Document doc = connector.getDocumentById("/" + tmpFile.getName());
        assertNotNull(doc);
    }

    @Test
    public void testSha1WhenContentDigestIsCached() {
        when(mockTranslator.getPrimaryTypeName(any(Document.class)))
                .thenReturn(NT_RESOURCE);
        when(mockNameFactory.create(anyString())).thenReturn(new BasicName("", tmpFile.getName()));
        when(binaryProperty.getFirstValue()).thenReturn(binaryValue);
        when(mockTranslator.getProperty(any(Document.class), eq(JCR_DATA)))
                .thenReturn(binaryProperty);

        final String chksum = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d";

        when(mockTranslator.getProperty(any(Document.class), eq(CONTENT_DIGEST)))
                .thenReturn(new BasicSingleValueProperty(new BasicName("", CONTENT_DIGEST.toString()),
                                     chksum));

        final String sha1 = connector.sha1(tmpFile);

        assertNotNull(sha1);
        assert(sha1.contains(chksum));
    }

    @Test
    public void testSha1ContentDigestIsNotCached() {
        when(mockTranslator.getPrimaryTypeName(any(Document.class)))
                .thenReturn(NT_RESOURCE);
        when(mockNameFactory.create(anyString())).thenReturn(new BasicName("", tmpFile.getName()));
        when(binaryProperty.getFirstValue()).thenReturn(binaryValue);
        when(mockTranslator.getProperty(any(Document.class), eq(JCR_DATA)))
                .thenReturn(binaryProperty);

        final String chksum = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d";

        when(mockTranslator.getProperty(any(Document.class), eq(CONTENT_DIGEST)))
                .thenReturn(null);

        final String sha1 = connector.sha1(tmpFile);

        assertNotNull(sha1);
        assert(sha1.contains(chksum));
    }

    @Test
    public void testRemoveDocument() throws IOException, RepositoryException {
        final String id = "/" + tmpFile2.getName();
        final FedoraFileSystemConnector spy = spy(connector);
        assertTrue("Removing document should return true!", spy.removeDocument(id));
        verify(spy).touchParent(id);
    }

    @Test
    public void testStoreDocument() throws IOException, RepositoryException {
        final String id = "/" + tmpFile.getName();
        final DocumentReader reader = mock(DocumentReader.class);
        final FedoraFileSystemConnector spy = spy(connector);
        doReturn(tmpFile).when(spy).fileFor(anyString());
        doReturn(reader).when(spy).readDocument(any(Document.class));
        doReturn(id).when(reader).getDocumentId();
        doReturn(NT_FILE).when(reader).getPrimaryTypeName();
        spy.storeDocument(spy.getDocumentById(id));
        verify(spy).touchParent(id);
    }
}
