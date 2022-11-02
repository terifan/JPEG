package examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import examples.res.R;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.swing.JOptionPane;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.decoder.IDCT;
import org.terifan.imageio.jpeg.encoder.FDCT;


public class JPEGEditorDemo
{
	private static BufferedImage mOriginalImage;

	private JComboBox mFileSelect;
	private FilterPanel mFilterPanel1;
	private FilterPanel mFilterPanel2;
	private ViewPanel mViewPanel1;
	private ViewPanel mViewPanel2;


	public JPEGEditorDemo(File aTestImageDirectory) throws IOException
	{
		Color bg = new Color(255, 255, 255);

		mFilterPanel1 = new FilterPanel();
		mFilterPanel2 = new FilterPanel();
		mViewPanel1 = new ViewPanel(mFilterPanel1, true);
		mViewPanel2 = new ViewPanel(mFilterPanel2, false);

		mFileSelect = new JComboBox(aTestImageDirectory.exists() ? aTestImageDirectory.listFiles() : new Object[]{"Lenna.png","Swallowtail.png"});
		mFileSelect.addActionListener(e->loadImage());

		JPanel filePanel = new JPanel(new FlowLayout());
		filePanel.add(new JLabel("File"));
		filePanel.add(mFileSelect);

		JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
		mainPanel.setBorder(BorderFactory.createLineBorder(bg, 4));
		mainPanel.setBackground(bg);
		mainPanel.add(mViewPanel1);
		mainPanel.add(mViewPanel2);
		mainPanel.add(mFilterPanel1);
		mainPanel.add(mFilterPanel2);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(filePanel, BorderLayout.NORTH);
		panel.add(mainPanel, BorderLayout.CENTER);

		JFrame frame = new JFrame();
		frame.setBackground(bg);
		frame.add(panel);
		frame.setSize(2000, 1300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		loadImage();

//		mViewPanel1.mQualitySlider.setValue(76);
//		mViewPanel1.mSubsamplingSelect.setSelectedIndex(2);
//		mViewPanel2.mQualitySlider.setValue(59);

		mViewPanel1.mQualitySlider.setValue(75);
		mViewPanel2.mQualitySlider.setValue(75);
		mViewPanel2.mFDCTSelect.setSelectedIndex(2);

		_ImagePanel[] p = {mViewPanel1.mImagePanel, mViewPanel2.mImagePanel, mFilterPanel1.mImagePanel, mFilterPanel2.mImagePanel};
		mViewPanel1.mImagePanel.setMirrorPanels(p);
		mViewPanel2.mImagePanel.setMirrorPanels(p);
		mFilterPanel1.mImagePanel.setMirrorPanels(p);
		mFilterPanel2.mImagePanel.setMirrorPanels(p);
	}


	private void loadImage()
	{
		try
		{
			Object item = mFileSelect.getSelectedItem();
			if (item instanceof File)
			{
				mOriginalImage = ImageIO.read((File)item);
			}
			else
			{
				mOriginalImage = ImageIO.read(R.class.getResource((String)item));
			}
			mViewPanel1.loadImage();
			mViewPanel2.loadImage();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, e.toString());
			e.printStackTrace(System.out);
		}
	}


	private class ViewPanel extends JPanel
	{
		FilterPanel mFilterPanel;
		boolean mLeft;
		JLabel mFileSizeLabel;
		JLabel mQualityLabel;
		JSlider mQualitySlider;
		JComboBox<SubsamplingMode> mSubsamplingSelect;
		JComboBox<CompressionType> mCompressionSelect;
		JComboBox<FDCT> mFDCTSelect;
		JComboBox<IDCT> mIDCTSelect;
		_ImagePanel mImagePanel;


		public ViewPanel(FilterPanel aFilterPanel, boolean aLeft)
		{
			mFilterPanel = aFilterPanel;

			JPanel controlPanel = new JPanel(new GridLayout(2, 5, 10, 0));

			mLeft = aLeft;
			mFileSizeLabel = new JLabel("");
			mQualityLabel = new JLabel("Quality");
			mQualitySlider = new JSlider(0, 100, 90);
			mSubsamplingSelect = new JComboBox(SubsamplingMode.values());
			mCompressionSelect = new JComboBox(CompressionType.values());
			mFDCTSelect = new JComboBox(new String[]
			{
				FDCTFloat.class.getSimpleName(), FDCTIntegerFast.class.getSimpleName(), FDCTIntegerSlow.class.getSimpleName()
			});
			mIDCTSelect = new JComboBox(new String[]
			{
				IDCTFloat.class.getSimpleName(), IDCTIntegerFast.class.getSimpleName(), IDCTIntegerSlow.class.getSimpleName()
			});

			mQualitySlider.addChangeListener(e -> loadImage());
			mCompressionSelect.addActionListener(e -> loadImage());
			mSubsamplingSelect.addActionListener(e -> loadImage());
			mFDCTSelect.addActionListener(e -> loadImage());
			mIDCTSelect.addActionListener(e -> loadImage());

			JPanel fileSizePanel = new JPanel(new FlowLayout(aLeft ? FlowLayout.RIGHT : FlowLayout.LEFT));
			fileSizePanel.add(mFileSizeLabel);

			controlPanel.add(mQualityLabel);
			controlPanel.add(new JLabel("Subsampling"));
			controlPanel.add(new JLabel("FDCT"));
			controlPanel.add(new JLabel("IDCT"));
			controlPanel.add(new JLabel("Compression"));
			controlPanel.add(mQualitySlider);
			controlPanel.add(mSubsamplingSelect);
			controlPanel.add(mFDCTSelect);
			controlPanel.add(mIDCTSelect);
			controlPanel.add(mCompressionSelect);
			controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

			mImagePanel = new _ImagePanel();

			super.setLayout(new BorderLayout());
			super.add(controlPanel, BorderLayout.NORTH);
			super.add(mImagePanel, BorderLayout.CENTER);
			super.add(fileSizePanel, BorderLayout.SOUTH);

			mFilterPanel.mViewPanel = this;
		}


