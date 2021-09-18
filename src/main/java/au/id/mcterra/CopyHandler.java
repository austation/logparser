package au.id.mcterra;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.GZIPOutputStream;

public class CopyHandler {
	public final Path inputDirectory;
	public final Path outputDirectory;
	public final boolean compress; // GZIP compression toggle

	public CopyHandler(Path inputDirectory, Path outputDirectory, boolean compress) {
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		this.compress = compress;
		if(!inputDirectory.toFile().exists()) {
			throw new RuntimeException(String.format("Input directory %s does not exist", inputDirectory.toString()));
		}
		if(!outputDirectory.toFile().exists()) {
			if(!outputDirectory.toFile().mkdirs()) {
				throw new RuntimeException(String.format("Output directory %s does not exist, nor could it be created", outputDirectory.toString()));
			}
		}
	}

	public void copyFile(Path relPath, boolean overwrite) throws IOException {
		Path inputPath = inputDirectory.resolve(relPath);
		Path outputPath = outputDirectory.resolve(relPath);
		if(!inputPath.toFile().exists()) {
			throw new IOException(String.format("Input file %s does not exist", inputPath.toString()));
		}
		else if(outputPath.toFile().exists() && !overwrite) {
			return;
		}
		outputPath.getParent().toFile().mkdirs();
		if(compress) {
			compress(inputPath, outputPath);
		} else {
			Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public void copyFolder(Path relPath, boolean overwrite) throws IOException {
		Path inputPath = inputDirectory.resolve(relPath);
		Path outputPath = outputDirectory.resolve(relPath);
		if(!inputPath.toFile().exists()) {
			throw new IOException(String.format("Input file %s does not exist", inputPath.toString()));
		}
		if(!outputPath.toFile().exists()) {
			if(!outputPath.toFile().mkdirs()) {
				throw new RuntimeException(String.format("Output directory %s does not exist, nor could it be created", outputPath.toString()));
			}
		}
		Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
			// Anon class for copying recursively
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path targetdir = outputPath.resolve(inputPath.relativize(dir));
				 try {
					 Files.copy(dir, targetdir);
				 } catch (FileAlreadyExistsException e) {
					  if (!Files.isDirectory(targetdir))
						  throw e;
				 }
				 return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path target = outputPath.resolve(inputPath.relativize(file));
				if(target.toFile().exists() && !overwrite) {
					return FileVisitResult.CONTINUE;
				}
				if(compress) {
					compress(file, target);
				} else {
					Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	// Basically just copy but it does a gz compression
	public void compress(Path file, Path target) throws IOException {
		// Need to take a string of the path and add the .gz extension
		target = Path.of(target.toString() + ".gz");
		GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(target));
		byte[] inputBytes = Files.readAllBytes(file);
		gzipStream.write(inputBytes);
		gzipStream.close();
	}
}
