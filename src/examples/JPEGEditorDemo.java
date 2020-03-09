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
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.decoder.IDCT;
import org.terifan.imageio.jpeg.encoder.FDCT;


public class JPEGEditorDemo
{
	private static BufferedImage mOriginalImage;


	public JPEGEditorDemo() throws IOException
	{
		Color bg = new Color(255, 255, 255);

		FilterPanel filterPanel1 = new FilterPanel();
		FilterPanel filterPanel2 = new FilterPanel();
		ViewPanel viewPanel1 = new ViewPanel(filterPanel1, true);
		ViewPanel viewPanel2 = new ViewPanel(filterPanel2, false);

		JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
		panel.setBorder(BorderFactory.createLineBorder(bg, 4));
		panel.setBackground(bg);
		panel.add(viewPanel1);
		panel.add(viewPanel2);
		panel.add(filterPanel1);
		panel.add(filterPanel2);

		JFrame frame = new JFrame();
		frame.setBackground(bg);
		frame.add(panel);
		frame.setSize(2000, 1300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		viewPanel1.mQualitySlider.setValue(76);
		viewPanel1.mSubsamplingSelect.setSelectedIndex(2);
		viewPanel2.mQualitySlider.setValue(59);
	}


	private static class ViewPanel extends JPanel
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

//			try
//			{
//				decoded = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace(System.out);
//			}

			mImagePanel.setImage(decoded);
			mQualityLabel.setText("Quality - " + quality);
			mFileSizeLabel.setText(baos.size() + " bytes");

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

			mImagePanel.setImage(new BufferedImage(mOriginalImage.getWidth(), mOriginalImage.getHeight(), BufferedImage.TYPE_INT_RGB));

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


		public void update()
		{
			mWorker.start();
		}


		// TODO: https://github.com/Rolinh/VQMT/tree/master/src
		public void measureError(BufferedImage aImage1, BufferedImage aImage2)
		{
			BufferedImage dst = mImagePanel.getImage();

			long deltaR = 0;
			long deltaG = 0;
			long deltaB = 0;
			long accumDiff = 0;
			long accumError = 0;
			int w = aImage1.getWidth();
			int h = aImage1.getHeight();

			for (int y = 0; y < h; y++)
			{
				for (int x = 0; x < w; x++)
				{
					int c0 = aImage1.getRGB(x, y);
					int c1 = aImage2.getRGB(x, y);

					int r0 = 0xff & (c0 >> 16);
					int g0 = 0xff & (c0 >>  8);
					int b0 = 0xff & (c0      );
					int r1 = 0xff & (c1 >> 16);
					int g1 = 0xff & (c1 >>  8);
					int b1 = 0xff & (c1      );

					if (r0 != r1)
					{
						deltaR += Math.pow(r0 - r1, 2);
					}
					if (g0 != g1)
					{
						deltaG += Math.pow(g0 - g1, 2);
					}
					if (b0 != b1)
					{
						deltaB += Math.pow(b0 - b1, 2);
					}

					int d = Math.abs(r0 - r1) + Math.abs(g0 - g1) + Math.abs(b0 - b1);
					accumDiff += d;

					if (d > 3 * 5)
					{
						accumError++;
					}

					dst.setRGB(x, y, (clamp(128 + r0 - r1) << 16) + (clamp(128 + g0 - g1) << 8) + clamp(128 + b0 - b1));
				}
			}

			double mse = (deltaR + deltaG + deltaB) / (w * h) / 3;

			double psnr = mse == 0 ? 0 : -10 * Math.log10(mse / Math.pow(255, 2));

			mResult[0].setText("Accum. RGB differance");
			mResult[1].setText("Accum. error");
			mResult[2].setText("MSE");
			mResult[3].setText("PSNR");
			mResult[4].setText("" + accumDiff);
			mResult[5].setText("" + accumError);
			mResult[6].setText("" + mse);
			mResult[7].setText("" + psnr + " dB");
		}


		private int clamp(int v)
		{
			return v < 0 ? 0 : v > 255 ? 255 : v;
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
			synchronized (SingleOp.class)
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
							synchronized (SingleOp.class)
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
		try
		{
			mOriginalImage = ImageIO.read(R.class.getResource("Lenna.png"));

			new JPEGEditorDemo();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
