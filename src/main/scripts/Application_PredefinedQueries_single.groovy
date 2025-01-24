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

def apTool = new AirPluginTool(this.args[0], this.args[1]) 
def props = apTool.getStepProperties()

String myName="Application_PredefinedQueries_single"
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
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility (mySiebJDB)

SiebelBusObject m_BusObj = null
SiebelBusComp m_BusComp = null
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

def getObjects = {
	//BUSINESS OBJECT NAME
	String busObjName = new String("Query List")
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusObj=m_dataBean.getBusObject(busObjName)
	if(m_BusObj==null)
	{
		return false
	}

	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	String busCompName = new String("Admin Query List");
	//Let's get a reference to the object, if it is null, we have failed.
	m_BusComp=m_dataBean.getBusComp(busCompName, m_BusObj)
	if(m_BusComp==null)
	{
		return false
	}
	siebObjList.add(m_BusObj)
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
	System.out.println(myName+": SetProperties")
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	//Here we set the properties of the object we want to set.
	//We do this because we need to break out the step specific properties from the 
	//server specific properties in order to set only the correct ones
	//to the 
	Properties dProps=new Properties()
	dProps['Business Object']=props['QueryObject']
	dProps['Description']=props['QueryName']
	dProps['Query']=props['Query']
	
	boolean isUpdate =props['Update'].equals("true")
	
	Properties dPropsSearch=new Properties()
	dPropsSearch['Description']=props['QueryName']
	
	// TODO See if we can make a generic method out of this...
	System.out.println(myName+": Setting Properties")
	
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
	
	System.out.println(myName+": doAction")
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

System.out.println(myName+": disconnect")
mySiebJDB.disconnect()

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file
