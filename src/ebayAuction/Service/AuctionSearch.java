package ebayAuction.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.util.*;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
public class AuctionSearch implements IAuctionSearch {
    IndexSearcher searcher;
    QueryParser parser;

    /* 
     * You will probably have to use JDBC to access MySQL data
     * Lucene IndexSearcher class to lookup Lucene index.
     * Read the corresponding tutorial to learn about how to use these.
     *
     * Your code will need to reference the directory which contains your
     * Lucene index files.  Make sure to read the environment variable 
     * $LUCENE_INDEX with System.getenv() to build the appropriate path.
     *
     * You may create helper functions or classes to simplify writing these
     * methods. Make sure that your helper functions are not public,
     * so that they are not exposed to outside of this class.
     *
     * Any new classes that you create should be part of
     * edu.ucla.cs.cs144 package and their source files should be
     * placed at src/edu/ucla/cs/cs144.
     *
     */
    public SearchResult[] basicSearch(String query, int numResultsToSkip, int numResultsToReturn) {
        SearchResult[] resultstore = new SearchResult[0];
        try 
        {
            // Access the lucene index
        	// String parent = new File("").getAbsolutePath(); 
        
        	 
        	File path = new File("C:\\Users\\Tony\\workspace\\ebayBidSearchService\\index-directory");

        	Directory index = FSDirectory.open(path);
        	IndexReader reader = DirectoryReader.open(index);
        	IndexSearcher searcher = new IndexSearcher(reader);
        	
            parser = new QueryParser("content", new StandardAnalyzer());

            Query q = parser.parse(query);        
            TopDocs hits = searcher.search(q, numResultsToReturn+numResultsToSkip);
            ScoreDoc[] sd= hits.scoreDocs;
            
            // Allocate the results array
            resultstore = new SearchResult[numResultsToReturn];
            
                
            
            // Iterators for the hits(matches found)
            // And for resultsStore to return
            int resultIndex = 0;
            for(int i= numResultsToSkip; i< numResultsToReturn+numResultsToSkip && i< sd.length; i++){
        	        int docId = sd[i].doc;
        	        
                    Document doc = searcher.doc(docId);
                    resultstore[resultIndex] = new SearchResult(doc.get("id"), doc.get("name"));
                    resultIndex++;
                }

              
                
           
        } 
        catch (Exception e) 
        {
            System.out.println(e);    
        }
        
        return resultstore;
    }

    /* Returns the date from the xml files' format (Dec-04-01 04:03:12)
     * converted to MySQL timestamp readable format.
     */
    static String convertDate(String dateString, String oldFormat, String newFormat) {


        SimpleDateFormat sdf = new SimpleDateFormat(oldFormat);
        String out = "";
        try {
            Date d = sdf.parse(dateString);
            sdf.applyPattern(newFormat);
            out = sdf.format(d);
        } catch (Exception e) {
            System.out.println("Could not format date");
        }

        return out;
    }

    // Build the lucene query from the given constraints
    public String buildLuceneQuery(SearchConstraint[] constraints) {
        // Add queries to a list
        Stack<String> fieldQueries = new Stack();

        // Iterate over constraints and push if 
        // on a field that is indexed by lucene
        int numConstraints = constraints.length;
        for(int i = 0; i < numConstraints; i++) {
            // Pull out Lucene indexed constraints
			// if you try to serach on name, category, description sepataely, you need to call three parser on three fileds sepatedly.
			//  and the nunion the result, it is wrong to call the content parser ??
            if(constraints[i].getFieldName().equals(FieldName.ItemName))
              //  fieldQueries.push("name:" + constraints[i].getValue());
               fieldQueries.push( constraints[i].getValue());
            else if(constraints[i].getFieldName().equals(FieldName.Category))
              //  fieldQueries.push("category:" + constraints[i].getValue());
                 fieldQueries.push( constraints[i].getValue());   
            else if(constraints[i].getFieldName().equals(FieldName.Description))
              //  fieldQueries.push("description:" + constraints[i].getValue());
		         fieldQueries.push( constraints[i].getValue());
        }

        // Build final query String
        StringBuilder query = new StringBuilder();
        if(!fieldQueries.empty()) {
            query.append(fieldQueries.pop());
            while(!fieldQueries.empty()) {
                String next = fieldQueries.pop();
                query.append(" AND ");
                query.append(next);
            }
        }

        return query.toString();
    }

