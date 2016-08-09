/**
 * © Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
package com.ibm.gr.urbancode.plugin.siebel

import java.util.Enumeration;
import java.util.Properties;

import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;
import com.siebel.data.*;
/*
 * This is a utility class to wrap a few things that we need for the Siebel plugin. 
 */
class UCDSiebelUtility 
{
	private static myName="UCDSiebelUtility";
	/*
	 * A reference to the Siebel Java Data Bean (JDB) wrapper.
	 */
	SiebelJavaDataBeanWrapper m_dataBean = null;
	
	public UCDSiebelUtility(SiebelJavaDataBeanWrapper aDataBeanWrapper) {
		m_dataBean = aDataBeanWrapper;
	}
	
	/*
	 * Simple get Class for the Siebel JDB
	 */
	public SiebelJavaDataBeanWrapper getSiebelJDB()
	{
		return(m_dataBean);
	}
	
	/*
	 * We create the search string that we will use the searchSpec. 
	 * We use a simple Properties object for this. It's a generic way to 
	 * search for multiple fields.
	 */
	public String createSearchExpr(Properties dProps)
	{
		String dExpr="";
		String dName="";
		Enumeration dE=dProps.propertyNames();
		
		while(dE.hasMoreElements())
		{
			dName=dE.nextElement();
			if(dE.hasMoreElements())
			{
				dExpr += dName+"='"+dProps.getProperty(dName)+"' and ";
			}
			else
			{
				dExpr += dName+"='"+dProps.getProperty(dName)+"'";
			}
		}
		System.out.println(myName+": Build Search String {"+dExpr+"}");
		return dExpr;
	}
	
	/*
	 * We set the field values of the Siebel object based on the parameters.	 * 
	 */
	public void setFieldValues(SiebelBusComp dBusComp, Properties dProps)
	{
		Enumeration dE=dProps.propertyNames();
		String dName="";
		while(dE.hasMoreElements())
		{
			try
			{
				dName=dE.nextElement();
				System.out.println(myName+": BusComp:["+dBusComp.name()+"] Setting property:["+dName+"]=["+dProps.getProperty(dName)+"]");
				dBusComp.setFieldValue(dName, dProps.getProperty(dName));
			}
			catch (SiebelException e)
			{
				e.printStackTrace();
				System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
			}
		}
	}
	
	/*
	 * Just a utility method to print the business component values based on a Siebel property set.
	 */
	public void printBusCompValues(SiebelBusComp busComp, SiebelPropertySet propSet)
	{
		System.out.println(myName+": ["+busComp.name()+"] Dumping BusComp object contents");
		SiebelPropertySet myValues = m_dataBean.createPropertySet();
		busComp.getMultipleFieldValues(propSet, myValues);
		
		Enumeration names=myValues.getPropertyNames();
		
		while(names.hasMoreElements())
		{
			String fieldName = (String) names.nextElement();
			System.out.println(myName+": ["+busComp.name()+"] ["+fieldName+"]=["+busComp.getFieldValue(fieldName)+"]");
		}
	}
	
	/*
	 * Just a utility method to print the business component values based on a simple property set.
	 */
	public void printBusCompValues(SiebelBusComp busComp, Properties propSet)
	{
		Enumeration dE=propSet.propertyNames();
		String dName="";
		while(dE.hasMoreElements())
		{
			dName=dE.nextElement();
			try
			{
				System.out.println(myName+": BusComp:["+busComp.name()+"] ["+dName+"]=["+busComp.getFieldValue(dName)+"]");
			}
			catch (SiebelException e)
			{
				e.printStackTrace();
				System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
			}
		}
	}
	
	/*
	 * a method to parse the parameters, we expect a long line of name value pairs 
	 * separated by a semi-colon(;). For example:
	 * name=value;name=value;name=value;
	 */
	public void parseParameters(String sParams, Properties sProps)
	{
		String[] sLines=sParams.split(";");
		for(int i=0; i<sLines.length; i++)
		{
			String[] tokens=sLines[i].split("=");
			System.out.println(myName+": Line:["+sLines[i]+"] Name=["+tokens[0]+"] Value=["+tokens[1]+"]");
			sProps.put(tokens[0],tokens[1]);
		}
	}
		
