package ebayAuction.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

@Path("/ItemSearchService")
public class SearchItemService {
	  @Path("{Id}")
	  @GET
	  @Produces("application/xml")
	  public Response getItemInformation(@PathParam("Id") int Id) throws JSONException {

	//	JSONObject jsonObject = new JSONObject();
		
		String Information = new AuctionSearch().getXMLDataForItemId(String.valueOf(Id));
	//	jsonObject.put("Item information", Information); 
		 

		// String result = "@Produces(\"application/xml\") Output: \n\n Retrieved item: \n\n" + Information;
		return Response.status(200).entity(Information).build();
	  }
	  
	 
}
