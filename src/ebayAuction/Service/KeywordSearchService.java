package ebayAuction.Service;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;


@Path("/KeywordSearchService")
public class KeywordSearchService {
	
 
	  @Path("{keyword}")
	  @GET
	 // @Produces("application/JSON")
	  public Response keywordSearch(@PathParam("keyword") String keyword) throws JSONException {

       JSONObject jsonObject = new JSONObject();
		 
		SearchResult[] itemList = new AuctionSearch().basicSearch(keyword, 0, 20);
		
		int index = 0;
		for(SearchResult item : itemList){
			jsonObject.put("Item "+(index++), "ID: "+item.getItemId()+" Name: "+item.getName()); 
			// jsonObject.put("Item name", item.getName()); 
		}
	
	//	return Response.status(200).entity("getUserById is called, id : " + itemList.length).build();

    	String result = "@Produces(\"application/json\") Output: \n\n items which contains the keyword: \n\n" + jsonObject;
		return Response.status(200).entity(result).build();
	  }
	  
	  
	  @Path( "{keyword}&{numberToEscape}&{numberToShow}" )
	  @GET
	 // @Produces("application/JSON")
	  public Response numberToEscape(@PathParam("keyword") String keyword,
			  @PathParam("numberToEscape") int numberToEscape,@PathParam("numberToShow") int numberToShow) throws JSONException {

       JSONObject jsonObject = new JSONObject();
		 
		SearchResult[] itemList = new AuctionSearch().basicSearch(keyword, numberToEscape, numberToShow);
		
		int index = 0;
		for(SearchResult item : itemList){
			jsonObject.put("Item "+(index++), "ID: "+item.getItemId()+" Name: "+item.getName()); 
			// jsonObject.put("Item name", item.getName()); 
		}
	
	//	return Response.status(200).entity("getUserById is called, id : " + itemList.length).build();

    	String result = "@Produces(\"application/json\") Output: \n\n items which contains the keyword: \n\n" + jsonObject;
		return Response.status(200).entity(result).build();
	  }
}
