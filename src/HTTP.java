import java.util.List;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class HTTP {
	public static HttpURLConnection con = null;

	public HTTP() {


		//		for(String key:episodeNames.keySet()) {
		//			System.out.println("---"+key+"---");
		//			ArrayList<String> season = episodeNames.get(key);
		//			for(int j = 0; j<season.size(); j++) {
		//				System.out.println(season.get(j));
		//			}
		//			System.out.println();
		//		}
	}
	public static String predictSeriesName(String path) {
		//get the token needed to send requests
		String token = getToken();

		System.out.println("Attempting to predict the series name so that you don't have to type it out!");

		String cleanedPath = path.replaceAll("/", "\\\\");
		String[] possibleSearchTerms = cleanedPath.split("\\\\");

		Scanner in = new Scanner(System.in);
		boolean predictedSeriesWasCorrect = false;
		ArrayList<Series> listOfSeries = null;
		int selectedSeries = -1;
		if(possibleSearchTerms.length >= 2) {
			String periodsRemoved = possibleSearchTerms[possibleSearchTerms.length - 2].replaceAll("\\.", " ");

			String[] wordsInTitle = periodsRemoved.split(" ");
			String term = "";
			for(int i = 0; listOfSeries == null || listOfSeries.size() >=98; i++) {
				if(i > wordsInTitle.length - 1)
					break;
				term = term+" "+wordsInTitle[i];
				System.out.println("Attempting search term: "+term);
				//search for the specified series
				String searchResult = searchForSeries(term, token);

				//Get the information about the search results
				listOfSeries = getIDsFromSearchForSeries(searchResult);
			}
			if(listOfSeries.size() > 0) {
				//try to predict the series
				int mostLikelySeries = 0;
				int currentMost = 0;
				for(int i = 0; i<listOfSeries.size(); i++) {
					Series c = listOfSeries.get(i);
					int cMatches = Main.sharedSubStrings(c.name, path).size();
					if(cMatches > currentMost) {
						mostLikelySeries = i;
						currentMost = cMatches;
					}
				}

				System.out.println("Predicted Series:");
				System.out.println(listOfSeries.get(mostLikelySeries).name + " - "+listOfSeries.get(mostLikelySeries).description);

				System.out.println("Is this correct? (y/n)");
				String resp = in.nextLine();
				if(resp.equalsIgnoreCase("y")||resp.equalsIgnoreCase("yes")) {
					predictedSeriesWasCorrect = true;
					selectedSeries = mostLikelySeries;
				}
			}
		}
		if(!predictedSeriesWasCorrect) {

			System.out.println("What is the name of this series?");
			String searchTerm = in.nextLine();

			//search for the specified series
			String searchResult = searchForSeries(searchTerm, token);

			//Get the information about the search results
			listOfSeries = getIDsFromSearchForSeries(searchResult);

			//print a list of the series that were found and the index they correspond to
			for(int i = 0; i<listOfSeries.size(); i++) {
				Series c = listOfSeries.get(i);
				System.out.println("["+i+"] "+c.name + " - " + c.description);
			}

			//prompt for the user to tell the program what series this is
			System.out.println("\nEnter the number of the series associated with this directory. If none of these match, enter the letter M for more results.");
			System.out.println("Input: ");
			boolean validInput = false;
			while(!validInput) {
				String input = in.nextLine();
				if(input.equalsIgnoreCase("m")) {
					validInput = true;
				}
				else if(Integer.valueOf(input) >= 0 && Integer.valueOf(input) < listOfSeries.size()) {
					selectedSeries = Integer.valueOf(input);
					validInput = true;
				}
				else {
					System.out.println("Please enter a valid input");
				}
			}

			if(selectedSeries==-1) {
				System.out.println(Main.ANSI_RED+"Unable to continue because none of the search results matched the desired series. EXITING"+Main.ANSI_RESET);
				System.exit(0);
			}
		}
		return listOfSeries.get(selectedSeries).id;
	}

	public static ArrayList<String> getEpisodeNamesForSeason(String path, String seriesName, ArrayList<String> videoFileNames, HashMap<String, ArrayList<String>> episodesInSeries){
		Scanner in = new Scanner(System.in);

		//HashMap<String, ArrayList<String>> res = getEpisodeNamesForSeries(seriesName, token);

		ArrayList<String> seasons = new ArrayList<String>(episodesInSeries.keySet());

		//try to predict the season
		int[] matchesPerSeason = new int[seasons.size()];
		String[] largestMatchPerSeason = new String[seasons.size()];
		int i = 0;
		for(String key:seasons) {
			for(int j = 0; j<videoFileNames.size(); j++) {
				ArrayList<String> sharedSubstrings = Main.sharedSubStrings(episodesInSeries.get(key).toString(), videoFileNames.get(j));
				matchesPerSeason[i] += sharedSubstrings.size();

				//determine the length of the largest shared substring
				for(String s:sharedSubstrings) {
					if(largestMatchPerSeason[i] == null || s.length() > largestMatchPerSeason[i].length())
						largestMatchPerSeason[i] = s;
				}
			}
			i++;
		}

		int greatestSize = -1;
		for(int j = 0; j< largestMatchPerSeason.length; j++) {
			if(largestMatchPerSeason[j] != null) {
				if(greatestSize == -1 || largestMatchPerSeason[j].length()>largestMatchPerSeason[greatestSize].length()) {
					greatestSize = j;
				}
			}
		}

		int greatestMatches = -1;
		for(int j = 0; j< matchesPerSeason.length; j++) {
			if(greatestSize == -1 || matchesPerSeason[j]>matchesPerSeason[greatestSize]) {
				greatestMatches = j;
			}
		}
		int greatest;
		if(greatestSize >= 0 && largestMatchPerSeason[greatestSize].length() > 5) {
			System.out.println("Using the season with the largest shared substring as the predicted substring.");
			greatest = greatestSize;
		}
		else {
			System.out.println("Using the season with the largest number of shared substrings as the predicted season.");
			greatest = greatestMatches;
		}
		System.out.println(Main.ANSI_GREEN+"Season being checked contains the following episodes: "+Main.ANSI_RESET);
		for(int j = 0; j<videoFileNames.size(); j++) {
			System.out.print(videoFileNames.get(j));
			if(j < videoFileNames.size() - 1)
				System.out.println(Main.ANSI_GREEN+","+Main.ANSI_RESET);
		}
		System.out.println("\n\n"+Main.ANSI_GREEN+"Predicted season is \""+seasons.get(greatest)+"\". It contains the following episodes: "+Main.ANSI_RESET);
		for(int j = 0; j<episodesInSeries.get(seasons.get(greatest)).size(); j++) {
			System.out.print(episodesInSeries.get(seasons.get(greatest)).get(j));
			if(j < episodesInSeries.get(seasons.get(greatest)).size() - 1)
				System.out.println(Main.ANSI_GREEN+","+Main.ANSI_RESET);
		}
		System.out.println("\n\nIs this correct? (y/n)");
		String input = in.nextLine();

		if(input.equalsIgnoreCase("y")||input.equalsIgnoreCase("yes")) {
			//in.close();
			return episodesInSeries.get(seasons.get(greatest));
		}
		else {
			String season = "";
			i = 0;
			for(String key:seasons) {
				System.out.println(Main.ANSI_GREEN+"["+i+"] "+"("+matchesPerSeason[i]+" shared substrings, largest shared substring: "+largestMatchPerSeason[i]+")"+Main.ANSI_RESET);
				System.out.println(episodesInSeries.get(key));
				System.out.println();
				i++;
			}
			while(true) {
				System.out.println("Which of these seasons represents the files in this directory?");
				season = in.nextLine();
				if(Integer.valueOf(season) >=0 && Integer.valueOf(season) < episodesInSeries.keySet().size())
					break;
			}
			//in.close();
			return episodesInSeries.get(seasons.get(Integer.valueOf(season)));
		}
	}

	public static HashMap<String, ArrayList<String>> getEpisodeNamesForSeries(String id, String token){
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		int page = 1;
		while(true) {
			URL url = null;
			try {
				//System.out.println("id is: "+id);
				url = new URL("https://api.thetvdb.com/series/"+id+"/episodes?page="+page);
				con = (HttpURLConnection)url.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("Content-Type", "application/json");
				con.setRequestProperty("Authorization", "Bearer "+token);

				con.setDoOutput(true);
				con.setConnectTimeout(5000);

				String fullResponse = getFullResponse(con);
				//System.out.println(fullResponse);

				//if this page has no results then stop sending requests for more results
				if(fullResponse.contains("No results for your query:"))
					break;

				String[] episodes = fullResponse.split("\\},\\{");
				//System.out.println("\n---getepisodenamesforseries----=");
				//String[] episodesInOrder = new String[episodes.length];
				
				//Structured as follows: HashMap<Season, HashMap<episode #, episode name>>
				HashMap<String, TreeMap<Integer, String>> episodesInOrder = new HashMap<String, TreeMap<Integer, String>>();
				
				for(String s:episodes) {
					//System.out.println(s);
					String episodeNumber = s.split("airedEpisodeNumber\":")[1].split(",")[0];
					String episodeSeason = s.split("airedSeason\":")[1].split(",")[0];
					String episodeName = s.split("episodeName\":\"")[1].split("\",\"firstAired")[0];

					if(!result.containsKey(episodeSeason))
						result.put(episodeSeason, new ArrayList<String>());
					
					if(!episodesInOrder.containsKey(episodeSeason))
						episodesInOrder.put(episodeSeason, new TreeMap<Integer, String>());

					episodesInOrder.get(episodeSeason).put(Integer.valueOf(episodeNumber), episodeName);
				}

				for(String season: episodesInOrder.keySet()) {
					for(int episodeNum: episodesInOrder.get(season).keySet()) {
						result.get(season).add(episodesInOrder.get(season).get(episodeNum));
						//System.out.println("added "+episodeNum+" "+episodesInOrder.get(season).get(episodeNum) + " to season: "+season);
					}
				}
				//System.out.println();
				//System.out.println(fullResponse);

			}
			catch(Exception e) {
				e.printStackTrace();
				return null;
			}
			page++;
		}
		return result;
	}
	public static ArrayList<Series> getIDsFromSearchForSeries(String data) {
		ArrayList<Series> result = new ArrayList<Series>();

		String response = data.split("Response: ")[1];

		String[] series = response.split("},");
		try {
			for(String s:series) {
				Series currentSeries = new Series();
				currentSeries.name = s.split("seriesName\":\"")[1].split("\",\"slug")[0];
				currentSeries.description = "No description.";
				currentSeries.id = s.split("\"id\":")[1].split(",")[0];
				if(s.contains("overview\":\""))
					currentSeries.description = s.split("overview\":\"")[1].split("\",\"seriesName")[0];
				result.add(currentSeries);
			}
		}
		catch (Exception e) {
			System.out.println("No search results found!");
			//System.out.println("Recieved data was:");
			//System.out.println(data);
			System.out.println();
		}

		return result;
	}
	public static String searchForSeries(String title, String token) {
		String result = "";
		URL url = null;
		try {
			url = new URL("https://api.thetvdb.com/search/series?name="+title);
			con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", "Bearer "+token);

			con.setDoOutput(true);
			con.setConnectTimeout(5000);

			result = getFullResponse(con);


		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	public static String getToken() {

		URL url = null;
		try {
			url = new URL("https://api.thetvdb.com/login");
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			Map<String, String> parameters = new HashMap<>();
			parameters.put("apikey", "7f3ee40ff2c1b99009c2726f0be5cde3");
			parameters.put("userkey", "5E07F55B406527.60164639");
			parameters.put("username", "benby");

			con.setDoOutput(true);
			con.setConnectTimeout(5000);


			DataOutputStream out = new DataOutputStream(con.getOutputStream());
			String paramString = getJsonParamString(parameters);//getParamsString(parameters);
			//System.out.println("paramString: \n"+paramString);
			out.writeBytes(paramString);
			out.flush();
			out.close();


			String fullResponse = getFullResponse(con);

			String tmp = fullResponse.split("\"token\":\"")[1];
			return tmp.substring(0, tmp.length()-2);
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	public static String getFullResponse(HttpURLConnection con) throws IOException {
		StringBuilder fullResponseBuilder = new StringBuilder();

		fullResponseBuilder.append(con.getResponseCode())
		.append(" ")
		.append(con.getResponseMessage())
		.append("\n");

		con.getHeaderFields()
		.entrySet()
		.stream()
		.filter(entry -> entry.getKey() != null)
		.forEach(entry -> {

			fullResponseBuilder.append(entry.getKey())
			.append(": ");

			List<String> headerValues = entry.getValue();
			Iterator<String> it = headerValues.iterator();
			if (it.hasNext()) {
				fullResponseBuilder.append(it.next());

				while (it.hasNext()) {
					fullResponseBuilder.append(", ")
					.append(it.next());
				}
			}

			fullResponseBuilder.append("\n");
		});

		Reader streamReader = null;

		if (con.getResponseCode() > 299) {
			streamReader = new InputStreamReader(con.getErrorStream());
		} else {
			streamReader = new InputStreamReader(con.getInputStream());
		}

		BufferedReader in = new BufferedReader(streamReader);
		String inputLine;
		StringBuilder content = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}

		in.close();

		fullResponseBuilder.append("Response: ")
		.append(content);

		return fullResponseBuilder.toString();
	}

	public static String getJsonParamString(Map<String, String> params) {
		String result = "{";
		for (Map.Entry<String, String> entry : params.entrySet()) {
			result = result + "\""+entry.getKey()+"\":\""+entry.getValue()+"\", ";
		}
		result = result.substring(0, result.length() - 2);
		result = result+"}";
		return result;
	}
	public static String getParamsString(Map<String, String> params) 
			throws UnsupportedEncodingException{
		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			result.append("&");
		}

		String resultString = result.toString();
		return resultString.length() > 0
				? resultString.substring(0, resultString.length() - 1)
						: resultString;
	}
}
