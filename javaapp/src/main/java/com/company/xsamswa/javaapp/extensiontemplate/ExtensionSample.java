
package com.company.xsamswa.javaapp.extensiontemplate;

// * <h1>Extension Templates!</h1>
// * CDS supports only read & query operations and does not support DML operations 
// * i.e we cannot insert, update, delete data using CDS data model.  
// * To support DML operations, one can write custom code as shown below
// * <p>
// * <b>Note:</b> This code is only for reference

// * @author  SAP
// * @version 1.0
// * @since   2016-08-23


import java.math.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.prefer.Preferences;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.gateway.v4.rt.api.extensions.DataProviderExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtendDataProvider;
import com.sap.gateway.v4.rt.api.extensions.ExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtensionException;
import com.sap.gateway.v4.rt.api.extensions.RequestType;
import com.sap.gateway.v4.rt.cds.api.CDSDSParams;


public class ExtensionSample {

	public static String dbnamespace = "plan_it.plandb";
	final static Logger logr = LoggerFactory.getLogger("ExtensionSample");

	
	 // * This method encapsulates CREATE functionality for Customer entity.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException
	 // * @throws SQLException
	 
	@ExtendDataProvider(entitySet = { "Resource" }, requestTypes = RequestType.CREATE)
	public void createRecord(ExtensionContext ecx)
			throws ODataApplicationException, ExtensionException, SQLException {
		logr.debug("Entering create Record method.");
		insertRecord(ecx);
	}

	
	// * @param ecx
	// * @throws ExtensionException
	// * @throws SQLException
	 
	private void insertRecord(ExtensionContext ecx) throws ExtensionException, SQLException {
		
		Connection conn = null;
		try {
			//Get the connection object for the current Database schema.
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			//Downcast Extension context object to DataProviderExtensionContext object in order to access data provider specific methods.
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			//Following 2 lines is used to get the entity object which represents the current request payload.
			DeserializerResult payload = dpCtx.getDeserializerResult();
			Entity customerEntity = payload.getEntity();

			PreparedStatement stmt = conn.prepareStatement("INSERT INTO \"" + dbnamespace + "::data.Resource\" VALUES ( ?,?,?,?,?)");
			
			stmt.setString(1, customerEntity.getProperty("id").getValue().toString());
			stmt.setString(2, customerEntity.getProperty("description").getValue().toString());
			stmt.setBigDecimal(3, new BigDecimal(customerEntity.getProperty("cost_rate_tc").getValue().toString()));
			stmt.setString(4, customerEntity.getProperty("currency_tc").getValue().toString());
			Entity assocEntity = customerEntity.getNavigationLink("version").getInlineEntity();
            stmt.setInt(5, Integer.parseInt(assocEntity.getProperty("id").getValue().toString()));			
            
			logr.trace("ExtensionSample  <<  {createRecord} SQL Statement ");
			
			stmt.executeUpdate();
			logr.info(" Record created successfully ");
//			conn.close();
			returnResponseIfRequestPrefers(dpCtx);

		} catch (SQLException e) {
			logr.error("ExtensionSample  << {insertRecord}", e);
			throw new ExtensionException(e);
		} catch (Exception e) {
			logr.error("ExtensionSample  << {insertRecord}", e);
			throw new ExtensionException(e);
		} finally {
			if (conn != null)
				//close connection. This is required to prevent connection leak.
				conn.close();
		}
	}

	
	 // * if the user sends Prefer return=minimal then don't read response
	 // * otherwise read response i.e call setEntityToBeRead() method
	 // * 
	 // * @param ecx
	 
	private void returnResponseIfRequestPrefers(DataProviderExtensionContext extCtx) {

		logr.debug("Entering ExtensionSample <<  {returnResponseIfRequestPrefers}");
		//The following check is done to see whether the request has the header "Prefer=return.minimal". If that is the case ,then the "read" following the create
		//or update is not required.
		final Preferences.Return returnPreference = OData.newInstance()
				.createPreferences(extCtx.getODataRequest().getHeaders(HttpHeader.PREFER)).getReturn();
		if (returnPreference == null || returnPreference == Preferences.Return.REPRESENTATION) {

			extCtx.setEntityToBeRead();
		}
	}

	
	 // * This method serves both PATCH and PUT requests for Customer entity.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws SQLException
	 // * @throws ExtensionException
	 