    public String buildSqlQuery(SearchConstraint[] constraints) {
            
        Stack<String> whereClauses = new Stack();
        // Iterate over constraints and push if
        // on a field that are indexed by lucene
        int numConstraints = constraints.length;
        boolean bidJoin = false;
        for(int i = 0; i < numConstraints; i++) {

            // Pull out MySQL indexed constraints
            if(constraints[i].getFieldName().equals(FieldName.SellerId))
                whereClauses.push("Seller_Id = '" + constraints[i].getValue() + "'");
           
            else if(constraints[i].getFieldName().equals(FieldName.BuyPrice))
                whereClauses.push("Buy_Price = " + constraints[i].getValue());
                    
            else if(constraints[i].getFieldName().equals(FieldName.EndTime))
                whereClauses.push("Ends = \"" + convertDate(constraints[i].getValue(), "MMM-dd-yy HH:mm:ss", "yyyy-MM-dd HH:mm:ss") + "\"");

            else if(constraints[i].getFieldName().equals(FieldName.BidderId)) {
                bidJoin = true;
                whereClauses.push("User_ID = '" + constraints[i].getValue() + "'");
            }
                
        }

        StringBuilder query = new StringBuilder();      
        if(!whereClauses.empty()) {
            query.append("SELECT Item_ID, Item_name FROM Items ");
            if (bidJoin)
                query.append("INNER JOIN Auctions ON Items.Item_ID = Auctions.Item_ID ");

            query.append("WHERE " + whereClauses.pop() + " ");

            while(!whereClauses.empty()) {
                query.append("AND " + whereClauses.pop() + " ");
            }
        }
        return query.toString();
    }

    public SearchResult[] advancedSearch(SearchConstraint[] constraints, 
        int numResultsToSkip, int numResultsToReturn) {
            
        // Handle Lucene querying
        Map<String, String> luceneResult = new HashMap<String, String>();
        try {
            String luceneQuery = buildLuceneQuery(constraints);
            System.out.println("lucene Query is "+luceneQuery);
			
            if(!luceneQuery.isEmpty()) {
                // Access the lucene index 
                // Query Parser has no default field because 
                // query will specify fields.
            	String parent = new File("").getAbsolutePath(); 
        
        	 
            	File path = new File("C:\\Users\\Tony\\workspace\\ebayBidSearchService\\index-directory");

            	Directory index = FSDirectory.open(path);
            	IndexReader reader = DirectoryReader.open(index);
            	IndexSearcher searcher = new IndexSearcher(reader);
            	
                parser = new QueryParser("content", new StandardAnalyzer());

                // Execute Query
                Query q = parser.parse(luceneQuery);     
                
                
                TopDocs hits = searcher.search(q, numResultsToReturn+numResultsToSkip);
                ScoreDoc[] sd= hits.scoreDocs;
                
                // Iterators for the hits(matches found)
                // And for resultsStore to return
                System.out.println("advanced search found "+sd.length+" results");
                for(int i= numResultsToSkip; i< numResultsToReturn+numResultsToSkip && i< sd.length; i++){
            	        int docId = sd[i].doc;
                        Document doc = searcher.doc(docId);
                        luceneResult.put(doc.get("id"), doc.get("name"));
						System.out.println("I found a item " );
                       
                    }
                }
           

        } catch (Exception e) {
            System.out.println(e);
        }
        
        // Handle SQL querying
        Map<String, String> sqlResult = new HashMap<String, String>();
        Connection conn = null;
        try {
            String sqlQuery = buildSqlQuery(constraints);

            conn = DbManager.getConnection(true);
            Statement stmt = conn.createStatement();

            // Execute query
			if(sqlQuery.length()>0){
            ResultSet items = stmt.executeQuery(sqlQuery);
			  while(items.next()) {
                    sqlResult.put(items.getString("Item_ID"), items.getString("Item_name"));         
                }
            }
         
        }
        catch (Exception e) {
            System.out.println(e);
        }

        // Create the final SearchResult
        // Determine if we must do an intersection 
		 
        Set<String> itemIds;
        Map<String, String> result;
        if(sqlResult.isEmpty()) {
            itemIds = luceneResult.keySet();
            result = luceneResult;
        } else if(luceneResult.isEmpty()) {
            itemIds = sqlResult.keySet();
            result = sqlResult;
        } else {
            // In this case do an intersection of the keys
            itemIds = luceneResult.keySet();
            itemIds.retainAll(sqlResult.keySet());
            result = new HashMap<String, String>();
			for(String item : luceneResult.keySet()){
				result.put(item, luceneResult.get(item));
			}
			
			for(String item : sqlResult.keySet()){
				result.put(item, sqlResult.get(item));
			}
        }

        // Add the keys and itemIds to the SearchResult array
        SearchResult[] resultstore = new SearchResult[itemIds.size()];

        Iterator<String> i = itemIds.iterator();
        int resultIndex = 0;

        while(i.hasNext()) {
            String itemId = i.next();
            resultstore[resultIndex] = new SearchResult(itemId, result.get(itemId));
            resultIndex++;
        }

        return resultstore;
		
		 
    }
     
	 
	 //overload basic serach
	 //  public SearchResult[] basicSearch(String query) {
		//  return basicSearch( query, 0, 100);  
	//}
	 


	 
	 
