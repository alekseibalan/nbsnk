package ab.nbsnk.lw;

// The code of this class is a copy of JavaFX routines
// com.sun.prism.impl.BaseMesh.doBuildPNTGeometry
// com.sun.prism.impl.MeshUtil.computeTBNNormalized
// com.sun.prism.impl.BaseMesh.convertNormalsToQuats
// com.sun.prism.impl.MeshUtil.fixTSpace
// com.sun.prism.impl.BaseMesh.buildVSQuat
// com.sun.prism.impl.MeshUtil.buildQuat
public class MeshUtil {

  public static void buildQuat(float[] vertex, float[] texture, float[] normal) {
    Vec3f pa = new Vec3f();
    Vec3f pb = new Vec3f();
    Vec3f pc = new Vec3f();
    Vec2f ta = new Vec2f();
    Vec2f tb = new Vec2f();
    Vec2f tc = new Vec2f();
    Vec3f[] tbt = {new Vec3f(), new Vec3f(), new Vec3f()};
    Vec3f[] normals = {new Vec3f(), new Vec3f(), new Vec3f()};
    Quat4f quat = new Quat4f();
    for (int iv = 0, it = 0, in = 0; iv < vertex.length;) {
      pa.x = vertex[iv++];
      pa.y = vertex[iv++];
      pa.z = vertex[iv++];
      pb.x = vertex[iv++];
      pb.y = vertex[iv++];
      pb.z = vertex[iv++];
      pc.x = vertex[iv++];
      pc.y = vertex[iv++];
      pc.z = vertex[iv++];
      ta.x = texture[it++];
      ta.y = texture[it++];
      tb.x = texture[it++];
      tb.y = texture[it++];
      tc.x = texture[it++];
      tc.y = texture[it++];
      computeTBNNormalized(pa, pb, pc, ta, tb, tc, tbt);
      for (int i = 0; i < 3; i++) {
        normals[0].x = normal[in++];
        normals[0].y = normal[in++];
        normals[0].z = normal[in++];
        in -= 3;
        normals[1].set(tbt[1]);
        normals[2].set(tbt[2]);
        fixTSpace(normals);
        buildVSQuat(normals, quat);
        normal[in++] = quat.x;
        normal[in++] = quat.y;
        normal[in++] = quat.z;
        normal[in++] = quat.w;
      }
    }
  }

  static float fabs(float x) {
    return x < 0 ? -x : x;
  }

  static void getOrt(Vec3f a, Vec3f b) {
    //return a ^ preOrt ^ a;
    b.cross(a, b);
    b.cross(b, a);
  }

  static void computeTBNNormalized(Vec3f pa, Vec3f pb, Vec3f pc,
      Vec2f ta, Vec2f tb, Vec2f tc, Vec3f[] norm) {
    MeshTempState instance = MeshTempState.getInstance();
    Vec3f n = instance.vec3f1;
    Vec3f v1 = instance.vec3f2;
    Vec3f v2 = instance.vec3f3;

    // compute Normal |(v1-v0)X(v2-v0)|
    v1.sub(pb, pa);
    v2.sub(pc, pa);
    n.cross(v1, v2);
    norm[0].set(n);
    norm[0].normalize(); // TODO: make sure each triangle area (size) will be considered

    v1.set(0, tb.x - ta.x, tb.y - ta.y);
    v2.set(0, tc.x - ta.x, tc.y - ta.y);

    if (v1.y * v2.z == v1.z * v2.y) {
      MeshUtil.generateTB(pa, pb, pc, norm);
      return;
    }

    // compute Tangent and Binomal
    v1.x = pb.x - pa.x;
    v2.x = pc.x - pa.x;
    n.cross(v1, v2);
    norm[1].x = -n.y / n.x;
    norm[2].x = -n.z / n.x;

    v1.x = pb.y - pa.y;
    v2.x = pc.y - pa.y;
    n.cross(v1, v2);
    norm[1].y = -n.y / n.x;
    norm[2].y = -n.z / n.x;

    v1.x = pb.z - pa.z;
    v2.x = pc.z - pa.z;
    n.cross(v1, v2);
    norm[1].z = -n.y / n.x;
    norm[2].z = -n.z / n.x;

    norm[1].normalize();
    norm[2].normalize();
  }

