/**
 * (c) Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.ibm.gr.urbancode.plugin.siebel.UCDSiebelUtility;
import com.ibm.gr.urbancode.plugin.siebel.AirPluginTool;
import com.urbancode.air.CommandHelper;
import com.siebel.data.*;

/* This gets us the plugin tool helper. 
 * This assumes that args[0] is input props file and args[1] is output props file.
 * By default, this is true. If your plugin.xml looks like the example. 
 * Any arguments you wish to pass from the plugin.xml to this script that you don't want to 
 * pass through the step properties can be accessed using this argument syntax
 */
def apTool = new AirPluginTool(this.args[0], this.args[1])

/* Here we call getStepProperties() to get a Properties object that contains the step properties
 * provided by the user. 
 */
def props = apTool.getStepProperties()

/* 
 * The Business Services are slightly different, since we use a Service to 
 * read the XML, but then we put the resulting property set into a Business Component
 */
String myName="BusinessService_ImportBSFromXML";
def siebObjList = []

//Connect to Siebel and get correct BusComp
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper()

System.out.println(myName+": Connect")
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards"
	System.exit(-1)
}

System.out.println(myName+": myUCDStepObject object")
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility(mySiebJDB)

SiebelService  m_BusServ = null
SiebelBusObject m_BusObj = null;
SiebelBusComp m_BusComp = null;
SiebelPropertySet m_FieldSetIN = null
SiebelPropertySet m_FieldSetOUT = null
String m_sMethod = null
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	//BUSINESS SERVICE NAME
	String busObjName = new String("EAI XML Read from File")
	m_BusServ=m_dataBean.getBusService(busObjName)
	if(m_BusServ==null)
	{
		return false;
	}	
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusServ)
	//BUSINESS OBJECT NAME
	busObjName = new String("Business Service");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusObj=m_dataBean.getBusObject(busObjName);
	if(m_BusObj==null)
	{
		return false;
	}
	siebObjList.add(m_BusObj)
	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	busCompName = new String("Business Service");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusComp=m_dataBean.getBusComp(busCompName, m_BusObj);
	if(m_BusComp==null)
	{
		return false;
	}
	siebObjList.add(m_BusComp)
	return true
}

def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}


System.out.println(myName+": getObjects")
if(getObjects())
{
	System.out.println(myName+": SetProperties");

	m_sMethod="ReadPropSet"
	
	Properties dProps=new Properties()
	dProps['FileName']=props['SM_XMLPath']
	
	m_FieldSetIN=myUCDStepObject.createSiebelPropertySet(dProps)
	myUCDStepObject.setSiebelPropertySet(m_FieldSetIN, dProps)
	
	m_FieldSetOUT=myUCDStepObject.createSiebelPropertySet((Properties)null)
	if(m_FieldSetIN==null)
	{
		System.out.println(myName+": createFieldProperties failed, check log upwards.")
		exitGracefullyWithFail()
	}

	System.out.println(myName+": doAction")

	String lOutputPropName=new String()
	System.out.println(myName+": doAction Starting")

	try
	{
		m_BusServ.invokeMethod(m_sMethod, m_FieldSetIN, m_FieldSetOUT)
	}
	catch (SiebelException e)
	{
		e.printStackTrace()
		System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage())
		exitGracefullyWithFail()
	}
	
	myUCDStepObject.printProps(m_FieldSetOUT)
	System.out.println(myName+": doAction Ending")
	
	//Let's create a search string from the properties of the XML file.
	Properties busServProps=new Properties()
	busServProps['Display Name']=m_FieldSetOUT.getProperty("Display Name")
	busServProps['Name']=m_FieldSetOUT.getProperty("Name")
	myUCDStepObject.printProps(busServProps)
	String myServSearchString=myUCDStepObject.createSearchExpr(busServProps)
	
	myUCDStepObject.setObjectFields(m_BusComp, m_FieldSetOUT, myServSearchString, busServProps, false)
	
	System.out.println(myName+": Finish")
	m_BusServ.release()
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
System.out.println(myName+": disconnect")
mySiebJDB.disconnect()

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");
apTool.storeOutputProperties() //write the output properties to the file