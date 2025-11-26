
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
	static String path = "Not changed after initialization.";
	static long startTime = System.currentTimeMillis();
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static long totalCharactersInNames = 0l;
	public static long totalCharactersInPaths = 0l;
	public static String[] doNotCapitalize = {"for", "and", "the", "a", "of", "with", "in", "on", "nor", "or", "so", "to", "at", "as", "an", "vs."};
	public static String[] doCapitalize = {"II", "III", "IV", "VI", "VII", "VIII", "IX", "X"};
	public static boolean useTitleCase = true;

	/**
	 * Main method, pass the path to the season you want organized as an argument.
	 * @param args - The path to the season you want organized. 
	 * 				 You can add the -t flag to prevent files from being renamed to title case.
	 */
	public static void main(String[] args) {
		HTTP http = new HTTP();

		if(args.length>0) {
			path = args[0];
			for(int i = 1; i<args.length; i++) {
				if(args[i].equalsIgnoreCase("-t"))
					useTitleCase = false;
			}
		}
		else {
			System.out.println(ANSI_RED+"usage: java -jar FileOrganizer.jar /path/to/show/you/want/organized"+ANSI_RESET);
			System.out.println(ANSI_RED+"possible flags: \n\t-t (File names will not be converted to title case.)"+ANSI_RESET);
			return;
		}
		Scanner in = new Scanner(System.in);
		//System.out.println("What is the name of the show?");
		//String name = in.nextLine();
		renameSeries(path);

	}

	/**
	 * Determine whether or not the given directory contains some video files.
	 * @param inputPath - The path to the directory which needs to be checked.
	 * @return - True if there are some video files within the directory, false if not.
	 */
	public static boolean directoryContainsMediaFiles(String inputPath) {
		File folder = new File(inputPath);
		try {
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {		
				if (listOfFiles[i].isFile()) {
					String pathTo = (inputPath+"/"+listOfFiles[i].getName());
					String fileExtension = getFileExtension(pathTo);
					if(fileExtension.equalsIgnoreCase(".mp4")||fileExtension.equalsIgnoreCase(".avi")||fileExtension.equalsIgnoreCase(".webm")||fileExtension.equalsIgnoreCase(".mkv")) {
						return true;
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}



	/**
	 * Add additional formating to an episode name to make it match what's expected by Jellyfin.
	 * This includes things like "00X" in front of the episode name to represent the order of the
	 * episode within it's season.
	 * @param searchResults - The original episode names obtained from theTVDB
	 * @return - The episode names after they've been changed to conform to what format is expected for use on Jellyfin.
	 */
	public static ArrayList<String> createEpisodeNamesFromSearch(ArrayList<String> searchResults){
		/*
		System.out.println("\n----search results----");
		for(String s: searchResults) {
			System.out.println(s);
		}
		*/
		System.out.println();
		ArrayList<String> names = new ArrayList<String>();
		for(int i = 0; i<searchResults.size(); i++) {
			String newName = padNumber(names.size()+1,2)+" "+searchResults.get(i);
			//if the new name has (#) at the end
			if(newName.contains("(")){
				int openingParenthesis = -1;
				int closingParenthesis = -1;
				for(int j = 0; j<newName.length();j++) {
					if(newName.charAt(j)=='(') {//start found at j
						openingParenthesis=j;
						for(int k = j+1; k<newName.length();k++) {
							if(!(isDigit(newName.charAt(k))||newName.charAt(k)==(')'))) {
								break;
							}
							else if(newName.charAt(k)==(')')) {//end found at j
								closingParenthesis=k;
								break;
							}
						}
					}
				}
				if(openingParenthesis!=-1&&closingParenthesis!=-1) {
					System.out.println("Found an opening parenthesis and a closing parenthesis.");
					System.out.println("Making the following change:");
					System.out.println("	"+newName);
					newName = newName.substring(0, openingParenthesis+1)+"Part "+newName.substring(openingParenthesis+1,newName.length());
					System.out.println("	--->"+newName);
				}
			}
			newName = newName.replaceAll("\\s+", " ").trim();
			newName = newName.replaceAll("/", "∕");
			if(useTitleCase)
				newName = toTitleCase(newName);
			names.add(newName);
		}
		return names;
	}

	/**
	 * An unused function for getting a list of episode names from the clipboard.
	 * (you would have to load the TVDB page representing a season, ctrl-a, then ctrl-c
	 * beforehand for this function to work)
	 */
	public static ArrayList<String> getEpisodeNamesFromClipboard(){

		ArrayList<String> names = new ArrayList<String>();
		while(true) {
			names.clear();
			String data = getClipboardData();
			String lines[] = data.split("\\r?\\n");

			if(lines[1].startsWith("TheTVDB.com")) {
				for(int i = 0; i<lines.length;i++) {
					if(lines[i].length() > 9) {
						if(lines[i].charAt(4) == 'S' && isDigit(lines[i].charAt(5)) && isDigit(lines[i].charAt(6)) && lines[i].charAt(7) == 'E' && isDigit(lines[i].charAt(8)) && isDigit(lines[i].charAt(9))) {

							String newName = padNumber(names.size()+1,2)+" "+lines[i].substring(11);
							//if the new name has (#) at the end
							if(newName.contains("(")){
								int openingParenthesis = -1;
								int closingParenthesis = -1;
								for(int j = 0; j<newName.length();j++) {
									if(newName.charAt(j)=='(') {//start found at j
										openingParenthesis=j;
										for(int k = j+1; k<newName.length();k++) {
											if(!(isDigit(newName.charAt(k))||newName.charAt(k)==(')'))) {
												break;
											}
											else if(newName.charAt(k)==(')')) {//end found at j
												closingParenthesis=k;
												break;
											}
										}
									}
								}
								if(openingParenthesis!=-1&&closingParenthesis!=-1) {
									System.out.println("Found an opening parenthesis and a closing parenthesis.");
									System.out.println("Making the following change:");
									System.out.println("	"+newName);
									newName = newName.substring(0, openingParenthesis+1)+"Part "+newName.substring(openingParenthesis+1,newName.length());
									System.out.println("	--->"+newName);
								}
							}
							newName = newName.replaceAll("\\s+", " ").trim();
							newName = newName.replaceAll("/", "∕");
							if(useTitleCase)
								newName = toTitleCase(newName);
							names.add(newName);
						}
					}
				}
				break;
			}
			else {
				System.out.println("Please copy some relevant data to the clipboard.");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {e.printStackTrace();}
			}
		}

		return names;
	}

	/**
	 * Old obsolete function for getting a list of episode names from the clipboard.
	 * (you would have had to load the TVDB page representing a season, ctrl-a, then ctrl-c
	 * beforehand for this function to work)
	 */
	public static ArrayList<String> getEpisodeNamesFromClipboardOld() {
		int start = -1;
		ArrayList<String> names = new ArrayList<String>();

		while(start==-1) {
			names.clear();
			String data = getClipboardData();
			String lines[] = data.split("\\r?\\n");

			for(int i = 0; i<lines.length;i++) {
				if(start!=-1) {
					if(lines[i].equals("Posters")) break;
					int begin = "1	".length();
					if(isDigit(lines[i].charAt(1))) begin = "11	".length();
					int end = lines[i].length() - "	2009-10-02	".length();

					String newName = padNumber(i-start,2)+" "+lines[i].substring(begin, end);
					//if the new name has (#) at the end
					if(newName.contains("(")){
						int openingParenthesis = -1;
						int closingParenthesis = -1;
						for(int j = 0; j<newName.length();j++) {
							if(newName.charAt(j)=='(') {//start found at j
								openingParenthesis=j;
								for(int k = j+1; k<newName.length();k++) {
									if(!(isDigit(newName.charAt(k))||newName.charAt(k)==(')'))) {
										break;
									}
									else if(newName.charAt(k)==(')')) {//end found at j
										closingParenthesis=k;
										break;
									}
								}
							}
						}
						if(openingParenthesis!=-1&&closingParenthesis!=-1) {
							System.out.println("Found an opening parenthesis and a closing parenthesis.");
							System.out.println("Making the following change:");
							System.out.println("	"+newName);
							newName = newName.substring(0, openingParenthesis+1)+"Part "+newName.substring(openingParenthesis+1,newName.length());
							System.out.println("	--->"+newName);
						}
					}
					newName = newName.replaceAll("\\s+", " ").trim();
					newName = newName.replaceAll("/", "∕");
					if(useTitleCase)
						newName = toTitleCase(newName);
					names.add(newName);
					//System.out.println(names.get(names.size()-1));
				}

				if(lines[i].contains("Originally Aired")) start=i;
			}
			if(start==-1) {
				System.out.println("Please copy some relevant data to the clipboard.");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
		return names;
	}

	/**
	 * Convert a string to title case e.g. "The Quick Brown Fox." instead of "The quick brown fox."
	 * @param str - The string to be converted.
	 * @return - The input string converted to title case.
	 */
	public static String toTitleCase(String str) {

		String result = "";
		String[] words = str.split(" ");
		for(int i = 0; i<words.length; i++) {
			String space = " ";
			if(i==words.length-1)
				space = "";
			if(i>1&&isNotCapitalizedInTitle(words[i])) {
				result = result + words[i].toLowerCase();
			}
			else if(isCapitalizedInTitle(words[i])) {
				result = result+words[i].toUpperCase();
			}
			else {
				//capitalize the first letter of the word
				String newWord = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
				if(words[i].charAt(0)=='('||words[i].charAt(0)=='['||words[i].charAt(0)=='{') 
					newWord = words[i].substring(0, 2).toUpperCase() + words[i].substring(2).toLowerCase();

				//if the word is an acronym capitalize each word
				for(int j = 1; j < newWord.length(); j++) {
					if((newWord.charAt(j)!= '.' && newWord.charAt(j-1) == '.')||(newWord.charAt(j)!= '-' && newWord.charAt(j-1) == '-')) {
						String start = newWord.substring(0, j);
						String middle = newWord.substring(j, j+1).toUpperCase();
						String end = newWord.substring(j+1);
						//System.out.println("Start: " + start);
						//System.out.println("Middle: " + middle);
						//System.out.println("End: " + end);
						newWord = start + middle + end;

					}
				}

				result = result + newWord;

			}
			result = result+space;
		}
		//System.out.println("Conversion to title case renamed \""+str+"\" to \""+result+"\"");
		return result;
	}

	/**
	 * Determine whether or not a word should have zero letters capitalized when used in an episode title.
	 * 
	 * @param word - The word to check.
	 * @return - True if the word should not be capitalized at all, false if not.
	 */
	public static boolean isNotCapitalizedInTitle(String word) {
		for(String conj: doNotCapitalize) {
			if(word.equalsIgnoreCase(conj))
				return true;
		}
		return false;
	}

	/**
	 * Determine whether or not a word should always have all letters capitalized when used in an episode title.
	 * 
	 * @param word - The word to check.
	 * @return - True if the word should be capitalized, false if not.
	 */
	public static boolean isCapitalizedInTitle(String word) {
		for(String conj: doCapitalize) {
			if(word.equalsIgnoreCase(conj))
				return true;
		}
		return false;
	}
	/**
	 * Given the url of a website, get a string representation of the data returned by sending a post request to that url.
	 * @param urlString - The url of the website to download from.
	 * @return - A string containing the data that was returned by the website.
	 */
	public static String loadPage(String urlString) {
		URL url;
		URLConnection uc;
		StringBuilder parsedContentFromUrl = new StringBuilder();
		try {
			//System.out.println("Getting content for URl : " + urlString);
			url = new URL(urlString);
			uc = url.openConnection();
			uc.connect();
			uc = url.openConnection();
			uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			//uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
			uc.getInputStream();
			BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
			int ch;
			while ((ch = in.read()) != -1) {
				parsedContentFromUrl.append((char) ch);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return "";
		}
		//System.out.println("result: "+parsedContentFromUrl.toString());
		return parsedContentFromUrl.toString();
	}

	/**
	 * List the files in a directory.
	 * @param path - The directory to check.
	 * @return - An array of files that are contained in the directory.
	 */
	public static File[] listFiles(String path) {
		File folder;
		File[] listOfFiles = null;
		try {
			folder = new File(path);
			listOfFiles = folder.listFiles();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return listOfFiles;
	}

	/**
	 * Rename an entire series.
	 * @param path - The file path of the series.
	 */
	public static void renameSeries(String path) {
		//get a list of the files in the path
		File[] listOfFiles = listFiles(path);

		//get a hashmap of the video files in each subdirectory
		HashMap<String, ArrayList<String>> videoFilesInPath = new HashMap<String, ArrayList<String>>();

		//get a hashmap of the useless files in each subdirectory
		HashMap<String, ArrayList<String>> uselessFilesInPath = new HashMap<String, ArrayList<String>>();

		//get a hashmap of the subtitle files in each subdirectory
		HashMap<String, ArrayList<String>> subtitleFilesInPath = new HashMap<String, ArrayList<String>>();

		//ArrayList<String> oldVideoNames = new ArrayList<String>();

		//populate the hashmaps
		for(int i = 0; i<listOfFiles.length; i++) {
			if(listOfFiles[i].isDirectory()) {
				//add an arraylist to the hashmap with this folder as key
				videoFilesInPath.put(listOfFiles[i].getName(), new ArrayList<String>());
				uselessFilesInPath.put(listOfFiles[i].getName(), new ArrayList<String>());
				subtitleFilesInPath.put(listOfFiles[i].getName(), new ArrayList<String>());

				File[] filesInFolder = listFiles(listOfFiles[i].getAbsolutePath());
				//add every file (that isn't a folder) inside this folder to the hashmap entry
				for (int j = 0; j < filesInFolder.length; j++) {	
					if (filesInFolder[j].isFile()) {
						String fileExtension = getFileExtension(filesInFolder[j].getName());
						//populate the video files hashmap
						if(fileExtension.equalsIgnoreCase(".mp4")||fileExtension.equalsIgnoreCase(".avi")||fileExtension.equalsIgnoreCase(".webm")||fileExtension.equalsIgnoreCase(".mkv")) {
							videoFilesInPath.get(listOfFiles[i].getName()).add(filesInFolder[j].getName());
						}
						//populate the useless files hashmap
						if(fileExtension.equalsIgnoreCase(".txt")||fileExtension.equalsIgnoreCase(".nfo")||fileExtension.equalsIgnoreCase(".jpeg")) {
							uselessFilesInPath.get(listOfFiles[i].getName()).add(filesInFolder[j].getName());
						}
						//populate the subtitle files hashmap
						if(fileExtension.equalsIgnoreCase(".srt")||fileExtension.equalsIgnoreCase(".sub")||fileExtension.equalsIgnoreCase(".vob")||fileExtension.equalsIgnoreCase(".idx")) {
							subtitleFilesInPath.get(listOfFiles[i].getName()).add(filesInFolder[j].getName());
						}
					}
				}
			}
		}

		//determine the name of the series
		String seriesName = HTTP.getSeriesNameFromPath(path+"/asdf");//predictSeriesName(path+"/asdf");

		//get the token needed to send requests
		String token = HTTP.getToken();

		//search theTVDB for the series to get a list of episode names
		HashMap<String, ArrayList<String>> episodeNameResults = HTTP.getEpisodeNamesForSeries(seriesName, token);

		//create a hashmap to store search results for episode names
		HashMap<String, ArrayList<String>> episodeNames = new HashMap<String, ArrayList<String>>();

		//for each season
		for(String season: videoFilesInPath.keySet()) {
			System.out.println("\n" + ANSI_GREEN + "Processing " + season + ANSI_RESET);
			Collections.sort(videoFilesInPath.get(season));
			Collections.sort(subtitleFilesInPath.get(season));
			ArrayList<String> videoNames = videoFilesInPath.get(season);
			ArrayList<String> subtitleNames = subtitleFilesInPath.get(season);

			//load the correct episode names into an arraylist
			ArrayList<String> episodeNamesForSeason = HTTP.getEpisodeNamesForSeason(path+File.separator+season, seriesName, videoNames, episodeNameResults);
			
			episodeNames.put(season, createEpisodeNamesFromSearch(episodeNamesForSeason));

			//make sure there are the same number of video files in the season folder as there are in the search results for the season
			if(videoNames.size()!=episodeNames.get(season).size()) {
				System.out.println("Number of video files in directory("+videoNames.size()+") does not equal expected episode count("+episodeNames.get(season).size()+")");
				//if there are more episode names than media files to assign them to
				if(episodeNames.get(season).size()>videoNames.size()) {
					System.out.println("Checking if any of the files contain multiple episodes...");
					int patternsFound = 0;//the number of files which actually contained 2 episodes
					//For each video file
					for(int i = 0; i<videoNames.size();i++) {

						//if the file contains two episodes
						if(fileContainsTwoEpisodes(videoNames.get(i))) {
							patternsFound++;
							//combine the corresponding episode name with the extra one after it
							String extraEpisodeName = episodeNames.get(season).remove(i+1);//remove the extra
							String firstEpisodeNumber = episodeNames.get(season).get(i).split(" ")[0];
							String secondEpisodeNumber = extraEpisodeName.split(" ")[0];
							String firstEpisodeName = episodeNames.get(season).get(i).substring((int)(firstEpisodeNumber.length()), (int)(episodeNames.get(season).get(i).length()));
							String secondEpisodeName = extraEpisodeName.substring(firstEpisodeNumber.length(), extraEpisodeName.length());

							if(firstEpisodeName.contains("(Part")&&secondEpisodeName.contains("(Part")) {
								//find the index of the opening parenthesis in the first episode name
								int firstParenthesisIndex;
								for(firstParenthesisIndex = firstEpisodeName.length()-1;firstParenthesisIndex>0&&firstEpisodeName.charAt(firstParenthesisIndex)!='(';firstParenthesisIndex--);

								//find the index of the opening parenthesis in the second episode name
								int secondParenthesisIndex;
								for(secondParenthesisIndex = secondEpisodeName.length()-1;secondParenthesisIndex>0&&secondEpisodeName.charAt(secondParenthesisIndex)!='(';secondParenthesisIndex--);

								//modify the first episode name to include the second
								String newName = firstEpisodeNumber+"-"+secondEpisodeNumber+" "+firstEpisodeName.substring(0, firstParenthesisIndex-1);
								episodeNames.get(season).set(i, newName.replaceAll("\\s+", " ").trim());
							}
							else {
								//modify the first episode name to include the second
								String newName = firstEpisodeNumber+"-"+secondEpisodeNumber+" "+firstEpisodeName+" _ "+secondEpisodeName;
								episodeNames.get(season).set(i, newName.replaceAll("\\s+", " ").trim());
							}
						}
					}
					if(patternsFound>0) {
						System.out.println("Found "+patternsFound+" files that contained 2 episodes.");
						System.out.println("The extra episode names were merged into a single string for each file.");
						//make sure that the changes were enough to solve the issue
						if(videoNames.size()!=episodeNames.get(season).size()) {
							System.out.println("It appears that these changes did not resolve the issue.");
							System.out.println("Number of video files in directory("+videoNames.size()+") does not equal expected episode count("+episodeNames.get(season).size()+")");
							System.out.println("\nThe program cannot continue.");
							System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
							return;
						}
					}
					else {
						System.out.println("Unable to find the pattern: \"SxxExxExx\" (where x is a digit).");
						System.out.println("There appears to be a file missing.");
						System.out.println("\nThe program cannot continue.");
						System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
						return;
					}
				}
				else {//There are less episode names than media files
					System.out.println("There appears to be more video files than available episode names.");
					System.out.println("\nThe program cannot continue.");
					System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
					return;
				}

			}
			//make sure that if there are subtitles, there is one for every episode
			if(subtitleNames.size()>0&&subtitleNames.size()%videoNames.size()!=0) {
				System.out.println("Number of subtitle files in directory("+subtitleNames.size()+") is not a multiple of episode count("+videoNames.size()+")");
				System.out.println("Unable to match them to their associated episodes.");
				System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
				return;
			}
		}//end of season loop

		//print changes
		System.out.println("The following changes are about to be made:\n");

		System.out.println("---------------moved--------------");
		for(String season: videoFilesInPath.keySet()) {
			ArrayList<String> videoNames = videoFilesInPath.get(season);
			for(int i = 0; i<episodeNames.get(season).size();i++) {
				System.out.println("\n"+videoFilesInPath.get(season).get(i)+"\n-->  "+ANSI_GREEN+episodeNames.get(season).get(i)+getFileExtension(videoNames.get(i))+ANSI_RESET);
				if(subtitleFilesInPath.get(season).size()>0) 
					System.out.println("     Subtitles will also be moved.");
			}
		}

		System.out.println("\n---------------deleted--------------");
		for(String season: videoFilesInPath.keySet()) {
			for(int i = 0; i<uselessFilesInPath.get(season).size();i++) {
				System.out.println("\n"+uselessFilesInPath.get(season).get(i)+"\n-->  "+ANSI_RED+"DELETED"+ANSI_RESET);
			}
		}

		//scan for input
		System.out.println("\nAre these changes acceptable? (y/n)");
		Scanner in = new Scanner(System.in);
		String input = in.nextLine();

		//exit if invalid input is detected
		if(input.length()>3) {
			System.out.println("Invalid Input");
			System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
			return;
		}
		//exit if the answer is not "y" or "yes"
		if(!input.equalsIgnoreCase("y")&&!input.equalsIgnoreCase("yes")){
			System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
			return;
		}







		startTime = System.currentTimeMillis();
		//move and rename files
		for(String season: videoFilesInPath.keySet()) {
			ArrayList<String> videoNames = videoFilesInPath.get(season);
			for(int i = 0; i<episodeNames.get(season).size();i++) {
				String dir = path+File.separator+season+File.separator+episodeNames.get(season).get(i);
				//create a folder for this episode if one doesn't exist yet
				new File(dir).mkdirs();

				//add file extension to the new file name
				String newName = episodeNames.get(season).get(i)+getFileExtension(videoNames.get(i));

				//path to the new file
				File currentFile = new File(path+File.separator+season+File.separator+videoNames.get(i));
				File newFile = new File(dir+File.separator+newName);

				boolean moved = currentFile.renameTo(newFile);
				if(moved==false) {
					if(!currentFile.exists())
						System.out.println(ANSI_RED+"Failed to move file because "+ANSI_RESET+path+File.separator+season+File.separator+videoNames.get(i) +ANSI_RED+" does not exist!"+ANSI_RESET);
					else {
						System.out.println(ANSI_RED+"Failed to move file. IDK why :("+ANSI_RESET);
						System.out.println("Old file: "+path+File.separator+videoNames.get(i));
						System.out.println("New file: "+dir+File.separator+newName);
					}
				}

				//if this file has subtitles
				if(subtitleFilesInPath.get(season).size()>0) {
					int subtitleFilesPerEpisode = subtitleFilesInPath.get(season).size()/episodeNames.get(season).size();
					//for each subtitle corresponding to the episode
					for(int j = 0; j<subtitleFilesPerEpisode; j++) {
						int subsIndex = (i*subtitleFilesPerEpisode)+j;
						//get the file extension
						String subtitleFileExtension = getFileExtension(subtitleFilesInPath.get(season).get(subsIndex));
						//add file extension to the new file name
						String newSubtitleName = episodeNames.get(season).get(i)+subtitleFileExtension;

						//path to the new file
						File currentSubtitleFile = new File(path+File.separator+subtitleFilesInPath.get(season).get(subsIndex));
						File newSubtitleFile = new File(dir+File.separator+newSubtitleName);

						boolean movedSub = currentSubtitleFile.renameTo(newSubtitleFile);
						if(movedSub==false) {
							System.out.println(ANSI_RED+"Failed to move subtitle file. IDK why :(");
							System.out.println("Attempted move was: ");
							System.out.println(currentSubtitleFile.getAbsolutePath());
							System.out.println("\t---> "+newSubtitleFile.getAbsolutePath());
							System.out.println(ANSI_RESET+"\n");
						}
					}
				}	
			}
		}
		//delete useless files
		for(String season: videoFilesInPath.keySet()) {
			for(int i = 0; i<uselessFilesInPath.get(season).size();i++) {
				File useless = new File(path+File.separator+season+File.separator+uselessFilesInPath.get(season).get(i));
				useless.delete();
			}
		}
		System.out.println(ANSI_GREEN+"Finished! (Elapsed time: "+(System.currentTimeMillis()-startTime)+"ms)"+ANSI_RESET);
	}

	/**
	 * The main meat of this program. This method finds the files within a season, checks the tvDB api to determine the real names of the episodes,
	 * renames the files to match their real episode names, puts them into folders with identical names to their associated episodes, and removes 
	 * any unneeded files from the directory.
	 * 
	 * @param path - A string representing the file path of a season which needs to have it's files reorganized and renamed.
	 */
	public static void renameFilesOld(String path) {
		File folder;
		File[] listOfFiles = null;
		try {
			folder = new File(path);
			listOfFiles = folder.listFiles();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		ArrayList<String> fileNames = new ArrayList<String>();
		//navigate directory to get a list of file names
		for (int i = 0; i < listOfFiles.length; i++) {		
			if (listOfFiles[i].isFile()) {
				fileNames.add(listOfFiles[i].getName());
			}
		}
		System.out.println("Found "+listOfFiles.length+" files.");
		ArrayList<String> videoNames = getVideoFiles(fileNames);
		System.out.println("Found "+videoNames.size()+" video files.");
		ArrayList<String> oldVideoNames = new ArrayList<String>(videoNames);
		ArrayList<String> uselessNames = getUselessFiles(fileNames); 
		ArrayList<String> subtitleNames = getSubtitleFiles(fileNames);
		if(videoNames.size()==0) {
			System.out.println(ANSI_RED+"No files found in directory."+ANSI_RESET);
			return;
		}
		for(int j = 0; j<videoNames.size();j++) {
			String replacement = videoNames.get(j).toLowerCase();
			videoNames.set(j, replacement);
		}
		Collections.sort(videoNames);

		//determine the series name
		String seriesName = HTTP.getSeriesNameFromPath(path);//predictSeriesName(path);

		//get the token needed to send requests
		String token = HTTP.getToken();

		//determine names of new files
		HashMap<String, ArrayList<String>> episodeNameResults = HTTP.getEpisodeNamesForSeries(seriesName, token);
		System.out.println("Performing search.");
		ArrayList<String> episodeNames = createEpisodeNamesFromSearch(HTTP.getEpisodeNamesForSeason(path, seriesName, videoNames, episodeNameResults));//getEpisodeNamesFromClipboard();
		

		System.out.println("Finished search.");
		//make sure there are the correct number of files in the directory
		if(videoNames.size()!=episodeNames.size()) {
			System.out.println("Number of video files in directory("+videoNames.size()+") does not equal expected episode count("+episodeNames.size()+")");
			//if there are more episode names than media files to assign them to
			if(episodeNames.size()>videoNames.size()) {
				System.out.println("Checking if any of the files contain multiple episodes...");
				int patternsFound = 0;//the number of files which actually contained 2 episodes
				//For each video file
				for(int i = 0; i<videoNames.size();i++) {

					//if the file contains two episodes
					if(fileContainsTwoEpisodes(videoNames.get(i))) {
						patternsFound++;
						//combine the corresponding episode name with the extra one after it
						String extraEpisodeName = episodeNames.remove(i+1);//remove the extra
						String firstEpisodeNumber = episodeNames.get(i).split(" ")[0];
						String secondEpisodeNumber = extraEpisodeName.split(" ")[0];
						String firstEpisodeName = episodeNames.get(i).substring(firstEpisodeNumber.length(), episodeNames.get(i).length());
						String secondEpisodeName = extraEpisodeName.substring(firstEpisodeNumber.length(), extraEpisodeName.length());

						if(firstEpisodeName.contains("(Part")&&secondEpisodeName.contains("(Part")) {
							//find the index of the opening parenthesis in the first episode name
							int firstParenthesisIndex;
							for(firstParenthesisIndex = firstEpisodeName.length()-1;firstParenthesisIndex>0&&firstEpisodeName.charAt(firstParenthesisIndex)!='(';firstParenthesisIndex--);

							//find the index of the opening parenthesis in the second episode name
							int secondParenthesisIndex;
							for(secondParenthesisIndex = secondEpisodeName.length()-1;secondParenthesisIndex>0&&secondEpisodeName.charAt(secondParenthesisIndex)!='(';secondParenthesisIndex--);

							//modify the first episode name to include the second
							String newName = firstEpisodeNumber+"-"+secondEpisodeNumber+" "+firstEpisodeName.substring(0, firstParenthesisIndex-1);
							episodeNames.set(i, newName.replaceAll("\\s+", " ").trim());
						}
						else {
							//modify the first episode name to include the second
							String newName = firstEpisodeNumber+"-"+secondEpisodeNumber+" "+firstEpisodeName+" _ "+secondEpisodeName;
							episodeNames.set(i, newName.replaceAll("\\s+", " ").trim());
						}
					}
				}
				if(patternsFound>0) {
					System.out.println("Found "+patternsFound+" files that contained 2 episodes.");
					System.out.println("The extra episode names were merged into a single string for each file.");
					//make sure that the changes were enough to solve the issue
					if(videoNames.size()!=episodeNames.size()) {
						System.out.println("It appears that these changes did not resolve the issue.");
						System.out.println("Number of video files in directory("+videoNames.size()+") does not equal expected episode count("+episodeNames.size()+")");
						System.out.println("\nThe program cannot continue.");
						System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
						return;
					}
				}
				else {
					System.out.println("Unable to find the pattern: \"SxxExxExx\" (where x is a digit).");
					System.out.println("There appears to be a file missing.");
					System.out.println("\nThe program cannot continue.");
					System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
					return;
				}
			}
			else {//There are less episode names than media files
				System.out.println("There appears to be more video files than available episode names.");
				System.out.println("\nThe program cannot continue.");
				System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
				return;
			}

		}
		//make sure that if there are subtitles, there is one for every episode
		if(subtitleNames.size()>0&&subtitleNames.size()%videoNames.size()!=0) {
			System.out.println("Number of subtitle files in directory("+subtitleNames.size()+") is not a multiple of episode count("+videoNames.size()+")");
			System.out.println("Unable to match them to their associated episodes.");
			System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
			return;
		}

		//print changes
		System.out.println("The following changes are about to be made:\n");

		System.out.println("---------------moved--------------");
		for(int i = 0; i<episodeNames.size();i++) {
			//find old video names that match the order of videoNames
			int oldIndex = 0;
			for(oldIndex = 0; oldIndex<oldVideoNames.size();oldIndex++) {
				if(oldVideoNames.get(oldIndex).equalsIgnoreCase(videoNames.get(i)))
					break;
			}
			System.out.println("\n"+oldVideoNames.get(oldIndex)+"\n-->  "+ANSI_GREEN+episodeNames.get(i)+getFileExtension(videoNames.get(i))+ANSI_RESET);
			if(subtitleNames.size()>0) 
				System.out.println("     Subtitles will also be moved.");
		}

		System.out.println("\n---------------deleted--------------");
		for(int i = 0; i<uselessNames.size();i++) {
			System.out.println("\n"+uselessNames.get(i)+"\n-->  "+ANSI_RED+"DELETED"+ANSI_RESET);
		}

		//scan for input
		System.out.println("\nAre these changes acceptable? (y/n)");
		Scanner in = new Scanner(System.in);
		String input = in.nextLine();

		//exit if invalid input is detected
		if(input.length()>3) {
			System.out.println("Invalid Input");
			System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
			return;
		}
		//exit if the answer is not "y" or "yes"
		if(!input.equalsIgnoreCase("y")&&!input.equalsIgnoreCase("yes")){
			System.out.println(ANSI_RED+"Exiting"+ANSI_RESET);
			return;
		}
		startTime = System.currentTimeMillis();
		//move and rename files
		for(int i = 0; i<episodeNames.size();i++) {
			String dir = path+File.separator+File.separator+episodeNames.get(i);
			//create a folder for this episode if one doesn't exist yet
			new File(dir).mkdirs();

			//add file extension to the new file name
			String newName = episodeNames.get(i)+getFileExtension(videoNames.get(i));

			//get old file name
			int oldIndex = 0;
			for(oldIndex = 0; oldIndex<oldVideoNames.size();oldIndex++) {
				if(oldVideoNames.get(oldIndex).equalsIgnoreCase(videoNames.get(i)))
					break;
			}
			//path to the new file
			File currentFile = new File(path+File.separator+oldVideoNames.get(oldIndex));
			File newFile = new File(dir+File.separator+newName);

			boolean moved = currentFile.renameTo(newFile);
			if(moved==false) {
				if(!currentFile.exists())
					System.out.println(ANSI_RED+"Failed to move file because "+ANSI_RESET+path+File.separator+oldVideoNames.get(oldIndex) +ANSI_RED+" does not exist!"+ANSI_RESET);
				else {
					System.out.println(ANSI_RED+"Failed to move file. IDK why :("+ANSI_RESET);
					System.out.println("Old file: "+path+File.separator+oldVideoNames.get(oldIndex));
					System.out.println("New file: "+dir+File.separator+newName);
				}
			}

			//if this file has subtitles
			if(subtitleNames.size()>0) {
				int subtitleFilesPerEpisode = subtitleNames.size()/episodeNames.size();
				//for each subtitle corresponding to the episode
				for(int j = 0; j<subtitleFilesPerEpisode; j++) {
					int subsIndex = (i*subtitleFilesPerEpisode)+j;
					//get the file extension
					String subtitleFileExtension = getFileExtension(subtitleNames.get(subsIndex));
					//add file extension to the new file name
					String newSubtitleName = episodeNames.get(i)+subtitleFileExtension;

					//path to the new file
					File currentSubtitleFile = new File(path+File.separator+subtitleNames.get(subsIndex));
					File newSubtitleFile = new File(dir+File.separator+newSubtitleName);

					boolean movedSub = currentSubtitleFile.renameTo(newSubtitleFile);
					if(movedSub==false) { 
						System.out.println(ANSI_RED+"Failed to move subtitle file. IDK why :("+ANSI_RESET);
					}
				}
			}	
		}
		for(int i = 0; i<uselessNames.size();i++) {
			File useless = new File(path+File.separator+uselessNames.get(i));
			useless.delete();
		}
		System.out.println(ANSI_GREEN+"Finished! (Elapsed time: "+(System.currentTimeMillis()-startTime)+"ms)"+ANSI_RESET);

	}

	/**
	 * Given a string which represents a video file, determine whether it represents two seperate episodes which are stored within the same file.
	 * @param name - The file name to check.
	 * @return - True if the file most likely represents two episodes, false if not.
	 */
	public static boolean fileContainsTwoEpisodes(String name) {
		//For each char in the file name
		for(int j = 0; j<name.length()-8;j++) {
			//determine if the pattern matches
			if((name.charAt(j)=='S'||name.charAt(j)=='s')) {
				if(isDigit(name.charAt(j+1))) {
					if(isDigit(name.charAt(j+2))) {
						if((name.charAt(j+3)=='E'||name.charAt(j+3)=='e')) {
							if(isDigit(name.charAt(j+4))) {
								if(isDigit(name.charAt(j+5))) {
									if((name.charAt(j+6)=='E'||name.charAt(j+6)=='e')) {
										if(isDigit(name.charAt(j+7))) {
											if(isDigit(name.charAt(j+8))) {
												return true;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println();
		return false;
	}

	/**
	 * Given a list of file names, determine which of them represent video files.
	 * @param fileNames - The original list of names to be checked.
	 * @return - The file names which from the fileNames list which represent video files.
	 */
	public static ArrayList<String> getVideoFiles(ArrayList<String> fileNames){
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i<fileNames.size();i++) {
			String fileExtension=getFileExtension(fileNames.get(i));
			if(fileExtension.equalsIgnoreCase(".mp4")||fileExtension.equalsIgnoreCase(".avi")||fileExtension.equalsIgnoreCase(".webm")||fileExtension.equalsIgnoreCase(".mkv")) {
				result.add(fileNames.get(i));
			}
		}
		return result;
	}

	/**
	 * Given a list of file names, determine which of them represent non-media files that are also not subtitle files.
	 * 
	 * @param fileNames - The original list of file names to be checked.
	 * @return - An arraylist of file names which represent files that aren't media files or subtitle files.
	 */
	public static ArrayList<String> getUselessFiles(ArrayList<String> fileNames){
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i<fileNames.size();i++) {
			String fileExtension=getFileExtension(fileNames.get(i));
			if(fileExtension.equalsIgnoreCase(".txt")||fileExtension.equalsIgnoreCase(".nfo")||fileExtension.equalsIgnoreCase(".jpeg")) {
				result.add(fileNames.get(i));
			}
		}
		return result;
	}

	/**
	 * Given a list of file names, determine which of them represent subtitle files.
	 * 
	 * @param fileNames - The original list of file names to be checked.
	 * @return - An arraylist of file names which represented subtitle file types.
	 */
	public static ArrayList<String> getSubtitleFiles(ArrayList<String> fileNames){
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i<fileNames.size();i++) {
			String fileExtension=getFileExtension(fileNames.get(i));
			//.sub and .vob always come in pairs
			if(fileExtension.equalsIgnoreCase(".srt")||fileExtension.equalsIgnoreCase(".sub")||fileExtension.equalsIgnoreCase(".vob")||fileExtension.equalsIgnoreCase(".idx")) {
				result.add(fileNames.get(i));
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Get the file extension of a string which represents a file name.
	 * 
	 * @param fileName - The string to check.
	 * @return - A substring containing every character after and including the last '.' in the file name.
	 */
	public static String getFileExtension(String fileName) {
		for(int i = fileName.length()-1; i>=0;i--) {
			if(fileName.charAt(i)=='.') {
				return fileName.substring(i);
			}
		}
		return null;
	}

	/**
	 * Get a list of all the substrings which are shared between two strings.
	 * 
	 * @param s1 - The first string to compare.
	 * @param s2 - The second string to compare.
	 * @return - An arraylist of substrings which are shared between s1 and s2.
	 */
	public static ArrayList<String> sharedSubStrings(String s1, String s2){
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i<s1.length();i++) {
			for(int j = 0; j<s2.length();j++) {

				int length = 0;
				for(int k=0; i+k<s1.length()-2&&j+k<s2.length()&&s1.charAt(i+k)==s2.charAt(j+k); k++) {
					length++;

				}
				if(length>=4) {
					result.add(s1.substring(i, i+length));
					i+=length;
					j+=length;

				}

			}
		}
		return result;
	}

	/**
	 * Gets a substring of the characters in between two known strings within the source string.
	 * 
	 * @param full - The original string.
	 * @param start - The substring that comes before the substring we want to retrieve.
	 * @param end - The substring that comes after the substring we want to retrieve.
	 * @return - The substring found between the start and end that were specified.
	 */
	public static String getSubstring(String full, String start, String end) {
		String result = "";
		int s=-1;//index of start
		int e=-1;//index of end
		for(int i = 0; i<full.length();i++) {
			//if the start hasn't been found yet
			if(s==-1) {
				for(int j = 0; j<start.length()&&full.charAt(i+j)==start.charAt(j);j++) {
					//if(full.charAt(i+j)==start.charAt(j)) {
					if(j==start.length()-1) {
						s = i+start.length();
						i+=start.length();
						break;
					}
					//}

				}
			}
			else if(e==-1){
				for(int j = 0; j<end.length()&&full.charAt(i+j)==end.charAt(j);j++) {
					//if(full.charAt(i+j)==end.charAt(j)) {
					if(j==end.length()-1) {
						e = i;
						break;
					}
					//}
				}
			}
			else {
				break;
			}
		}
		if(s!=-1&&e!=-1) return full.substring(s, e);
		return result;
	}
	/**
	 * Get data from the clipboard.
	 * 
	 * @return - A string containing the clipboard data.
	 */
	public static String getClipboardData() {
		String data;
		try {
			data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			return data;
		} catch (HeadlessException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
		return null;
	}

	/**
	 * Used to determine if the given character is a digit or not.
	 * 
	 * @param a - The char to check.
	 * @return - True if a represents a digit, false if not.
	 */
	public static boolean isDigit(char a) {
		return Character.isDigit(a);
	}

	/**
	 * Add zeroes in front of a number within a string to make it reach the desired width.
	 * 
	 * @param num - The original number.
	 * @param desiredWidth - The desired number of digits in the resulting number.
	 * @return A string representing the original number with some zeroes in front of it for padding.
	 */
	public static String padNumber(int num, int desiredWidth) {
		String result = num+"";
		while(result.length()<desiredWidth) result = "0"+result;
		return result;
	}

}
