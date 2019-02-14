import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class TgaConverter
{
	public static void main(String[] args)
	{
		System.out.println("Starting... Please wait a few minutes!");
		
		String path = TgaConverter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		File folder;
		String outFolderName;
		try
		{
			folder = new File(path).getParentFile();
			outFolderName = folder.getAbsolutePath().concat("/output/");
			
			if (!folder.isDirectory())
				return;
			
			File outFolder = new File(outFolderName);
			
			if (!outFolder.exists())
				outFolder.mkdir();
			
			for (File f : folder.listFiles())
			{
				if (!f.getName().endsWith(".tga"))
					continue;
				
				File newImage = new File(outFolderName.concat(f.getName().replaceAll(".tga", ".png")));
				
				if (!newImage.exists())
				{
					BufferedImage newBufferedImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
					newBufferedImage.createGraphics().drawImage(TargaReader.getImage(f), 0, 0, Color.WHITE, null);
					
					newImage.createNewFile();
					
					// записываем новое изображение в формате jpg
					ImageIO.write(newBufferedImage, "png", newImage);
				}
			}
			
			System.out.println("Finish!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
