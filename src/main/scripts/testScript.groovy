/**
 * © Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.siebel.data.*;
import com.siebel.data.SiebelException;

SiebelJavaDataBeanWrapper siebJDB = new SiebelJavaDataBeanWrapper();
//This is the hard coded connect string. Change it here to connect to something else.
siebJDB.connect("192.168.203.46", "2321", "siebeldev", "EAIObjMgr_enu", "sadmin", "osiebdev");

//Defined the Siebel Component and objects here.
SiebelBusObject siebBusObj=siebJDB.getBusObject("Comm Package");
SiebelBusComp siebBusComp=siebJDB.getBusComp("Comm Package", siebBusObj);
//Let's see all the organizations in Siebel by setting the view mode to 3
siebBusComp.setViewMode((int)3)

Properties props=new Properties()

//Fields of the Siebel Object 
props['Bookmark Flag']=""
props['Business Object Name']=""
props['Comm Method']=""
props['Comments']=""
props['Create Activiy Flag']=""
props['Default Flag']=""
props['Delivery Profile']=""
props['Delivery Profile Id']=""
props['Display Template Text']=""
props['Draft Status']=""
props['HTML Format Flag']=""
props['LOV Type']=""
props['Language Code']=""
props['Locale Code']=""
props['Locale Id']=""
props['Media Type']=""
props['Name']=""
props['Offer Number']=""
props['Public Flag']=""
props['Recipient BusComp']=""
props['Recipient BusComp']=""
props['Section Type']=""
props['Sent Time']=""
props['Sequence Number']=""
props['Status']=""
props['Subject Text']=""
props['Substitutions']=""
props['Template Text']=""

/*
 * NOTE: We are not using the UCDSiebelUtility here on purpose. I usually use this
 * or a copy of this to test my assumptions on the Siebel Java Data Bean, so it is 
 * as bare bones as possible.
 * The utility library is used for the proper steps in order to make the maintenance easier.
 */

//closure to activate all the fields in the props.
def activateFields = { Properties lProps ->
	String dName="";
	Enumeration dE=lProps.propertyNames();
	
	while(dE.hasMoreElements())
	{
		dName=dE.nextElement();
		println "PropertyName: ${dName}"
		siebBusComp.activateField(dName)
	}
}

/*
 * Closure to print all the fields in the props. Don't worry about 
 * end of file errors if you see any, I can't be bothered to catch the 
 * exception for the test script.
 */
def printComp = { Properties lProps ->
	println "BEGIN---------------------------"
	String dName="";
	Enumeration dE=lProps.propertyNames();
	
	while(dE.hasMoreElements())
	{
		dName=dE.nextElement();
		println "${siebBusComp.name()} ${dName}=${siebBusComp.getFieldValue(dName)}"
	}
	println "END---------------------------"
}

activateFields(props)

def count=1
//Set the search spec for the business component.
if(siebBusComp.setSearchSpec("Name",""))
{
	if(siebBusComp.executeQuery(false))
	{
		if(siebBusComp.firstRecord())
		{
			println "COUNT: ${count}"
			printComp(props)
			count++
			while(siebBusComp.nextRecord())
			{
				println "COUNT: ${count}"
				printComp(props)
				count++
			}
		}
	}
}

siebBusComp.release()
siebBusObj.release()
siebJDB.disconnect();