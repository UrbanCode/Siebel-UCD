/*
 * Here is the default imports that all scripts should have as a minimum
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
/*
 * I have set up myName to be able to distinguish easily the steps from the logs
 */
String myName="Group_InternalDivision_single";
/*
 * This is a little list in which we add all the Siebel components we need to clean
 * in an error condition.
 */
def siebObjList = []

//Create a new Siebel Java Data Bean Wrapper.
SiebelJavaDataBeanWrapper mySiebJDB = new SiebelJavaDataBeanWrapper();

/*
 * Here we connect to the Siebel Connection Broker Server. We connect to the 
 * EAIObjMgr_enu Object Manager, which seems to be the most well behaved for Siebel.
 * We pick up all the options from the Step options. 
 */
if(!mySiebJDB.connect(props['SiebelCBServer'], props['SiebelCBPort'], props['SiebelEnt'], "EAIObjMgr_enu", props['SiebelUser'], props['SiebelPass']))
{
	println "${myName}: Connection to Siebel Server failed - check log upwards";
	System.exit(-1);
}

/*
 * Let's create a utilitity object. We pass it the reference to the 
 * Siebel Java Data Bean wrapper, since some things are used internally 
 * in the utility class.
 */
UCDSiebelUtility myUCDStepObject=new UCDSiebelUtility(mySiebJDB);

/*
 * These are the objects that we are going to use later. We define them
 * here in order to be able to use them in the closures we define later
 */
SiebelBusObject m_BusObj = null;
SiebelBusComp m_BusComp = null;
//Let's get a reference to the SiebelJavaDataBean.
SiebelJavaDataBeanWrapper m_dataBean=myUCDStepObject.getSiebelJDB()

/*
 * This is the getObjects() closure that get the object and component(s)
 * we want. It's defined in every script, since it is different in every
 * script. No matter how many components or objects we want to use, it is
 * better to defined them here. We also add them to the list in case we have 
 * to release them in an error exit from the script.
 */
def getObjects = {
	//BUSINESS OBJECT NAME
	String busObjName = new String("Admin Signal VOD Definition");
	m_BusObj=m_dataBean.getBusObject(busObjName);
	if(m_BusObj==null)
	{
		return false;
	}

	//BUSINESS COMPONENT NAME - child of BUSINESS OBJECT
	String busCompName = new String("Signal VOD BusComp");
	m_BusComp=m_dataBean.getBusComp(busCompName, m_BusObj);
	if(m_BusComp==null)
	{
		return false;
	}
		
	siebObjList.add(m_BusObj)
	siebObjList.add(m_BusComp)
	
	return true
}

/*
 * This is a little closure that we will use if we exit with an error.
 */
def exitGracefullyWithFail = {
	siebObjList.each {if(it){it.release()}}
	mySiebJDB.disconnect()
	System.exit(-1)
}

/*
 * Let's call getObjects() and if it succeeds, let's continue.
 */
if(getObjects())
{
	System.out.println(myName+": SetProperties");
	//BE VERY CAREFUL WITH THE CASE OF THE SIEBEL FIELDS, they are case sensitive.
	/*
	 * Here we set the properties of the object we want to set. The names of
	 * the properties in the dProps hash is the same as the field names in the
	 * Business Component. Here we translate the step properties to 
	 * Business Component Properties.
	 */
	Properties dProps=new Properties();
	dProps['VOD Name']=props['SignalName'];
	dProps['Description']=props['SignalDescription'];
	
	/*
	 * If we only want to search on different fieldset, then we also
	 * make a dPropsSearch hash that only holds the fields that we want 
	 * to perform a query on in the Siebel Component.
	 */
	Properties dPropsSearch=new Properties();
	dPropsSearch['VOD Name']=props['SignalName'];
	
	/*
	 * This is a little flag to see if this is an update. 
	 * If it is an update and we don't find anything in the 
	 * Business Component, then we fail. If we don't set this field, 
	 * if the entry is not found it is created, otherwise it is updated.
	 */
	boolean isUpdate =props['Update'].equals("true")

	/*
	 * Here we create a Siebel Property set that we are going to use when
	 * we create the Business Object row. 
	 */
	SiebelPropertySet mainSiebelProps=myUCDStepObject.createSiebelPropertySet(dProps)
	/*
	 * If we have a dPropsSearch, then we create a search string on those, 
	 * otherwise we create a search string on all fields that we have defined
	 * in dProps.
	 */
	String mySearchString=null
	if(dPropsSearch)
	{
		mySearchString=myUCDStepObject.createSearchExpr(dPropsSearch)
	}
	else
	{
		mySearchString=myUCDStepObject.createSearchExpr(dProps)
	}

	/*
	 * Let's actually set the fields and write the record. 
	 */
	if(!myUCDStepObject.setObjectFields(m_BusComp, mainSiebelProps, mySearchString, dProps, isUpdate))
	{
		System.out.println(myName+": doAction failed, check log upwards.")
		//If something fails, call the exitGracefullyWithFail closure.
		exitGracefullyWithFail.call()
	}
	/*
	 * Everything went well, let's release the object. I am not using the list
	 * on purpose here, to make sure that we know what we are releasing.
	 */
	m_BusComp.release();
	m_BusObj.release();
}
else
{
	//getObjects() failed, call the exitGracefullyWithFail closure.
	exitGracefullyWithFail.call()
}
//We are finished, disconnect from the Siebel server.
mySiebJDB.disconnect();

//Set an output property
//apTool.setOutputProperty("outPropName", "outPropValue");

apTool.storeOutputProperties();//write the output properties to the file