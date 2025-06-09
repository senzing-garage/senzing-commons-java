package com.senzing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

/**
 * Provides a pool of threads to join other threads.
 *
 */
public class ThreadJoinerPool implements AutoCloseable {
  /**
   * An interface used for registering callbacks to be executed after
   * the {@link Thread} is joined.
   */
  public interface PostJoinCallback<T extends Thread> {
    void onJoined(T thread);
  }

  /**
   * The available {@link Joiner} instances.
   */
  private final List<Joiner> joiners;

  /**
   * All {@link Joiner} instances.
   */
  private final List<Joiner> allJoiners;

  /**
   * Indicates if this instance is already destroyed.
   */
  private boolean closed;

  /**
   * Constructs a pool of threads whose job it is to background join other
   * threads so you can throttle the number of outstanding threads waiting
   * in the background for completion.
   *
   * @param poolSize The max pool size which is what throttles how many
   *                 concurrent threads.
   */
  public ThreadJoinerPool(int poolSize) {
    this.joiners = new LinkedList<>();
    this.allJoiners = new ArrayList<>(poolSize);
    this.closed = false;

    for (int index = 0; index < poolSize; index++) {
      Joiner joiner = new Joiner();
      this.joiners.add(joiner);
      this.allJoiners.add(joiner);
      joiner.start();
    }
  }

  /**
   * Immediately returns if available joiners in the pool, but if all
   * joiners are busy, then this blocks until one is available.
   *
   * @param thread The {@link Thread} to join against.
   */
  public void join(Thread thread) {
    this.join(thread, null);
  }

  /**
   * Immediately returns if available joiners in the pool, but if all
   * joiners are busy, then this blocks until one is available.
   *
   * @param thread   The {@link Thread} to join against.
   *
   * @param callback The {@link PostJoinCallback} to execute after the joining.
   */
  public void join(Thread thread, PostJoinCallback callback) {
    Joiner joiner = null;
    // wait until a joiner is available
    synchronized (this.joiners) {
      while (this.joiners.size() == 0 && !this.closed) {
        try {
          this.joiners.wait(5000L);
        } catch (InterruptedException ignore) {
          // do nothing
        }
      }
      if (this.joiners.size() > 0) {
        joiner = this.joiners.remove(0);
      }
    }

    // check if we have a joiner
    if (joiner == null) {
      throw new IllegalStateException(
          "This instance already destroyed -- cannot join thread.");
    }

    // join the thread
    joiner.join(thread, callback);
  }

  /**
   * Internal method to handle marking this instance as destroyed and notifying
   * all joiners.
   */
  private void doClose() {
    // flag this instance as closed
    synchronized (this.joiners) {
      // check if already closed
      if (this.closed)
        return;

      this.closed = true;
      this.joiners.notifyAll();
    }

    // notify all joiners that it is time to shut down
    this.allJoiners.forEach(joiner -> {
      synchronized (joiner) {
        joiner.notifyAll();
      }
    });

  }

  /**
   * Joins against all outstanding threads and closes this instance.
   */
  public void joinAndClose() {
    this.doClose();

    // interrupt any joiners that are lingering
    this.allJoiners.forEach(joiner -> {
      try {
        joiner.join();
      } catch (InterruptedException ignore) {
        // ignore
      }
    });
  }

  /**
   * Closes this instance and interrupts all joiner threads.
   */
  public void close() {
    this.doClose();

    // interrupt any joiners that are lingering
    this.allJoiners.forEach(joiner -> {
      if (joiner.isAlive()) {
        joiner.interrupt();
      }
    });
  }

  /**
   * Checks if this instance has been closed.
   *
   * @return <code>true</code> if this instance has been closed, otherwise
   *         <code>false</code>
   */
  public boolean isClosed() {
    synchronized (this.joiners) {
      return this.closed;
    }
  }

  /**
   * Inner class to handle the joining.
   */
  private class Joiner extends Thread {
    private Thread currentThread;
    private PostJoinCallback callback;

    private Joiner() {
      this.currentThread = null;
      this.callback = null;
    }

    private synchronized void join(Thread thread, PostJoinCallback callback) {
      if (this.currentThread != null) {
        throw new IllegalStateException(
            "Cannot join a thread while one is pending.");
      }
      this.currentThread = thread;
      this.callback = callback;
      this.notifyAll();
    }

    @SuppressWarnings("unchecked")
    public void run() {
      final ThreadJoinerPool pool = ThreadJoinerPool.this;
      while (!pool.isClosed()) {
        synchronized (this) {
          // wait until a request is available
          if (this.currentThread == null) {
            try {
              this.wait(15000L);
            } catch (InterruptedException ignore) {
              continue;
            }
          }
          if (this.currentThread == null)
            continue;
        }

        // get the thread to join
        Thread thread = this.currentThread;
        PostJoinCallback postJoin = this.callback;

        // clear the field holding the reference to the current thread
        this.currentThread = null;
        this.callback = null;

        // join the thread
        try {
          thread.join();

          // check if we have a callback
          if (postJoin != null) {
            postJoin.onJoined(thread);
          }

        } catch (InterruptedException e) {
          if (pool.isClosed())
            return;
        }

        synchronized (pool.joiners) {
          pool.joiners.add(this);
          pool.joiners.notifyAll();
        }
      }
    }
  }
}