	/*
	 * This is the main method to set the Object fields of a Business Component.
	 * 
	 * 
	 */
	
	public boolean setObjectFields(SiebelBusComp lBusComp, SiebelPropertySet lPropSet, String lQuery, Properties lProps, boolean lIsUpd)
	{
		lBusComp.activateMultipleFields(lPropSet);
		
		//Let's clear any other query we had before.
		lBusComp.clearToQuery();
		
		System.out.println(myName+": Set Search Expression");
		if(lBusComp.setSearchExpr(lQuery))
		{
			System.out.println(myName+": Execute Query");
			if(lBusComp.executeQuery(false))
			{
				//Let's see if we found anything.
				boolean myFlag=lBusComp.firstRecord();
				//IF this is an update and we haven't found a record, we should exit.
				if(!myFlag && lIsUpd)
				{
					System.out.println(myName+": Exiting - Updating a non-existent object");
					return false;
				}
				//If it's no new record is found create one.
				else if(!myFlag)
				{
					System.out.println(myName+": Nothing found, create object.");
					lBusComp.newRecord(false);
				}
				//found a record let's update it.
				else
				{
					System.out.println(myName+": Found record, continuing...");
				}
								
				System.out.println(myName+": What have we got?");
				printBusCompValues(lBusComp, lPropSet);
				System.out.println(myName+": Setting Fields");
				//Let's set the field values for the record.
				setFieldValues(lBusComp, lProps);
				System.out.println(myName+": What have we got now?");
				printBusCompValues(lBusComp, lPropSet);
				System.out.println(myName+": Saving");
				//Save the record.
				lBusComp.writeRecord();
			}
			else
			{
				System.out.println(myName+": Execute Query - failed");
			}
		}
		return true;
	}
	
	/*
	 * This is the main method to set a Business Component parameter object.
	 * 
	 * 
	 */
	public boolean setObjectParamFields(SiebelBusComp lBusComp, SiebelPropertySet lPropSet, Properties lObjProps, Properties lProps, String propName, String propValue, String objFieldName)
	{
		System.out.println(myName+": searching and setting parameters for parameters");
		
		String lName="";
		String commonExpr=new String(objFieldName+"='"+lObjProps[objFieldName]+"'");
		Enumeration paramEnum=lProps.propertyNames();
		while(paramEnum.hasMoreElements())
		{
			lName=paramEnum.nextElement();
			String mySearchStringParams=commonExpr+" and "+propName+"='"+lName+"'";
			
			//Need to figure out how the parameters work properly....
			System.out.println(myName+": Search String:"+mySearchStringParams);
			
			lBusComp.activateMultipleFields(lPropSet);
			
			lBusComp.setSearchExpr(mySearchStringParams);
			lBusComp.clearToQuery();
			
			if(lBusComp.executeQuery(false))
			{
				if(lBusComp.firstRecord())
				{
					printBusCompValues(lBusComp, lPropSet);
				}
				else
				{
					lBusComp.newRecord(false);
				}

				lBusComp.setFieldValue(propName, lName);
				lBusComp.setFieldValue(propValue, lProps.getProperty(lName));

				printBusCompValues(lBusComp, lPropSet);
				lBusComp.writeRecord();
			}
		}
		return true;
	}
	
	/*
	 * Activate some fields for a Business Component bases on some properties. 
	 * If we don't activate the fields the JDB will return them empty.
	 */
	void activateFields(SiebelBusComp lBusComp, Properties lProps)
	{
		Enumeration dE=lProps.propertyNames();
		String dName="";
		while(dE.hasMoreElements())
		{
			try
			{
				dName=dE.nextElement();
				System.out.println(myName+": BusComp:["+lBusComp.name()+"] Activating Field:["+dName+"]");
				lBusComp.activateField(dName);
			}
			catch (SiebelException e)
			{
				e.printStackTrace();
				System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
			}
		}
	}
	