		void loadImage()
		{
			mWorker.start();
		}


		private SingleOp mWorker = new SingleOp(() ->
		{
			Class[] fdctTypes =
			{
				FDCTFloat.class, FDCTIntegerFast.class, FDCTIntegerSlow.class
			};
			Class[] idctTypes =
			{
				IDCTFloat.class, IDCTIntegerFast.class, IDCTIntegerSlow.class
			};

			int quality = mQualitySlider.getValue();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			new JPEGImageIO()
				.setQuality(quality)
				.setFDCT(fdctTypes[mFDCTSelect.getSelectedIndex()])
				.setCompressionType((CompressionType)mCompressionSelect.getSelectedItem())
				.setSubsampling((SubsamplingMode)mSubsamplingSelect.getSelectedItem())
				.write(mOriginalImage, baos);

			BufferedImage decoded;

			decoded = new JPEGImageIO()
				.setIDCT(idctTypes[mIDCTSelect.getSelectedIndex()])
				.setLog(System.out)
				.read(baos.toByteArray());

//			if (ViewPanel.this == mViewPanel2)
//			{
//				try
//				{
//					decoded = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
//				}
//				catch (Exception e)
//				{
//					decoded = null;
//					e.printStackTrace(System.out);
//				}
//			}

			mImagePanel.setImage(decoded);
			mQualityLabel.setText("Quality - " + quality);
			mFileSizeLabel.setText(baos.size() + " bytes");

			mImagePanel.repaint();
			repaint();

			mFilterPanel.update();
		});
	}


	private static class FilterPanel extends JPanel
	{
		private _ImagePanel mImagePanel;
		private ViewPanel mViewPanel;
		private JLabel[] mResult;


		public FilterPanel()
		{
			mImagePanel = new _ImagePanel();
			mImagePanel.setImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

			JPanel panel = new JPanel(new GridLayout(2, 3));
			mResult = new JLabel[8];
			for (int i = 0; i < mResult.length; i++)
			{
				panel.add(mResult[i] = new JLabel(" "));
			}

			super.setLayout(new BorderLayout());
			super.add(panel, BorderLayout.NORTH);
			super.add(mImagePanel, BorderLayout.CENTER);
		}


		public void update()
		{
			if (mImagePanel.getImage().getWidth() != mOriginalImage.getWidth() || mImagePanel.getImage().getHeight() != mOriginalImage.getHeight())
			{
				mImagePanel.setImage(new BufferedImage(mOriginalImage.getWidth(), mOriginalImage.getHeight(), BufferedImage.TYPE_INT_RGB));
			}
			mWorker.start();
		}


		private SingleOp mWorker = new SingleOp(() ->
		{
			BufferedImage image = mViewPanel.mImagePanel.getImage();

			if (image != null)
			{
				mImagePanel.repaint();

				measureError(image, mOriginalImage);

				repaint();
			}
		});


		public void measureError(BufferedImage aImage1, BufferedImage aImage2)
		{
			BufferedImage dst = mImagePanel.getImage();

			_ImageQualityTest result = new _ImageQualityTest(aImage1, aImage2, dst);

			mResult[0].setText("Accum. RGB differance");
			mResult[1].setText("Pixel errors");
			mResult[2].setText("MSE");
			mResult[3].setText("PSNR");
			mResult[4].setText("" + result.accumDiff);
			mResult[5].setText("" + result.pixelErrors);
			mResult[6].setText("" + result.mse);
			mResult[7].setText("" + result.psnr + " dB");
		}
	}


	private static class SingleOp
	{
		private boolean mWorking;
		private boolean mPendingWork;
		private Runnable mRunnable;


		public SingleOp(Runnable aRunnable)
		{
			mRunnable = aRunnable;
		}


		public void start()
		{
			synchronized (this)
			{
				if (mWorking)
				{
					mPendingWork = true;
					return;
				}

				mWorking = true;
			}

			new Thread()
			{
				@Override
				public void run()
				{
					for (;;)
					{
						try
						{
							mRunnable.run();
						}
						catch (Throwable e)
						{
							e.printStackTrace(System.out);
						}
						finally
						{
							synchronized (this)
							{
								if (!mPendingWork)
								{
									mWorking = false;
									return;
								}
								mPendingWork = false;
							}
						}
					}
				}
			}.start();
		}
	}


	public static void main(String... args)
	{
		File dir = new File("c:\\pictures\\image compression suit");

		try
		{
			new JPEGEditorDemo(dir);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
