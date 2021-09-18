package au.id.mcterra;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.GZIPOutputStream;

public class LogFileVisitor implements FileVisitor<Path> {
	public static final String TIMESTAMP_REGEX = "(\\[\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\])";
	public final Path inputPath;
	public final Path outputPath;
	public final String roundId;
	public final String[] logBlacklist;

	public LogFileVisitor(Path inputPath, Path outputPath, String roundId, String[] logBlacklist) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.roundId = roundId;
		this.logBlacklist = logBlacklist;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path targetdir = outputPath.resolve(inputPath.relativize(dir));
		String name = dir.getFileName().toString();
		// Skip over current round
		if(name.equals("round-" + roundId)) {
			System.out.println("Skipping " + name + " as it is the current round.");
			return FileVisitResult.SKIP_SUBTREE;
		}

		try {
			Files.copy(dir, targetdir);
		} catch(FileAlreadyExistsException e) {
			if(name.startsWith("round-")) {
				System.out.println("Skipping " + name + " as it already exists");
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		System.out.println("Copying " + name);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path target = outputPath.resolve(Path.of(inputPath.relativize(file).toString()));
		if(target.toFile().exists()) {
			return FileVisitResult.CONTINUE;
		}
		String extension = "";
		String name = file.getFileName().toString();
		if(checkLogBlacklist(name)) {
			return FileVisitResult.CONTINUE;
		}
		int i = name.lastIndexOf(".");
		if(i > -1) {
			extension = name.substring(i + 1);
		}
		// Only copy known extensions
		switch(extension) {
			case "log":
			case "html":
			case "json":
			case "csv":
				String fileString = "";
				try{
					fileString = Files.readString(file);
				} catch(MalformedInputException e) {
					System.err.println("Failed to read file " + file.toString());
					e.printStackTrace();
					return FileVisitResult.CONTINUE;
				} catch(AccessDeniedException e) {
					System.err.println("Access denied to file " + file.toString());
					return FileVisitResult.CONTINUE;
				}
				fileString = filterString(fileString);
				compress(fileString.getBytes(), target);
				break;
			case "png":
				byte[] fileContents = Files.readAllBytes(file);
				compress(fileContents, target);
				break;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		exc.printStackTrace();
		return FileVisitResult.TERMINATE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if(exc != null) {
			exc.printStackTrace();
			return FileVisitResult.TERMINATE;
		}
		return FileVisitResult.CONTINUE;
	}

	private String filterString(String str) {
		String[] lines = str.split("\n");
		// each type of censoring
		for(int i = 0; i < lines.length; i++) {
			String working = lines[i]; // avoid unneeded array access
			working = working.replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?:-\\d{10})?", "[CENSORED: IP/CID]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?ADMINPRIVATE:.*$", "$1 [CENSORED: ASAY/AHELP/NOTE/BAN]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?MENTOR:.*$", "$1 [CENSORED: MSAY/MHELP]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?SQL:.*$", "$1 [CENSORED: SQL]");
			working = working.replaceFirst("(\\s-\\sUser\\sAgent:)\\s.+", "$1 [CENSORED: USER-AGENT]");
			lines[i] = working;
		}
		return String.join("\n", lines);
	}

	// Basically just copy but it does a gz compression
	private void compress(byte[] bytes, Path target) throws IOException {
		// Need to take a string of the path and add the .gz extension
		target = Path.of(target.toString() + ".gz");
		GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(target));
		gzipStream.write(bytes);
		gzipStream.close();
	}

	private boolean checkLogBlacklist(String name) {
		for(String s : logBlacklist) {
			if(name.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
