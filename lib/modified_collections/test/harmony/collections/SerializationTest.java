/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Alexey V. Varlamov
*/

// The origin of this file is Apache Harmony SVN repository,
// location: classlib/modules/luni/src/main/java/java/util
// checked out Dec 3, 2022.
package harmony.collections;


import junit.framework.TestCase;

import java.io.*;
import java.lang.reflect.Method;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;


/**
 * Framework for serialization testing. Subclasses only need to override
 * getData() method and, optionally, assertDeserialized() method. The first one
 * returns array of objects to be de/serialized in tests, and the second
 * compares reference and deserialized objects (needed only if tested objects do
 * not provide specific method equals()). <br>
 * There are two modes of test run: <b>reference generation mode </b> and
 * <b>testing mode </b>. The actual mode is selected via
 * <b>&quot;test.mode&quot; </b> system property. The <b>testing mode </b> is
 * the default mode. <br>
 * To turn on the <b>reference generation mode </b>, the test.mode property
 * should be set to value &quot;serial.reference&quot;. In this mode, no testing
 * is performed but golden files are produced, which contain reference
 * serialized objects. This mode should be run on a pure 
 * Implementation classes, which are targeted for compartibility. <br>
 * The location of golden files (in both modes) is controlled via
 * <b>&quot;RESOURCE_DIR&quot; </b> system property.
 * 
 */
public abstract class SerializationTest extends TestCase {

    /**
     * Property name for the testing mode.
     */
    public static final String MODE_KEY = "test.mode";


    /**
     * Testing mode.
     */
    public static String mode = System.getProperty(MODE_KEY);

    /**
     * Reference files generation mode switch.
     */
    public static final String SERIAL_REFERENCE_MODE = "serial.reference";

    /**
     * Key to a system property defining root location of golden files.
     */
    public static final String GOLDEN_PATH = "RESOURCE_DIR";

    private static final String outputPath = System.getProperty(GOLDEN_PATH,
            "src/test/resources/serialization");

    /**
     * Parameterized c-tor inherited from superclass.
     */
    public SerializationTest(String name) {
        super(name);
    }

    /**
     * Default c-tor inherited from superclass.
     */
    public SerializationTest() {
        super();
    }

    /**
     * Depending on testing mode, produces golden files or performs testing.
     */
    @Override
    public void runBare() throws Throwable {

        if (mode != null && mode.equals(SERIAL_REFERENCE_MODE)) {
            produceGoldenFiles();
        } else {
            super.runBare();
        }
    }

    /**
     * This is the main working method of this framework. Subclasses must
     * override it to provide actual objects for testing.
     * 
     * @return array of objects to be de/serialized in tests.
     */
    protected abstract Object[] getData();

    /**
     * Tests that data objects can be serialized and deserialized without
     * exceptions, and that deserialization really produces deeply cloned
     * objects.
     */
    public void testSelf() throws Throwable {

        if (this instanceof SerializableAssert) {
            verifySelf(getData(), (SerializableAssert) this);
        } else {
            verifySelf(getData());

        }
    }

    /**
     * Tests that data objects can be deserialized from golden files, to verify
     * compartibility with Reference Implementation.
     */
    public void testGolden() throws Throwable {
        
        verifyGolden(this, getData());
    }

    /**
     * Returns golden file for an object being tested.
     * 
     * @param index array index of tested data (as returned by
     *        {@link #getData() getData()})
     * @return corresponding golden file
     */
    protected File getDataFile(int index) {
        String name = this.getClass().getName();
        int dot = name.lastIndexOf('.');
        String path = name.substring(0, dot).replace('.', File.separatorChar);
        if (outputPath != null && outputPath.length() != 0) {
            path = outputPath + File.separator + path;
        }

        return new File(path, name.substring(dot + 1) + "." + index + ".dat");
    }

    /**
     * Working method for files generation mode. Serializes test objects
     * returned by {@link #getData() getData()}to golden files, each object to
     * a separate file.
     * 
     * @throws IOException
     */
    protected void produceGoldenFiles() throws IOException {

        String goldenPath = outputPath + File.separatorChar
                + getClass().getName().replace('.', File.separatorChar)
                + ".golden.";

        Object[] data = getData();
        for (int i = 0; i < data.length; i++) {

            File goldenFile = new File(goldenPath + i + ".ser");
            goldenFile.getParentFile().mkdirs();
            goldenFile.createNewFile();

            putObjectToStream(data[i], new FileOutputStream(goldenFile));
        }
    }

