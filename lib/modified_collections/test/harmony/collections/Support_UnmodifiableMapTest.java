/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// The origin of this file is Apache Harmony SVN repository,
// location: classlib/modules/luni/src/main/java/java/util
// checked out Dec 3, 2022.
package harmony.collections;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class Support_UnmodifiableMapTest extends TestCase {

	Map<String, Integer> map;

	// must be a map containing the string keys "0"-"99" paired with the Integer
	// values Integer(0) to Integer(99)

	public Support_UnmodifiableMapTest(String p1) {
		super(p1);
	}

	public Support_UnmodifiableMapTest(String p1, Map<String, Integer> m) {
		super(p1);
		map = m;
	}

	@Override
    public void runTest() {
		// containsKey
		assertTrue("UnmodifiableMapTest - Should contain the key \"0\"", map
				.containsKey("0"));
		assertTrue("UnmodifiableMapTest - Should contain the key \"50\"", map
				.containsKey("50"));
		assertFalse("UnmodifiableMapTest - Should not contain the key \"100\"", map.containsKey("100"));

		// containsValue
		assertTrue("UnmodifiableMapTest - Should contain the value 0", map
				.containsValue(0));
		assertTrue("UnmodifiableMapTest - Should contain the value 50", map
				.containsValue(50));
		assertFalse("UnmodifiableMapTest - Should not contain value 100", map
				.containsValue(100));

		// entrySet
		Set<?> entrySet = map.entrySet();
		Iterator<?> entrySetIterator = entrySet.iterator();
		int myCounter = 0;
		while (entrySetIterator.hasNext()) {
			Map.Entry<?, ?> me = (Map.Entry<?, ?>) entrySetIterator.next();
			assertEquals("UnmodifiableMapTest - Incorrect Map.Entry returned", map.get(me.getKey()), me.getValue());
			myCounter++;
		}
		assertEquals("UnmodifiableMapTest - Incorrect number of map entries returned",
				100, myCounter);

		// get
		assertEquals("UnmodifiableMapTest - getting \"0\" didn't return 0", 0, map.get("0").intValue());
		assertEquals("UnmodifiableMapTest - getting \"50\" didn't return 0", 0, map.get("0").intValue());
		assertNull("UnmodifiableMapTest - getting \"100\" didn't return null",
				map.get("100"));

		// isEmpty
		assertFalse("UnmodifiableMapTest - should have returned false to isEmpty", map.isEmpty());

		// keySet
		Set<?> keySet = map.keySet();
		t_KeySet(keySet);

		// size
		assertEquals("Size should return 100, returned: " + map.size(), 100, map
				.size());

		// values
		new Support_UnmodifiableCollectionTest("Unmod--from map test", map
				.values());

	}

	void t_KeySet(Set<?> keySet) {
		// keySet should be a set of the strings "0" to "99"

		// contains
		assertTrue("UnmodifiableMapTest - keySetTest - should contain \"0\"",
				keySet.contains("0"));
		assertTrue("UnmodifiableMapTest - keySetTest - should contain \"50\"",
				keySet.contains("50"));
		assertFalse("UnmodifiableMapTest - keySetTest - should not contain \"100\"", keySet.contains("100"));

		// containsAll
		HashSet<String> hs = new HashSet<>();
		hs.add("0");
		hs.add("25");
		hs.add("99");
		assertTrue(
				"UnmodifiableMapTest - keySetTest - should contain set of \"0\", \"25\", and \"99\"",
				keySet.containsAll(hs));
		hs.add("100");
		assertFalse("UnmodifiableMapTest - keySetTest - should not contain set of \"0\", \"25\", \"99\" and \"100\"", keySet.containsAll(hs));

		// isEmpty
		assertFalse("UnmodifiableMapTest - keySetTest - should not be empty", keySet.isEmpty());

		// iterator
		Iterator<?> it = keySet.iterator();
		while (it.hasNext()) {
			assertTrue(
					"UnmodifiableMapTest - keySetTest - Iterator returned wrong values",
					keySet.contains(it.next()));
		}

		// size
		assertEquals("UnmodifiableMapTest - keySetTest - returned wrong size.  Wanted 100, got: "
				+ keySet.size(), 100, keySet.size());

		// toArray
		Object[] objArray;
		objArray = keySet.toArray();
		for (int counter = 0; it.hasNext(); counter++) {
			assertSame("UnmodifiableMapTest - keySetTest - toArray returned incorrect array", objArray[counter], it.next());
		}

		// toArray (Object[])
		objArray = new Object[100];
		keySet.toArray(objArray);
		for (int counter = 0; it.hasNext(); counter++) {
			assertSame("UnmodifiableMapTest - keySetTest - toArray(Object) filled array incorrectly", objArray[counter], it.next());
		}
	}

}
