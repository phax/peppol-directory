/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.secure.page;

import javax.annotation.Nonnull;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.core.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

public final class PageSecureParticipantCount extends AbstractAppWebPage
{
  private static final AjaxFunctionDeclaration s_aExportAllIDs;
  private static final AjaxFunctionDeclaration s_aExportAllBCs;

  static
  {
    s_aExportAllIDs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement aRoot = aDoc.appendElement ("root");
      final ICommonsSortedSet <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                              .getAllContainedParticipantIDs ();
      for (final IParticipantIdentifier aParticipantID : aAllIDs)
      {
        final String sParticipantID = aParticipantID.getURIEncoded ();
        aRoot.appendElement ("item").appendText (sParticipantID);
      }
      res.xml (aDoc);
      res.attachment ("directory-participant-list.xml");
    });
    s_aExportAllBCs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = new MicroDocument ();
      final String sNamespaceURI = "http://www.peppol.eu/schema/pd/businesscard/201806/";
      final IMicroElement aRoot = aDoc.appendElement (sNamespaceURI, "root");
      final Query aQuery = PDQueryManager.andNotDeleted (new MatchAllDocsQuery ());
      PDMetaManager.getStorageMgr ()
                   .searchAllDocuments (aQuery,
                                        -1,
                                        x -> aRoot.appendChild (x.getAsBusinessCard ()
                                                                 .getAsMicroXML (sNamespaceURI, "businessentity")));
      res.xml (aDoc);
      res.attachment ("directory-business-entities.xml");
    });
  }

  public PageSecureParticipantCount (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant count");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Export all IDs", s_aExportAllIDs.getInvocationURL (aRequestScope), EDefaultIcon.SAVE);
      aToolbar.addButton ("Export all Business Entities",
                          s_aExportAllBCs.getInvocationURL (aRequestScope),
                          EDefaultIcon.SAVE);
      aNodeList.addChild (aToolbar);
    }

    final int nCount = PDMetaManager.getStorageMgr ().getContainedParticipantCount ();
    aNodeList.addChild (new HCH3 ().addChild (nCount + " participants are contained"));

    final int nReIndexCount = PDMetaManager.getIndexerMgr ().getReIndexList ().getItemCount ();
    aNodeList.addChild (new HCH3 ().addChild (nReIndexCount + " re-index items are contained"));

    final int nDeadCount = PDMetaManager.getIndexerMgr ().getDeadList ().getItemCount ();
    aNodeList.addChild (new HCH3 ().addChild (nDeadCount + " dead items are contained"));
  }
}