  static void generateTB(Vec3f v0, Vec3f v1, Vec3f v2, Vec3f[] ntb) {
    MeshTempState instance = MeshTempState.getInstance();
    Vec3f a = instance.vec3f1;
    a.sub(v1, v0);
    Vec3f b = instance.vec3f2;
    b.sub(v2, v0);

    if (a.dot(a) > b.dot(b)) {
      ntb[1].set(a);
      ntb[1].normalize(); // TODO: make sure each triangle area (size) will be considered
      ntb[2].cross(ntb[0], ntb[1]);
    } else {
      ntb[2].set(b);
      ntb[2].normalize(); // TODO: make sure each triangle area (size) will be considered
      ntb[1].cross(ntb[2], ntb[0]);
    }
  }

  static double clamp(double x, double min, double max) {
    return x < max ? (x > min ? x : min) : max;
  }

  static void fixTSpace(Vec3f[] norm) {
    float nNorm = norm[0].length();

    MeshTempState instance = MeshTempState.getInstance();
    Vec3f n1 = instance.vec3f1;
    n1.set(norm[1]);
    Vec3f n2 = instance.vec3f2;
    n2.set(norm[2]);
    getOrt(norm[0], n1);
    getOrt(norm[0], n2);

    float n1Length = n1.length();
    float n2Length = n2.length();

    double cosPhi = (n1.dot(n2)) / (n1Length * n2Length);
    Vec3f e1 = instance.vec3f3;
    Vec3f e2 = instance.vec3f4;

    if (fabs((float) cosPhi) > 0.998) {
      Vec3f n2fix = instance.vec3f5;
      n2fix.cross(norm[0], n1);
      n2fix.normalize();

      e2.set(n2fix);
      if (n2fix.dot(n2) < 0) {
        e2.mul(-1);
      }
      e1.set(n1);
      e1.mul(1f / n1Length);
    } else {
      double phi = Math.acos(clamp(cosPhi, -1, 1));
      double alpha = (Math.PI * 0.5 - phi) * 0.5;
      Vec2f e1Local = instance.vec2f1;
      e1Local.set((float) Math.sin(alpha), (float) Math.cos(alpha));
      Vec2f e2Local = instance.vec2f2;
      e2Local.set((float) Math.sin(alpha + phi), (float) Math.cos(alpha + phi));

      Vec3f n1T = instance.vec3f5;
      n1T.set(n2);
      getOrt(n1, n1T);
      float n1TLength = n1T.length();

      // e1 = float(e1_local.y/l1) * n1 - float(e1_local.x/l_n1T) * n1T;
      e1.set(n1);
      e1.mul(e1Local.y / n1Length);

      Vec3f n1TT = instance.vec3f6;
      n1TT.set(n1T);
      n1TT.mul(e1Local.x / n1TLength);
      e1.sub(n1TT);

      // e2 = float(e2_local.y/l1) * n1 + float(e2_local.x/l_n1T) * n1T;
      e2.set(n1);
      e2.mul(e2Local.y / n1Length);

      // Recycle n1TT for temp computation
      n1TT.set(n1T);
      n1TT.mul(e2Local.x / n1TLength);
      e2.add(n1TT);

      float e1DotN1 = e1.dot(n1);
      float e2DotN2 = e2.dot(n2);

      // This will interfer with degenerated triangle unit test.
      // assert ((e1DotN1 / n1Length - e2DotN2 / n2Length) < 0.001);
    }

    norm[1].set(e1);
    norm[2].set(e2);
    norm[0].mul(1f / nNorm);
  }

