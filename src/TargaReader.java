import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class TargaReader
{
	private static final byte[] uTGAcompare = new byte[]{0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0};	// Uncompressed TGA Header
	private static final byte[] cTGAcompare = new byte[]{0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0};	// Compressed TGA Header
	
	public static BufferedImage getImage(File file) throws IOException // Load a TGA file
	{				
		InputStream in = new FileInputStream(file);
		
		byte[] header = new byte[12];
		
		readBuffer(in, header);
		
		if(Arrays.equals(uTGAcompare, header)) // See if header matches the predefined header of
			return loadUncompressedTGA(in); // If so, jump to Uncompressed TGA loading code
		
		else if (Arrays.equals(cTGAcompare, header)) // See if header matches the predefined header of	
	       return loadCompressedTGA( in); // If so, jump to Compressed TGA loading code
		
		else
		{
			// If header matches neither type
			in.close();
			throw new IOException("TGA file be type 2 or type 10 "); // Display an error
		}
	}
	
	private static void readBuffer(InputStream in, byte[] buffer) throws IOException
	{
		int bytesRead = 0;
		int bytesToRead = buffer.length;
		
		while (bytesToRead > 0)
		{
			int read = in.read(buffer, bytesRead, bytesToRead);
			bytesRead  += read;
			bytesToRead -= read;
		}
	}
	
	private static BufferedImage loadUncompressedTGA(InputStream in) throws IOException // Load an uncompressed TGA (note, much of this code is based on NeHe's
	{
		// TGA Loading code nehe.gamedev.net)
		byte[] header = new byte[6];
		readBuffer(in, header);
		
		int imageHeight = (unsignedByteToInt(header[3]) << 8) + unsignedByteToInt(header[2]); // Determine The TGA height	(highbyte*256+lowbyte)
		int imageWidth = (unsignedByteToInt(header[1]) << 8) + unsignedByteToInt(header[0]); // Determine The TGA width	(highbyte*256+lowbyte)
		int bpp = unsignedByteToInt(header[4]); // Determine the bits per pixel
		
		if ((imageWidth  <= 0) || (imageHeight <= 0) || ((bpp != 24) && (bpp!= 32)))
			throw new IOException("Invalid texture information"); // Display Error
		
		int bytesPerPixel = (bpp / 8); // Compute the number of BYTES per pixel
		int imageSize = (bytesPerPixel * imageWidth * imageHeight); // Compute the total amout ofmemory needed to store data
		byte imageData[]   = new byte[imageSize]; // Allocate that much memory
		
		readBuffer(in, imageData);
		
		BufferedImage  bufferedImage  = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		for(int j = 0; j < imageHeight; j++)
		{
			for(int i = 0; i < imageWidth; i++)
			{
				int index = ((imageHeight - 1 - j) * imageWidth + i) * bytesPerPixel;
				int value = (255 & 0xFF) << 24| (imageData[index + 0] & 0xFF) << 16 | (imageData[index + 1] & 0xFF) <<  8 | (imageData[index + 2] & 0xFF);
				bufferedImage.setRGB(i, j,value);
			}
		}
		
		return bufferedImage;
	}
	
	private static BufferedImage loadCompressedTGA(InputStream fTGA) throws IOException // Load COMPRESSED TGAs
	{
		byte[] header = new byte[6];
		readBuffer(fTGA, header);
		
		int imageHeight = (unsignedByteToInt(header[3]) << 8) + unsignedByteToInt(header[2]); // Determine The TGA height	(highbyte*256+lowbyte)
		int imageWidth = (unsignedByteToInt(header[1]) << 8) + unsignedByteToInt(header[0]); // Determine The TGA width	(highbyte*256+lowbyte)
		int bpp = unsignedByteToInt(header[4]); // Determine the bits per pixel
		
		if ((imageWidth  <= 0) || (imageHeight <= 0) || ((bpp != 24) && (bpp!= 32)))
			throw new IOException("Invalid texture information"); // Display Error
		
		int  bytesPerPixel = (bpp / 8); // Compute the number of BYTES per pixel
		int imageSize = (bytesPerPixel * imageWidth * imageHeight); // Compute the total amout ofmemory needed to store data
		byte imageData[]   = new byte[imageSize]; // Allocate that much memory
		
		int pixelcount = imageHeight * imageWidth; // Nuber of pixels in the image
		int currentbyte = 0; // Current byte
		int currentpixel = 0; // Current pixel being read
		
		byte[] colorbuffer = new byte[bytesPerPixel]; // Storage for 1 pixel
		
		// Storage for "chunk" header
		do
		{
			int chunkheader = 0;											
			try
			{
				chunkheader = unsignedByteToInt((byte) fTGA.read());
			}
			catch (IOException e)
			{
				throw new IOException("Could not read RLE header"); // Display Error
			}
			
			// If the ehader is < 128, it means the that is the number of RAW color packets minus 1
			if(chunkheader < 128)
			{
				chunkheader++; // add 1 to get number of following color values
				
				// Read RAW color values
				for(short counter = 0; counter < chunkheader; counter++)
				{
					readBuffer(fTGA, colorbuffer);
					// write to memory
					imageData[currentbyte   ]  = colorbuffer[2]; // Flip R and B vcolor values around in the process
					imageData[currentbyte + 1] = colorbuffer[1];
					imageData[currentbyte + 2] = colorbuffer[0];
					
					if(bytesPerPixel == 4) // if its a 32 bpp image
						imageData[currentbyte + 3] = colorbuffer[3]; // copy the 4th byte
					
					currentbyte += bytesPerPixel; // Increase thecurrent byte by the number of bytes per pixel
					currentpixel++; // Increase current pixel by 1
					
					// Make sure we havent read too many pixels
					if(currentpixel > pixelcount)
						throw new IOException("Too many pixels read"); // if there is too many... Display an error!
				}
			}
			// chunkheader > 128 RLE data, next color reapeated chunkheader - 127 times
			else
			{
				chunkheader -= 127; // Subteact 127 to get rid of the ID bit
				
				readBuffer(fTGA, colorbuffer);
				
				// copy the color into the image data as many times as dictated
				for(short counter = 0; counter < chunkheader; counter++)
				{
					imageData[currentbyte] = colorbuffer[2]; // switch R and B bytes areound while copying
					
					imageData[currentbyte + 1] = colorbuffer[1];
					imageData[currentbyte + 2] = colorbuffer[0];
					
					if(bytesPerPixel == 4) // If TGA images is 32 bpp
						imageData[currentbyte + 3] = colorbuffer[3]; // Copy 4th byte
					
					currentbyte +=  bytesPerPixel; // Increase current byte by the number of bytes per pixel
					
					currentpixel++; // Increase pixel count by 1
					
					if(currentpixel > pixelcount)
						throw new IOException("Too many pixels read"); // if there is too many... Display an error!
				}
			}
		}
		while (currentpixel < pixelcount); // Loop while there are still pixels left
		
		BufferedImage  bufferedImage  = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		for(int j = 0; j < imageHeight; j++)
		{	
			for(int i = 0; i < imageWidth; i++)
			{
				int index = ((imageHeight - 1 - j) * imageWidth + i) * bytesPerPixel;
				int value = (255 & 0xFF) << 24 | (imageData[index + 0] & 0xFF) << 16 | (imageData[index + 1] & 0xFF) <<  8 | (imageData[index + 2] & 0xFF);
				
				bufferedImage.setRGB(i, j,value);
			}
		}
		
		return bufferedImage;
	}
	
	private static int unsignedByteToInt(byte b)
	{
		return (int) b & 0xFF;
	}
}