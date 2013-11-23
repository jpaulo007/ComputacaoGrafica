package pt.ipb.esact.compgraf.engine.obj;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

import pt.ipb.esact.compgraf.tools.GlTools;
import pt.ipb.esact.compgraf.tools.math.Color;
import pt.ipb.esact.compgraf.tools.math.GlMath;
import pt.ipb.esact.compgraf.tools.math.Vector;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class Obj {

	private static final int VERTS_DATA = 0;
	
	private static final int NORMS_DATA = 1;
	
	private static final int TEXES_DATA = 2;
	
	private static final int OBJ_BUFFER_COUNT = 3;
	
	private Multimap<String, Integer> triIndexes = ArrayListMultimap.create();
	
	private Map<String, Integer> triBufferObjects = Maps.newHashMap();
	
	private Multimap<String, Integer> quadIndexes = ArrayListMultimap.create();
	
	private Map<String, Integer> quadBufferObjects = Maps.newHashMap();
	
	private Map<String, ObjMaterial> material = Maps.newHashMap();
	
	private Map<String, String> objMaterial = Maps.newHashMap();
	
	private List<String> objNames = Lists.newArrayList();
	
	private Map<String, Boolean> shadeModel = Maps.newHashMap();
	
	private List<Vector> vertices = Lists.newArrayList();
	
	private List<Vector> normals = Lists.newArrayList();
	
	private List<Vector> texcoords = Lists.newArrayList();

	private IntBuffer vboIds = IntBuffer.allocate(OBJ_BUFFER_COUNT);
	
	private float scale = 1.0f;
	
	private boolean compress = false;
	
	private Vector bbMax = new Vector();
	
	private Vector bbMin = new Vector();
	
	private static final Pattern MAT_LINE_PATTERN = Pattern.compile("([^ ]*)[ ]+([^ ]*)(|[ ]+([^ ]*))(|[ ]+([^ ]*))");
	
	private static final Pattern ro = Pattern.compile("^o[ ]+(.+)$");
	private static final Pattern rf = Pattern.compile("^f[ ]+(.+)$");
	private static final Pattern rv = Pattern.compile("^v[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)");
	private static final Pattern rvt = Pattern.compile("^vt[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)");
	private static final Pattern rvt3 = Pattern.compile("^vt[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)");
	private static final Pattern rvn = Pattern.compile("^vn[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)[ ]+([\\-0-9.]+)");
	private static final Pattern rs = Pattern.compile("^s[ ]+(off|\\d+)$");
	private static final Pattern rusemtl = Pattern.compile("^usemtl[ ]+(.+)$");

	public Obj() {
	}
	
	public void load(Object reference, String model, String material) {
		if(reference == null)
			throw new GLException("The reference object cannot be null");
		
		// extract prefix from model
		String prefix = "";
		int last = model.lastIndexOf(File.separatorChar);
		if(last != -1)
			prefix = model.substring(0, last) + "/";
		
		try (
			BufferedReader modelStream = new BufferedReader(new InputStreamReader(reference.getClass().getResourceAsStream(model)));
			BufferedReader materialStream = new BufferedReader(new InputStreamReader(reference.getClass().getResourceAsStream(material)));
		) {
			String line = null;

			List<String> materialLines = Lists.newArrayList();
			while((line = materialStream.readLine()) != null)
				materialLines.add(line);
			parseMaterial(reference, materialLines, prefix);

			List<String> modelLines = Lists.newArrayList();
			while((line = modelStream.readLine()) != null)
				modelLines.add(line);
			parse(reference, modelLines, prefix);
		} catch (IOException e) {
			GlTools.exit(e.getMessage());
		}
	}
	
	private void parseMaterial(Object reference, List<String> lines, String prefix) {
		material.clear();
		
		String currentMtl = null;
		
		for(String line : lines) {
			Matcher m = MAT_LINE_PATTERN.matcher(line);
			if(!m.matches())
				continue;
			
			String prop = m.group(1);
			
			if("newmtl".equals(prop)) {
				currentMtl = m.group(2);
				material.put(currentMtl, new ObjMaterial(currentMtl));
			}
			
			if(Strings.isNullOrEmpty(currentMtl))
				continue;
			
			if("Ka".equals(prop)) {
				float r = GlMath.clamp(Float.parseFloat(m.group(2)), 0.0f, 1.0f);
				float g = GlMath.clamp(Float.parseFloat(m.group(4)), 0.0f, 1.0f);
				float b = GlMath.clamp(Float.parseFloat(m.group(6)), 0.0f, 1.0f);
				material.get(currentMtl).setKa(r, g, b);
			}

			if("Kd".equals(prop)) {
				float r = GlMath.clamp(Float.parseFloat(m.group(2)), 0.0f, 1.0f);
				float g = GlMath.clamp(Float.parseFloat(m.group(4)), 0.0f, 1.0f);
				float b = GlMath.clamp(Float.parseFloat(m.group(6)), 0.0f, 1.0f);
				material.get(currentMtl).setKd(r, g, b);
			}

			if("Ks".equals(prop)) {
				float r = GlMath.clamp(Float.parseFloat(m.group(2)), 0.0f, 1.0f);
				float g = GlMath.clamp(Float.parseFloat(m.group(4)), 0.0f, 1.0f);
				float b = GlMath.clamp(Float.parseFloat(m.group(6)), 0.0f, 1.0f);
				material.get(currentMtl).setKs(r, g, b);
			}
			
			if("map_Kd".equals(prop)) {
				String value = m.group(2);
				material.get(currentMtl).setMapKd(reference, prefix, value);
			}

			if("map_Bump".equals(prop)) {
				String value = m.group(2);
				material.get(currentMtl).setMapBump(reference, prefix, value);
			}

			if("d".equals(prop)) {
				String value = m.group(2);
				material.get(currentMtl).setD(Float.parseFloat(value));
			}
			
			if("Ns".equals(prop)) {
				String value = m.group(2);
				material.get(currentMtl).setNs(Float.parseFloat(value));
			}
		}
	}

	private void parse(Object reference, List<String> lines, String prefix) {
		vertices.clear();
		texcoords.clear();
		normals.clear();

		triIndexes.clear();
		quadIndexes.clear();

		triBufferObjects.clear();
		quadBufferObjects.clear();

		objNames.clear();

		shadeModel.clear();
		
		GL2 gl = GlTools.getGL2();
		
		List<Vector> verts = Lists.newArrayList();
		List<Vector> norms = Lists.newArrayList();
		List<Vector> texes = Lists.newArrayList();
		
		gl.glGenBuffers(OBJ_BUFFER_COUNT, vboIds);
		
		String oname = "root";
		for(String line : lines) {
			Matcher m;
			
			m = ro.matcher(line);
			if(m.matches()) {
				oname = m.group(1);
				addObject(oname);
			}
			
			m = rv.matcher(line);
			if(m.matches()) {
				verts.add(new Vector(
					Float.parseFloat(m.group(1)) * scale,
					Float.parseFloat(m.group(2)) * scale,
					Float.parseFloat(m.group(3)) * scale
				));
			}
			
			m = rvt.matcher(line);
			if(m.matches()) {
				texes.add(new Vector(
					Float.parseFloat(m.group(1)) * scale,
					Float.parseFloat(m.group(2)) * scale,
					0.0f
				));
			}
			
			m = rvt3.matcher(line);
			if(m.matches()) {
				texes.add(new Vector(
					Float.parseFloat(m.group(1)) * scale,
					Float.parseFloat(m.group(2)) * scale,
					0.0f
				));
			}
			
			m = rvn.matcher(line);
			if(m.matches()) {
				norms.add(new Vector(
					Float.parseFloat(m.group(1)) * scale,
					Float.parseFloat(m.group(2)) * scale,
					Float.parseFloat(m.group(3)) * scale
				));
			}
			
			m = rs.matcher(line);
			if(m.matches())
				shadeModel.put(oname, Integer.parseInt(m.group(1)) == 1);
			
			m = rusemtl.matcher(line);
			if(m.matches())
				objMaterial.put(oname, m.group(1));
			
			m = rf.matcher(line);
			if(m.matches()) {
				List<String> tokens = Lists.newArrayList(Splitter.onPattern("[ ]+").split(m.group(1)));
				int vertexCount = tokens.size();
				
				Vector fVerts[] = new Vector[vertexCount];
				Vector fNorms[] = new Vector[vertexCount];
				Vector fTexes[] = new Vector[vertexCount];
				
				for(int f=0; f<tokens.size(); f++) {
					List<String> ns = Lists.newArrayList(Splitter.onPattern("/").split(tokens.get(f)));
					try {
						int vx = Integer.parseInt(ns.get(0));
						fVerts[f] = verts.get(vx-1);
					} catch (NumberFormatException e) {
						fVerts[f] = new Vector();
					}
					try {
						int tx = Integer.parseInt(ns.get(1));
						fTexes[f] = texes.get(tx-1);
					} catch (NumberFormatException e) {
						fTexes[f] = new Vector();
					}
					try {
						int nx = Integer.parseInt(ns.get(2));
						fNorms[f] = norms.get(nx-1);
					} catch (NumberFormatException e) {
						fNorms[f] = new Vector();
					}
				}
				
				addFace(fVerts, fNorms, fTexes, vertexCount, oname);
			}
		}
		
		if(objNames.isEmpty())
			addObject("root");
		
		FloatBuffer vertsPtr = vertsPointer();
		FloatBuffer normsPtr = normsPointer();
		FloatBuffer texesPtr = texesPointer();
		
		final int FLOAT_SIZE = Float.SIZE / 8;
		final int INT_SIZE = Integer.SIZE / 8;
		
		// Generate the Buffers
		// Vertex Data
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(VERTS_DATA));
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, FLOAT_SIZE * vertices.size() * 3, vertsPtr, GL2.GL_STATIC_DRAW);

		// Texture coordinates
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(TEXES_DATA));
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, FLOAT_SIZE * texcoords.size() * 2, texesPtr, GL2.GL_STATIC_DRAW);

		// Normal data
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(NORMS_DATA));
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, FLOAT_SIZE * normals.size() * 3, normsPtr, GL2.GL_STATIC_DRAW);

		// Triangle Indexes
		for(String o : triBufferObjects.keySet()) {
			int b = triBufferObjects.get(o);
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, b);
			Collection<Integer> idxList = triIndexes.get(o);
			IntBuffer data = IntBuffer.allocate(idxList.size());
			for(int idx : idxList)
				data.put(idx);
			gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, INT_SIZE * triIndexes.get(o).size(), data, GL2.GL_STATIC_DRAW);
		}

		// Quad Indexes
		for(String o : quadBufferObjects.keySet()) {
			int b = quadBufferObjects.get(o);
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, b);
			Collection<Integer> idxList = quadIndexes.get(o);
			IntBuffer data = IntBuffer.allocate(idxList.size());
			for(int idx : idxList)
				data.put(idx);
			gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, INT_SIZE * quadIndexes.get(o).size(), data, GL2.GL_STATIC_DRAW);
		}

		System.out.println("vertices = " + vertices.size());
		System.out.println("normals = " + normals.size());		
		System.out.println("texes = " + texes.size());		
	}

	private FloatBuffer vertsPointer() {
		FloatBuffer ptr = FloatBuffer.allocate(vertices.size() * 3);
		for(int i=0; i<vertices.size(); i++) {
			Vector v = vertices.get(i);
			ptr.put(3 * i + 0, v.x);
			ptr.put(3 * i + 1, v.y);
			ptr.put(3 * i + 2, v.z);
		}
		return ptr;
	}

	private FloatBuffer normsPointer() {
		FloatBuffer ptr = FloatBuffer.allocate(normals.size() * 3);
		for(int i=0; i<normals.size(); i++) {
			Vector v = normals.get(i);
			ptr.put(3 * i + 0, v.x);
			ptr.put(3 * i + 1, v.y);
			ptr.put(3 * i + 2, v.z);
		}
		return ptr;
	}
	
	private FloatBuffer texesPointer() {
		FloatBuffer ptr = FloatBuffer.allocate(texcoords.size() * 2);
		for(int i=0; i<texcoords.size(); i++) {
			Vector v = texcoords.get(i);
			ptr.put(2 * i + 0, v.x);
			ptr.put(2 * i + 1, v.y);
		}
		return ptr;
	}

	private void addFace(Vector[] verts, Vector[] norms, Vector[] texes, int count, String o) {
		if(count < 3 || count > 4) {
			System.out.println(count + " not supported");
			return;
		}

		for(int i=0; i<count; i++)
			norms[i].normalize();

		final float e = 0.00001f;

		for(int i=0; i<count; i++) {
			Vector v = verts[i];
			Vector t = texes[i];
			Vector n = norms[i];

			int matchIdx = 0;
			if(compress) {
				for(matchIdx = 0; matchIdx < vertices.size(); matchIdx++) {
					if(
						v.sub(vertices.get(matchIdx)).lengthSquared() <= e &&
						n.sub(normals.get(matchIdx)).lengthSquared() <= e &&
						t.sub(texcoords.get(matchIdx)).lengthSquared() <= e
					) { // there's a match
						boolean match = false;
						if(count == 3) { triIndexes.get(o).add(matchIdx); match=true; }
						if(count == 4) { quadIndexes.get(o).add(matchIdx); match=true; }
						if(match)
							break;
					}
				}
			}

			// No match
			if(matchIdx == vertices.size() || !compress) {
				if(count == 3) triIndexes.get(o).add(vertices.size());
				if(count == 4) quadIndexes.get(o).add(vertices.size());
				vertices.add(v);
				normals.add(n);
				texcoords.add(t);

				if(v.x > bbMax.x) bbMax.x = v.x;
				if(v.x < bbMin.x) bbMin.x = v.x;

				if(v.y > bbMax.y) bbMax.y = v.y;
				if(v.y < bbMin.y) bbMin.y = v.y;

				if(v.z > bbMax.z) bbMax.z = v.z;
				if(v.z < bbMin.z) bbMin.z = v.z;
			}
		}
	}

	private void addObject(String oname) {
		objNames.add(oname);
		
		GL2 gl = GlTools.getGL2();
		
		IntBuffer buffer = IntBuffer.allocate(1);
		
		// Buffer for triangle indexes
		gl.glGenBuffers(1, buffer);
		triBufferObjects.put(oname, buffer.get(0));
		
		// Buffer for quad indexes
		gl.glGenBuffers(1, buffer);
		quadBufferObjects.put(oname, buffer.get(0));
	}
	
	public void release() {
		GL2 gl = GlTools.getGL2();
		gl.glDeleteBuffers(OBJ_BUFFER_COUNT, vboIds);

		for(String s: triBufferObjects.keySet()) {
			int b = triBufferObjects.get(s);
			gl.glDeleteBuffers(1, new int[] {b}, 0);
		}

		for(String s: quadBufferObjects.keySet()) {
			int b = quadBufferObjects.get(s);
			gl.glDeleteBuffers(1, new int[] {b}, 0);
		}
	}
	
	public void render() {
		render(false);
	}
	
	public void render(boolean mesh) {
		GL2 gl = GlTools.getGL2();
		
		gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT);

			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			if(!mesh)
				gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(VERTS_DATA));
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
	
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(NORMS_DATA));
			gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);
	
			if(!mesh) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboIds.get(TEXES_DATA));
				gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);
			}
	
			for(String o : objNames) {
				if(!mesh) {
					String mat = objMaterial.get(o);
					if(material.containsKey(mat))
						material.get(mat).set();
					gl.glShadeModel(shadeModel.get(o) ? GL2.GL_SMOOTH : GL2.GL_FLAT);
				} else {
					Color.WHITE.set();
				}
	
				// Triangl.gle Indexes
				if(triBufferObjects.containsKey(o)) {
					int b = triBufferObjects.get(o);
					gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, b);
					gl.glDrawElements(!mesh ? GL2.GL_TRIANGLES : GL2.GL_LINE_STRIP, triIndexes.get(o).size(), GL2.GL_UNSIGNED_INT, 0);
				}
	
				// Quads Indexes
				if(quadBufferObjects.containsKey(o)) {
					int b = quadBufferObjects.get(o);
					gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, b);
					gl.glDrawElements(!mesh ? GL2.GL_QUADS : GL2.GL_LINE_STRIP, quadIndexes.get(o).size(), GL2.GL_UNSIGNED_INT, 0);
				}
	
			}
	
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
	
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
			if(!mesh)
				gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glPopAttrib();
	}
	
}