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

// The origin of this file is Apache Harmony SVN repository,
// location: classlib/modules/luni/src/main/java/java/util
// checked out Dec 3, 2022.
package harmony.collections;


import java.io.Serializable;
import java.text.CollationKey;
import java.text.Collator;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertNotEquals;

public class TreeMapTest extends junit.framework.TestCase {

    public static class ReversedComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return -(((Comparable) o1).compareTo(o2));
        }

        public boolean equals(Object o1, Object o2) {
            return (((Comparable) o1).compareTo(o2)) == 0;
        }
    }

    // Regression for Harmony-1026
    public static class MockComparator<T extends Comparable<T>> implements
            Comparator<T>, Serializable {

        public int compare(T o1, T o2) {
            if (o1 == o2) {
                return 0;
            }
            if (null == o1 || null == o2) {
                return -1;
            }
            T c1 = o1;
            T c2 = o2;
            return c1.compareTo(c2);
        }
    }

    // Regression for Harmony-1161
    static class MockComparatorNullTolerable implements Comparator<String> {

        public int compare(String o1, String o2) {
            if (o1 == o2) {
                return 0;
            }
            if (null == o1) {
                return -1;
            }
            if (null == o2) { // comparator should be symmetric
                return 1;
            }
            return o1.compareTo(o2);
        }
    }

    TreeMap tm;

    Object[] objArray = new Object[1000];

    /**
     * Tests java.util.TreeMap#TreeMap()
     */
    public void test_Constructor() {
        // Test for method java.util.TreeMap()
        new Support_MapTest2(new TreeMap()).runTest();

        assertTrue("New treeMap non-empty", new TreeMap().isEmpty());
    }

    /**
     * Tests java.util.TreeMap#TreeMap(java.util.Comparator)
     */
    public void test_ConstructorLjava_util_Comparator() {
        // Test for method java.util.TreeMap(java.util.Comparator)
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        assertSame("TreeMap answered incorrect comparator", reversedTreeMap
                .comparator(), comp);
        reversedTreeMap.put(Integer.toString(1), 1);
        reversedTreeMap.put(Integer.toString(2), 2);
        assertEquals("TreeMap does not use comparator (firstKey was incorrect)", reversedTreeMap.firstKey(), Integer.toString(2));
        assertEquals("TreeMap does not use comparator (lastKey was incorrect)", reversedTreeMap.lastKey(), Integer.toString(1));

    }

    /**
     * Tests java.util.TreeMap#TreeMap(java.util.Map)
     */
    public void test_ConstructorLjava_util_Map() {
        // Test for method java.util.TreeMap(java.util.Map)
        TreeMap myTreeMap = new TreeMap(new HashMap(tm));
        assertEquals("Map is incorrect size", myTreeMap.size(), objArray.length);
        for (Object element : objArray) {
            assertEquals("Map has incorrect mappings", myTreeMap.get(
                    element.toString()), element);
        }
    }

    /**
     * Tests java.util.TreeMap#TreeMap(java.util.SortedMap)
     */
    public void test_ConstructorLjava_util_SortedMap() {
        // Test for method java.util.TreeMap(java.util.SortedMap)
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        reversedTreeMap.put(Integer.toString(1), 1);
        reversedTreeMap.put(Integer.toString(2), 2);
        TreeMap anotherTreeMap = new TreeMap(reversedTreeMap);
        assertSame("New tree map does not answer correct comparator", anotherTreeMap.comparator(), comp);
        assertEquals("TreeMap does not use comparator (firstKey was incorrect)", anotherTreeMap.firstKey(), Integer.toString(2));
        assertEquals("TreeMap does not use comparator (lastKey was incorrect)", anotherTreeMap.lastKey(), Integer.toString(1));

    }

    /**
     * Tests java.util.TreeMap#clear()
     */
    public void test_clear() {
        // Test for method void java.util.TreeMap.clear()
        tm.clear();
        assertEquals("Cleared map returned non-zero size", 0, tm.size());
    }

    /**
     * Tests java.util.TreeMap#clone()
     */
    public void test_clone() {
        // Test for method java.lang.Object java.util.TreeMap.clone()
        TreeMap clonedMap = (TreeMap) tm.clone();
        assertEquals("Cloned map does not equal the original map", clonedMap, tm);
        assertNotSame("Cloned map is the same reference as the original map", clonedMap, tm);
        for (Object element : objArray) {
            assertSame("Cloned map contains incorrect elements", clonedMap
                    .get(element.toString()), tm.get(element.toString()));
        }

        TreeMap map = new TreeMap();
        map.put("key", "value");
        // get the keySet() and values() on the original Map
        Set keys = map.keySet();
        Collection values = map.values();
        assertEquals("values() does not work", "value", values.iterator()
                .next());
        assertEquals("keySet() does not work", "key", keys.iterator().next());
        AbstractMap map2 = (AbstractMap) map.clone();
        map2.put("key", "value2");
        Collection values2 = map2.values();
        assertNotSame("values() is identical", values2, values);
        // values() and keySet() on the cloned() map should be different
        assertEquals("values() was not cloned", "value2", values2.iterator()
                .next());
        map2.clear();
        map2.put("key2", "value3");
        Set key2 = map2.keySet();
        assertNotSame("keySet() is identical", key2, keys);
        assertEquals("keySet() was not cloned", "key2", key2.iterator().next());
    }

    /**
     * Tests java.util.TreeMap#comparator()
     */
    public void test_comparator() {
        // Test for method java.util.Comparator java.util.TreeMap.comparator()\
        Comparator comp = new ReversedComparator();
        TreeMap reversedTreeMap = new TreeMap(comp);
        assertSame("TreeMap answered incorrect comparator", reversedTreeMap
                .comparator(), comp);
        reversedTreeMap.put(Integer.toString(1), 1);
        reversedTreeMap.put(Integer.toString(2), 2);
        assertEquals("TreeMap does not use comparator (firstKey was incorrect)", reversedTreeMap.firstKey(), Integer.toString(2));
        assertEquals("TreeMap does not use comparator (lastKey was incorrect)", reversedTreeMap.lastKey(), Integer.toString(1));
    }

    /**
     * Tests java.util.TreeMap#containsKey(java.lang.Object)
     */
    public void test_containsKeyLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsKey(java.lang.Object)
        assertTrue("Returned false for valid key", tm.containsKey("95"));
        assertFalse("Returned true for invalid key", tm.containsKey("XXXXX"));
    }

    /**
     * Tests java.util.TreeMap#containsValue(java.lang.Object)
     */
    public void test_containsValueLjava_lang_Object() {
        // Test for method boolean
        // java.util.TreeMap.containsValue(java.lang.Object)
        assertTrue("Returned false for valid value", tm
                .containsValue(objArray[986]));
        assertFalse("Returned true for invalid value", tm
                .containsValue(new Object()));
    }

    /**
     * Tests java.util.TreeMap#entrySet()
     */
    public void test_entrySet() {
        // Test for method java.util.Set java.util.TreeMap.entrySet()
        Set anEntrySet = tm.entrySet();
        Iterator entrySetIterator = anEntrySet.iterator();
        assertEquals("EntrySet is incorrect size", anEntrySet.size(), objArray.length);
        Map.Entry entry;
        while (entrySetIterator.hasNext()) {
            entry = (Map.Entry) entrySetIterator.next();
            assertSame("EntrySet does not contain correct mappings", tm
                    .get(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Tests java.util.TreeMap#firstKey()
     */
    public void test_firstKey() {
        // Test for method java.lang.Object java.util.TreeMap.firstKey()
        assertEquals("Returned incorrect first key", "0", tm.firstKey());
    }

    /**
     * Tests java.util.TreeMap#get(java.lang.Object)
     */
    public void test_getLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.get(java.lang.Object)
        Object o = new Object();
        tm.put("Hello", o);
        assertSame("Failed to get mapping", tm.get("Hello"), o);

    }

    /**
     * Tests java.util.TreeMap#headMap(java.lang.Object)
     */
    public void test_headMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.headMap(java.lang.Object)
        Map head = tm.headMap("100");
        assertEquals("Returned map of incorrect size", 3, head.size());
        assertTrue("Returned incorrect elements", head.containsKey("0")
                && head.containsValue(new Integer("1"))
                && head.containsKey("10"));

        // Regression for Harmony-1026
        TreeMap<Integer, Double> map = new TreeMap<Integer, Double>(
                new MockComparator());
        map.put(1, 2.1);
        map.put(2, 3.1);
        map.put(3, 4.5);
        map.put(7, 21.3);
        map.put(null, null);

        SortedMap<Integer, Double> smap = map.headMap(null);
        assertEquals(0, smap.size());

        Set<Integer> keySet = smap.keySet();
        assertEquals(0, keySet.size());

        Set<Map.Entry<Integer, Double>> entrySet = smap.entrySet();
        assertEquals(0, entrySet.size());

        Collection<Double> valueCollection = smap.values();
        assertEquals(0, valueCollection.size());

        // Regression for Harmony-1066
        assertTrue(head instanceof Serializable);

        // Regression for ill-behaved collator
        Collator c = new Collator() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 == null) {
                    return 0;
                }
                return o1.compareTo(o2);
            }

            @Override
            public CollationKey getCollationKey(String string) {
                return null;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };

        TreeMap<String, String> treemap = new TreeMap<>(c);
        assertEquals(0, treemap.headMap(null).size());
        
        treemap = new TreeMap();
		SortedMap<String, String> headMap =  treemap.headMap("100");
		headMap.headMap("100");

	SortedMap<Integer,Integer> intMap,sub;
        int size = 16;
        intMap = new TreeMap<>();
        for(int i=0; i<size; i++) {
            intMap.put(i,i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero",sub.size(),0);
        assertTrue("submap should be empty",sub.isEmpty());
        try{
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }
        
        try{
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }

        size = 256;
        intMap = new TreeMap<>();
        for(int i=0; i<size; i++) {
            intMap.put(i,i);
        }
        sub = intMap.headMap(-1);
        assertEquals("size should be zero",sub.size(),0);
        assertTrue("submap should be empty",sub.isEmpty());
        try{
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }
        
        try{
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }

    }

    /**
     * Tests java.util.TreeMap#keySet()
     */
    public void test_keySet() {
        // Test for method java.util.Set java.util.TreeMap.keySet()
        Set ks = tm.keySet();
        assertEquals("Returned set of incorrect size", ks.size(), objArray.length);
        for (int i = 0; i < tm.size(); i++) {
            assertTrue("Returned set is missing keys", ks.contains(Integer.toString(i)));
        }
    }

    /**
     * Tests java.util.TreeMap#lastKey()
     */
    public void test_lastKey() {
        // Test for method java.lang.Object java.util.TreeMap.lastKey()
        assertEquals("Returned incorrect last key", tm.lastKey(), objArray[objArray.length - 1].toString());
    }

    /**
     * Tests java.util.TreeMap#put(java.lang.Object, java.lang.Object)
     */
    public void test_putLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.put(java.lang.Object, java.lang.Object)
        Object o = new Object();
        tm.put("Hello", o);
        assertSame("Failed to put mapping", tm.get("Hello"), o);

        // regression for Harmony-780
        tm = new TreeMap();
        assertNull(tm.put(new Object(), new Object()));
        try {
            tm.put(1, new Object());
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }

        tm = new TreeMap();
        assertNull(tm.put(1, new Object()));

        // regression for Harmony-2474
        tm = new TreeMap();
        tm.remove(o);
    }

    /**
     * Tests java.util.TreeMap#putAll(java.util.Map)
     */
    public void test_putAllLjava_util_Map() {
        // Test for method void java.util.TreeMap.putAll(java.util.Map)
        TreeMap x = new TreeMap();
        x.putAll(tm);
        assertEquals("Map incorrect size after put", x.size(), tm.size());
        for (Object element : objArray) {
            assertEquals("Failed to put all elements", x.get(element.toString()), element);
        }
    }

    /**
     * Tests java.util.TreeMap#remove(java.lang.Object)
     */
    public void test_removeLjava_lang_Object() {
        // Test for method java.lang.Object
        // java.util.TreeMap.remove(java.lang.Object)
        tm.remove("990");
        assertFalse("Failed to remove mapping", tm.containsKey("990"));

    }

    /**
     * Tests java.util.TreeMap#size()
     */
    public void test_size() {
        // Test for method int java.util.TreeMap.size()
        assertEquals("Returned incorrect size", 1000, tm.size());
    }

    /**
     * Tests java.util.TreeMap#subMap(java.lang.Object, java.lang.Object)
     */
    public void test_subMapLjava_lang_ObjectLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.subMap(java.lang.Object, java.lang.Object)
        SortedMap subMap = tm.subMap(objArray[100].toString(), objArray[109]
                .toString());
        assertEquals("subMap is of incorrect size", 9, subMap.size());
        for (int counter = 100; counter < 109; counter++) {
            assertEquals("SubMap contains incorrect elements", subMap.get(
                    objArray[counter].toString()), objArray[counter]);
        }

        try {
            tm.subMap(objArray[9].toString(), objArray[1].toString());
            fail("end key less than start key should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Regression for Harmony-1161
        TreeMap<String, String> treeMapWithNull = new TreeMap<>(
                new MockComparatorNullTolerable());
        treeMapWithNull.put("key1", "value1"); //$NON-NLS-1$ //$NON-NLS-2$
        treeMapWithNull.put(null, "value2"); //$NON-NLS-1$
        SortedMap<String, String> subMapWithNull = treeMapWithNull.subMap(null,
                "key1"); //$NON-NLS-1$
        assertEquals("Size of subMap should be 1:", 1, subMapWithNull.size()); //$NON-NLS-1$

        // Regression test for typo in lastKey method
        SortedMap<String, String> map = new TreeMap<>();
        map.put("1", "one"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("2", "two"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("3", "three"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("3", map.lastKey());
        SortedMap<String, String> sub = map.subMap("1", "3"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("2", sub.lastKey()); //$NON-NLS-1$
    }
    
    
    /**
     * Tests java.util.TreeMap#subMap(java.lang.Object, java.lang.Object)
     */
    public void test_subMap_Iterator() {
        TreeMap<String, String> map = new TreeMap<>();

        String[] keys = { "1", "2", "3" };
        String[] values = { "one", "two", "three" };
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }

        assertEquals(3, map.size());

        Map subMap = map.subMap("", "test");
        assertEquals(3, subMap.size());

        Set entrySet = subMap.entrySet();
        Iterator iter = entrySet.iterator();
        int size = 0;
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter
                    .next();
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            size++;
        }
        assertEquals(map.size(), size);

        Set<String> keySet = subMap.keySet();
        iter = keySet.iterator();
        size = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            assertTrue(map.containsKey(key));
            size++;
        }
        assertEquals(map.size(), size);
    }


    /**
     * Tests java.util.TreeMap#tailMap(java.lang.Object)
     */
    public void test_tailMapLjava_lang_Object() {
        // Test for method java.util.SortedMap
        // java.util.TreeMap.tailMap(java.lang.Object)
        Map tail = tm.tailMap(objArray[900].toString());
        assertEquals("Returned map of incorrect size : " + tail.size(), tail
                .size(), (objArray.length - 900) + 9);
        for (int i = 900; i < objArray.length; i++) {
            assertTrue("Map contains incorrect entries", tail
                    .containsValue(objArray[i]));
        }

        // Regression for Harmony-1066
        assertTrue(tail instanceof Serializable);

	SortedMap<Integer,Integer> intMap,sub;
        int size = 16;
        intMap = new TreeMap<>();
        for(int i=0; i<size; i++) {
            intMap.put(i,i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero",sub.size(),0);
        assertTrue("submap should be empty",sub.isEmpty());
        try{
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }
        
        try{
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }

        size = 256;
        intMap = new TreeMap<>();
        for(int i=0; i<size; i++) {
            intMap.put(i,i);
        }
        sub = intMap.tailMap(size);
        assertEquals("size should be zero",sub.size(),0);
        assertTrue("submap should be empty",sub.isEmpty());
        try{
            sub.firstKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }
        
        try{
            sub.lastKey();
            fail("java.util.NoSuchElementException should be thrown");
        } catch(java.util.NoSuchElementException e) {
        }

    }

    /**
     * Tests java.util.TreeMap#values()
     */
    public void test_values() {
        // Test for method java.util.Collection java.util.TreeMap.values()
        Collection vals = tm.values();
        vals.iterator();
        assertEquals("Returned collection of incorrect size", vals.size(), objArray.length);
        for (Object element : objArray) {
            assertTrue("Collection contains incorrect elements", vals
                    .contains(element));
        }

        TreeMap myTreeMap = new TreeMap();
        for (int i = 0; i < 100; i++) {
            myTreeMap.put(objArray[i], objArray[i]);
        }
        Collection values = myTreeMap.values();
        new Support_UnmodifiableCollectionTest(
                "Test Returned Collection From TreeMap.values()", values)
                .runTest();
        values.remove(0);
        assertFalse("Removing from the values collection should remove from the original map", myTreeMap.containsValue(0));
    }

    /**
     * Tests java.util.TreeMap#SerializationTest()
     */
    // Regression for Harmony-1066
    public void test_SubMap_Serializable() throws Exception {
        TreeMap<Integer, Double> map = new TreeMap<>();
        map.put(1, 2.1);
        map.put(2, 3.1);
        map.put(3, 4.5);
        map.put(7, 21.3);
        SortedMap<Integer, Double> headMap = map.headMap(3);
        assertTrue(headMap instanceof Serializable);
        assertFalse(headMap instanceof TreeMap);
        assertTrue(headMap instanceof SortedMap);

        assertFalse(headMap.entrySet() instanceof Serializable);
        assertFalse(headMap.keySet() instanceof Serializable);
        assertFalse(headMap.values() instanceof Serializable);

        // This assertion will fail on RI. This is a bug of RI.
        SerializationTest.verifySelf(headMap);
    }

    /**
     * Tests equals() method.
     * Tests that no ClassCastException will be thrown in all cases.
     * Regression test for HARMONY-1639.
     */
    public void test_equals() throws Exception {
        // comparing TreeMaps with different object types
        Map m1 = new TreeMap();
        Map m2 = new TreeMap();
        m1.put("key1", "val1");
        m1.put("key2", "val2");
        m2.put(1, "val1");
        m2.put(2, "val2");
        assertNotEquals("Maps should not be equal 1", m1, m2);
        assertNotEquals("Maps should not be equal 2", m2, m1);

        // comparing TreeMap with HashMap
        m1 = new TreeMap();
        m2 = new HashMap();
        m1.put("key", "val");
        m2.put(new Object(), "val");
        assertNotEquals("Maps should not be equal 3", m1, m2);
        assertNotEquals("Maps should not be equal 4", m2, m1);

        // comparing TreeMaps with not-comparable objects inside
        m1 = new TreeMap();
        m2 = new TreeMap();
        m1.put(new Object(), "val1");
        m2.put(new Object(), "val1");
        assertNotEquals("Maps should not be equal 5", m1, m2);
        assertNotEquals("Maps should not be equal 6", m2, m1);
    }

    /**
     * Tests entrySet().contains() method behaviour with respect to entries
     * with null values.
     * Regression test for HARMONY-5788.
     */
    public void test_entrySet_contains() throws Exception {
        TreeMap master = new TreeMap<String, String>();
        TreeMap test_map = new TreeMap<String, String>();

        master.put("null", null);
        Object[] entry = master.entrySet().toArray();
        assertFalse("Empty map should not contain the null-valued entry",
                    test_map.entrySet().contains(entry[0]));

        Map<String, String> submap = test_map.subMap("a","z");
        entry = master.entrySet().toArray();
        assertFalse("Empty submap should not contain the null-valued entry",
                    submap.entrySet().contains(entry[0]));

        test_map.put("null", null);
        assertTrue("entrySet().containsAll(...) should work with null values",
                   test_map.entrySet().containsAll(master.entrySet()));

        master.clear();
        master.put("null", '0');
        entry = master.entrySet().toArray();
        assertFalse("Null-valued entry should not equal non-null-valued entry",
                    test_map.entrySet().contains(entry[0]));
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() {
        tm = new TreeMap();
        for (int i = 0; i < objArray.length; i++) {
            Object x = objArray[i] = i;
            tm.put(x.toString(), x);
        }
    }

}
