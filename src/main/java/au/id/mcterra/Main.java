package au.id.mcterra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

public class Main
{
	public static Config config;
	public static Status status;

	public static void main( String[] args )
	{
		System.out.println("Starting up log parser");
		// Load the config from file
		try {
			config = new Config("config.json");
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		int code = 0;
		String content = "";
		// Fetch the current round ID from the API
		try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(config.apiUrl);
			try(CloseableHttpResponse response = httpClient.execute(httpGet)) {
				code = response.getCode();
				content = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch(IOException e) {
			System.err.println("Failed to request information from the server API. Check config.");
			e.printStackTrace();
			System.exit(1);
		}

		if(code != 200) {
			System.err.println("API returned an error. Contact SysOp.");
			System.err.println("API Response: " + content);
			System.exit(1);
		}

		try {
			status = new Status(content);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Copying config files
		CopyHandler configHandler = new CopyHandler(config.serverDirectory, config.outputDirectory, true);

		// Directories
		for(Path p : config.configDirectories) {
			try {
				configHandler.copyFolder(p, true);
			} catch(IOException e) {
				e.printStackTrace();
				continue;
			}
		}

		// Files
		for(Path p : config.configFiles) {
			try {
				configHandler.copyFile(p, true);
			} catch(IOException e) {
				e.printStackTrace();
				continue;
			}
		}

		System.out.println("Copied config and data files");

		try {
			Files.walkFileTree(
							config.serverDirectory.resolve("data").resolve("logs"),
							new LogFileVisitor(
								config.serverDirectory.resolve("data").resolve("logs"),
								config.outputDirectory.resolve("data").resolve("logs"),
								status.roundId,
								config.logBlacklist
							)
			);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		// This seems to happen all the time. Just ignore it lmfao
		} catch(NullPointerException e) {}
	}
}
