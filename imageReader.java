import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class imageReader {
	public final int WIDTH = 352;
	public final int HEIGHT = 288;	
	public final int TOTAL = WIDTH*HEIGHT;
	public final double PI = 3.14;
	final double root2 = 1/Math.sqrt(2.0);
	
	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img;
	BufferedImage OutImg;
	
	private double[] R, Rsp;
	private double[] G, Gsp;
	private double[] B, Bsp;
	private short[] Rshort, Gshort, Bshort;
	
	private int QLevel;
	private int Latency;
	private int mode;
	
	
	private double[] together() {
		double[] ret = new double[TOTAL*3];
		for(int i = 0; i < TOTAL; i++) {
			ret[i] = Math.round(R[i]);
			ret[i+TOTAL] = Math.round(G[i]);
			ret[i+TOTAL*2] = Math.round(B[i]);
			
			//change 0~255 to -128 ~ 127
			ret[i] = (ret[i] >= 128.0 ? ret[i] - 256.0 : ret[i]);
			ret[i+TOTAL] = (ret[i+TOTAL] >= 128.0 ? ret[i+TOTAL] - 256.0 : ret[i+TOTAL]);
			ret[i+TOTAL*2] = (ret[i+TOTAL*2] >= 128.0 ? ret[i+TOTAL*2] - 256.0 : ret[i+TOTAL*2]);
			
			//boundary check
			ret[i] = ret[i] > 127.0 ? 127.0: (ret[i] < -128.0 ? -128.0 : ret[i]);
			ret[i+TOTAL] = ret[i+TOTAL] > 127.0 ? 127: (ret[i+TOTAL] < -128.0 ? -128.0 : ret[i+TOTAL]);
			ret[i+TOTAL*2] = ret[i+TOTAL*2] > 127.0 ? 127.0: (ret[i+TOTAL*2] < -128.0 ? -128.0 : ret[i+TOTAL*2]);
		}
		return ret;
	}
	
	private void getOneBlock() {
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				int idx = i * WIDTH + j;
				System.out.print(Math.round(R[idx])+", ");
			}
			System.out.println();
		}
	}
	
	private void fillBlock(double[] channel, double[][] block, int x, int y) {
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				int idx = (x+i)*WIDTH + y+j;
				channel[idx] = Math.round(block[i][j]);

				channel[idx] = channel[idx] > 255.0 ? 255.0: (channel[idx] < 0.0 ? 0.0 : channel[idx]);
				channel[idx] = (channel[idx] >= 128.0 ? channel[idx] - 256.0 : channel[idx]);
				channel[i] = channel[i] > 127.0 ? 127.0: (channel[i] < -128.0 ? -128.0 : channel[i]);
				
			}
		}
	}
	
	private void updateImageBaseLine(int x, int y) {
		for(int i = 0; i < 8; i++)
			for(int j = 0; j < 8; j++) {
				int Y = x+i;
				int X = y+j;
				int idx = (x+i)*WIDTH + y+j;
				byte r = (byte)R[idx];
				byte g = (byte)G[idx];
				byte b = (byte)B[idx];
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				OutImg.setRGB(X,Y,pix);
			}
			lbIm2.setIcon(new ImageIcon(OutImg));
		try {
			Thread.sleep(Latency);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("thread exception for sleep");
			e.printStackTrace();
		}
	}
	
	private void parseIntoRGB(byte[] rgb) {
		R = new double[TOTAL];
		G = new double[TOTAL];
		B = new double[TOTAL];
		for(int i = 0; i < TOTAL; i++) {
			double r = (double)rgb[i];
			double g = (double)rgb[i+TOTAL];
			double b = (double)rgb[i+2*TOTAL];
			
			//change range from -128.0 ~ 127 to 0~255
			r = r < 0 ? r + 256.0 : r;
			g = g < 0 ? g + 256.0 : g;
			b = b < 0 ? b + 256.0 : b;
			R[i] = r;
			G[i] = g;
			B[i] = b;
		}
	}
	
	
	private double[][] blockIDCT(double[] channel, int i, int j){
		double[][] ret = new double[8][8];
		for(int x = 0; x < 8; x++) {
			for(int y = 0; y < 8; y++) {
				
				double coeff = 0.0;
				for(int u = 0; u < 8; u++) {
					double c1 = Math.cos((2*x+1)*u*PI/16);
					double CU = u == 0 ? root2 : 1.0;
					for(int v = 0; v < 8; v++) {
						int idx = (i+u)*WIDTH + (j+v);
						double CV = v == 0 ? root2 : 1.0;
						coeff += CU*CV * channel[idx] * c1 * Math.cos((2*y+1)*v*PI/16);
					}
				}
				ret[x][y]= coeff / 4.0;
			}
		}
		return ret;
	}
	
	private void IDCTSeq() {
		for(int i = 0; i < HEIGHT; i += 8) {
			for(int j = 0; j < WIDTH; j += 8) {
				double[][] blockR = blockIDCT(R, i, j);
				double[][] blockG = blockIDCT(G, i, j);
				double[][] blockB = blockIDCT(B, i, j);
				
				fillBlock(R, blockR, i, j);
				fillBlock(G, blockG, i, j);
				fillBlock(B, blockB, i, j);
			
				updateImageBaseLine(i, j);
			}
		}
	}
	
	private double[] IDCTForChannel(double[] dct) {
		double[] ret = new double[TOTAL];
		for(int i = 0; i < HEIGHT; i++)
			for(int j = 0; j < WIDTH; j++) {
				int c_idx = i*WIDTH + j;
				int x = i % 8;
				int y = j % 8;
				double coeff = 0.0d;
				int startU = i - x;
				int startV = j - y;
				for(int u = 0; u < 8; u++) {
					double c1 = Math.cos((2*x+1)*u*PI/16);
					double CU = u == 0 ? root2 : 1.0;
					for(int v = 0; v < 8; v++) {
						double CV = v == 0 ? root2 : 1.0;
						int idx = (startU + u)*WIDTH + startV+v;
						coeff += CU*CV * dct[idx] * c1 * Math.cos((2*y+1)*v*PI/16);
					}
				}
				ret[c_idx] = Math.round(coeff / 4.0);
			}
		return ret;
	}
	
	private void showImage(double[] Rt, double[] Gt, double[] Bt) {
		double r, g, b;
		int idx = 0;
		for(int x = 0; x < HEIGHT; x++) {
			for(int y = 0; y < WIDTH; y++) {
				r = Math.round(Rt[idx]);
				g = Math.round(Gt[idx]);
				b = Math.round(Bt[idx]);
				
				//limit the rgb value to 0~255
				r = r > 255.0 ? 255.0: (r < 0.0 ? 0.0 : r);
				g = g > 255.0 ? 255: (g < 0.0 ? 0.0 : g);
				b = b > 255.0 ? 255.0: (b < 0.0 ? 0.0 : b);
				
				//change 0~255 to -128 ~ 127
				r = (r >= 128.0 ? r - 256.0 : r);
				g = (g >= 128.0 ? g - 256.0 : g);
				b = (b >= 128.0 ? b - 256.0 : b);
				
				//boundary check
				r = r > 127.0 ? 127.0: (r < -128.0 ? -128.0 : r);
				g = g > 127.0 ? 127: (g < -128.0 ? -128.0 : g);
				b = b > 127.0 ? 127.0: (b < -128.0 ? -128.0 : b);
				
				byte a = 0;
				byte rb = (byte)r;
				byte gb = (byte)g;
				byte bb = (byte)b; 

				int pix = 0xff000000 | ((rb & 0xff) << 16) | ((gb & 0xff) << 8) | (bb & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				OutImg.setRGB(y,x,pix);
				
				idx++;
			}
		}
		lbIm2.setIcon(new ImageIcon(OutImg));
	}
	
	private void IDCTforCurInfo(int i, int j) {
		//for spectral selection
		for(int x = 0; x < HEIGHT; x += 8 ) {
			for(int y = 0; y < WIDTH; y += 8) {
				int idx = (x+i) * WIDTH + j + y;
				Rsp[idx] = R[idx];
				Gsp[idx] = G[idx];
				Bsp[idx] = B[idx];
			}
		}
		double[] Rt = IDCTForChannel(Rsp);
		double[] Gt = IDCTForChannel(Gsp);
		double[] Bt = IDCTForChannel(Bsp);
		showImage(Rt, Gt, Bt);
	}
	
	private void IDCTSpec() {
		Rsp = new double[TOTAL];
		Gsp = new double[TOTAL];
		Bsp = new double[TOTAL];
		int i = 0, j = 0, deltaI = 0, deltaJ = 0;;
		while(i < 8 && j < 8) {
			//System.out.println(i+","+j);
			IDCTforCurInfo(i, j);
			try {
				Thread.sleep(Latency);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(i == 0 || i == 7) {
				if(j%2 == 0) {
					j++;
					continue;
				}else {
					if(i == 0) {
						i++;
						j--;
						deltaI = 1;
						deltaJ = -1;
					}else {
						i--;
						j++;
						deltaI = -1;
						deltaJ = 1;
					}
					continue;
				}
			}
			if(j == 0 || j == 7) {
				if(i % 2 == 1) {
					i++;
					continue;
				}else {
					if(j == 0) {
						i--;
						j++;
						deltaI = -1;
						deltaJ = 1;
					}else {
						i++;
						j--;
						deltaI = 1;
						deltaJ = -1;
					}
					continue;
				}
			}
			i += deltaI;
			j += deltaJ;
		}
	}
	
	private void IDCTBits() {
		Rshort = new short[TOTAL];
		Gshort = new short[TOTAL];
		Bshort = new short[TOTAL];
		Rsp = new double[TOTAL];
		Gsp = new double[TOTAL];
		Bsp = new double[TOTAL];
		for(int bp = 15; bp > -1; bp--) {
			boolean update = false;
			short mode = (short)Math.pow(2, bp);
			for(int i = 0; i < TOTAL; i++) {
				short r = (short)R[i];
				short g = (short)G[i];
				short b = (short)B[i];
				
				if((r & mode)!= 0) {
					Rshort[i] |= mode;
					Rsp[i] = (double)Rshort[i];
					update = true;
				}
				if((g & mode)!= 0) {
					Gshort[i] |= mode;
					Gsp[i] = (double)Gshort[i];
					update = true;
				}
				if((b & mode)!= 0) {
					Bshort[i] |= mode;
					Bsp[i] = (double)Bshort[i];
					update = true;
				}
			}
			if(update) {
				double[] Rt = IDCTForChannel(Rsp);
				double[] Gt = IDCTForChannel(Gsp);
				double[] Bt = IDCTForChannel(Bsp);
				showImage(Rt, Gt, Bt);
                try {
                    Thread.sleep(Latency);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    System.out.println("thread exception for sleep");
                    e.printStackTrace();
                }
			}
		}
	}
	
	private void deQuantize(double[] quan) {
		for(int i = 0; i < TOTAL; i++) {
			quan[i] *= Math.pow(2, QLevel);
		}
	}
	
	public void decode() {
		deQuantize(R);
		deQuantize(G);
		deQuantize(B);
		if(mode == 1)
			IDCTSeq();
		else if(mode == 2)
			IDCTSpec();
		else if(mode == 3) {
			
			IDCTBits();
			//showImage(R, G, B);
		}
		
	}
	
	private double[] DCT(double[] channel) {
		double[] ret = new double[TOTAL];
		for(int i = 0; i < HEIGHT; i ++) {
			for(int j = 0; j < WIDTH; j++) {
				int c_idx = i * WIDTH + j;
				
				int u = i % 8;
				int v = j % 8;
				double CU = u == 0 ? root2 : 1.0;
				double CV = v == 0 ? root2 : 1.0;
				double CUV = CU*CV;
				double coeff = 0.0f;
				int startX = i - u;
				int startY = j - v;
				for(int x = 0; x < 8; x++) {
					double c1 = Math.cos((2*x+1)*u*PI/16);
					for(int y = 0; y < 8; y++) {
						int curX = startX +x;
						int curY = startY +y;
						int idx = curX*WIDTH + curY;
						
						double c2 = Math.cos((2*y+1)*v*PI/16);
						//double cur = channel[idx] * c1 * c2;
						coeff += channel[idx] * c1 * c2;
					}
				}
				ret[c_idx] = coeff * CUV / 4.0;
			}
		}
		return ret;
	}
	
	private void Quantize(double[] channel) {
		int base = (int)Math.pow(2, QLevel);
		for(int i = 0; i < TOTAL; i++) {
			channel[i] = Math.round(channel[i]/base);
		}
	}
	
	public void encode(byte[] rgb) {
		parseIntoRGB(rgb);
		//System.out.println("BEFORE DCT: "+R[3]);
		R = DCT(R);
		Quantize(R);
		G = DCT(G);
		Quantize(G);
		B = DCT(B);
		Quantize(B);
	}
	
	
	public void showIms(String[] args){

		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		OutImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		QLevel = Integer.parseInt(args[1]);
        if(QLevel < 0 || QLevel > 7){
            System.out.println("quantization level is not valid! please check and run again!");
            return;
        }
		mode = Integer.parseInt(args[2]);
        if(mode < 1 || mode > 4){
            System.out.println("mode level is not valid! please check and run again!");
            return;
        }

		Latency = Integer.parseInt(args[3]);
        if(mode < 0){
            System.out.println("latency is not valid! please check and run again!");
            return;
        }
		//QLevel = 0;
		try {
			/*String filePath = "src/"+args[0];
			File file = new File(filePath);*/
			
			File file = new File(args[0]);
			
			InputStream is = new FileInputStream(file);

			long len = file.length();
			byte[] bytes = new byte[(int)len];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}


			int ind = 0;
			for(int y = 0; y < HEIGHT; y++){

				for(int x = 0; x < WIDTH; x++){

					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+HEIGHT*WIDTH];
					byte b = bytes[ind+HEIGHT*WIDTH*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
			
			
			encode(bytes);
			
			
			ind = 0;
			for(int y = 0; y < HEIGHT; y++){
				for(int x = 0; x < WIDTH; x++){

					byte a = 0;
					byte r = 127;
					byte g = 127;
					byte b = 127; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					OutImg.setRGB(x,y,pix);
					ind++;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(img));
		lbIm2 = new JLabel(new ImageIcon(OutImg));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
		
		decode();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		imageReader ren = new imageReader();
		ren.showIms(args);
	}
	
}
