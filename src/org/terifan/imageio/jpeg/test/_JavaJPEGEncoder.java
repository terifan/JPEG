package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import org.terifan.imageio.jpeg.SubsamplingMode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class _JavaJPEGEncoder
{
	public static void write(BufferedImage aImage, OutputStream aOutput, int aCompression, SubsamplingMode aSubsamplingMode)
	{
		if (aSubsamplingMode == SubsamplingMode._440)
		{
			aSubsamplingMode = SubsamplingMode._422;
		}

		try
		{
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

			ImageWriteParam iwp = writer.getDefaultWriteParam();
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iwp.setCompressionQuality(aCompression / 100f);

			try (ImageOutputStream ios = ImageIO.createImageOutputStream(aOutput))
			{
				writer.setOutput(ios);

				IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(aImage.getColorModel(), aImage.getSampleModel()), iwp);

				setJpegSubsamplingMode(metadata, aSubsamplingMode == SubsamplingMode._444 ? (16 | 1) : aSubsamplingMode == SubsamplingMode._422 ? (16 | 2) : aSubsamplingMode == SubsamplingMode._420 ? (32 | 2) : aSubsamplingMode == SubsamplingMode._411 ? (16 | 4) : -1);

				writer.write(null, new IIOImage(aImage, null, metadata), iwp);
				writer.dispose();
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	// https://codereview.appspot.com/3082041/patch/204004/210007
	private static void setJpegSubsamplingMode(IIOMetadata metadata, int aSamplingMode) throws IIOInvalidTreeException
	{
		// Tweaking the image metadata to override default subsampling(4:2:0) with 4:4:4.
		Node rootNode = metadata.getAsTree("javax_imageio_jpeg_image_1.0"); // com.sun.imageio.plugins.jpeg.JPEG.nativeImageMetadataFormatName
		boolean metadataUpdated = false;
		// The top level root node has two children, out of which the second one will
		// contain all the information related to image markers.
		if (rootNode.getLastChild() != null)
		{
			Node markerNode = rootNode.getLastChild();
			NodeList markers = markerNode.getChildNodes();
			// Search for 'SOF' marker where subsampling information is stored.
			for (int i = 0; i < markers.getLength(); i++)
			{
				Node node = markers.item(i);
				// 'SOF' marker can have
				//   1 child node if the color representation is greyscale,
				//   3 child nodes if the color representation is YCbCr, and
				//   4 child nodes if the color representation is YCMK.
				// This subsampling applies only to YCbCr.
				if (node.getNodeName().equalsIgnoreCase("sof") && node.hasChildNodes() && node.getChildNodes().getLength() == 3)
				{
					// In 'SOF' marker, first child corresponds to the luminance channel, and setting
					// the HsamplingFactor and VsamplingFactor to 1, will imply 4:4:4 chroma subsampling.
					NamedNodeMap attrMap = node.getFirstChild().getAttributes();
					attrMap.getNamedItem("HsamplingFactor").setNodeValue((aSamplingMode & 0xf) + "");
					attrMap.getNamedItem("VsamplingFactor").setNodeValue(((aSamplingMode >> 4) & 0xf) + "");
					metadataUpdated = true;
					break;
				}
			}
		}

		// Read the updated metadata from the metadata node tree.
		if (metadataUpdated)
		{
			metadata.setFromTree("javax_imageio_jpeg_image_1.0", rootNode); // com.sun.imageio.plugins.jpeg.JPEG.nativeImageMetadataFormatName
		}
	}
}
