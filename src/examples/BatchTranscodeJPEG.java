package examples;

import java.io.File;
import org.terifan.imageio.jpeg.JPEGImageIO;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import javax.swing.JOptionPane;
import org.terifan.imageio.jpeg.CompressionType;


public class BatchTranscodeJPEG
{
	public static void main(String... args)
	{
		try
		{
			File dir = new File(JOptionPane.showInputDialog("Path to directory with JPEG files to perform tests on:"));

			System.out.printf("%-9s ", "Original");
			for (int i = 0; i < CompressionType.values().length; i++)
			{
				System.out.printf("%-26s  ", CompressionType.values()[i]);
			}
			System.out.println("Image File");

			for (File file : dir.listFiles(e->e.getName().matches("(?i).*jpg")))
			{
				byte[] input = new byte[(int)file.length()];
				try (DataInputStream in = new DataInputStream(new FileInputStream(file)))
				{
					in.readFully(input);
				}

				ByteArrayOutputStream output = new ByteArrayOutputStream();

				System.out.printf("%9d ", input.length);

				for (int i = 0; i < CompressionType.values().length; i++)
				{
					new JPEGImageIO().setCompressionType(CompressionType.values()[i]).transcode(input, output);

					System.out.printf("[%9d %5.1f%% %7d]  ", output.size(), output.size() * 100.0 / input.length, input.length - output.size());

					output.reset();
				}

				System.out.printf("%s%n", file);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
