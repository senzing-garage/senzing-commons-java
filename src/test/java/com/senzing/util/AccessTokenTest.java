package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;

import static com.senzing.util.AccessToken.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AccessToken}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class AccessTokenTest {
  @Test
  public void getThreadAccessTokenTest() {
    try {
      Map<Long, AccessToken>  accessTokenMap  = new LinkedHashMap<>();
      Set<Long>               failureSet      = new LinkedHashSet<>();

      final int LOOP_COUNT    = 50;
      final int THREAD_COUNT  = 10;

      Runnable runnable = () -> {
        Long threadId = Thread.currentThread().getId();
        for (int index = 0; index < LOOP_COUNT; index++) {
          AccessToken threadToken = getThreadAccessToken();
          AccessToken prevToken = null;
          synchronized (accessTokenMap) {
            prevToken = accessTokenMap.put(threadId, threadToken);
          }
          if (prevToken != null && prevToken != threadToken) {
            synchronized (failureSet) {
              failureSet.add(threadId);
            }
          }
        }
      };

      List<Thread> threads = new ArrayList<>(THREAD_COUNT);
      for (int index = 0; index < THREAD_COUNT; index++) {
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
      }

      for (Thread thread: threads) {
        thread.join();
      }

      assertEquals(0, failureSet.size(),
                   "At least one of the threads failed to get the same access "
                   + "token on repeated calls: " + failureSet);

      Set<AccessToken> set = new LinkedHashSet<>();
      set.addAll(accessTokenMap.values());

      assertEquals(THREAD_COUNT, set.size(),
                   "Multiple threads got the same thread access token");

    } catch (Exception e) {
      e.printStackTrace();
      fail("getThreadAccessTokenTest() failed with exception: " + e);
    }
  }

  @Test
  public void claimThreadAccessTokenTest() {
    try {
      Set<AccessToken>        tokenSet        = new LinkedHashSet<>();
      Map<Long, AccessToken>  accessTokenMap  = new LinkedHashMap<>();
      Set<Long>               failureSet      = new LinkedHashSet<>();

      final int LOOP_COUNT    = 50;
      final int THREAD_COUNT  = 10;

      Runnable runnable = () -> {
        Long threadId = Thread.currentThread().getId();
        for (int index = 0; index < LOOP_COUNT; index++) {
          AccessToken threadToken = claimThreadAccessToken();
          synchronized (tokenSet) {
            tokenSet.add(threadToken);
          }
          AccessToken prevToken = null;
          synchronized (accessTokenMap) {
            prevToken = accessTokenMap.put(threadId, threadToken);
          }
          if (prevToken == threadToken) {
            synchronized (failureSet) {
              failureSet.add(threadId);
            }
          }
        }
      };

      List<Thread> threads = new ArrayList<>(THREAD_COUNT);
      for (int index = 0; index < THREAD_COUNT; index++) {
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
      }

      for (Thread thread: threads) {
        thread.join();
      }

      assertEquals(0, failureSet.size(),
                   "At least one of the threads claimed the same access "
                       + "token on repeated calls: " + failureSet);

      assertEquals(LOOP_COUNT * THREAD_COUNT, tokenSet.size(),
                   "Not all claimed access tokens were unique.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("claimThreadAccessTokenTest() failed with exception: " + e);
    }
  }
}