	/*
	 * This method will take a buscomp, two sets of properties and a field name and will do the following:
	 * Search the buscomp for an object. Get the associated buscomp using the field. Search for an object 
	 * in the associated component and if it finds one, it will associate it with the initial component.
	 * For example, Responsibilities and Views (see Application_Responsibility_View_associate).
	 */
	boolean associateBusComp(SiebelBusComp lBusComp, Properties mainProps, Properties assocProps, String assocField)
	{
		String mainSearch=createSearchExpr(mainProps);
		String assocSearch=createSearchExpr(assocProps);
		try
		{	
			lBusComp.setSearchExpr(mainSearch);
			activateFields(lBusComp, mainProps);
			//lBusComp.clearToQuery();
			if(lBusComp.executeQuery(false))
			{
				if(lBusComp.firstRecord())
				{
					println(myName+" ["+lBusComp.name()+"] Found record matching:["+mainSearch+"]");
					printBusCompValues(lBusComp,mainProps)
					println(myName+": Getting reference to associated Business Object for Field:"+assocField)
					SiebelBusComp assocBC=lBusComp.getMVGBusComp(assocField).getAssocBusComp();
					assocBC.setSearchExpr(assocSearch);
					activateFields(assocBC, assocProps);
					//assocBC.clearToQuery();
					if(assocBC.executeQuery(false))
					{
						if(assocBC.firstRecord())
						{
							println(myName+" ["+assocBC.name()+"] Found record matching:["+assocSearch+"]");
							printBusCompValues(assocBC,assocProps)
							try
							{
								assocBC.associate(false);
							}
							catch (SiebelException e)
							{
								println(myName+" ["+assocBC.name()+"] Failed to associate Business Comp");
								if(e.getErrorCode().toString().equalsIgnoreCase("7668069"))
								{
									println(myName+" ["+assocBC.name()+"] association already exists");
									return true;
								}
								else
								{
									return false;
								}
							}
						}
						else
						{
							println(myName+" ["+assocBC.name()+"] Cannot find any objects matching:["+assocSearch+"]");
							return false;
						}
					}
					else
					{
						println(myName+" ["+assocBC.name()+"] Cannot execute query]");
						return false;
					}
				}
				else
				{
					println(myName+" ["+lBusComp.name()+"] Cannot find any objects matching:["+mainSearch+"]");
					return false;
				}
			}
			else
			{
				println(myName+" ["+lBusComp.name()+"] Cannot execute query]");
				return false;
			}
		}
		catch (SiebelException e)
		{
			e.printStackTrace();
			System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
			return false;
		}

		return true;
	}

	/*
	 * Simple method to create a Siebel Property Set. It creates empty on purpose
	 * since sometime, we need an empty property set. There is anothe method to 
	 * set the values (see below).	
	 */
	public SiebelPropertySet createSiebelPropertySet(Properties lcProps)
	{
		println "${myName}: creating Siebel Property Set"
		SiebelPropertySet propSet = m_dataBean.createPropertySet()
		if(lcProps)
		{
			Enumeration dE=lcProps.propertyNames()
			String dName=""
			while(dE.hasMoreElements())
			{
				dName=dE.nextElement()
				propSet.setProperty(dName,"")
				println "${myName}: Initializing property [${dName}]"
			}
		}
		return propSet
	}
	
	/*
	 * Simple method to set the Siebel Property set values based on a Properties object.
	 */
	public void setSiebelPropertySet(SiebelPropertySet propSet, Properties lcProps)
	{
		Enumeration dE=lcProps.propertyNames()
		lcProps.get
		String dName=""
		while(dE.hasMoreElements())
		{
			dName=dE.nextElement()
			propSet.setProperty(dName, lcProps.getProperty(dName))
		}
	}
	
	/*
	 * Print all the properties in a Properties object.
	 */
	public void printProps(Properties lcProps)
	{
		Enumeration dE=lcProps.propertyNames()
		String dName=""
		while(dE.hasMoreElements())
		{
			dName=dE.nextElement()
			println "Properties: [${dName}]:[${lcProps.getProperty(dName)}]"
		}
	}
	
	/*
	 * Print all the properties in a Siebel Property set.
	 */
	public void printProps(SiebelPropertySet propSet)
	{
		String dName=propSet.getFirstProperty()
		while(dName!="")
		{
			println "Properties: [${dName}]:[${propSet.getProperty(dName)}]"
			dName=propSet.getNextProperty()
		}
	}
}
