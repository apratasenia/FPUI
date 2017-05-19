/*
package com.company.xsamswa.javaapp.extensiontemplate;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlOperation;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.gateway.v4.rt.api.extensions.ExtendMetadata;

public class ModelExtensionsSample {
	
	//Create logger
	final Logger logger = LoggerFactory.getLogger(ModelExtensionsSample.class);
	
	
	 // Annotation which defines the list of functions and actions for the service name.
	 // serviceName parameter denotes the fully qualified namespace of your schema. It is optional.
	 // The method should return List<CsdlOperation> which contains all the actions and functions.
	 //
	@ExtendMetadata(serviceName="MySalesOrderService")
	public List<CsdlOperation> createOps(){
		final List<CsdlOperation> opsList = new ArrayList<CsdlOperation>();
		
		
		 //Sequence for creating an operation
		 // 1) Set the parameter(s) for the function/ action. Operations might not have input parameters. 
		 //    So this step is optional
		 // 2) Set the return type for the operation. Return Type is MUST for a function but not for Action
		 // 3) Using above, create the Operation. 
		 
		
		 
		 //  Creating a bound function
		 //  Return Type Entity Collection 
		 //  Without Input Parameter
		 //  Though there is no input parameter, Entity or Entity Set is created as a binding parameter for a
		 //  bound function
		 //  The first parameter of a function is taken as the bound parameter.
		 //
		
		//Create Binding Parameter
		final List<CsdlParameter> boundParamList = new ArrayList<CsdlParameter>();
		final CsdlParameter boundParam = new CsdlParameter();
		// Name of binding parameter MUST be the Entity Set name
		boundParam.setName("SalesOrderHeader");
		// Binding is with Entity Collection, hence setCollection(true)
		boundParam.setCollection(true);
		
		//   Type is the Full Qualified Name. Below, it's <ServiceName>.<EntitySetName>. 
		//   There is no namspace for the service
		//   Full qualified name is same as it appears for the attribute EntityType for Entity element 
		//   in entity container section of the metadata.
		//   Only the entitySetName is enough if there is no cross service referencing.
		
		boundParam.setType("MySalesOrderService.SalesOrderHeader");
		boundParam.setNullable(false);
		boundParamList.add(boundParam);
		
		
		//Creating the return type. It returns a collection
		final CsdlReturnType maxSOvalReturn = new CsdlReturnType();
		maxSOvalReturn.setCollection(true).setType("MySalesOrderService.SalesOrderHeader");
		
		// Creating the function itself, with name BiggestSalesOrder
		// Here we bind the return type and parameters and since it is a bound function, we mark it as setBound(true)
		final CsdlFunction BiggestSOFunc = new CsdlFunction();
		BiggestSOFunc.setName("BiggestSalesOrder").setReturnType(maxSOvalReturn).setBound(true).setParameters(boundParamList);
		// Adding the function to the operation list. All other function and actions would be added subsequently
		opsList.add(BiggestSOFunc);
		
		// Function Returning Complex Type Collection
		// This is an unbound function, which takes an input parameter and returns a complex type collection
		final CsdlReturnType retTypeAddr = new CsdlReturnType();
		// Setting the return type. Here, "Address" is the complex type in the model 
		retTypeAddr.setCollection(true).setType("MySalesOrderService.Address");
		
		// Set the input parameter. First, the parameter list is created and then the parameters are individually added
		final List<CsdlParameter> cplxParams = new ArrayList<CsdlParameter>();
		// Now creating a single input parameter with Name Country and type as String
		final CsdlParameter countryParam = new CsdlParameter();
		countryParam.setName("Country");
		countryParam.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
		// Adding parameter to parameter list for the function
		cplxParams.add(countryParam);
		
		// Creating the function and setting the parameters and return type. Note that it is not set as bound
		final CsdlFunction getAddr = new CsdlFunction();
		getAddr.setName("GetAddressesForCountry");
		getAddr.setParameters(cplxParams);
		getAddr.setReturnType(retTypeAddr);
		
		opsList.add(getAddr);
		
		//Creating function for returning a single primitive value: No Input Parameter
		//This is an unbound function
		
		final CsdlReturnType retTypePrimitive = new CsdlReturnType();
		retTypePrimitive.setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
		final CsdlFunction totalSales = new CsdlFunction();
		totalSales.setName("TotalSalesAmount");
		totalSales.setReturnType(retTypePrimitive);
		
		opsList.add(totalSales);
		
		//Creating a bound action with input parameter and no return type
		List<CsdlParameter> actParamList = new ArrayList<CsdlParameter>();
		// Creating a binding parameter with binding for Entity
		final CsdlParameter boundSO = new CsdlParameter();
		// As for function, this MUST be same as the Entity Set Name
		boundSO.setName("SalesOrderHeader");
		// Full qualified name of the Entity set
		boundSO.setType("MySalesOrderService.SalesOrderHeader");
		boundSO.setCollection(false);
		boundSO.setNullable(false);
		actParamList.add(boundSO);
		
		// There is one bound parameter, which would be sent in the request payload. Since the Action is 
		//   bound to an Entity, key predicate of the entity on which the action has to be taken 
		//   shall be picked up from URI info in the data provider extension class
		final CsdlParameter cod = new CsdlParameter();
		cod.setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()).setName("IsCOD");
		actParamList.add(cod);
		
		//Creating the action
		final CsdlAction chgCOD = new CsdlAction();
		chgCOD.setParameters(actParamList);
		chgCOD.setName("ChangeCashOnDeliveryOption");
		//Setting the function as bound
		//chgCOD.setBound(true);
		// Adding the action to operations list
		opsList.add(chgCOD);
		
		return opsList;
		
	}

}
*/
