package examples;

import java.io.File;
import org.terifan.imageio.jpeg.JPEGImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import org.terifan.imageio.jpeg.CompressionType;


/**
 * Perform batch conversion on all JPEGs in a path and print results. No files are updated.
 */
public class BatchTranscodeJPEG
{
	public static void main(String... args)
	{
		try
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(null) == JFileChooser.CANCEL_OPTION)
			{
				return;
			}

			File dir = chooser.getSelectedFile();

			int columnCount = CompressionType.values().length;

			System.out.printf("%-9s ", "Original");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("%-28s  ", CompressionType.values()[i]);
			}
			System.out.println("Image File");

			int[] outputSizes = new int[columnCount];
			int[] inputSizes = new int[columnCount];
			int[] diffSizes = new int[columnCount];

			for (Path file : Files.walk(dir.toPath(), 10).filter(e -> e.toString().matches("(?i).*jpg")).limit(10_000).collect(Collectors.toList()))
			{
				try
				{
					byte[] input = Files.readAllBytes(file);

					ByteArrayOutputStream output = new ByteArrayOutputStream();

					System.out.printf("%9d ", input.length);

					for (int i = 0; i < columnCount; i++)
					{
						new JPEGImageIO().setCompressionType(CompressionType.values()[i]).transcode(input, output);

						System.out.printf("[%9d %5.1f%% %9d]  ", output.size(), output.size() * 100.0 / input.length, output.size() - input.length);

						outputSizes[i] += output.size();
						inputSizes[i] += input.length;
						diffSizes[i] += output.size() - input.length;

						output.reset();
					}

					System.out.printf("%s%n", file);
				}
				catch (Exception e)
				{
					System.out.printf("\033[0;31m%s\033[0m%n", e.getMessage() + " -- " + file);
				}
			}

			System.out.printf("%-9s ", "=========");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("%-28s  ", "============================");
			}
			System.out.println("");

			System.out.printf("%-9s ", "");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("[%9d %5.1f%% %9d]  ", outputSizes[i], outputSizes[i] * 100.0 / inputSizes[i], diffSizes[i]);
			}
			System.out.println();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
