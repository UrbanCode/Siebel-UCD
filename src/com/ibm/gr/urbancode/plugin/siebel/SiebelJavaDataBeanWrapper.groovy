/**
 * © Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.ibm.gr.urbancode.plugin.siebel

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;
import java.text.Normalizer;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.siebel.data.*;
import com.siebel.data.SiebelException;

class SiebelJavaDataBeanWrapper {
	private SiebelDataBean m_dataBean = null;
	private myName="SiebelJavaDataBeanWrapper";
	
	public SiebelJavaDataBeanWrapper() {
		System.out.println(myName+": Creating Bean");
		m_dataBean = new SiebelDataBean();
		// TODO Auto-generated constructor stub
	}

	public boolean connect(String server, String port, String enterprise, String objMgr, String user, String pass)
	{
		try
		{
			String connString = new String ("siebel.TCPIP.None.None://"+server+":"+port+"/"+enterprise+"/"+objMgr);
			System.out.println(myName+": Connect string set to ["+connString+"]");
			m_dataBean.login(connString, user, pass, "enu");
			System.out.println("Logged In");
			
			return true;
		}
		catch (SiebelException e)
		{
			e.printStackTrace();
			System.out.println("SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			System.out.println("SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		}
		
		return false
	}

	public boolean disconnect()
	{
		try
		{
			System.out.println("Disconnecting");
			return m_dataBean.logoff();
		}
		catch (SiebelException e)
		{
			e.printStackTrace();
			System.out.println("SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		}
		return false;
	}
	
	public SiebelDataBean getDataBean() {
		return m_dataBean;
	}

	public SiebelBusObject getBusObject(String busObjectName) {
		System.out.println("Getting Business Object:["+busObjectName+"]");
		SiebelBusObject busObj = null;
		busObj = m_dataBean.getBusObject(busObjectName); //Is name the only way to get a reference to this?
		return busObj;
	}

	public SiebelBusComp getBusComp(String busCompName, SiebelBusObject busObj) {
		System.out.println("Getting Business Component:["+busCompName+"]");
		SiebelBusComp busComp = null;
		busComp = busObj.getBusComp(busCompName); //Is this always the same as BusObject?
		return busComp;
	}
	
	public SiebelService getBusService(String busServiceName)
	{
		SiebelService myService=null;
		
		System.out.println("Getting Business Service:["+busServiceName+"]");
		
		try
		{
			myService=m_dataBean.getService(busServiceName);
		}
		catch (SiebelException e)
		{
			e.printStackTrace();
			System.out.println("SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		}
		return myService;
	}
	
	public SiebelPropertySet createPropertySet()
	{
		SiebelPropertySet localPS=null;
		try
		{
			localPS=m_dataBean.newPropertySet();
		}
		catch (SiebelException e)
		{
			e.printStackTrace();
			System.out.println("SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		}
		return localPS;
	}
	
}
