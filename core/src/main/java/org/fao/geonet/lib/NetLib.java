//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.lib;

import jeeves.server.context.ServiceContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.XmlRequest;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.setting.SettingManager;

import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

//=============================================================================

public class NetLib
{
	public static final String ENABLED  = "system/proxy/use";
	public static final String HOST     = "system/proxy/host";
	public static final String PORT     = "system/proxy/port";
	public static final String USERNAME = "system/proxy/username";
	public static final String PASSWORD = "system/proxy/password";
    public static final String IGNOREHOSTLIST = "system/proxy/ignorehostlist";

	//---------------------------------------------------------------------------
	//---
	//--- API methods
	//---
	//---------------------------------------------------------------------------

	public void setupProxy(ServiceContext context, XmlRequest req)
	{
		GeonetContext  gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		SettingManager sm = gc.getBean(SettingManager.class);

		setupProxy(sm, req);
	}

	//---------------------------------------------------------------------------
	/** Setup proxy for XmlRequest
	  */

	public void setupProxy(SettingManager sm, XmlRequest req)
	{
		boolean enabled = sm.getValueAsBool(ENABLED, false);
		String  host    = sm.getValue(HOST);
		String  port    = sm.getValue(PORT);
		String  username= sm.getValue(USERNAME);
		String  password= sm.getValue(PASSWORD);
        String ignoreHostList = sm.getValue(IGNOREHOSTLIST);

        if (!enabled) {
			req.setUseProxy(false);
		} else {
			if (!Lib.type.isInteger(port))
				Log.error(Geonet.GEONETWORK, "Proxy port is not an integer : "+ port);
			else
			{
                if (!isProxyHostException(req.getHost(), ignoreHostList)) {
                    req.setUseProxy(true);
                    req.setProxyHost(host);
                    req.setProxyPort(Integer.parseInt(port));
                    if (username.trim().length()!=0) {
                        req.setProxyCredentials(username, password);
                    }
                } else {
                    Log.info(Geonet.GEONETWORK, "Proxy configuration ignored, host: "+ req.getHost() + " is in proxy ignore list");
                    req.setUseProxy(false);
                }

			}
		}
	}

	//---------------------------------------------------------------------------

	public CredentialsProvider setupProxy(ServiceContext context, HttpClientBuilder client, String requestHost)
	{
		GeonetContext  gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		SettingManager sm = gc.getBean(SettingManager.class);

		return setupProxy(sm, client, requestHost);
	}

	//---------------------------------------------------------------------------

	/** Setup proxy for http client
	  */
	public CredentialsProvider setupProxy(SettingManager sm, HttpClientBuilder client, String requestHost)
	{
		boolean enabled = sm.getValueAsBool(ENABLED, false);
		String  host    = sm.getValue(HOST);
		String  port    = sm.getValue(PORT);
		String  username= sm.getValue(USERNAME);
		String  password= sm.getValue(PASSWORD);
        String ignoreHostList = sm.getValue(IGNOREHOSTLIST);

        CredentialsProvider provider = new BasicCredentialsProvider();
        if (enabled) {
            if (!Lib.type.isInteger(port)) {
                Log.error(Geonet.GEONETWORK, "Proxy port is not an integer : "+ port);
            } else {
                if (!isProxyHostException(requestHost, ignoreHostList)) {
                    final HttpHost proxy = new HttpHost(host, Integer.parseInt(port));
                    client.setProxy(proxy);

                    if (username.trim().length() != 0) {
                        provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(username, password));
                        client.setDefaultCredentialsProvider(provider);
                    }
                } else {
                    client.setProxy(null);
                }

			}
		}

        return provider;
	}

	//---------------------------------------------------------------------------

	public void setupProxy(ServiceContext context)
	{
		GeonetContext  gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		SettingManager sm = gc.getBean(SettingManager.class);

		setupProxy(sm);
	}

	//---------------------------------------------------------------------------

	/** Setup proxy for http client
	  */
	public void setupProxy(SettingManager sm)
	{
		String  host    = sm.getValue(HOST);
		String  port    = sm.getValue(PORT);
		String  username= sm.getValue(USERNAME);
        String ignoreHostList = sm.getValue(IGNOREHOSTLIST);

        Properties props = System.getProperties();
		props.put("http.proxyHost", host);
		props.put("http.proxyPort", port);
        props.put("http.nonProxyHosts", ignoreHostList);

        if (username.trim().length() > 0) {
			Log.error(Geonet.GEONETWORK, "Proxy credentials cannot be used");
		}

	}

	//---------------------------------------------------------------------------

	/**
	 * Setups proxy for java.net.URL.
	 *
	 * @param context
	 * @param url
	 * @return
	 * @throws IOException
     */
	public URLConnection setupProxy(ServiceContext context, URL url) throws IOException
	{
		GeonetContext  gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		SettingManager sm = gc.getBean(SettingManager.class);

		boolean enabled = sm.getValueAsBool(ENABLED, false);
		String  host    = sm.getValue(HOST);
		String  port    = sm.getValue(PORT);
		String  username= sm.getValue(USERNAME);
		String  password= sm.getValue(PASSWORD);
		String ignoreHostList = sm.getValue(IGNOREHOSTLIST);

		URLConnection conn = null;
		if (enabled) {
			if (!Lib.type.isInteger(port)) {
				Log.error(Geonet.GEONETWORK, "Proxy port is not an integer : "+ port);
			} else {
				if (!isProxyHostException(url.getHost(), ignoreHostList)) {

					InetSocketAddress sa = new InetSocketAddress(host, Integer.parseInt(port));
					Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
					conn = url.openConnection(proxy);

					if (username.trim().length() != 0) {
						String encodedUserPwd = new Base64().encodeAsString((username + ":" + password).getBytes());
						conn.setRequestProperty("Accept-Charset", "UTF-8");
						conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
					}

				} else {
					conn = url.openConnection();
				}
			}
		} else {
			conn = url.openConnection();
		}

		return conn;
	}
	//---------------------------------------------------------------------------

	public boolean isUrlValid(String url)
	{
		try {
			new URL(url);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}

    //---------------------------------------------------------------------------

    /**
     * Checks if a host matches a ignore host list.
     *
     * Ignore host list format should be: string with host names or ip's separated by | that allows wildcards.
     *
     * @param requestHost
     * @param ignoreHostList
     * @return
     */
    public boolean isProxyHostException(String requestHost, String ignoreHostList) {
        if (StringUtils.isEmpty(requestHost)) return false;
        if (StringUtils.isEmpty(ignoreHostList)) return false;

        try {
            return (requestHost.matches(ignoreHostList));
        } catch (PatternSyntaxException ex) {
            Log.error(Geonet.GEONETWORK + ".httpproxy", "Proxy ignore host list expression is not valid: " + ex.getMessage());
        }

        return false;
    }
}

//=============================================================================
