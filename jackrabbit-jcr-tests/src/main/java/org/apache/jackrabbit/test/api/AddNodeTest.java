/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api;

import java.text.Normalizer;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.Session;

/**
 * <code>AddNodeTest</code> contains the test cases for the method
 * <code>Node.addNode(String, String)</code>.
 *
 */
public class AddNodeTest extends AbstractJCRTest {

    /**
     * Tests if the name of the created node is correct.
     */
    public void testName() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        assertEquals("Wrong node name.", n1.getName(), nodeName1);
    }

    /**
     * Tests if the node type of the created node is correct.
     */
    public void testNodeType() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        String ntName = n1.getPrimaryNodeType().getName();
        assertEquals("Wrong node NodeType name.", testNodeType, ntName);
    }

    /**
     * Tests if same name siblings have equal names or if same name
     * siblings are not supported a ItemExistsException is thrown.
     */
    public void testSameNameSiblings() throws RepositoryException {
        if (testRootNode.getDefinition().allowsSameNameSiblings()) {
            Node n1 = testRootNode.addNode(nodeName1, testNodeType);
            Node n2 = testRootNode.addNode(nodeName1, testNodeType);
            testRootNode.getSession().save();
            assertEquals("Names of same name siblings are not equal.",
                    n1.getName(), n2.getName());
        } else {
            testRootNode.addNode(nodeName1, testNodeType);
            try {
                testRootNode.addNode(nodeName1, testNodeType);
                fail("Expected ItemExistsException.");
            } catch (ItemExistsException e) {
                // correct
            }
        }
    }

    /**
     * Tests if addNode() throws a NoSuchNodeTypeException in case
     * of an unknown node type.
     */
    public void testUnknownNodeType() throws RepositoryException {
        try {
            testRootNode.addNode(nodeName1, testNodeType + "unknownSuffix");
            fail("Expected NoSuchNodeTypeException.");
        } catch (NoSuchNodeTypeException e) {
            // correct.
        }
    }

    /**
     * Tests if addNode() throws a ConstraintViolationException in case
     * of an abstract node type.
     */
    public void testAbstractNodeType() throws RepositoryException {
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = ntMgr.getPrimaryNodeTypes();
        while (nts.hasNext()) {
            NodeType nt = nts.nextNodeType();
            if (nt.isAbstract()) {
                try {
                    testRootNode.addNode(nodeName1, nt.getName());
                    superuser.save();
                    fail("Expected ConstraintViolationException.");
                } catch (ConstraintViolationException e) {
                    // correct.
                } finally {
                    superuser.refresh(false);
                }
            }
        }
    }

    /**
     * Tests if addNode() throws a ConstraintViolationException in case
     * of an mixin node type.
     */
    public void testMixinNodeType() throws RepositoryException, NotExecutableException {
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = ntMgr.getMixinNodeTypes();
        if (nts.hasNext()) {
            try {
                testRootNode.addNode(nodeName1, nts.nextNodeType().getName());
                superuser.save();
                fail("Expected ConstraintViolationException.");
            } catch (ConstraintViolationException e) {
                // correct.
            }
        } else {
            throw new NotExecutableException("no mixins.");
        }
    }

    /**
     * Tests if the path of the created node is correct.
     */
    public void testPath() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        String expected = testRootNode.getPath() + "/" + nodeName1;
        assertEquals("Wrong path for created node.", expected, n1.getPath());
    }

    /**
     * Tests if addNode() throws a PathNotFoundException in case
     * intermediary nodes do not exist.
     */
    public void testPathNotFound() throws RepositoryException {
        try {
            testRootNode.addNode(nodeName1 + "/" + nodeName1, testNodeType);
            fail("Expected PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // correct.
        }
    }

    /**
     * Tests if a ConstraintViolationException is thrown when one attempts
     * to add a node at a path that references a property.
     */
    public void testConstraintViolation() throws RepositoryException {
        try {
            Node rootNode = superuser.getRootNode();
            String propPath = testPath + "/" + jcrPrimaryType;
            rootNode.addNode(propPath + "/" + nodeName1, testNodeType);
            fail("Expected ConstraintViolationException.");
        } catch (ConstraintViolationException e) {
            // correct.
        }
    }

    /**
     * Tests if a RepositoryException is thrown in case the path
     * for the new node contains an index.
     */
    public void testRepositoryException() {
        try {
            testRootNode.addNode(nodeName1 + "[1]");
            fail("Expected RepositoryException.");
        } catch (RepositoryException e) {
            // correct.
        }
        try {
            testRootNode.addNode(nodeName1 + "[1]", testNodeType);
            fail("Expected RepositoryException.");
        } catch (RepositoryException e) {
            // correct.
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String,String)}, saves using
     * {@link javax.jcr.Node#save()} on parent node. Uses a second session to
     * verify if the node have been saved.
     */
    public void testAddNodeParentSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new node
        defaultRootNode.save();

        // use a different session to verify if the node is there
        Session session = getHelper().getReadOnlySession();
        try {
            session.getItem(testNode.getPath());
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String, String)}, saves using
     * {@link javax.jcr.Session#save()}. Uses a second session to verify if the
     * node has been safed.
     */
    public void testAddNodeSessionSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new node
        superuser.save();

        // use a different session to verify if the node is there
        Session session = getHelper().getReadOnlySession();
        try {
            session.getItem(testNode.getPath());
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String, String)}, then tries
     * to call {@link javax.jcr.Node#save()} on the new node.
     * <p>
     * This should throw an {@link RepositoryException}.
     */
    public void testAddNodeRepositoryExceptionSaveOnNewNode() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        try {
            // try to call save on newly created node
            testNode.save();
            fail("Calling Node.save() on a newly created node should throw RepositoryException");
        } catch (RepositoryException e) {
            // ok, works as expected.
        }
    }

    /**
     * Tests the behavior with respect to case-sensitivity
     */
    public void testSimilarNodeNamesUpperLower() throws RepositoryException {

        internalTestSimilarNodeNames("Test-a", "Test-A");
    }

    /**
     * Tests the behavior with respect to Unicode normalization
     */
    public void testSimilarNodeNamesNfcNfd() throws RepositoryException {

        String precomposed = "Test-\u00e4"; // a umlaut
        String decomposed = Normalizer.normalize(precomposed, Normalizer.Form.NFD);
        assertFalse(precomposed.equals(decomposed)); // sanity check
        internalTestSimilarNodeNames(precomposed, decomposed);
    }

    /**
     * Tests behavior for creation of "similarly" named nodes
     * @throws RepositoryException 
     */
    private void internalTestSimilarNodeNames(String name1, String name2) throws RepositoryException {

        Node n1 = null, n2 = null;
        Session s = testRootNode.getSession();

        try {
            n1 = testRootNode.addNode(name1);
            assertEquals(name1, n1.getName());
            s.save();

            assertFalse(testRootNode.hasNode(name2));
        } catch (ConstraintViolationException e) {
            // accepted
        }
        try {
            n2 = testRootNode.addNode(name2);
            assertEquals(name2, n2.getName());
            s.save();
        } catch (ConstraintViolationException e) {
            // accepted
        }

        // If both nodes have been created, do further checks
        if (n1 != null && n2 != null) {
            assertFalse(n1.isSame(n2));
            assertFalse(n1.getIdentifier().equals(n2.getIdentifier()));
            String n2path = n2.getPath();
            n1.remove();
            s.save();
            Node n3 = s.getNode(n2path);
            assertTrue(n3.isSame(n2));
        }
    }
}