    public String getXMLDataForItemId(String itemId) {
        String xmlstore = "";

        Connection conn = null;

        // Create a connection to the database
        try 
        {     // System.out.print("before connection");
            // Connection to db manager
            conn = DbManager.getConnection(true);
            Statement statement = conn.createStatement();
           // System.out.print("successfully connected to database");

            // Geting the items
            ResultSet result = statement.executeQuery("SELECT * FROM Items "
                                                    + "WHERE Items.Item_ID = " + itemId);
                
            result.first();
            // Somethings in it
            
            if (result.getRow() != 0) 
            {                             
                // System.out.println("I found a item " );
                DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
                DocumentBuilder b          = fac.newDocumentBuilder();
                org.w3c.dom.Document doc   = b.newDocument();

                // root element
                Element root = doc.createElement("Item");
                root.setAttribute("ItemID", itemId);
                doc.appendChild(root);

                Element element_name = doc.createElement("Name");
                element_name.appendChild(doc.createTextNode(replacespecial(result.getString("Item_name"))));
                root.appendChild(element_name);

                
       /*         // Build Category Elements
                // Get the Categories
                Statement catstatement = conn.createStatement();
                ResultSet catresult = catstatement.executeQuery("SELECT Category "
                                                              + "FROM Category,Item_Category "
                                                              + "WHERE Item_Category.Item_ID = " + itemId + " "
                                                              + "AND Item_Category.Category_ID = Category.Category_ID");

                Element category_element;
                while (catresult.next()) {
                    category_element = doc.createElement("Category");
                    category_element.appendChild(doc.createTextNode(replacespecial(catresult.getString("Category"))));
                    root.appendChild(category_element);
                }

                catresult.close();
                catstatement.close();*/

                // Build Item price elements
                if (result.getString("Currently") != null) {
                Element currently_element = doc.createElement("Currently");
                currently_element.appendChild(doc.createTextNode("$" + result.getString("Currently")));
                root.appendChild(currently_element);
                }
                
                
                if (result.getString("Buy_Price") != null) {
                    Element buyprice_element = doc.createElement("Buy_Price");
                    buyprice_element.appendChild(doc.createTextNode("$" + result.getString("Buy_Price")));
                    root.appendChild(buyprice_element);
                }
                
                
                if (result.getString("First_Bid") != null) {
                Element start_element = doc.createElement("First_Bid");
                start_element.appendChild(doc.createTextNode("$" + result.getString("First_Bid")));
                root.appendChild(start_element);
                }
                
                
                // num bids
                if (result.getString("Number_of_Bids") != null) {
                Element numberbids_elements = doc.createElement("Number_of_Bids");
                numberbids_elements.appendChild(doc.createTextNode(result.getString("Number_of_Bids")));
                root.appendChild(numberbids_elements);
                }
                // description
                
                if (result.getString("Description") != null) {
                Element description_element = doc.createElement("Description");
                description_element.appendChild(doc.createTextNode(replacespecial(result.getString("Description"))));
                root.appendChild(description_element);
                }
                             

             /*  // location
                Element location_element = doc.createElement("Location");
                location_element.appendChild(doc.createTextNode((replacespecial(result.getString("Location")))));
                root.appendChild(location_element);

                // country
                Element country_element = doc.createElement("Country");
                country_element.appendChild(doc.createTextNode((replacespecial(result.getString("Country")))));
                root.appendChild(country_element);

                // started
                Element started_elem = doc.createElement("Started");
                started_elem.appendChild(doc.createTextNode(convertDate(result.getString("Started"), "yyyy-MM-dd HH:mm:ss", "MMM-dd-yy HH:mm:ss")));
                root.appendChild(started_elem);

                // ends
                Element ends_element = doc.createElement("Ends");
                ends_element.appendChild(doc.createTextNode(convertDate(result.getString("Ends"), "yyyy-MM-dd HH:mm:ss", "MMM-dd-yy HH:mm:ss")));
                root.appendChild(ends_element);*/
                
                
           
                
/*                // Build Bid Elements
                Statement bidstatement = conn.createStatement();
                ResultSet bidresult = bidstatement.executeQuery("SELECT * " 
                                                              + "FROM Auctions, AuctionUser "  
                                                              + "WHERE Auctions.Item_Id = " + itemId + " "
                                                              + "AND Auctions.User_ID = AuctionUser.User_ID");

                Element bids_element = doc.createElement("Bids");

                while (bidresult.next()) {
                    try {
                        Element bid_element = doc.createElement("Bid");
                        Element bidder_element = doc.createElement("Bidder");
                        bidder_element.setAttribute("UserID", replacespecial(bidresult.getString("User_ID")));
                       // bidder_element.setAttribute("Rating", bidresult.getString("Rating"));

                        // Add Location and Country elements if they aren't NULL
                        if (!bidresult.getString("Location").equals("")) {
                            Element location_element = doc.createElement("Location");
                            location_element.appendChild(doc.createTextNode((replacespecial(bidresult.getString("Location")))));
                            bidder_element.appendChild(location_element);
                        }
                        if (!bidresult.getString("Country").equals("")) {
                            Element country_element = doc.createElement("Country");
                            country_element.appendChild(doc.createTextNode((replacespecial(bidresult.getString("Country")))));
                            bidder_element.appendChild(country_element);
                        }
                        bid_element.appendChild(bidder_element);

                        // time
                        Element time_element = doc.createElement("Time");
                        time_element.appendChild(doc.createTextNode(convertDate(bidresult.getString("Bid_Time"), "yyyy-MM-dd HH:mm:ss", "MMM-dd-yy HH:mm:ss")));
                        bid_element.appendChild(time_element);

                        // amount
                        Element amount_element = doc.createElement("Amount"); 
                        amount_element.appendChild(doc.createTextNode(bidresult.getString("Bid_Time")));
                        bid_element.appendChild(amount_element);

                        bids_element.appendChild(bid_element);
                    } catch (SQLException e) {
                        System.out.println(e);
                    }
                }

                root.appendChild(bids_element);

                bidresult.close();
                bidstatement.close();*/
                
                
                
                
                
/*                // Get the Seller data
                Statement sellstatement = conn.createStatement();
                ResultSet sellres = sellstatement.executeQuery("SELECT User_ID, Rating, Location, Country "
                                                             + "FROM Items, AuctionUser " 
                                                             + "WHERE Items.Item_ID" + " = " + itemId + " "
                                                             + "AND Items.Seller_Id = AuctionUser.User_ID");
                sellres.first();
                
                // seller
                                Element sellerElem = doc.createElement("Seller");
                                sellerElem.setAttribute("UserID", (replacespecial(sellres.getString("UserID"))));
                                sellerElem.setAttribute("Rating", sellres.getString("Rating"));
                                root.appendChild(sellerElem);
                                sellres.close();
                                sellstatement.close();*/
                                
                           
                                
                                

               
                // Write the XML
                TransformerFactory newfactory = TransformerFactory.newInstance();
                Transformer transform = newfactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                
                StringWriter writer = new StringWriter();
                StreamResult res = new StreamResult(writer);
                transform.setOutputProperty(OutputKeys.INDENT, "yes");
                transform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transform.transform(source, res);
                xmlstore = writer.toString();
                xmlstore = replacespecial1(xmlstore);
            }

            result.close();
            statement.close();

            conn.close();

        } catch (SQLException e) {
            System.out.println(e);            
        } catch (ParserConfigurationException e) {
            System.out.println("oops");
        } catch (TransformerException e) {
            System.out.println("oops");
        }

        return xmlstore;
    
    }
       private String replacespecial(String s) {
/*       	return s.replaceAll("\"", "thisisaquotethatwehavetohandle")
                                .replaceAll("\'", "thisisanaposweneedtohandle")
                                
                                ;*/
                return s;
              
        }
       private String replacespecial1(String s) {
       	return s.replaceAll( "thisisaquotethatwehavetohandle", "&quot;")
                                .replaceAll( "thisisanaposweneedtohandle","&apos;")
                                
                               ;
                
              
        }
    
    public String echo(String message) {
        return message;
    }
}

