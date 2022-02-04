package au.id.mcterra.logparser;

import java.io.File;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogFileVisitor implements FileVisitor<Path> {
	public static final String TIMESTAMP_REGEX = "(\\[\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\])";
	public final Path inputPath;
	public final Path outputPath;
	public final long roundId;
	public final String[] logBlacklist;
	// Current round zip file for output
	private ZipOutputStream currentZip = null;
	// Persistent variable used to get relative paths for zip output
	private Path currentRound = null;

	public LogFileVisitor(Path inputPath, Path outputPath, long roundId, String[] logBlacklist) {
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
		if (name.equals("round-" + roundId)) {
			System.out.println("Skipping " + name + " as it is the current round.");
			return FileVisitResult.SKIP_SUBTREE;
		}

		boolean isRound = name.startsWith("round-");

		try {
			Files.copy(dir, targetdir);
		} catch (FileAlreadyExistsException e) {
			if (isRound) {
				System.out.println("Skipping " + name + " as it already exists");
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		// Get the zip file output set up if required (rounds)
		if (isRound) {
			File zipFile = Path.of(targetdir.toString() + ".zip").toFile();
			if (!zipFile.exists()) {
				System.out.println("Creating zip file " + zipFile.getName() + " as it does not exists");
				currentRound = dir;
				currentZip = new ZipOutputStream(new FileOutputStream(Path.of(targetdir.toString() + ".zip").toFile()));
			}
		}

		System.out.println("Copying " + name);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path target = Path.of(outputPath.resolve(Path.of(inputPath.relativize(file).toString())).toString() + ".gz");
		if (target.toFile().exists()) {
			return FileVisitResult.CONTINUE;
		}
		String extension = "";
		String name = file.getFileName().toString();
		if (checkLogBlacklist(name)) {
			return FileVisitResult.CONTINUE;
		}
		int i = name.lastIndexOf(".");
		if (i > -1) {
			extension = name.substring(i + 1);
		}
		// Only copy known extensions
		try {
			switch (extension) {
				case "log":
				case "html":
				case "json":
				case "csv":
					String fileString = "";
					fileString = Files.readString(file);
					fileString = filterString(fileString);
					compress(fileString.getBytes(), target);
					break;
				case "png":
					byte[] fileContents = Files.readAllBytes(file);
					compress(fileContents, target);
					break;
				default:
					return FileVisitResult.CONTINUE;
			}
		} catch (MalformedInputException e) {
			System.err.println("Failed to read file " + file.toString());
			e.printStackTrace();
			return FileVisitResult.CONTINUE;
		} catch (AccessDeniedException e) {
			System.err.println("Access denied to file " + file.toString());
			return FileVisitResult.CONTINUE;
		}
		if (currentZip != null && currentRound != null) {
			// Relativise the path against current round
			String zipPath = currentRound.relativize(file).toString();
			ZipEntry entry = new ZipEntry(zipPath);
			currentZip.putNextEntry(entry);
			byte[] fileBytes = Files.readAllBytes(target);
			currentZip.write(fileBytes, 0, fileBytes.length);
			currentZip.closeEntry();
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
		if (exc != null) {
			exc.printStackTrace();
			return FileVisitResult.TERMINATE;
		}
		if(dir.equals(currentRound)) {
			// Close the zip up, we've finished copying this round folder
			currentZip.close();
			currentZip = null;
			currentRound = null;
		}
		return FileVisitResult.CONTINUE;
	}

	private String filterString(String str) {
		String[] lines = str.split("\n");
		// each type of censoring
		for (int i = 0; i < lines.length; i++) {
			String working = lines[i]; // avoid unneeded array access
			working = working.replaceAll("(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?:\\d{9,10})", "[CENSORED: IP/CID]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?ADMINPRIVATE:.*$", "$1 [CENSORED: ASAY/AHELP/NOTE/BAN]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?MENTOR:.*$", "$1 [CENSORED: MSAY/MHELP]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?SQL:.*$", "$1 [CENSORED: SQL]");
			working = working.replaceFirst(TIMESTAMP_REGEX + "\\s?TOPIC:.*$", "$1 [CENSORED: TOPIC]");
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
		for (String s : logBlacklist) {
			if (name.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