    /**
     * Serializes specified object to an output stream.
     */
    public static void putObjectToStream(Object obj, OutputStream os)
        throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
    }

    /**
     * Deserializes single object from an input stream.
     */
    public static Serializable getObjectFromStream(InputStream is) throws IOException,
        ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        Object result = ois.readObject();
        ois.close();
        return (Serializable)result;
    }
    
    /**
     * Interface to compare (de)serialized objects
     * 
     * Should be implemented if a class under test does not provide specific
     * equals() method and it's instances should to be compared manually.
     */
    public interface SerializableAssert {

        /**
         * Compares deserialized and reference objects.
         * 
         * @param initial -
         *            initial object used for creating serialized form
         * @param deserialized -
         *            deserialized object
         */
        void assertDeserialized(Serializable initial, Serializable deserialized);
    }

    // default comparator for a class that has equals(Object) method
    private final static SerializableAssert DEFAULT_COMPARATOR = (initial, deserialized) -> assertEquals(initial, deserialized);

    /**
     * Comparator for verifying that deserialized object is the same as initial.
     */
    public final static SerializableAssert SAME_COMPARATOR = (initial, deserialized) -> assertSame(initial, deserialized);

    /**
     * Comparator for java.lang.Throwable objects
     */
    public final static SerializableAssert THROWABLE_COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial, Serializable deserialized) {

            Throwable initThr = (Throwable) initial;
            Throwable dserThr = (Throwable) deserialized;

            // verify class
            assertEquals(initThr.getClass(), dserThr.getClass());

            // verify message
            assertEquals(initThr.getMessage(), dserThr.getMessage());

            // verify cause
            if (initThr.getCause() == null) {
                assertNull(dserThr.getCause());
            } else {
                assertNotNull(dserThr.getCause());

                THROWABLE_COMPARATOR.assertDeserialized(initThr.getCause(),
                        dserThr.getCause());
            }
        }
    };

    /**
     * Comparator for java.security.PermissionCollection objects
     */
    public final static SerializableAssert PERMISSION_COLLECTION_COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial, Serializable deserialized) {

            PermissionCollection initPC = (PermissionCollection) initial;
            PermissionCollection dserPC = (PermissionCollection) deserialized;

            // verify class
            assertEquals(initPC.getClass(), dserPC.getClass());

            // verify 'readOnly' field
            assertEquals(initPC.isReadOnly(), dserPC.isReadOnly());

            // verify collection of permissions
            Collection<Permission> refCollection = new HashSet<>(
                    Collections.list(initPC.elements()));
            Collection<Permission> tstCollection = new HashSet<>(
                    Collections.list(dserPC.elements()));

            assertEquals(refCollection.size(), tstCollection.size());
            int size = refCollection.size();
            if (size > 0) {
				ArrayList<Permission> refList = Collections.list(initPC
						.elements());
				ArrayList<Permission> tstList = Collections.list(dserPC
						.elements());
				if (refList.get(0) instanceof UnresolvedPermission
						&& tstList.get(0) instanceof UnresolvedPermission) {
					boolean found;
					UnresolvedPermission refPerm, tstPerm;
					for (int i = 0; i < size; i++) {
						found = false;
						refPerm = (UnresolvedPermission) refList.get(i);
						for (int j = 0; j < size; j++) {
							tstPerm = (UnresolvedPermission) tstList.get(i);
							if (equalsUnresolvedPermission(refPerm, tstPerm)) {
								found = true;
								break;
							}
						}

						assertTrue(found);
					}
				} else {
					assertEquals(refCollection, tstCollection);
				}
			}
        }
    
        /*
		 * check whether the given two UnresolvedPermission objects equal to
		 * each other
		 */
		private boolean equalsUnresolvedPermission(UnresolvedPermission up1,
				UnresolvedPermission up2) {
			java.security.cert.Certificate[] certs = up1.getUnresolvedCerts();
			if (certs != null && certs.length == 0) {
				if (null == up2.getUnresolvedCerts()) {
					if (up1.getName().equals(up2.getName())) {
						String up1Name = up1.getUnresolvedName();
						String up2Name = up2.getUnresolvedName();
						if (up1Name == null ? up2Name == null : up1Name
								.equals(up2Name)) {
							String up1Actions = up1.getUnresolvedActions();
							String up2Actions = up2.getUnresolvedActions();
							return up1Actions == null ? up2Actions == null
									: up1Actions.equals(up2Actions);
						}
					}
				}
				return false;
			}
			return up1.equals(up2);
		}
    };

    /**
	 * Comparator for java.security.UnresolvedPermission objects
	 */
	public final static SerializableAssert UNRESOLVED_PERMISSION_COMPARATOR =
            (initial, deserialized) -> {
                UnresolvedPermission initPerm = (UnresolvedPermission) initial;
                UnresolvedPermission dserPerm = (UnresolvedPermission) deserialized;
                java.security.cert.Certificate[] certs = initPerm
                        .getUnresolvedCerts();
                if (certs != null && certs.length == 0) {
                    assertEquals(initPerm.getUnresolvedType(), dserPerm
                            .getUnresolvedType());
                    assertEquals(initPerm.getUnresolvedName(), dserPerm
                            .getUnresolvedName());
                    assertEquals(initPerm.getUnresolvedActions(), dserPerm
                            .getUnresolvedActions());
                    assertNull(dserPerm.getUnresolvedCerts());
                } else {
                    assertEquals(initPerm, dserPerm);
                }
            };
    
    /**
     * Returns <code>comparator</code> for provided serializable
     * <code>object</code>.
     * 
     * The <code>comparator</code> is searched in the following order: <br>-
     * if <code>test</code> implements SerializableAssert interface then it is
     * selected as </code>comparator</code>.<br>- if passed <code>object</code>
     * has class in its classes hierarchy that overrides <code>equals(Object)</code>
     * method then <code>DEFAULT_COMPARATOR</code> is selected.<br> - the
     * method tries to select one of known comparators basing on <code>object's</code>
     * class,for example, if passed <code>object</code> is instance of
     * java.lang.Throwable then <code>THROWABLE_COMPARATOR</code> is used.<br>-
     * otherwise RuntimeException is thrown
     * 
     * @param test -
     *            test case
     * @param object -
     *            object to be compared
     * @return object's comparator
     */
    public static SerializableAssert defineComparator(TestCase test,
            Object object) throws Exception {

        if (test instanceof SerializableAssert) {
            return (SerializableAssert) test;
        }

        Method m = object.getClass().getMethod("equals",
                Object.class);

        if (m.getDeclaringClass() != Object.class) {
        	if (object instanceof UnresolvedPermission) {
				// object is an instance of UnresolvedPermission, use
				// UNRESOLVED_PERMISSION_COMPARATOR
				return UNRESOLVED_PERMISSION_COMPARATOR;
			}
            // one of classes overrides Object.equals(Object) method
            // use default comparator
            return DEFAULT_COMPARATOR;
        }

        // Originally_TODO use generics to detect comparator
        // instead of 'instanceof' for the first element
        if (object instanceof java.lang.Throwable) {
            return THROWABLE_COMPARATOR;
        } else if (object instanceof java.security.PermissionCollection) {
            return PERMISSION_COLLECTION_COMPARATOR;
        }

        throw new RuntimeException("Failed to detect comparator");
    }
    
    /**
     * Verifies that object deserialized from golden file correctly.
     * 
     * The method invokes <br>
     * verifyGolden(test, object, defineComparator(test, object));
     * 
     * @param test -
     *            test case
     * @param object -
     *            to be compared
     */
    public static void verifyGolden(TestCase test, Object object)
            throws Exception {

        verifyGolden(test, object, defineComparator(test, object));
    }

    /**
     * Verifies that object deserialized from golden file correctly.
     * 
     * The method loads "<code>testName</code>.golden.ser" resource file
     * from "<module root>/src/test/resources/serialization/<code>testPackage</code>"
     * folder, reads an object from the loaded file and compares it with
     * <code>object</code> using specified <code>comparator</code>.
     * 
     * @param test-
     *            test case
     * @param object-
     *            to be compared
     * @param comparator -
     *            for comparing (de)serialized objects
     */
    public static void verifyGolden(TestCase test, Object object,
            SerializableAssert comparator) throws Exception {

        assertNotNull("Null comparator", comparator);

        Serializable deserialized = getObject(test, ".golden.ser");

        comparator.assertDeserialized((Serializable) object, deserialized);
    }

    /**
     * Verifies that objects from array deserialized from golden files
     * correctly.
     * 
     * The method invokes <br>
     * verifyGolden(test, objects, defineComparator(test, object[0]));
     * 
     * @param test -
     *            test case
     * @param objects -
     *            array of objects to be compared
     */
    public static void verifyGolden(TestCase test, Object[] objects)
            throws Exception {

        assertFalse("Empty array", objects.length == 0);
        verifyGolden(test, objects, defineComparator(test, objects[0]));
    }

    /**
     * Verifies that objects from array deserialized from golden files
     * correctly.
     * 
     * The method loads "<code>testName</code>.golden.<code>N</code>.ser"
     * resource files from "<module root>/src/test/resources/serialization/<code>testPackage</code>"
     * folder, from each loaded file it reads an object from and compares it
     * with corresponding object in provided array (i.e. <code>objects[N]</code>)
     * using specified <code>comparator</code>. (<code>N</code> is index
     * in object's array.)
     * 
     * @param test-
     *            test case
     * @param objects -
     *            array of objects to be compared
     * @param comparator -
     *            for comparing (de)serialized objects
     */
    public static void verifyGolden(TestCase test, Object[] objects,
            SerializableAssert comparator) throws Exception {

        assertFalse("Empty array", objects.length == 0);
        for (int i = 0; i < objects.length; i++) {
            Serializable deserialized = getObject(test, ".golden." + i + ".ser");
            comparator.assertDeserialized((Serializable) objects[i],
                    deserialized);
        }
    }
    
    /**
     * Verifies that object can be smoothly serialized/deserialized.
     * 
     * The method invokes <br>
     * verifySelf(object, defineComparator(null, object));
     * 
     * @param object -
     *            to be serialized/deserialized
     */
    public static void verifySelf(Object object)
            throws Exception {

        verifySelf(object, defineComparator(null, object));
    }
    
    /**
     * Verifies that object can be smoothly serialized/deserialized.
     * 
     * The method serialize/deserialize <code>object</code> and compare it
     * with initial <code>object</code>.
     * 
     * @param object -
     *            object to be serialized/deserialized
     * @param comparator -
     *            for comparing serialized/deserialized object with initial
     *            object
     */
    public static void verifySelf(Object object, SerializableAssert comparator)
            throws Exception {

        Serializable initial = (Serializable) object;

        comparator.assertDeserialized(initial, copySerializable(initial));
    }

    /**
     * Verifies that that objects from array can be smoothly
     * serialized/deserialized.
     * 
     * The method invokes <br>
     * verifySelf(objects, defineComparator(null, object[0]));
     * 
     * @param objects -
     *            array of objects to be serialized/deserialized
     */
    public static void verifySelf(Object[] objects)
            throws Exception {

        assertFalse("Empty array", objects.length == 0);
        verifySelf(objects, defineComparator(null, objects[0]));
    }
    
    /**
     * Verifies that that objects from array can be smoothly
     * serialized/deserialized.
     * 
     * The method serialize/deserialize each object in <code>objects</code>
     * array and compare it with initial object.
     * 
     * @param objects -
     *            array of objects to be serialized/deserialized
     * @param comparator -
     *            for comparing serialized/deserialized object with initial
     *            object
     */
    public static void verifySelf(Object[] objects, SerializableAssert comparator)
            throws Exception {

        assertFalse("Empty array", objects.length == 0);
        for(Object entry: objects){
            verifySelf(entry, comparator);
        }
    }

    private static Serializable getObject(TestCase test, String toAppend)
            throws Exception {

        StringBuilder path = new StringBuilder("serialization");

        path.append(File.separatorChar);
        path.append(test.getClass().getName().replace('.', File.separatorChar));
        path.append(toAppend);

        InputStream in = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(path.toString());

        assertNotNull("Failed to load serialization resource file: "
                + path, in);

        return getObjectFromStream(in);
    }
    
    /**
     * Creates golden file.
     * 
     * The folder for created file is: <code>root + test's package name</code>.
     * The file name is: <code>test's name + "golden.ser"</code>
     * 
     * @param root -
     *            root directory for serialization resource files
     * @param test -
     *            test case
     * @param object -
     *            object to be serialized
     * @throws IOException -
     *             if I/O error
     */
    public static void createGoldenFile(String root, TestCase test,
            Object object) throws IOException {

        String goldenPath = test.getClass().getName().replace('.',
                File.separatorChar)
                + ".golden.ser";

        if (root != null) {
            goldenPath = root + File.separatorChar + goldenPath;
        }


        File goldenFile = new File(goldenPath);
        goldenFile.getParentFile().mkdirs();
        goldenFile.createNewFile();

        putObjectToStream(object, new FileOutputStream(goldenFile));

        // don't forget to remove it from test case after using
        fail("Generating golden file.\nGolden file name:"
                + goldenFile.getAbsolutePath());
    }
    
    /**
     * Copies an object by serializing/deserializing it.
     * 
     * @param initial -
     *            an object to be copied
     * @return copy of provided object
     */
    public static Serializable copySerializable(Serializable initial)
            throws IOException, ClassNotFoundException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        putObjectToStream(initial, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        return getObjectFromStream(in);
    }
}
