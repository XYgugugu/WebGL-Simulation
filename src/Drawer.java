import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Drawer {
    private class Vector {
        // x,y,z,w,X,Y,R,G,B,A;
        double[] vector;
        //Concatenate position and color vector into one
        Vector(double[] pos, double[] color, boolean hyp) {
            vector = new double[pos.length+color.length];
            int i = 0;
            // Perform a separate operation for hyperbolic mode
            if (hyp == true) {
                double w = pos[3];
                for (double d : pos) {
                    vector[i++] = d;
                }
                
                for (double d : color) {
                    vector[i++] = d/w;
                }
                vector[2] /= w;
                vector[3] = 1/w;
            } else {
                for (double d : pos) {
                    vector[i++] = d;
                }
                for (double d : color) {
                    vector[i++] = d;
                }
            }
        }
        //Copy constructor
        Vector(Vector other) {
            vector = Arrays.copyOf(other.vector, other.vector.length);
        }
        
        //Getter
        public double GetDimension(int d) {
            assert d < vector.length;
            return vector[d];
        }

        @Override
        public String toString() {
            return "Vector{" + "values=" + Arrays.toString(vector) + "}";
            // return Double.toString(vector[5]);
        }
        public String XYtoString() {
            return String.format("(%f, %f)", vector[4], vector[5]);
        }
        public String RGBtoString() {
            return String.format("(rgb = (%f, %f, %f))", vector[6], vector[7], vector[8]);
        }
        // Basic Math
        public void add(double[] v) {
            assert vector.length == v.length;
            for (int i = 0; i < vector.length; i++) {
                vector[i] += v[i];
            }
        }
        public void add(Vector p) {
            add(p.vector);
        }
        public void sub(double[] v) {
            assert vector.length == v.length;
            for (int i = 0; i < vector.length; i++) {
                vector[i] -= v[i];
            }
        }
        public void sub(Vector p) {
            sub(p.vector);
        }
        public void multiply(double d) {
            assert d != 0;
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= d;
            }
        }
        public void divide(double d) {
            assert d != 0;
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= d;
            }
        }
        public static double compare(Vector a, Vector b, int d) {
            return a.vector[d] - b.vector[d];
        }
    }
    private int width, height;
    private Flags flags;
    /**
     * This region is specifically for:         drawTriangles
     */
    private Vector t, b, m;
    // Assign three vertex in order based on Y-coordinate value
    Drawer(Flags f, int w, int h, double[] pos0, double[] pos1, double[] pos2, double[] color0, double[] color1, double[] color2) {
        flags = f;
        width = w;
        height = h;

        // Find three Vectors in order
        Vector x = new Vector(pos0, color0, f.hyp);
        Vector y = new Vector(pos1, color1, f.hyp);
        Vector z = new Vector(pos2, color2, f.hyp);
        
        //find top-mid-bot vertex based on their Y-coordinate
        if (Vector.compare(x, y, 5) < 0 && Vector.compare(x, z, 5) < 0) {
            t = x;
            if (Vector.compare(y, z, 5) < 0) {
                m = y;
                b = z;
            } else {
                m = z;
                b = y;
            }
        } else if (Vector.compare(y, z, 5) < 0) {
            t = y;
            if (Vector.compare(x, z, 5) < 0) {
                m = x;
                b = z;
            } else {
                m = z;
                b = x;
            }
        } else {
            t = z;
            if (Vector.compare(x, y, 5) < 0) {
                m = x;
                b = y;
            } else {
                m = y;
                b = x;
            }
        }
    
    }
    public void drawTriangles(WritableRaster raster) {
        //dimension for Y-pos
        int d = 5;
        //DDA t to b:
        Vector plong = null, slong = null;
        Vector[] pslong;
        if ((pslong = DDA17(t, b, d)) == null) return;
        plong = pslong[0];
        slong = pslong[1];
        //Find Vectors in the top half of the triangle:
        //DDA t to m
        Vector p = null, s = null;
        Vector[] ps;
        if ((ps = DDA17(t, m, d)) != null) {
            // there is a top half triangle
            p = ps[0];
            s = ps[1];
            DDA8(p, s, plong, slong, m.GetDimension(d), raster);
        }
        //Find points in the bottom half of the triangle:
        //DDA m to b
        if ((ps = DDA17(m, b, d)) != null) {
            // there is a bottom half triangle
            p = ps[0];
            s = ps[1];
            DDA8(p, s, plong, slong, b.GetDimension(d), raster);
        }
    }
    /**
     * Run DDA step 1-7 
     * https://cs418.cs.illinois.edu/website/text/dda.html 
     * @return null if no point found, otherwise Vector[] of p and s
     */
    private Vector[] DDA17(Vector a, Vector b, int dimension) {
        // Step 1
        if (Vector.compare(a, b, dimension) == 0) return null;
        // Step 2
        if (Vector.compare(a, b, dimension) > 0) {
            //swap
            Vector temp = a;
            a = b;
            b = temp;
        }
        // Step 3
        Vector s = new Vector(b);
        s.sub(a);
        // Step 4
        s.divide(s.GetDimension(dimension));
        // Step 5
        double e = Math.ceil(a.GetDimension(dimension)) - a.GetDimension(dimension);
        // Step 6
        Vector p = new Vector(s);
        p.multiply(e);
        // Step 7
        p.add(a);
        return new Vector[] {p, s};
    }

    /**
     * Run DDA loop step 8
     * https://cs418.cs.illinois.edu/website/text/dda.html 
     */
    private void DDA8(Vector p, Vector s, Vector plong, Vector slong, double endPoint, WritableRaster raster) {
        int X = 4, Y = 5;
        while (p.GetDimension(Y) < endPoint) {
            // System.out.println("DDA8: " + plong.XYtoString() + plong.RGBtoString() + " to " + p.XYtoString() + p.RGBtoString());
            DDADraw(p, plong, X, raster);
            p.add(s);
            plong.add(slong);
        }
        return;
    }
    /**
     * Draw pixels
     */
    private void DDADraw(Vector a, Vector b, int xDimension, WritableRaster raster) {
        Vector[] ps = DDA17(a, b, xDimension);
        if (ps == null) return;
        double endPoint = Math.max(a.GetDimension(xDimension), b.GetDimension(xDimension));
        while (ps[0].GetDimension(xDimension) < endPoint) {
            // System.out.println(String.format("(x,y) = (%f, %f)", ps[0].GetDimension(xDimension), ps[0].GetDimension(xDimension + 1)));
            int x = (int) ps[0].GetDimension(xDimension);
            int y = (int) ps[0].GetDimension(xDimension + 1);
            if (x < 0 || x >= width || y < 0 || y >= height) {
                ps[0].add(ps[1]);
                continue;
            }
            //if <depth> is enabled, ignore pixels with same (x,y), but further distance (z)
            if (flags.depth) {
                Double currentZ = ps[0].GetDimension(2);
                int xy = y * width + x;
                // System.out.println("xy: " + xy);
                Double recordedZ = flags.depthMap.get(xy);
                if (recordedZ != null && recordedZ < currentZ) {
                    ps[0].add(ps[1]);
                    continue;
                }
                flags.depthMap.put(xy, currentZ);
            }
            double WPrime = ps[0].GetDimension(xDimension - 1);
            RGB rgb = new RGB(
                                ps[0].GetDimension(xDimension + 2), 
                                ps[0].GetDimension(xDimension + 3), 
                                ps[0].GetDimension(xDimension + 4), 
                                flags.color == 3 ? 1.0 : ps[0].GetDimension(xDimension + 5),
                                flags.hyp,
                                WPrime,
                                this
                            );
            if (flags.color == 3) {
                raster.setPixel(x, y, new double[]{rgb.R * 255.0, rgb.G * 255.0, rgb.B * 255.0, 255.0});
            } else {
                //handle transparency
                int xy = y * width + x;
                RGB base = flags.rgbMap.get(xy);
                if (base == null) {
                    //no existing base
                    flags.rgbMap.put(xy, rgb);
                    raster.setPixel(x, y, new double[]{rgb.R * 255.0, rgb.G * 255.0, rgb.B * 255.0, rgb.A * 255.0});
                } else {
                    //overlap
                    double newA = rgb.A + base.A * (1 - rgb.A);
                    RGB over = RGB.combine(rgb, base, newA);
                    flags.rgbMap.put(xy, over);
                    raster.setPixel(x, y, new double[]{over.R * 255.0, over.G * 255.0, over.B * 255.0, over.A * 255.0});
                }
            }
            ps[0].add(ps[1]);
        }
    }
    /**
     * Called to perform Gamma Correction on a color value
     * Gamma Correction will only be done if and only if Flags.sRGB == true
     * @param value the color value in range of 0.0-1.0
     * @return returns the corrected color value as a double ranging 0.0-1.0
     */
    public double GammaCorection(double value) {
        if (flags.sRGB == false) return value;
        // if (value <= 0.04045) 
        // {
        //     return (value / 12.92);
        // } else 
        // {
        //     return (Math.pow(((value + 0.055) / 1.055), 2.4));
        // }
        if (value <= 0.0031308) 
        {
            return 12.92 * value;
        } else 
        {
            return 1.055 * Math.pow(value, 1/2.4) - 0.055;
        }
    }
}