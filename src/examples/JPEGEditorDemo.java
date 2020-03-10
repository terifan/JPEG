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

			_ImageQualityTest result = new _ImageQualityTest(aImage1, aImage2, dst);

			mResult[0].setText("Accum. RGB differance");
			mResult[1].setText("Accum. error");
			mResult[2].setText("MSE");
			mResult[3].setText("PSNR");
			mResult[4].setText("" + result.accumDiff);
			mResult[5].setText("" + result.accumError);
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
