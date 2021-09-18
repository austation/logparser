package au.id.mcterra;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
	public final Path serverDirectory;
	public final Path outputDirectory;
	public final Path[] configFiles;
	public final Path[] configDirectories;
	public final String[] logBlacklist;
	public final String apiUrl;

	public Config(String filename) throws Exception {
		String text = Files.readString(Path.of(filename));
		JSONParser parser = new JSONParser();
		Object result = parser.parse(text);
		// This will throw typecast exception if it tries to cast a null I guess
		JSONObject jsonConfig = (JSONObject)result;
		serverDirectory = Path.of((String)jsonConfig.get("serverDirectory"));
		outputDirectory = Path.of((String)jsonConfig.get("outputDirectory"));

		// CBT
		Object[] tempConfFiles = ((JSONArray)jsonConfig.get("configFiles")).toArray();
		String[] tempStrConfFiles = Arrays.copyOf(tempConfFiles, tempConfFiles.length, String[].class);
		configFiles = new Path[tempStrConfFiles.length];
		for(int i = 0; i < tempStrConfFiles.length; i++) {
			configFiles[i] = Path.of(tempStrConfFiles[i]);
		}

		Object[] tempConfDirs = ((JSONArray)jsonConfig.get("configDirectories")).toArray();
		String[] tempStrConfDirs = Arrays.copyOf(tempConfDirs, tempConfDirs.length, String[].class);
		configDirectories = new Path[tempStrConfDirs.length];
		for(int i = 0; i < tempStrConfDirs.length; i++) {
			configDirectories[i] = Path.of(tempStrConfDirs[i]);
		}

		Object[] tempLogBlacklist = ((JSONArray)jsonConfig.get("logBlacklist")).toArray();
		logBlacklist = Arrays.copyOf(tempLogBlacklist, tempLogBlacklist.length, String[].class);

		apiUrl = (String) jsonConfig.get("apiUrl");
	}
}
