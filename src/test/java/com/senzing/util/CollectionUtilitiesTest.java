package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;

import static com.senzing.util.CollectionUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CollectionUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class CollectionUtilitiesTest {
  @Test
  @SuppressWarnings("unchecked")
  public void recursivelyUnmodifiableListTest() {
    try {
      List list = new LinkedList();
      list.add(new LinkedList());
      list.add(new HashSet());
      list.add(new HashMap());
      List list2 = new LinkedList();
      list2.add(new LinkedList());
      list2.add(new HashSet());
      list2.add(new HashMap());
      list.add(list2);

      list = recursivelyUnmodifiableList(list);

      testRecursivelyUnmodifiable(list, 0, "collectionType=[ list ]");

    } catch (Exception e) {
      e.printStackTrace();
      fail("recursivelyUnmodifiableListTest() failed with exception: " + e);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void recursivelyUnmodifiableMapTest() {
    try {
      Map map = new HashMap();
      map.put("List", new LinkedList());
      map.put("Set", new HashSet());
      Map map2 = new HashMap();
      map2.put("List", new LinkedList());
      map2.put("Set", new HashSet());
      map2.put("Map", new HashMap());
      map.put("Map", map2);

      map = recursivelyUnmodifiableMap(map);

      testRecursivelyUnmodifiable(map, 0, "collectionType=[ map ]");

    } catch (Exception e) {
      e.printStackTrace();
      fail("recursivelyUnmodifiableMapTest() failed with exception: " + e);
    }
  }

  @Test
  public void listTest() {
    try {
      List<String> list = list("A", "B", "C");

      list.add("D");

    } catch (Exception e) {
      e.printStackTrace();
      fail("listTest() failed with exception: " + e);
    }
  }

  @Test
  public void setTest() {
    try {
      Set<String> set = set("A", "B", "C");

      set.add("D");

    } catch (Exception e) {
      e.printStackTrace();
      fail("listTest() failed with exception: " + e);
    }
  }

  @SuppressWarnings("unchecked")
  private void testRecursivelyUnmodifiable(Object   obj,
                                           int      depth,
                                           String   testInfo)
  {
    String suffix = (testInfo == null) ? "" : ", " + testInfo;

    if (obj instanceof Collection) {
      Collection coll = null;
      try {
        coll = (Collection) obj;
        coll.add("Should Fail");
        fail("Succeeded in adding to a collection that should be "
                 + "unmodifiable.  depth=[ " + depth + " ]" + testInfo);

      } catch (UnsupportedOperationException expected) {
        // ignore
      }

      if (coll.size() > 0) {
        for (Object elem: coll) {
          testRecursivelyUnmodifiable(elem, depth + 1, testInfo);
        }
      }

    } else if (obj instanceof Map) {
      Map map = (Map) obj;
      try {
        map.put("Should", "fail");
        fail("Succeeded in adding to a map that should be "
                 + "unmodifiable.  depth=[ " + depth + " ]" + testInfo);

      } catch (UnsupportedOperationException expected) {
        // ignore
      }

      map.values().forEach((value) -> {
        testRecursivelyUnmodifiable(value, depth + 1, testInfo);
      });
    }
  }
}
