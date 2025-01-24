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
String myName="Applications_Views_single";
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

SiebelBusObject m_BusObjViewAccess = null;
SiebelBusComp m_BusCompFeature = null;
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	//BUSINESS OBJECT NAME
	String busObjName = new String("View Access")
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusObj=m_dataBean.getBusObject(busObjName)
	if(m_BusObj==null)
	{
		return false
	}

	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	String busCompName = new String("Feature Access");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusComp=m_dataBean.getBusComp(busCompName, m_BusObj)
	if(m_BusComp==null)
	{
		return false
	}
	m_BusComp.setViewMode((int)3);
		
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
	Properties dProps=new Properties();
	dProps['Name']=props['ViewName'];
	dProps['Description']=props['ViewDescription'];
	dProps['Local Access']=props['ViewLocalAccess'];
	
	boolean isUpdate=props['Update'].equals("true")
	
	Properties dPropsSearch=new Properties();
	dPropsSearch['Name']=props['ViewName'];
	
	SiebelPropertySet mainSiebelProps=myUCDStepObject.createSiebelPropertySet(dProps)
	
	String mySearchString=null
	if(dPropsSearch)
	{
		mySearchString=myUCDStepObject.createSearchExpr(dPropsSearch)
	}
	else
	{
		mySearchString=myUCDStepObject.createSearchExpr(dProps)
	}
	
	if(!myUCDStepObject.setObjectFields(m_BusComp, mainSiebelProps, mySearchString, dProps, isUpdate))
	{
		System.out.println(myName+": doAction failed, check log upwards.")
		exitGracefullyWithFail.call()
	}
	
	System.out.println(myName+": Finish")

	m_BusComp.release()
	m_BusObj.release()
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
