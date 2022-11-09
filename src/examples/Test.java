package examples;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.zip.InflaterInputStream;



public class Test
{
	public static void main(String ... args)
	{
		try
		{
			try (ObjectInputStream ois = new ObjectInputStream(new InflaterInputStream(new FileInputStream("d:\\desktop\\request_b7433511-62e6-48c3-81d7-edbe647a7a51_RIXY50172.cache"))))
			{
				ois.readObject();
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
