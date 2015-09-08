/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer.rest;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.pyp.indexer.EIndexerWorkItemType;
import com.helger.pyp.indexer.IndexerManager;
import com.helger.pyp.indexer.clientcert.ClientCertificateValidationResult;
import com.helger.pyp.indexer.clientcert.ClientCertificateValidator;
import com.helger.pyp.storage.PYPStorageManager;

/**
 * Indexer resource (exposed at "1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (IndexerResource.class);

  /**
   * Check if the current request contains a client certificate.
   *
   * @param aHttpServletRequest
   *        The current servlet request.
   * @return The validation result
   */
  @Nonnull
  private static ClientCertificateValidationResult _checkClientCertificate (@Nonnull final HttpServletRequest aHttpServletRequest)
  {
    try
    {
      return ClientCertificateValidator.isClientCertificateValid (aHttpServletRequest);
    }
    catch (final RuntimeException ex)
    {
      s_aLogger.warn ("Error validating client certificate", ex);
    }
    return ClientCertificateValidationResult.createFailure ();
  }

  @PUT
  public Response createOrUpdateParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @Nonnull final String sParticipantID)
  {
    final ClientCertificateValidationResult aResult = _checkClientCertificate (aHttpServletRequest);
    if (aResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    // Parse identifier
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createFromURIPart (sParticipantID);

    // Queue for handling
    IndexerManager.getInstance ().queueWorkItem (aPI, EIndexerWorkItemType.CREATE_UPDATE, aResult.getClientID ());

    // And done
    return Response.noContent ().build ();
  }

  @DELETE
  @Path ("{participantID}")
  public Response deleteParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                     @PathParam ("participantID") @Nonnull final String sParticipantID)
  {
    final ClientCertificateValidationResult aResult = _checkClientCertificate (aHttpServletRequest);
    if (aResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    // Parse identifier
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createFromURIPart (sParticipantID);

    // Check if PI exists
    if (!PYPStorageManager.getInstance ().containsEntry (aPI))
      return Response.status (Response.Status.NOT_FOUND).build ();

    // Queue for handling
    IndexerManager.getInstance ().queueWorkItem (aPI, EIndexerWorkItemType.DELETE, aResult.getClientID ());

    // And done
    return Response.noContent ().build ();
  }
}