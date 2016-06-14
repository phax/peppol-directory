/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.indexer.index;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.concurrent.ExtendedDefaultThreadFactory;
import com.helger.commons.concurrent.ManagedExecutorService;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;

/**
 * The indexer queue that holds all items to be indexed initially. If indexing
 * fails, items are shifted to the re-index queue where graceful retries will
 * happen.
 *
 * @author Philip Helger
 */
public final class IndexerWorkItemQueue
{
  private final ConcurrentCollectorSingle <IIndexerWorkItem> m_aImmediateCollector;
  private final ThreadFactory m_aThreadFactory = new ExtendedDefaultThreadFactory ("IndexerWorkQueue");
  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (1,
                                                                              1,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <Runnable> (),
                                                                              m_aThreadFactory);

  public IndexerWorkItemQueue (@Nonnull final IConcurrentPerformer <IIndexerWorkItem> aPerformer)
  {
    m_aImmediateCollector = new ConcurrentCollectorSingle <> (new LinkedBlockingQueue <> ());
    m_aImmediateCollector.setPerformer (aPerformer);

    // Start the collector
    m_aSenderThreadPool.submit (m_aImmediateCollector);
  }

  /**
   * Stop the indexer work queue immediately.
   *
   * @return The list of all remaining objects in the queue. Never
   *         <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IIndexerWorkItem> stop ()
  {
    // don't take any more actions
    m_aImmediateCollector.stopQueuingNewObjects ();

    // Get all remaining objects and save them for late reuse
    final ICommonsList <IIndexerWorkItem> aRemainingItems = m_aImmediateCollector.drainQueue ();

    // Shutdown the thread pool afterwards
    ManagedExecutorService.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);

    return aRemainingItems;
  }

  public void queueObject (@Nonnull final IIndexerWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aImmediateCollector.queueObject (aItem);
  }
}