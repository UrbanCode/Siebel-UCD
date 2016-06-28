/**
 * © Copyright IBM Corporation 2015.
 * This is licensed under the following license.
 * The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
import com.ibm.gr.urbancode.plugin.siebel.SiebelJavaDataBeanWrapper;
import com.ibm.gr.urbancode.plugin.siebel.UCDSiebelUtility;
import com.urbancode.air.AirPluginTool;
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
String myName="Enterprises_ProfileConfiguration_single";
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

SiebelBusObject m_BusObj = null;
SiebelBusComp m_BusComp = null;
SiebelBusComp m_BusCompParams = null;
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	//BUSINESS OBJECT NAME
	String busObjName = new String("Server Admin");
	m_BusObj=m_dataBean.getBusObject(busObjName);
	if(m_BusObj==null)
	{
		return false;
	}

	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	String busCompName = new String("SA-VBC Named Subsystem");
	m_BusComp=m_dataBean.getBusComp(busCompName, m_BusObj);
	if(m_BusComp==null)
	{
		return false;
	}

	//BUSINESS COMPONENT PARAMETERS - child of BUSINESS OBJECT
	String busCompParamName = new String("SA-VBC Named Subsystem Parameter");
	m_BusCompParams=m_dataBean.getBusComp(busCompParamName, m_BusObj);
	if(m_BusCompParams==null)
	{
		return false;
	}
	siebObjList.add(m_BusObj)
	siebObjList.add(m_BusComp)
	siebObjList.add(m_BusCompParams)
			
	return true;
}

def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}

Properties myBusObjParams=new Properties();
System.out.println(myName+": parseParameter");
myUCDStepObject.parseParameters(props['Parameters'].toString(),myBusObjParams);

System.out.println(myName+": getObjects");
if(getObjects())
{
	System.out.println(myName+": SetProperties")
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	Properties dProps=new Properties()
	dProps['NSS_NAME']=props['Profile']
	dProps['NSS_ALIAS']=props['Alias']
	dProps['SS_ALIAS']=props['SubsystemType']
	dProps["SS_SRD_SAID"]=""
	dProps["NSS_DESC"]=""
	
	Properties dParamProps=new Properties()
	dParamProps["NSS_ALIAS"]=""
	dParamProps["PA_ALIAS"]=""
	dParamProps["PA_DATATYPE"]=""
	dParamProps["PA_DESC"]=""
	dParamProps["PA_DISP_SETLEVEL"]=""
	dParamProps["PA_NAME"]=""
	dParamProps["PA_SAID"]=""
	dParamProps["PA_SCOPE"]=""
	dParamProps["PA_SETLEVEL"]=""
	dParamProps["PA_SUBSYSTEM"]=""
	dParamProps["PA_VALUE"]=""
	
	boolean isUpdate =props['Update'].equals("true")
	
	System.out.println(myName+": setParameters");
	SiebelPropertySet m_FieldSet=myUCDStepObject.createSiebelPropertySet(dProps)
	SiebelPropertySet m_FieldSetParam=myUCDStepObject.createSiebelPropertySet(dParamProps)
	
	System.out.println(myName+": doAction");
	
	// TODO See if we can make a generic method out of this...
	String mySearchString=null;
	mySearchString=myUCDStepObject.createSearchExpr(dProps);

	if(!myUCDStepObject.setObjectFields(m_BusComp, m_FieldSet, mySearchString, dProps, isUpdate))
	{
		System.out.println(myName+": doAction failed, check log upwards.")
		exitGracefullyWithFail.call()
	}
	System.out.println(myName+": searching and setting parameters for parameters");
	
	myUCDStepObject.setObjectParamFields(m_BusCompParams, m_FieldSetParam, dProps, myBusObjParams, "PA_NAME", "PA_VALUE", "NSS_ALIAS");

	System.out.println(myName+": Finish");
	m_BusCompParams.release();
	m_BusComp.release();
	m_BusObj.release();
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
