/*
package com.company.xsamswa.javaapp.extensiontemplate;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.gateway.v4.rt.api.extensions.DataProviderExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtendDataProvider;
import com.sap.gateway.v4.rt.api.extensions.ExtensionContext;
import com.sap.gateway.v4.rt.cds.api.CDSDSParams;


public class DataProviderExtensionforOperationsSample {
	final Logger logger = LoggerFactory.getLogger(DataProviderExtensionforOperationsSample.class);

	
	 // This annotation denotes that this function would be called when the particular operation is called.
	 // It takes in two parameter.
	 // serviceName denotes the namespace of your service. It is optional. If not provided it applies to all service. 
	 // operationName denotes what operation triggers the method.
	 // The user should be aware of what he is returning as part of his operation and return the value accordingly
	 
	//This method demonstrates a bound function which return a Entity Collection
	@ExtendDataProvider(serviceName = "MySalesOrderService", operationName = "BiggestSalesOrder")
	public void findBiggestSO(ExtensionContext ectx) throws ODataApplicationException {
		ResultSet rs= null;
		logger.info("Entering Function-BiggestSalesOrder");
		
		DataProviderExtensionContext dpExt = ectx.asDataProviderContext();
		EntityCollection resultSet = new EntityCollection();
		CDSDSParams dsParam = (CDSDSParams) ectx.getDSParams();
		Connection conn = dsParam.getConnection();

		PreparedStatement getBiggestSO = null;
		try {
			// SQL Query to find the sales order(s) with highest vale
			getBiggestSO = conn.prepareStatement("SELECT * FROM  \"MySalesOrderService.SalesOrderHeader\" WHERE \"NetAmount\"= (SELECT max(\"NetAmount\") FROM  \"MySalesOrderService.SalesOrderHeader\")");
			rs = getBiggestSO.executeQuery();
		
		while (rs.next()){
			Entity ent = new Entity();
			ent.addProperty(new Property(null,"SalesOrderId",ValueType.PRIMITIVE,rs.getString("SalesOrderId")));
			ent.addProperty(new Property(null,"Note",ValueType.PRIMITIVE,rs.getString("Note")));
			ent.addProperty(new Property(null,"CurrencyCode",ValueType.PRIMITIVE,rs.getString("CurrencyCode")));
			ent.addProperty(new Property(null,"NetAmount",ValueType.PRIMITIVE,rs.getDouble("NetAmount")));
			ent.addProperty(new Property(null,"TaxAmount",ValueType.PRIMITIVE,rs.getInt("TaxAmount")));
			ent.addProperty(new Property(null,"OrderDate",ValueType.PRIMITIVE,rs.getDate("OrderDate")));
			ent.addProperty(new Property(null,"CallTime",ValueType.PRIMITIVE,rs.getTime("CallTime")));
			ent.addProperty(new Property(null,"DeliveryETA",ValueType.PRIMITIVE,rs.getObject("DeliveryETA")));
			ent.addProperty(new Property(null,"CashOnDelivery",ValueType.PRIMITIVE,rs.getBoolean("CashOnDelivery")));
			ent.addProperty(new Property(null,"CustomerID",ValueType.PRIMITIVE,rs.getInt("CustomerID")));
			ent.addProperty(new Property(null,"Type",ValueType.PRIMITIVE,rs.getString("Type")));
			// Adding each entity to the Entity Collection
			resultSet.getEntities().add(ent);
		}
		// Set Entity Collection to result
		dpExt.setResultEntityCollection(resultSet);
		
		} catch (SQLException e) {
			logger.error("Some SQL Exception occurred while trying to fetch biggest SO");
			e.printStackTrace();
		}finally{
			if (conn != null) {
		        try {
		            conn.close();
		        } catch (SQLException e) {
		        	logger.error("Error in closing SQL Connection");
		        }
		    }
		}
	}
	
	//This method demonstrates a function which takes a parameter and return a collection of complex types
	@ExtendDataProvider(serviceName = "MySalesOrderService", operationName = "GetAddressesForCountry")
	public void getAddresses(ExtensionContext ectx){
		ResultSet rs= null;
		logger.info("Entering Function-GetAddressesForCountry");
		
		DataProviderExtensionContext dpExt = ectx.asDataProviderContext();
		CDSDSParams dsParam = (CDSDSParams) ectx.getDSParams();
		Connection conn = dsParam.getConnection();
		// Get the UriInfo object as for a function, input parameter is passed as a part of the URI
		UriInfo uriInfo = dpExt.getUriInfo();
		// We know that there is only one parameter, this only one resource. So getting its value
		UriResource firstSeg = uriInfo.getUriResourceParts().get(0);
		UriResourceFunction func = (UriResourceFunction) firstSeg;
		String ctryName = func.getParameters().get(0).getText().toString();
		// Parameter, if it's string type, is returned with single quotes, which need to be removed
		String countryName = ctryName.substring(1, ctryName.length()-1);
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement("Select \"CustAddress.Street\",\"CustAddress.Area\",\"CustAddress.City\",\"CustAddress.State\",\"CustAddress.Country\" From \"MySalesOrderService.Customer\" WHERE \"CustAddress.Country\"= ?");
			ps.setString(1, countryName);
			rs = ps.executeQuery();
			// Create list of complex values to be added to Complex Type Collection
			List<ComplexValue> cplxValList = new ArrayList<ComplexValue>();
			
			while(rs.next()){
				ComplexValue cv = new ComplexValue();
				List<Property> propList = cv.getValue();
				propList.add(new Property(null,"Street",ValueType.PRIMITIVE,rs.getString("CustAddress.Street")));
				propList.add(new Property(null,"Area",ValueType.PRIMITIVE,rs.getString("CustAddress.Area")));
				propList.add(new Property(null,"City",ValueType.PRIMITIVE,rs.getString("CustAddress.City")));
				propList.add(new Property(null,"State",ValueType.PRIMITIVE,rs.getString("CustAddress.State")));
				propList.add(new Property(null,"Country",ValueType.PRIMITIVE,rs.getString("CustAddress.Country")));
				cplxValList.add(cv);
			}
			// Add the list of complex values to a property to create complex type collection
			Property prop = new Property(null,"AddrCollection",ValueType.COLLECTION_COMPLEX,cplxValList);
			
			dpExt.setResult(prop);
			
		} catch (SQLException e) {
			logger.error("Some SQL Error Occurred", e);
			e.printStackTrace();
		}finally{
			if (conn != null) {
		        try {
		            conn.close();
		        } catch (SQLException e) {
		        	logger.error("Error in closing SQL Connection");
		        }
		    }
		}		
	}
	
	//This method demonstrates a function which returns a primitive data type
	@ExtendDataProvider(serviceName = "MySalesOrderService", operationName = "TotalSalesAmount")
	public void totalSalesAmount(ExtensionContext ectx){
		ResultSet rs= null;
		logger.info("Entering Function-TotalSalesAmount");
		
		DataProviderExtensionContext dpExt = ectx.asDataProviderContext();
		CDSDSParams dsParam = (CDSDSParams) ectx.getDSParams();
		Connection conn = dsParam.getConnection();
		
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("Select sum(\"NetAmount\") AS \"TotalSales\" FROM  \"MySalesOrderService.SalesOrderHeader\"");
			rs = ps.executeQuery();
			Property prop= null;
			while(rs.next()){
				prop = new Property(null, "TotalSales", ValueType.PRIMITIVE, rs.getDouble("TotalSales"));
			}
			// Set the single primitive value to the result
			dpExt.setResult(prop);
		} catch (SQLException e) {
			logger.error("Some Error Occurred in SQL for TotalSalesOrder", e);
			e.printStackTrace();
		}
	}
	
	//This method demonstrates bound action which reads data from the json post body and return nothing
	@ExtendDataProvider(serviceName = "MySalesOrderService", operationName = "ChangeCashOnDeliveryOption")
	public void chgCODOpt(ExtensionContext ectx){
		CDSDSParams dsParam = (CDSDSParams) ectx.getDSParams();
		Connection conn = dsParam.getConnection();
		
		DataProviderExtensionContext dpCtx = ectx.asDataProviderContext();
		// Since it's the action bound to an Entity, the key predicates can be obtained from UriInfo
		UriInfo uri = dpCtx.getUriInfo();
		//Obtain the entity. For this action, we know that the first URI resource is the entity set
		UriResourceEntitySet entity= (UriResourceEntitySet)uri.getUriResourceParts().get(0);
		// Obtain the key predicate value. We know that there is only one key predicate value
		String soIDTemp = entity.getKeyPredicates().get(0).getText();
		// trimming the single quotes for the String type key predicate
		String soID = soIDTemp.substring(1, soIDTemp.length()-1);
		// Now, read the request payload
		InputStream is = dpCtx.getODataRequest().getBody();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			// Parsing JSON
			JsonNode rootNode = objectMapper.readTree(is);
			// Get the parameter value. Parameter is type boolean
			boolean isCod = rootNode.get("IsCOD").asBoolean();
			
			PreparedStatement ps = null;
			ps = conn.prepareStatement("UPDATE \"MySalesOrderService.SalesOrderHeader\" SET \"CashOnDelivery\"=? WHERE \"SalesOrderId\"=? ");
			
			ps.setString(2, soID);
			ps.setBoolean(1, isCod);
			
			ps.executeUpdate();
			
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			logger.error("Some Error Occurred in SQL for ChangeCashOnDeliveryOption", e);
			e.printStackTrace();
		}finally{
			if (conn != null) {
		        try {
		            conn.close();
		        } catch (SQLException e) {
		        	logger.error("Error in closing SQL Connection");
		        }
		    }
		}
		

	}

}
*/