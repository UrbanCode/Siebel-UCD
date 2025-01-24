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
import com.siebel.data.SiebelException;


def apTool = new AirPluginTool(this.args[0], this.args[1]) 
def props = apTool.getStepProperties();
String myName="ServerConfiguration_JobTemplates_single"
def siebObjList = []

//Connect to Siebel and get correct BusComp
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper()

System.out.println(myName+": Connect");
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards"
	System.exit(-1)
}
System.out.println(myName+": myUCDStepObject object")
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility (mySiebJDB)

SiebelBusObject m_BusObj = null
SiebelBusComp m_BusComp = null
SiebelBusComp m_BusCompParams = null
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	String myObjectName=null;
	//BUSINESS OBJECT NAME
	myObjectName = new String("Server Admin")
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusObj=m_dataBean.getBusObject(myObjectName)
	if(m_BusObj==null)
	{
		return false
	}
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusObj)
	
	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	myObjectName = new String("Component Job");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusComp=m_dataBean.getBusComp(myObjectName, m_BusObj)
	if(m_BusComp==null)
	{
		return false
	}
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusComp)
	
	//BUSINESS COMPONENT PARAMETER NAME - child of BUSINESS OBJECT
	myObjectName = new String("Component Job Parameter");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusCompParams=m_dataBean.getBusComp(myObjectName, m_BusObj)
	if(m_BusCompParams==null)
	{
		return false
	}
	//Add the object to a small list to know what to clean up later
	siebObjList.add(m_BusCompParams)
	
	return true
}

def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}
//BEGIN DOING THINGS -----------------------------------------------------------------------------------------

System.out.println(myName+": getObjects")
if(getObjects())
{
	System.out.println(myName+": SetProperties")
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	Properties dProps=new Properties()
	dProps['Name']=props['JobTempName']
	dProps['Short Name']=props['JobTempShortName']
	dProps['Description']=props['JobTempDescription']
	dProps['Component']=props['JobTempComp']
	dProps['Enabled?']=props['Enabled']
	/*
	 * Here's a tricky one, if I initialize the property I need to get the parameters, it's nulled in my context
	 * If I don't initialize it, then I get it correctly from the BusComp later, despite the fact that I haven't
	 * activated the Field. Welcome to Siebel JDB API...
	 */
	//dProps['Component Id']=""
	boolean isUpdate =props['Update'].equals("true")
	
	Properties dPropsSearch=new Properties()
	dPropsSearch['Name']=props['JobTempName']
	
	//We have a Parameters object, let's create some properties for it.
	Properties myBusObjParams=new Properties()
	System.out.println(myName+": parseParameter")
	myUCDStepObject.parseParameters(props['Parameters'].toString(),myBusObjParams)
	myUCDStepObject.printProps(myBusObjParams)
	
	System.out.println(myName+": doAction")
	
	Properties paramProps=new Properties()
	paramProps['Component Id']=""
	paramProps['Parameter Code']=""
	paramProps['Name']=""
	paramProps['Value']=""
	paramProps['Description']=""
	
	SiebelPropertySet mainSiebelProps=myUCDStepObject.createSiebelPropertySet(dProps)
	SiebelPropertySet paramSiebelProps=myUCDStepObject.createSiebelPropertySet(paramProps)
	
	/*
	 * If you want to search with all the properties, uncommment the dProps
	 * line, otherwise define only the fields you want to search in dPropsSearch 
	 * and uncomment that line.
	 * Careful, you need dProps for other things, don't get rid of it.
	 */
	//String mySearchString=myUCDStepObject.createSearchExpr(dProps)
	String mySearchString=myUCDStepObject.createSearchExpr(dPropsSearch)
	

	if(!myUCDStepObject.setObjectFields(m_BusComp, mainSiebelProps, mySearchString, dProps, isUpdate))
	{
		System.out.println(myName+": doAction failed, check log upwards.");
		exitGracefullyWithFail.call()
	}
	
	//Populate the correct property with the dynamically generated value.
	dProps['Component Id']=m_BusComp.getFieldValue("Component Id")
	System.out.println(myName+": searching and setting parameters for parameters");
	
	myUCDStepObject.setObjectParamFields(m_BusCompParams, paramSiebelProps, dProps, myBusObjParams, "Name", "Value", "Component Id");

	System.out.println(myName+": Finish");
	//Don't forget to release the objects you have created.
	m_BusCompParams.release();
	m_BusComp.release()
	m_BusObj.release()
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
//FINISH OF DOING THINGS ----------------------------------------------------------------------
System.out.println(myName+": disconnect");
mySiebJDB.disconnect();

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
