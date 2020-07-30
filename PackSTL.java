import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import png.PngWriter;

public class PackSTL {
    public static void main(String[] args) throws Exception {
        HashMap<String, Vertex> vertexmap = new HashMap<>();
        ArrayList<Vertex> vertexlist = new ArrayList<>();
        ArrayList<Integer> indexlist = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            float nx = 0, ny = 0, nz = 0;
            String line;
            while ((line = br.readLine()) != null) {
                int pos = line.indexOf('#');
                if (pos > -1)
                    line = line.substring(0, pos);
                line = line.trim();
                if (line.startsWith("facet normal")) {
                    String normal[] = line.split("\\s+");
                    nx = Float.parseFloat(normal[2]);
                    ny = Float.parseFloat(normal[3]);
                    nz = Float.parseFloat(normal[4]);
                } else if (line.startsWith("vertex")) {
                    Vertex v = vertexmap.get(line);
                    if (v == null) {
                        String coords[] = line.split("\\s+");
                        v = new Vertex(
                                Float.parseFloat(coords[1]),
                                Float.parseFloat(coords[2]),
                                Float.parseFloat(coords[3]));
                        vertexmap.put(line, v);
                        vertexlist.add(v);
                    }
                    v.update(indexlist.size() / 3, nx, ny, nz);
                    indexlist.add(v.index);
                }
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(('M' << 24) + ('e' << 16) + ('s' << 8) + 'h'); // 0
        dos.writeFloat(Vertex.minx); // 4
        dos.writeFloat(Vertex.maxx); // 8
        dos.writeFloat(Vertex.miny); // 12
        dos.writeFloat(Vertex.maxy); // 16
        dos.writeFloat(Vertex.minz); // 20
        dos.writeFloat(Vertex.maxz); // 24
        dos.writeByte(0); // chunks // 28

        int triangles = indexlist.size() / 3;

        final int remap[] = new int[vertexlist.size()];
        final int mapped[] = new int[65536];
        int next;

        int chunks = 0;
        while (triangles > 0) {
            chunks++;
            Arrays.fill(remap, -1);
            next = 0;

            final ArrayList<Integer> indices = new ArrayList<>();

            while (triangles > 0 && next < 65530) {
                int maxtris = -1;
                int maxidx = -1;
                for (int i = 0; i < vertexlist.size(); i++) {
                    final ArrayList<Integer> tris = vertexlist.get(i).tris;
                    if (tris.size() > maxtris) {
                        maxtris = tris.size();
                        maxidx = i;
                    }
                }

                if (maxtris > 0) {
                    mapped[next] = maxidx;
                    remap[maxidx] = next;
                    next++;

                    int idx = next - 1;

                    while (next < 65530 && idx < next /* && triangles>0 */) {
                        final ArrayList<Integer> tris = vertexlist.get(mapped[idx]).tris;
                        while (tris.size() > 0 && next < 65534) {
                            Integer tri = tris.get(0);
                            for (int i = 0; i < 3; i++) {
                                int vertex = indexlist.get(tri * 3 + i);
                                indexlist.set(tri * 3 + i, null);
                                if (remap[vertex] == -1) {
                                    mapped[next] = vertex;
                                    remap[vertex] = next;
                                    next++;
                                }
                                indices.add(remap[vertex]);
                                if (!vertexlist.get(vertex).tris.remove(tri))
                                    throw new Exception("Should not happen - attempting to remove nonexisting triangle");
                            }
                            triangles--;
                        }
                        idx++;
                    }
                }
            }

            dos.writeShort(next);
            for (int i = 0; i < next; i++) {
                final Vertex v = vertexlist.get(mapped[i]);
                dos.writeFloat(v.x);
                dos.writeFloat(v.y);
                dos.writeFloat(v.z);
                dos.writeFloat(v.nx);
                dos.writeFloat(v.ny);
                dos.writeFloat(v.nz);
            }
            dos.writeInt(indices.size());
            for (Integer i : indices) {
                dos.writeShort(i);
            }
        }
        dos.close();
        byte data[] = baos.toByteArray();
        data[28] = (byte) chunks;
        byte line[] = new byte[4096 * 3];
        int lines = (data.length + line.length - 1) / line.length;
        data = Arrays.copyOf(data, line.length * lines);
        try (FileOutputStream fos = new FileOutputStream(args[1])) {
            PngWriter png = new PngWriter(fos, 4096, lines, PngWriter.TYPE_TRUECOLOR, null);
            for (int i = 0; i < lines; i++) {
                System.arraycopy(data, i * line.length, line, 0, line.length);
                png.writeline(line);
            }
        }
    }

    static class Vertex {
        static float minx = Float.MAX_VALUE;
        static float miny = Float.MAX_VALUE;
        static float minz = Float.MAX_VALUE;
        static float maxx = -Float.MAX_VALUE;
        static float maxy = -Float.MAX_VALUE;
        static float maxz = -Float.MAX_VALUE;

        static private int next = 0;
        final int index = next++;
        final float x, y, z;
        float nx, ny, nz;
        ArrayList<Integer> tris = new ArrayList<>();

        Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            if (x < minx)
                minx = x;
            if (y < miny)
                miny = y;
            if (z < minz)
                minz = z;
            if (x > maxx)
                maxx = x;
            if (y > maxy)
                maxy = y;
            if (z > maxz)
                maxz = z;
        }

        void update(int tri, float nx, float ny, float nz) {
            this.nx += nx;
            this.ny += ny;
            this.nz += nz;
            tris.add(tri);
        }

        void docalc() {
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= len;
            ny /= len;
            nz /= len;
        }
    }
}