	@ExtendDataProvider(entitySet = { "Resource" }, requestTypes = RequestType.UPDATE )
	public void updateRecord(ExtensionContext ecx)
			throws ODataApplicationException, SQLException, ExtensionException {
		String resourceId = null;
		logr.debug("Entering ExtensionSample  << {updateRecord}");
		Connection conn = null;
		try {
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			DeserializerResult payload = dpCtx.getDeserializerResult();
			Entity entity = payload.getEntity();
			HttpMethod httpMethod = ecx.getODataRequest().getMethod();
			
			//The following code gets the object "UriInfo" which is supposed to give us all the information in the current URL of the OData query. We are specially
			//interested in the "key predicates" information.
			UriInfo uri = dpCtx.getUriInfo();
			UriResourceEntitySet entitySet = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
			List<UriParameter> keyPredicateList = entitySet.getKeyPredicates();
			//The above obtained List<UriParameter> is nothing but the list of key parameters specified in the Current Url. We extract each such value to form
			//the SQL to delete the correct entry.
			for (UriParameter uriParam : keyPredicateList) {
				if (uriParam.getName().equals("id")) {
					resourceId = uriParam.getText();
				}
			}

			PreparedStatement stmt = null;
			
			//There are 2 cases to consider before forming the Update SQL. 1 is HTTP Method PUT. The other is HTTP method PATCH. If the method is PUT we update ALL
			//properties of the entity, if a property is not specified in the payload we set it to null,But when the method is PATCH, 
			//we only update the specified properties.
			if (httpMethod.equals(HttpMethod.PUT)) {
				
				stmt = conn.prepareStatement("UPDATE \"" + dbnamespace + "::data.Resource\"  " + "SET \"description\"=?, \"cost_rate_tc\"=?, \"currency_tc\"=?, \"version.id\"=? " + 
     				" Where \"id\"=?");
				
				stmt.setString(1, String.valueOf(entity.getProperty("description").getValue()));
				stmt.setBigDecimal(2, new BigDecimal(String.valueOf(entity.getProperty("cost_rate_tc").getValue())));
				stmt.setString(3, String.valueOf(entity.getProperty("currency_tc").getValue()));
			    Entity assocEntity = entity.getNavigationLink("version").getInlineEntity();
                stmt.setInt(4, Integer.parseInt(assocEntity.getProperty("id").getValue().toString()));				
                stmt.setString(5, resourceId);
			}
			else if (httpMethod.equals(HttpMethod.PATCH)) {
				
				String sqlSet = "UPDATE \"" + dbnamespace + "::data.Resource\"  " + " SET ";
				String sqlParams = "";
				String fieldName = "description";
				Property prop = entity.getProperty(fieldName);
				if (prop != null && prop.getValue() != null) {
					sqlParams += " \"" + fieldName + "\"='" + String.valueOf(prop.getValue()) + "'";
					prop = null;
				}
				
				fieldName = "cost_rate_tc";
				prop = entity.getProperty(fieldName);
				if (prop != null && prop.getValue() != null) {
					if (sqlParams != "") sqlParams += ",";
					sqlParams += " \"" + fieldName + "\"='" + String.valueOf(prop.getValue()) + "'";
					prop = null;
				}
				
				fieldName = "currency_tc";
				prop = entity.getProperty(fieldName);
				if (prop != null && prop.getValue() != null) {
					if (sqlParams != "") sqlParams += ",";
					sqlParams += " \"" + fieldName + "\"='" + String.valueOf(prop.getValue()) + "'";
					prop = null;
				}

				fieldName = "version";
				prop = entity.getProperty(fieldName);
				if (prop != null && prop.getValue() != null) {
					if (sqlParams != "") sqlParams += ",";
					sqlParams += " \"version.id\"='" + String.valueOf(prop.getValue()) + "'";
					prop = null;
				}

				sqlParams = sqlParams + " Where \"id\"=" + resourceId;
				stmt = conn.prepareStatement(sqlSet + sqlParams);
				
			}
			try {
				logr.trace("ExtensionSample  << {updateRecord} SQL Statement ");
				stmt.executeUpdate();
				logr.info("record updated successfully ");
			} catch (SQLException sqlEx) {
				logr.error("ExtensionSample << {updateRecord}", sqlEx);
				throw new ExtensionException(sqlEx);
			}
			returnResponseIfRequestPrefers(dpCtx);

		} catch (Exception ex) {
			logr.error("ExtensionSample  << {updateRecord}", ex);
			throw new ExtensionException(ex);
		} finally {
			if (conn != null)
				conn.close();
		}
		logr.debug("Exiting ExtensionSample  << {updateRecord}");
	}

