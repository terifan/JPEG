package examples;

import org.terifan.imageio.jpeg.JPEGImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.FixedThreadExecutor;


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

			int columnCount = CompressionType.values().length;

			System.out.printf("%9s ", "Original");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("%-28s  ", CompressionType.values()[i]);
			}
			System.out.println("Image File");

			System.out.printf("%9s ", "=========");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("%-28s  ", "============================");
			}
			System.out.println("");

			int[] outputSizes = new int[columnCount];
			int[] inputSizes = new int[columnCount];

			for (Path file : Files.walk(chooser.getSelectedFile().toPath(), 10).filter(e -> e.toString().matches("(?i).*jpg")).limit(10_000).collect(Collectors.toList()))
			{
				try
				{
					byte[] input = Files.readAllBytes(file);

					System.out.printf("%9d ", input.length);

					String[] result = new String[columnCount];

					try ( FixedThreadExecutor exe = new FixedThreadExecutor(1f))
					{
						for (int i = 0; i < columnCount; i++)
						{
							int _i = i;

							exe.submit(() ->
							{
								try
								{
									ByteArrayOutputStream output = new ByteArrayOutputStream();

									new JPEGImageIO().setCompressionType(CompressionType.values()[_i]).transcode(input, output);

									result[_i] = String.format("[%9d %5.1f%% %9d]  ", output.size(), output.size() * 100.0 / input.length, output.size() - input.length);

									inputSizes[_i] += input.length;
									outputSizes[_i] += output.size();
								}
								catch (Exception e)
								{
									result[_i] = "\033[0;31m" + e.toString() + "\033[0m -- ";
								}
							});
						}
					}

					for (int i = 0; i < columnCount; i++)
					{
						System.out.print(result[i]);

						if (result[i].contains("\033[0;31m"))
						{
							break;
						}
					}

					System.out.printf("%s%n", file);
				}
				catch (Exception e)
				{
					System.out.printf("\033[0;31m%s\033[0m%n", e.getMessage() + " -- " + file);
				}
			}

			System.out.printf("%9s ", "=========");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("%-28s  ", "============================");
			}
			System.out.println("");

			System.out.printf("%9s ", "");
			for (int i = 0; i < columnCount; i++)
			{
				System.out.printf("[%9d %5.1f%% %9d]  ", outputSizes[i], outputSizes[i] * 100.0 / inputSizes[i], outputSizes[i] - inputSizes[i]);
			}
			System.out.println();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