  static void buildQuat(Vec3f[] tm, Quat4f quat) {
    MeshTempState instance = MeshTempState.getInstance();
    float[][] m = instance.matrix;
    float[] tmp = instance.vector;

    for (int i = 0; i < 3; i++) {
      m[i][0] = tm[i].x;
      m[i][1] = tm[i].y;
      m[i][2] = tm[i].z;
    }

    float trace = m[0][0] + m[1][1] + m[2][2];

    if (trace > 0) {
      float s = (float) Math.sqrt(trace + 1.0f);
      float t = 0.5f / s;
      quat.w = 0.5f * s;
      quat.x = (m[1][2] - m[2][1]) * t;
      quat.y = (m[2][0] - m[0][2]) * t;
      quat.z = (m[0][1] - m[1][0]) * t;

    } else {
      int[] next = {1, 2, 0};
      int i = 0;

      if (m[1][1] > m[0][0]) {
        i = 1;
      }
      if (m[2][2] > m[i][i]) {
        i = 2;
      }

      int j = next[i], k = next[j];
      float s = (float) Math.sqrt(m[i][i] - m[j][j] - m[k][k] + 1.0f);

      if (m[j][k] < m[k][j]) {
        s = -s;
      }

      float t = 0.5f / s;

      tmp[i] = 0.5f * s;
      quat.w = (m[j][k] - m[k][j]) * t;
      tmp[j] = (m[i][j] + m[j][i]) * t;
      tmp[k] = (m[i][k] + m[k][i]) * t;
      quat.x = tmp[0];
      quat.y = tmp[1];
      quat.z = tmp[2];
    }
  }

  static void buildVSQuat(Vec3f[] tm, Quat4f quat) {
    Vec3f v = MeshTempState.getInstance().vec3f1;
    v.cross(tm[1], tm[2]);
    float d = tm[0].dot(v);
    if (d < 0) {
      tm[2].mul(-1);
    }

    MeshUtil.buildQuat(tm, quat);

    // This will interfer with degenerated triangle unit test.
    // assert (quat.w >= 0);

    if (d < 0) {
      if (quat.w == 0) {
        quat.w = 1E-10f; // MeshUtil.MAGIC_SMALL;
      }
      quat.scale(-1);
    }
  }

  static class MeshTempState {
    Vec3f vec3f1 = new Vec3f();
    Vec3f vec3f2 = new Vec3f();
    Vec3f vec3f3 = new Vec3f();
    Vec3f vec3f4 = new Vec3f();
    Vec3f vec3f5 = new Vec3f();
    Vec3f vec3f6 = new Vec3f();
    Vec2f vec2f1 = new Vec2f();
    Vec2f vec2f2 = new Vec2f();
    float[][] matrix = new float[3][3];
    float[] vector = new float[3];
    static MeshTempState getInstance() {
      return new MeshTempState();
    }
  }

  public static class Vec3f {
    float x;
    float y;
    float z;

    public void set(Vec3f v) {
      this.x = v.x;
      this.y = v.y;
      this.z = v.z;
    }

    public void set(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public final void mul(float s) {
      this.x *= s;
      this.y *= s;
      this.z *= s;
    }

    public void sub(Vec3f t1, Vec3f t2) {
      this.x = t1.x - t2.x;
      this.y = t1.y - t2.y;
      this.z = t1.z - t2.z;
    }

    public void sub(Vec3f t1) {
      this.x -= t1.x;
      this.y -= t1.y;
      this.z -= t1.z;
    }

    public void add(Vec3f t1) {
      this.x += t1.x;
      this.y += t1.y;
      this.z += t1.z;
    }

    public float length() {
      return (float) Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
    }

    public void normalize() {
      float norm = 1.0f / length();
      this.x = this.x * norm;
      this.y = this.y * norm;
      this.z = this.z * norm;
    }

    public void cross(Vec3f v1, Vec3f v2) {
      float tmpX;
      float tmpY;

      tmpX = v1.y * v2.z - v1.z * v2.y;
      tmpY = v2.x * v1.z - v2.z * v1.x;
      this.z = v1.x * v2.y - v1.y * v2.x;
      this.x = tmpX;
      this.y = tmpY;
    }

    public float dot(Vec3f v1) {
      return this.x * v1.x + this.y * v1.y + this.z * v1.z;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Vec3f) {
        Vec3f v = (Vec3f) obj;
        return (x == v.x) && (y == v.y) && (z == v.z);
      }
      return false;
    }
  }

  public static class Vec2f {
    public float x;
    public float y;

    public void set(float x, float y) {
      this.x = x;
      this.y = y;
    }
  }

  public static class Quat4f {
    public float x;
    public float y;
    public float z;
    public float w;

    public final void scale(float s) {
      this.x *= s;
      this.y *= s;
      this.z *= s;
      this.w *= s;
    }
  }
}