	 // * This method is used to delete entity
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException 
	 
	@ExtendDataProvider(entitySet = { "Resource" }, requestTypes = RequestType.DELETE)
	public void deleteRecord(ExtensionContext ecx) throws ODataApplicationException, SQLException, ExtensionException {
		String resourceId = null;
		Connection conn = null;

		logr.debug("Entering ExtensionSample  << {deleteRecord}");

		try {
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			
			//The following code gets the object "UriInfo" which is supposed to give us all the information in the current URL of the OData query. We are specially
			//interested in the "key predicates" information.
			UriInfo uri = dpCtx.getUriInfo();
			UriResourceEntitySet entity = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
			List<UriParameter> keyPredicateList = entity.getKeyPredicates();
			//The above obtained List<UriParameter> is nothing but the list of key parameters specified in the Current Url. We extract each such value to form
			//the SQL to delete the correct entry.
			for (UriParameter uriParam : keyPredicateList) {
				if (uriParam.getName().equals("id")) {
					resourceId = uriParam.getText();
				}
			}

			PreparedStatement stmt = conn.prepareStatement("DELETE  FROM \"" + dbnamespace + "::data.Resource\" WHERE \"id\" = ? ");
			stmt.setString(1, resourceId);

			logr.trace("ExtensionSample  << {deleteRecord} SQL Statement");
			int rowAffected = stmt.executeUpdate();
			if (rowAffected == 0) {
				throw new ODataApplicationException("Entity not found", 404, Locale.US, "404");
			} else {
				logr.info( " Customer deleted successfully ");
			}
//			conn.close();

		} catch (SQLException sqlEx) {
			logr.error("ExtensionSample  << {deleteRecord}", sqlEx);
			throw new ExtensionException(sqlEx);
		} catch (Exception ex) {
			logr.error("ExtensionSample  << {deleteRecord}", ex);
			throw new ExtensionException(ex);
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	
	// * This method is used to perform read operation. This is optional as READ functionality is provided by default by the framework. Still it can be used,
	// * if some pre/post processing needs to be done in additional to the default implementation.
	// * @param ecx
	// * @throws ODataApplicationException
	// * @throws ExtensionException
	 
	@ExtendDataProvider(entitySet = { "jsonContainer" }, requestTypes = RequestType.READ)
	public void readRecord(ExtensionContext ecx) throws ODataApplicationException, ExtensionException {

		{
			logr.debug("Entering ExtensionSample << {readRecord}");

			try {
				//performDefault is used to invoke the default handling for the current method(Here READ). 
				ecx.performDefault();
				logr.debug("Exiting ExtensionSample <<  {readRecord}");
			} catch (Exception e) {
				logr.error("ExtensionSample <<  {readRecord}", e);
				throw new ExtensionException(e);
			}
		}
	}
	
	
	 // * This method encapsulates CREATE functionality for Customer entity under Service2.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException
	 // * @throws SQLException
	 
	//Here, in addition to the entitySet and requestType parameters there is a 3rd parameter called serviceName, under which the OData service name of the current
	//entity has to be specified. This is done to resolve the ambiguity if multiple OData services expose entities under the same name.
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.CREATE, serviceName = "Service2")
	public void createCustomerOtherService(ExtensionContext ecx)
			throws ODataApplicationException, ExtensionException, SQLException {
		logr.debug("Entering create Customer method.");
		
		 //Insert logic . . . 
		 
	}
	
	
	
}

/*COPY OF TEMPLATE---------------------------------*/

/*
package com.company.xsamswa.javaapp.extensiontemplate;

// * <h1>Extension Templates!</h1>
// * CDS supports only read & query operations and does not support DML operations 
// * i.e we cannot insert, update, delete data using CDS data model.  
// * To support DML operations, one can write custom code as shown below
// * <p>
// * <b>Note:</b> This code is only for reference

// * @author  SAP
// * @version 1.0
// * @since   2016-08-23



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.prefer.Preferences;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.gateway.v4.rt.api.extensions.DataProviderExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtendDataProvider;
import com.sap.gateway.v4.rt.api.extensions.ExtensionContext;
import com.sap.gateway.v4.rt.api.extensions.ExtensionException;
import com.sap.gateway.v4.rt.api.extensions.RequestType;
import com.sap.gateway.v4.rt.cds.api.CDSDSParams;


public class ExtensionSample {

	
	final static Logger logr = LoggerFactory.getLogger("ExtensionSample");

	
	 // * This method encapsulates CREATE functionality for Customer entity.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException
	 // * @throws SQLException
	 
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.CREATE)
	public void createCustomer(ExtensionContext ecx)
			throws ODataApplicationException, ExtensionException, SQLException {
		logr.debug("Entering create Customer method.");
		insertCustomer(ecx);
	}

	
	// * @param ecx
	// * @throws ExtensionException
	// * @throws SQLException
	 
	private void insertCustomer(ExtensionContext ecx) throws ExtensionException, SQLException {
		
		Connection conn = null;
		try {
			String Street = null, Area = null, City = null, State = null, Country = null;
			//Get the connection object for the current Database schema.
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			//Downcast Extension context object to DataProviderExtensionContext object in order to access data provider specific methods.
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			//Following 2 lines is used to get the entity object which represents the current request payload.
			DeserializerResult payload = dpCtx.getDeserializerResult();
			Entity customerEntity = payload.getEntity();
			//Once we get the payload in the form of an Entity object we can retrieve different property values by using getProperty() method.
			//For complex properties, if we need to get the List of individual properties we need to call method sequence asComplex().getValue() as in the following
			//line.
			List<Property> custAddress = customerEntity.getProperty("CustAddress").asComplex().getValue();
			Iterator<Property> itr = custAddress.iterator();
			Property prop = new Property();
			while (itr.hasNext()) {
				prop = itr.next();
				String propName = prop.getName();
				if (propName.equals("Street")) {
					Street = String.valueOf(prop.getValue());
				} else if (propName.equals("Area")) {
					Area = String.valueOf(prop.getValue());
				} else if (propName.equals("City")) {
					City = String.valueOf(prop.getValue());
				} else if (propName.equals("State")) {
					State = String.valueOf(prop.getValue());
				} else if (propName.equals("Country")) {
					Country = String.valueOf(prop.getValue());
				}
			}

			PreparedStatement stmt = conn.prepareStatement("INSERT INTO \"MySalesOrderService.Customer\" VALUES ( ?,?,?,?,?,?,?,?,?,?)");
			
			stmt.setInt(1, Integer.parseInt(customerEntity.getProperty("CustomerID").getValue().toString()));
			stmt.setString(2, customerEntity.getProperty("Type").getValue().toString());
			stmt.setString(3, customerEntity.getProperty("CustomerName").getValue().toString());
			stmt.setString(4,Street);
			stmt.setString(5,Area);
			stmt.setString(6,City);
			stmt.setString(7,State);
			stmt.setString(8,Country);
			stmt.setInt(9, Integer.parseInt(customerEntity.getProperty("CustomerID").getValue().toString()));
			stmt.setString(10, customerEntity.getProperty("Type").getValue().toString());
			
			
			logr.trace("ExtensionSample  <<  {createCustomer} SQL Statement ");
			
			stmt.executeUpdate();
			logr.info(" customer created successfully ");
			conn.close();
			returnResponseIfRequestPrefers(dpCtx);

		} catch (SQLException e) {
			logr.error("ExtensionSample  << {insertCustomer}", e);
			throw new ExtensionException(e);
		} catch (Exception e) {
			logr.error("ExtensionSample  << {insertCustomer}", e);
			throw new ExtensionException(e);
		} finally {
			if (conn != null)
				//close connection. This is required to prevent connection leak.
				conn.close();
		}
	}

	
	 // * if the user sends Prefer return=minimal then don't read response
	 // * otherwise read response i.e call setEntityToBeRead() method
	 // * 
	 // * @param ecx
	 
	private void returnResponseIfRequestPrefers(DataProviderExtensionContext extCtx) {

		logr.debug("Entering ExtensionSample <<  {returnResponseIfRequestPrefers}");
		//The following check is done to see whether the request has the header "Prefer=return.minimal". If that is the case ,then the "read" following the create
		//or update is not required.
		final Preferences.Return returnPreference = OData.newInstance()
				.createPreferences(extCtx.getODataRequest().getHeaders(HttpHeader.PREFER)).getReturn();
		if (returnPreference == null || returnPreference == Preferences.Return.REPRESENTATION) {

			extCtx.setEntityToBeRead();
		}
	}

	
	 // * This method serves both PATCH and PUT requests for Customer entity.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws SQLException
	 // * @throws ExtensionException
	 
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.UPDATE )
	public void updateCustomer(ExtensionContext ecx)
			throws ODataApplicationException, SQLException, ExtensionException {
		String custId = null, custType = null, Street = null, Area = null, City = null, State = null, Country = null;
		logr.debug("Entering ExtensionSample  << {updateCustomer}");
		Connection conn = null;
		try {
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			DeserializerResult payload = dpCtx.getDeserializerResult();
			Entity entity = payload.getEntity();
			HttpMethod httpMethod = ecx.getODataRequest().getMethod();
			
			//The following code gets the object "UriInfo" which is supposed to give us all the information in the current URL of the OData query. We are specially
			//interested in the "key predicates" information.
			UriInfo uri = dpCtx.getUriInfo();
			UriResourceEntitySet entitySet = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
			List<UriParameter> keyPredicateList = entitySet.getKeyPredicates();
			//The above obtained List<UriParameter> is nothing but the list of key parameters specified in the Current Url. We extract each such value to form
			//the SQL to delete the correct entry.
			for (UriParameter uriParam : keyPredicateList) {
				if (uriParam.getName().equals("CustomerID")) {
					custId = uriParam.getText();
				}
				if (uriParam.getName().equals("Type")) {
					custType = uriParam.getText();
					custType = custType.substring(1, custType.length()-1);
				}
			}
			try {
				List<Property> custAddress = entity.getProperty("CustAddress").asComplex().getValue();
				if (custAddress != null) {
					Iterator<Property> itr = custAddress.iterator();
					Property prop = new Property();
					while (itr.hasNext()) {
						prop = itr.next();
						String propName = prop.getName();
						if (propName.equals("Street")) {
							Street = String.valueOf(prop.getValue());
						} else if (propName.equals("Area")) {
							Area = String.valueOf(prop.getValue());
						} else if (propName.equals("City")) {
							City = String.valueOf(prop.getValue());
						} else if (propName.equals("State")) {
							State = String.valueOf(prop.getValue());
						} else if (propName.equals("Country")) {
							Country = String.valueOf(prop.getValue());
						}
					}
				}
			} catch (Exception ex) {

				logr.error("ExtensionSample  << {updateCustomer}", ex);
			}

			PreparedStatement stmt = null;
			
			//There are 2 cases to consider before forming the Update SQL. 1 is HTTP Method PUT. The other is HTTP method PATCH. If the method is PUT we update ALL
			//properties of the entity, if a property is not specified in the payload we set it to null,But when the method is PATCH, 
			//we only update the specified properties.
			if (httpMethod.equals(HttpMethod.PUT)) {
				
				stmt = conn.prepareStatement("UPDATE \"MySalesOrderService.Customer\"  " + "SET \"CustomerName\"=?, \"CustAddress.City\"=?,"
						+ " \"CustAddress.Country\"= ?, \"CustAddress.State\"= ? ,\"CustAddress.Area\"=?,"
						+ " \"CustAddress.Street\"=? Where \"CustomerID\"=? AND \"Type\"= ?");
				
				stmt.setString(1, String.valueOf(entity.getProperty("CustomerName").getValue()));
				stmt.setString(2, City);
				stmt.setString(3, Country);
				stmt.setString(4, State);
				stmt.setString(5, Area);
				stmt.setString(6, Street);
				stmt.setInt(7, Integer.parseInt(custId));
				stmt.setString(8, custType);
			}
			else if (httpMethod.equals(HttpMethod.PATCH)) {
				
				String sqlPatch = "UPDATE \"MySalesOrderService.Customer\"  " + " SET ";
				StringBuffer sbSqlPatch = new StringBuffer(sqlPatch);
				if (entity.getProperty("CustomerName").getValue() != null) {

					sbSqlPatch.append(
							" \"CustomerName\"='" + String.valueOf(entity.getProperty("CustomerName").getValue()));
				}
				if (City != null) {

					sbSqlPatch.append("'," + " \"CustAddress.City\"='" + City);
				}
				if (Country != null) {

					sbSqlPatch.append("', \"CustAddress.Country\"='" + Country + "'");
				}
				if (State != null)
					sbSqlPatch.append(", \"CustAddress.State\"='" + State);
				if (Area != null)
					sbSqlPatch.append("',\"CustAddress.Area\"='" + Area);
				if (Street != null)
					sbSqlPatch.append("',\"CustAddress.Street\"='" + Street + "' ");

				sbSqlPatch.append("Where \"CustomerID\"=" + custId);
				sbSqlPatch.append(" AND \"Type\"='" + custType + "'");
				
				stmt = conn.prepareStatement(sqlPatch + sbSqlPatch);
				
			}
			try {
				logr.trace("ExtensionSample  << {updateCustomer} SQL Statement ");
				stmt.executeUpdate();
				logr.info("customer updated successfully ");
			} catch (SQLException sqlEx) {
				logr.error("ExtensionSample << {updateCustomer}", sqlEx);
				throw new ExtensionException(sqlEx);
			}
			returnResponseIfRequestPrefers(dpCtx);

		} catch (Exception ex) {
			logr.error("ExtensionSample  << {updateCustomer}", ex);
			throw new ExtensionException(ex);
		} finally {
			if (conn != null)
				conn.close();
		}
		logr.debug("Exiting ExtensionSample  << {updateCustomer}");
	}

	
	 // * This method is used to delete entity
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException 
	 
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.DELETE)
	public void deleteCustomer(ExtensionContext ecx) throws ODataApplicationException, SQLException, ExtensionException {
		String custId = null, custType = null;
		Connection conn = null;

		logr.debug("Entering ExtensionSample  << {deleteCustomer}");

		try {
			DataProviderExtensionContext dpCtx = ecx.asDataProviderContext();
			conn = ((CDSDSParams) ecx.getDSParams()).getConnection();
			
			//The following code gets the object "UriInfo" which is supposed to give us all the information in the current URL of the OData query. We are specially
			//interested in the "key predicates" information.
			UriInfo uri = dpCtx.getUriInfo();
			UriResourceEntitySet entity = (UriResourceEntitySet) uri.getUriResourceParts().get(0);
			List<UriParameter> keyPredicateList = entity.getKeyPredicates();
			//The above obtained List<UriParameter> is nothing but the list of key parameters specified in the Current Url. We extract each such value to form
			//the SQL to delete the correct entry.
			for (UriParameter uriParam : keyPredicateList) {
				if (uriParam.getName().equals("CustomerID")) {
					custId = uriParam.getText();
				}
				if (uriParam.getName().equals("Type")) {
					custType = uriParam.getText();
					custType = custType.substring(1, custType.length()-1);
				}
			}

			PreparedStatement stmt = conn.prepareStatement("DELETE  FROM \"MySalesOrderService.Customer\" WHERE \"CustomerID\" = ? AND \"Type\"= ?");
			stmt.setInt(1, Integer.parseInt(custId));
			stmt.setString(2, custType);
			logr.trace("ExtensionSample  << {deleteCustomer} SQL Statement");
			int rowAffected = stmt.executeUpdate();
			if (rowAffected == 0) {
				throw new ODataApplicationException("Entity not found", 404, Locale.US, "404");
			} else {
				logr.info( " Customer deleted successfully ");
			}
			conn.close();

		} catch (SQLException sqlEx) {
			logr.error("ExtensionSample  << {deleteCustomer}", sqlEx);
			throw new ExtensionException(sqlEx);
		} catch (Exception ex) {
			logr.error("ExtensionSample  << {deleteCustomer}", ex);
			throw new ExtensionException(ex);
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	
	// * This method is used to perform read operation. This is optional as READ functionality is provided by default by the framework. Still it can be used,
	// * if some pre/post processing needs to be done in additional to the default implementation.
	// * @param ecx
	// * @throws ODataApplicationException
	// * @throws ExtensionException
	 
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.READ)
	public void readCustomer(ExtensionContext ecx) throws ODataApplicationException, ExtensionException {

		{
			logr.debug("Entering ExtensionSample << {readCustomer}");

			try {
				//performDefault is used to invoke the default handling for the current method(Here READ). 
				ecx.performDefault();
				logr.debug("Exiting ExtensionSample <<  {readCustomer}");
			} catch (Exception e) {
				logr.error("ExtensionSample <<  {readCustomer}", e);
				throw new ExtensionException(e);
			}
		}
	}
	
	
	 // * This method encapsulates CREATE functionality for Customer entity under Service2.
	 // * 
	 // * @param ecx
	 // * @throws ODataApplicationException
	 // * @throws ExtensionException
	 // * @throws SQLException
	 
	//Here, in addition to the entitySet and requestType parameters there is a 3rd parameter called serviceName, under which the OData service name of the current
	//entity has to be specified. This is done to resolve the ambiguity if multiple OData services expose entities under the same name.
	@ExtendDataProvider(entitySet = { "Customer" }, requestTypes = RequestType.CREATE, serviceName = "Service2")
	public void createCustomerOtherService(ExtensionContext ecx)
			throws ODataApplicationException, ExtensionException, SQLException {
		logr.debug("Entering create Customer method.");
		
		 //Insert logic . . . 
		 
	}
	
	
	
}

*/






