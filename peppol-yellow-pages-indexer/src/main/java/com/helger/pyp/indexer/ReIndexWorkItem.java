package com.helger.pyp.indexer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDateTime;

import com.helger.commons.ValueEnforcer;
import com.helger.datetime.PDTFactory;
import com.helger.pyp.settings.PYPSettings;

/**
 * This class holds a single item to be re-indexed. It is only invoked if
 * regular indexing failed.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class ReIndexWorkItem
{
  private final IndexerWorkItem m_aWorkItem;
  private final LocalDateTime m_aMaxRetryDT;
  private int m_nRetries;
  private LocalDateTime m_aPreviousRetryDT;
  private LocalDateTime m_aNextRetryDT;

  public ReIndexWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    // The next retry happens from now in the configured number of minutes
    this (aWorkItem,
          0,
          (LocalDateTime) null,
          PDTFactory.getCurrentLocalDateTime ().plusMinutes (PYPSettings.getReIndexRetryMinutes ()));
  }

  ReIndexWorkItem (@Nonnull final IndexerWorkItem aWorkItem,
                   final int nRetries,
                   @Nullable final LocalDateTime aPreviousRetryDT,
                   @Nullable final LocalDateTime aNextRetryDT)
  {
    m_aWorkItem = ValueEnforcer.notNull (aWorkItem, "WorkItem");
    m_aMaxRetryDT = aWorkItem.getCreationDT ().plusHours (PYPSettings.getReIndexMaxRetryHours ());
    m_nRetries = ValueEnforcer.isGE0 (nRetries, "Retries");
    m_aPreviousRetryDT = aPreviousRetryDT;
    if (nRetries > 0)
      ValueEnforcer.notNull (aPreviousRetryDT, "PreviousRetryDT");
    m_aNextRetryDT = ValueEnforcer.notNull (aNextRetryDT, "NextRetryDT");
  }

  /**
   * @return <code>true</code> if this item is to be expired, because the
   *         retry-time has been exceeded.
   */
  public boolean isExpired ()
  {
    return m_aMaxRetryDT.isBefore (PDTFactory.getCurrentLocalDateTime ());
  }

  /**
   * @return <code>true</code> if the time for the next retry is here.
   */
  public boolean isRetryPossible ()
  {
    return PDTFactory.getCurrentLocalDateTime ().isAfter (m_aNextRetryDT);
  }

  public void incRetryCount ()
  {
    m_nRetries++;
    m_aPreviousRetryDT = PDTFactory.getCurrentLocalDateTime ();
    m_aNextRetryDT = m_aPreviousRetryDT.plusMinutes (PYPSettings.getReIndexRetryMinutes ());
  }
}
