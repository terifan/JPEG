package org.terifan.imageio.jpeg.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import org.terifan.imageio.jpeg.examples.res.R;


public class JPEGEditorDemo
{
	private static BufferedImage mImage;
	private static FilterPanel mFilerPanel;


	public JPEGEditorDemo() throws IOException
	{
		Color bg = new Color(255, 255, 255);

		ViewPanel viewPanel1 = new ViewPanel(true);
		ViewPanel viewPanel2 = new ViewPanel(false);

		mFilerPanel = new FilterPanel(viewPanel1, viewPanel2);

		JPanel panel2 = new JPanel(new GridLayout(1, 2, 10, 0));
		panel2.setBackground(bg);

		JPanel panel1 = new JPanel(new GridLayout(2, 1, 0, 10));
		panel1.setBackground(bg);
		panel1.add(panel2);
		panel1.setBorder(BorderFactory.createLineBorder(bg, 4));

		panel2.add(viewPanel1);
		panel2.add(viewPanel2);
		panel1.add(mFilerPanel);

		JFrame frame = new JFrame();
		frame.setBackground(bg);
		frame.add(panel1);
		frame.setSize(1024, 768);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		viewPanel1.mQualitySlider.setValue(50);
		viewPanel2.mQualitySlider.setValue(95);
	}


	private static class ViewPanel extends JPanel
	{
		JLabel mFileSizeLabel;
		JLabel mQualityLabel;
		JSlider mQualitySlider;
		JComboBox mSubsamplingSelect;
		JComboBox mCompressionSelect;
		JComboBox mFDCTSelect;
		JComboBox mIDCTSelect;
		_ImagePanel mImagePanel;


		public ViewPanel(boolean aLeft)
		{
			JPanel controlPanel = new JPanel(new GridLayout(2, 5, 10, 0));

			mFileSizeLabel = new JLabel("");
			mQualityLabel = new JLabel("Quality");
			mQualitySlider = new JSlider(0, 100, 90);
			mSubsamplingSelect = new JComboBox(new String[]
			{
				"4:4:4", "4:2:2", "4:2:0"
			});
			mCompressionSelect = new JComboBox(new String[]
			{
				"Sequential", "Progressive"
			});
			mFDCTSelect = new JComboBox(new String[]
			{
				"Float", "FastInt", "SlowInt"
			});
			mIDCTSelect = new JComboBox(new String[]
			{
				"Float", "FastInt", "SlowInt"
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
			new JPEGImageIO().setQuality(quality).setProgressive(mCompressionSelect.getSelectedIndex() == 1).setFDCT(fdctTypes[mFDCTSelect.getSelectedIndex()]).write(mImage, baos);

			mImagePanel.setImage(new JPEGImageIO().setIDCT(idctTypes[mIDCTSelect.getSelectedIndex()]).read(baos.toByteArray()));
			mQualityLabel.setText("Quality - " + quality);
			mFileSizeLabel.setText(baos.size() + " bytes");

			repaint();
			mFilerPanel.update();
		});
	}


	private static class FilterPanel extends JPanel
	{
		private _ImagePanel mImagePanel;
		private ViewPanel mViewPanel1;
		private ViewPanel mViewPanel2;
		private JLabel[] mResult;


		public FilterPanel(ViewPanel aViewPanel1, ViewPanel aViewPanel2)
		{

			mViewPanel1 = aViewPanel1;
			mViewPanel2 = aViewPanel2;
			mImagePanel = new _ImagePanel();

			mImagePanel.setImage(new BufferedImage(mImage.getWidth(), mImage.getHeight(), BufferedImage.TYPE_INT_RGB));

			JPanel panel = new JPanel(new GridLayout(4, 4));
			mResult = new JLabel[16];
			for (int i = 0; i < 16; i++)
			{
				mResult[i] = new JLabel((i % 4) == 3 ? " " : "n/a");
				panel.add(mResult[i]);
			}
			mResult[0].setText("Red");
			mResult[1].setText("Green");
			mResult[2].setText("Blue");

			super.setLayout(new BorderLayout());
			super.add(panel, BorderLayout.NORTH);
			super.add(mImagePanel, BorderLayout.CENTER);
		}

		private SingleOp mWorker = new SingleOp(() ->
		{
			BufferedImage image1 = mViewPanel1.mImagePanel.getImage();
			BufferedImage image2 = mViewPanel2.mImagePanel.getImage();
			BufferedImage dst = mImagePanel.getImage();

			long diff = 0;

			for (int y = 0; y < image1.getHeight(); y++)
			{
				for (int x = 0; x < image1.getWidth(); x++)
				{
					int rgb1 = image1.getRGB(x, y);
					int rgb2 = image2.getRGB(x, y);

					int r = (0xff & (rgb1 >> 16)) - (0xff & (rgb2 >> 16));
					int g = (0xff & (rgb1 >> 8)) - (0xff & (rgb2 >> 8));
					int b = (0xff & rgb1) - (0xff & rgb2);

					diff += Math.abs(r) + Math.abs(g) + Math.abs(b);

					dst.setRGB(x, y, ((128 + r) << 16) + ((128 + g) << 8) + (128 + b));
				}
			}

			mResult[7].setText("RGBdiff " + diff);

			mImagePanel.repaint();

			calculatePSNR(image1, image2);
		});


		public void update()
		{
			mWorker.start();
		}


		public void calculatePSNR(BufferedImage aImage1, BufferedImage aImage2)
		{
			double[] peak = new double[3];
			double[] noise = new double[3];

			for (int y = 0; y < aImage1.getHeight(); y++)
			{
				for (int x = 0; x < aImage1.getWidth(); x++)
				{
					for (int c = 0; c < 3; c++)
					{
						int i = 0xff & (aImage1.getRGB(x, y) >> (8 * c));
						int j = 0xff & (aImage2.getRGB(x, y) >> (8 * c));

						if (i != j)
						{
							noise[c] += pow(i - j, 2);
						}
						if (peak[c] < i)
						{
							peak[c] = i;
						}
					}
				}
			}

			double db = 0;

			for (int c = 0; c < 3; c++)
			{
				double mse = noise[c] / (aImage1.getWidth() * aImage1.getHeight());

				mResult[4 * c + 0 + 4].setText("MSE: " + mse);
				mResult[4 * c + 1 + 4].setText("PSNR(max=255): " + (10 * log10(255 * 255 / mse)));
				mResult[4 * c + 2 + 4].setText("PSNR(max=" + peak[c] + "): " + 10 * log10(pow(peak[c], 2) / mse));

				db += mse == 0 || peak[c] == 0 ? 0 : 10 * log10(pow(peak[c], 2) / mse);
			}

			mResult[15].setText("dB " + db / 3);
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
			mImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari-prog.jpg"));

			new JPEGEditorDemo();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
