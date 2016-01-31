/**
 *  Author: 	Chengjun Yuan  
 *				<cy3yb@virginia.edu>
 *	Time:		Jan. 2016 
 *	Purpose:	extract words from readme files and code files of a number of java projects.
 *	HowToRun:	compile:	javac -cp '.:libstemmer.jar' PreprocessForWord2Vec.java
				run:		java -cp '.:libstemmer.jar' PreprocessForWord2Vec projectsFolder
 */
 
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.*;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class PreprocessForWord2Vec {
	int numLoadedFiles = 0;
	int maxNumLoadedFiles = 2000;
	StringBuilder codeSb, readMeSb, commitSb;
	SnowballStemmer stemmer;
	HashMap<String, Integer> wordsMap;
	HashSet<String> stopWords;
 
	public PreprocessForWord2Vec(){
		
		/*try{
			reviewsWriter = new PrintWriter(new BufferedWriter(new FileWriter(reviewsFile, true)));
		}catch (IOException e) {
			e.printStackTrace();
		}*/
		
		codeSb = new StringBuilder();
		readMeSb = new StringBuilder();
		commitSb = new StringBuilder();
		stemmer = new englishStemmer();
		wordsMap = new HashMap<String, Integer>();
		stopWords = new HashSet<String>();
		 
	}
	
	public void dumpGitLog(String topFolder){
		File dir = new File(topFolder);
		File[] allFiles = dir.listFiles();
		ArrayList<File> allDirs = new ArrayList<File>();
		for (File file : allFiles) {
			if (file.isDirectory()) {
				allDirs.add(file.getAbsoluteFile());
			}
		}
		int i = 0;
		for (File folder : allDirs) {
			try {
				i++;
				System.out.println(String.valueOf(i) + folder.getAbsolutePath());
				String[] cmd = new String[]{"/bin/bash", "-c", "git log --date=iso --no-merges > change_log.txt"};
				ProcessBuilder pb = new ProcessBuilder().command(cmd).directory(folder);
				Process p = pb.start();
				int exit = p.waitFor();
				if(exit != 0){
					System.out.println("Not normal process **********");
					//System.exit(exit);
				} 
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String LoadReadMeFile(String fileName) {
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String str;
			while ((str = in.readLine()) != null){
				str = TokenizerNormalizationStemming(str);
				sb.append(str);
			}
			in.close();
			return sb.toString();
		}catch(FileNotFoundException ex) {
            ex.printStackTrace();
			return null;
        }catch(IOException e) {
        	e.printStackTrace();
			return null;
        }
	}
	
	public String parseConnectedWords(String text) {
		StringBuilder sb = new StringBuilder();
		int start = 0; int i = 0;
		for(i = 0; i < text.length(); i++) {
			if(Character.isUpperCase(text.charAt(i)) && i != 0) {
				String token = text.substring(start, i).toLowerCase();
				stemmer.setCurrent(token);
				if(stemmer.stem()) token = stemmer.getCurrent();
				sb.append(token); sb.append(" ");
				start = i;
			}
		}
		String token = text.substring(start, i).toLowerCase();
		stemmer.setCurrent(token);
		if(stemmer.stem()) token = stemmer.getCurrent();
		sb.append(token); sb.append(" ");
		//System.out.println(text + " PCW " + sb.toString());
		return sb.toString();
	}
	
	public String parseImportedPackages(String text) {
		String[] tokens = text.split("[^a-zA-Z']+");
		StringBuilder sb = new StringBuilder();
		for(String token : tokens) {
			if(token.equals(token.toLowerCase()) || token.equals(token.toUpperCase())) {
				stemmer.setCurrent(token.toLowerCase());
				if(stemmer.stem()) token = stemmer.getCurrent();
				sb.append(token);
				sb.append(" ");
			} else {
				sb.append(parseConnectedWords(token)); 
			}
		}
		return sb.toString();
	}
	
	public String parseClassMethods(String text) {
		String[] tokens = text.split("[^a-zA-Z']+");
		StringBuilder sb = new StringBuilder();
		for(String token : tokens) {
			if(token.equals(token.toLowerCase()) || token.equals(token.toUpperCase())) {
				stemmer.setCurrent(token.toLowerCase());
				if(stemmer.stem()) token = stemmer.getCurrent();
				sb.append(token);
				sb.append(" ");
			} else {
				sb.append(parseConnectedWords(token)); 
			}
		}
		return sb.toString();
	}
	
	public String loadCodeFile(String fileName) {
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String str = in.readLine();
			
			while(str != null) {
				str = str.trim();  // remove the leading and trailing spaces.
				if(str.startsWith("import")) {
					sb.append(parseImportedPackages(str));
				}else if(str.startsWith("public") || str.startsWith("private") || str.startsWith("class") || str.startsWith("void")) {
					sb.append(parseClassMethods(str));
				} else ;				
				str = in.readLine();
			}
			in.close();
			return sb.toString();
		}catch(FileNotFoundException ex) {
            ex.printStackTrace();
			return null;
        }catch(IOException e) {
        	e.printStackTrace();
			return null;
        }
	}
	
	/* read commit message from commit file - change_log.txt */
	public String loadCommitFile(String fileName){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            StringBuilder sb = new StringBuilder(); String line;  String author; String date; String emailAddress; String message; 
            String modifiedFileName; String commitID; String indexID; String patchID;
            line=reader.readLine();
            while(line!=null){
                if(line.startsWith("commit")){
                    commitID=new String(line.substring(7));
                    line = reader.readLine();
                    author = new String(line.substring(line.indexOf(" ")+1,line.lastIndexOf(" ")));
					emailAddress = new String(line.substring(line.lastIndexOf(" ")+1));
                    line = reader.readLine();
                    date = new String(line.substring(8));
					//sb.setLength(0);
                    while((line = reader.readLine()) != null && (!line.startsWith("commit"))){
                        sb.append(line); sb.append(" ");
                    }
                }
				else line=reader.readLine();
            }
            reader.close();
			message = TokenizerNormalizationStemming(sb.toString());
			return message;
        }catch(IOException e){
            System.err.format("[Error]Failed to open file %s ", fileName);
            e.printStackTrace();
			return null;
        }
    }
	
	// recursively load files in a directory 
	public void LoadFilesFromFolder(String folder, String prefix, String suffix) {
		//File dir = new File(folder); 
		Queue<File> queue = new LinkedList<File>();
		queue.add(new File(folder));
		while(!queue.isEmpty()) {
			File dir = queue.poll();
			for(File f : dir.listFiles()){
				if(f.isFile()) {
					if(f.getName().toLowerCase().startsWith(prefix)) {
						//System.out.println(numLoadedFiles + " load README file"+" : " + f.getName());
						readMeSb.append(LoadReadMeFile(f.getAbsolutePath()));
						numLoadedFiles ++;
					} else if(f.getName().endsWith(suffix)) {
						//System.out.println(numLoadedFiles + " load code file"+" : "+f.getName());
						codeSb.append(loadCodeFile(f.getAbsolutePath()));
						numLoadedFiles ++;
					} else if(f.getName().toLowerCase().startsWith("change_log")) {
						commitSb.append(loadCommitFile(f.getAbsolutePath()));
						numLoadedFiles ++;
					}
				}
				else if(f.isDirectory())
					queue.add(f);
					//LoadFilesFromFolder(f.getAbsolutePath(), prefix, suffix);
			}
		}
		//reviewsWriter.close();
	}
	
	// save String data into file under folder 
	public void saveStringIntoFileUnderFolder(String folder, String fileName, String str) {
		try {
            File dir = new File(folder); 
			File file = new File(dir, fileName);
            FileOutputStream is = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(is);    
            Writer w = new BufferedWriter(osw);
            w.write(str);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
	}
	
	// parse each project folder and save the preprocessed data in their own folder.
	public void parseProjects(String folder) {
		File dir = new File(folder); 
		int i = 1;
		for(File f:dir.listFiles()){ 
			/* detect project folder */
			if(f.isDirectory()) {
				System.out.println("project #"+ String.valueOf(i) + f.getAbsolutePath());
				readMeSb.setLength(0);
				codeSb.setLength(0);
				commitSb.setLength(0);
				LoadFilesFromFolder(f.getAbsolutePath(), "readme", "java");
				saveStringIntoFileUnderFolder(f.getAbsolutePath(), "re.prepro", readMeSb.toString());
				saveStringIntoFileUnderFolder(f.getAbsolutePath(), "code.prepro", codeSb.toString());
				saveStringIntoFileUnderFolder(f.getAbsolutePath(), "commit.prepro", commitSb.toString());
			}
			i ++;
			//if(i >= 2) break;
		}
	}
	
	public String TokenizerNormalizationStemming(String text){
		if(text == null || text.length() == 0)return "";
		StringBuilder sb = new StringBuilder();
		String[] tokens = text.split("[^a-zA-Z']+");
		for(String token : tokens) {
			//token=token.replaceAll("\\W+", "");
			//token = token.replaceAll("\\p{Punct}+","");
			token = token.toLowerCase();
			//if(token.matches(".*\\d+.*")) token = "";
			if(!token.isEmpty()){
				stemmer.setCurrent(token);
				if(stemmer.stem()) token = stemmer.getCurrent();
				sb.append(token);
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public void wordsStatics(String folder) {
		try {
			String[] preproFiles = {"re.prepro", "code.prepro", "commit.prepro"};
			System.out.println("start words statics...");
			File dir = new File(folder); 
			for(File f:dir.listFiles()){ 
				/* detect project folder */
				if(f.isDirectory()) {
					for(String preproFile : preproFiles) {
						if(new File(f, preproFile).exists()) {
							String line = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath(), preproFile)));
							String[] tokens = line.split(" ");
							for(String token : tokens) {
								if(token != null && token.length() > 0) {
									if(wordsMap.containsKey(token)) wordsMap.put(token, wordsMap.get(token) + 1);
									else wordsMap.put(token, 1);
								}
							}
						} else System.out.println(f.getAbsolutePath() + "  " + preproFile);
					}
				}
			}
			System.out.println("Finish words statics !! ");
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public void generateStopWords(String stopWordsFileName, int offset) {
		Map<String,Integer> sortedWordsMap = sortByComparator(wordsMap); // sort the wordsMap based on value
		try {
			/* write wordsStatics into file */
			PrintWriter writer = new PrintWriter("wordsStatics.txt", "UTF-8");
			for(String keyToken : sortedWordsMap.keySet()) {
				writer.println(keyToken + "," + String.valueOf(sortedWordsMap.get(keyToken)));
				if(sortedWordsMap.get(keyToken) > offset) stopWords.add(keyToken);
			}
			writer.close();
			/* load old stopWords file */
			List<String> words = Files.readAllLines(Paths.get(stopWordsFileName), Charset.defaultCharset());
			for(String word : words) {
				if(!word.isEmpty()) {
					stemmer.setCurrent(word.toLowerCase());
					if(stemmer.stem()) word = stemmer.getCurrent();
					if(!stopWords.contains(word)) stopWords.add(word);
				}
			}
        }catch(FileNotFoundException ex){
            ex.printStackTrace();
        }catch(IOException e){
        	e.printStackTrace();
        }
	}
	
	public void removeStopWords(String folder) {
		try {
			String[] preproFiles = {"re.prepro", "code.prepro", "commit.prepro"};
			System.out.println("start remove stopWords...");
			File dir = new File(folder); 
			StringBuilder sb = new StringBuilder();
			for(File f:dir.listFiles()){ 
				/* detect project folder */
				if(f.isDirectory()) {
					for(String preproFile : preproFiles) {
						if(new File(f, preproFile).exists()) {
							sb.setLength(0);
							String line = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath(), preproFile)));
							String[] tokens = line.split(" ");
							for(String token : tokens) {
								if(!token.isEmpty()) {
									if(!stopWords.contains(token)) sb.append(token + " ");
								}
							}
							saveStringIntoFileUnderFolder(f.getAbsolutePath(), preproFile + "_rs", sb.toString());
						}
					}
				}
			}
			System.out.println("Finish remove stopWords !! ");
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	// sort HashMap by value
	private Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list =new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
                                           Map.Entry<String, Integer> o2) {
				return -1*(o1.getValue()).compareTo(o2.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	private static Map<String, Double> sortByComparatorDouble(Map<String, Double> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Double>> list =new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
                                           Map.Entry<String, Double> o2) {
				return -1*(o1.getValue()).compareTo(o2.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	public static void main(String[] args)throws IOException{		
		if(args.length != 1) {
			System.out.println("input command : java PreprocessForWord2Vec folder");
			System.exit(0);
		}
		PreprocessForWord2Vec preprocess = new PreprocessForWord2Vec();
		//preprocess.dumpGitLog(args[0]);
		preprocess.parseProjects(args[0]);
		preprocess.wordsStatics(args[0]);
		preprocess.generateStopWords("english.stop", 150);
		preprocess.removeStopWords(args[0]);
		System.out.println("Done ");
	}
}


