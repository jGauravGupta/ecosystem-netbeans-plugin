/*
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.netbeans.modules.payara.common.nodes.actions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.payara.tooling.admin.ResultString;
import org.netbeans.modules.payara.spi.ServerUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.NodeAction;

/**
 * @author Gaurav Gupta
 */
public class EnableCDIProbeModeAction extends NodeAction {

    private static final String WELD_PROBE_URL = "/weld-probe";
    @Override
    protected void performAction(Node[] nodes) {
        if((nodes == null) || (nodes.length < 1)) {
            return;
        }

        targets.clear();
        for (Node n : nodes) {
            targets.add(n.getDisplayName());
        }
        String aDup = getDup(targets);
        if (null != aDup) {
            // open dialog
            NotifyDescriptor m = new NotifyDescriptor.Message(NbBundle.getMessage(EnableCDIProbeModeAction.class, "ERR_HAS_DUPS", aDup),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(m);
            return;
        }

        RequestProcessor enabler = new RequestProcessor("pf-enable-cdi-probe-mode");
        
        for(Node node : nodes) {
            EnableCDIProbeModeCookie uCookie = node.getCookie(EnableCDIProbeModeCookie.class);
            final OpenURLActionCookie oCookie = node.getCookie(OpenURLActionCookie.class);
            
            if(uCookie != null) {
                final Future<ResultString> result = uCookie.enableCDIProbeMode();
                final Node pNode = node.getParentNode().getParentNode();
                final Node fnode = node;

                enabler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            result.get(ServerUtilities.ACTION_TIMEOUT, ServerUtilities.ACTION_TIMEOUT_UNIT);
                            if (oCookie != null) {
                                try {
                                    HtmlBrowser.URLDisplayer.getDefault().showURL(new URL(oCookie.getWebURL() + WELD_PROBE_URL));
                                } catch (MalformedURLException ex) {
                                    Logger.getLogger("payara").log(Level.INFO, ex.getLocalizedMessage(), ex);
                                }
                            }
                        } catch(TimeoutException ex) {
                            Logger.getLogger("payara").log(Level.WARNING, "Enable CDI probe mode action timed out for {0}", fnode.getDisplayName());
                        } catch (InterruptedException ie) {
                            // we can ignore this
                        }catch(Exception ex) {
                            Logger.getLogger("payara").log(Level.INFO, ex.getLocalizedMessage(), ex);
                        }
                    }
                });
            }
        }
    }

    private List<String> targets = new ArrayList<String>();

    @Override
    protected boolean enable(Node[] nodes) {
        for(Node node : nodes) {
            EnableCDIProbeModeCookie cookie = node.getCookie(EnableCDIProbeModeCookie.class);
            if(cookie == null || cookie.isRunning()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(EnableCDIProbeModeAction.class, "LBL_EnableCDIProbeModeAction");
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public org.openide.util.HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    static String getDup(List<String> targets) {
        Map<String,String> uniqTargets = new HashMap<>();
        if (null == targets) {
            return null;
        }
        for (String target : targets) {
            int colon = target.indexOf(":");
            if (-1 == colon) {
                colon = target.length();
            }
            String shortName = target.substring(0,colon);
            if (uniqTargets.containsKey(shortName)) {
                return shortName;
            } else {
                uniqTargets.put(shortName,shortName);
            }
        }
        return null;
    }
}
