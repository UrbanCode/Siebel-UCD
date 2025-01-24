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
def props = apTool.getStepProperties();

/* This is how you retrieve properties from the object. You provide the "name" attribute of the 
 * <property> element 
 * 
 */
String myName="ImportSiebelMessage_single";
def siebObjList = []

//Connect to Siebel and get correct BusComp
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper();

System.out.println(myName+": Connect");
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards";
	System.exit(-1);
}

System.out.println(myName+": myUCDStepObject object");
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility(mySiebJDB);

SiebelService  eaiXmlConv = null;
SiebelService  eaiAdapt = null;
SiebelPropertySet m_FieldSetIN = null;
SiebelPropertySet m_FieldSetMID = null;
SiebelPropertySet m_FieldSetOUT = null;
String m_sMethod = null;

SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	//GET SERVICES
	eaiXmlConv=m_dataBean.getBusService("EAI XML Read from File");
	if(eaiXmlConv==null)
	{
		return false;
	}
	eaiAdapt=m_dataBean.getBusService("EAI Siebel Adapter");
	if(eaiAdapt==null)
	{
		return false;
	}
	
	//Add the object to a small list to know what to clean up later
	siebObjList.add(eaiXmlConv)
	siebObjList.add(eaiAdapt)
	return true
}

def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}

System.out.println(myName+": getObjects");
if(getObjects())
{
	System.out.println(myName+": SetProperties");
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	//Here we set the properties of the object we want to set.
	//We do this because we need to break out the step specific properties from the 
	//server specific properties in order to set only the correct ones
	//to the 
	Properties dProps=new Properties();
	dProps['FullPath']=props['SM_XMLPath'];
	
	m_FieldSetIN = myUCDStepObject.createSiebelPropertySet(dProps)
	m_FieldSetMID = myUCDStepObject.createSiebelPropertySet((Properties)null)
	m_FieldSetOUT = myUCDStepObject.createSiebelPropertySet((Properties)null)
	
	if(m_FieldSetIN==null || m_FieldSetMID==null || m_FieldSetOUT==null)
	{
		System.out.println(myName+": createFieldProperties failed, check log upwards.")
		exitGracefullyWithFail()
	}
	
	m_sMethod="ReadEAIMsg"

	System.out.println(myName+": doAction");

	String lOutputPropName=new String();
	// TODO See if we can make a generic method out of this...
	System.out.println(myName+": doAction Starting");
	
	String sFileName=dProps['FullPath'].toString();
	System.out.println(myName+": Parsing file:["+sFileName+"] ");
					
	m_FieldSetIN.setProperty("FileName", sFileName);
	
	//This is the method to read an EAI msg for the "EAI XML Read from File" service. 
	//We can identify which objects these are by the <SiebelMessage> tag they contain.
	//m_sMethod="ReadEAIMsg";
	//This method will be used at some point to read in generic XML files.
	//m_sMethod="ReadXMLHier";	
	try
	{
		//Let's read the XML file in. It needs to be local to the server that runs the Siebel Server. 
		eaiXmlConv.invokeMethod(m_sMethod, m_FieldSetIN, m_FieldSetMID);			
	}
	catch (SiebelException e)
	{
		e.printStackTrace();
		System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		exitGracefullyWithFail()
	}
			
	System.out.println(myName+": Output of XML read:["+m_FieldSetMID.toString()+"]");
	
	/*
	 * This is the method to commit the XML to the repository for the "EAI Siebel Adapter" service.
	 * The options here are as follows (from this link-http://docs.oracle.com/cd/B40099_02/books/EAI2/EAI2_UseEAIAdapt3.html#wp250102)
	 * Synchronize:
		You can use the Synchronize method to make the values in a business object instance match those of an integration object instance. This operation can result in
		updates, insertions, or deletions in the business components. The following rules apply to the results of this method:
		
		If a child component is not present in the integration object instance, the corresponding child business component rows are left untouched.
		If the integration object instance's child component has an empty container, then all child records in the corresponding business component are deleted.
		For a particular child component, records that exist in both the integration object instance and business component are updated. Records that exist in the 
		integration object hierarchy and not in the business component are inserted. Records in the business component and not in the integration object instance 
		are deleted.
		Only the fields specified in the integration component instance are updated.
	 * Insert:
		This method is also similar to the Synchronize method with the exception that the EAI Siebel Adapter generates an error if a matching root component is
		found; otherwise, it inserts the root component and synchronizes all the children. It is important to note that when you insert a record, there is a 
		possibility that the business component would create default children for the record, which will be removed by the Insert method. The Insert method 
		synchronizes the children, which deletes all the default children. For example, if you insert an account associated with a specific organization, it
		will also be automatically associated with a default organization. As part of the Insert method, the EAI Siebel Adapter deletes the default association, 
		and associates the new account with only the organization that was originally defined in the input integration object instance. The EAI Siebel Adapter 
		achieves this by synchronizing the children.
	 * Upsert:
		The Upsert method is similar to the Synchronize method with one exception; the Upsert method does not delete any records.
		
		The Upsert method will result in insert or update operations. If the record exists, it will be updated. If the record does not exist, 
		it will be inserted. Unlike the Synchronize method, upsert will not delete any children.
		
		To determine if an update or insert is performed, the EAI Siebel Adapter runs a query using user keys fields or the search specifications to 
		determine if the parent or primary record already exists. If the parent record exists, it will be updated. If no matching parent record is found, 
		the new record will be inserted. Once again, upsert will not delete any children. If existing children are found, they are updated.
	 * Update:
		This method is similar to the Synchronize method, except that the EAI Siebel Adapter returns an error if no match is found for the root component; 
		otherwise, it updates the matching record and synchronizes all the children.
		NOTE:  During an update operation, the EAI Siebel Adapter expects a single record to be returned from the user key search. If more than one record is returned, 
		EAI Siebel Adapter throws an error.
	 */
	m_sMethod="Upsert";
	try
	{
		eaiAdapt.invokeMethod(m_sMethod, m_FieldSetMID, m_FieldSetOUT);			
	}
	catch (SiebelException e)
	{
		e.printStackTrace();
		System.out.println(myName+": SiebelErrorCode:["+e.getErrorCode()+"] ErrorMessage:"+e.getErrorMessage());
		exitGracefullyWithFail()
	}

	System.out.println(myName+": Output:["+m_FieldSetOUT.toString()+"]");
	System.out.println(myName+": doAction Ending");
	
	System.out.println(myName+": Finish");
	//release all the objects before exiting.
	eaiXmlConv.release()
	eaiAdapt.release()
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
System.out.println(myName+": disconnect");
mySiebJDB.disconnect();

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
