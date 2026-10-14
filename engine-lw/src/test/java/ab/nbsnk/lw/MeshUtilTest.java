/*
 * Copyright (C) 2026 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.nbsnk.lw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeshUtilTest {

  MeshUtil.Vec3f Vec3f(double x, double y, double z) {
    MeshUtil.Vec3f v = new MeshUtil.Vec3f();
    v.x = (float) x;
    v.y = (float) y;
    v.z = (float) z;
    return v;
  }

  MeshUtil.Vec2f Vec2f(double x, double y) {
    MeshUtil.Vec2f v = new MeshUtil.Vec2f();
    v.x = (float) x;
    v.y = (float) y;
    return v;
  }

  @Test
  void tangent() {
    MeshUtil.Vec3f pa = Vec3f(0.0, 2.4, -1.4);
    MeshUtil.Vec3f pb = Vec3f(0.229712, 2.4, -1.38197);
    MeshUtil.Vec3f pc = Vec3f(0.227403, 2.43544, -1.36807);
    MeshUtil.Vec2f ta = Vec2f(-0.109561, 1.71761);
    MeshUtil.Vec2f tb = Vec2f(0.120858, 1.71761);
    MeshUtil.Vec2f tc = Vec2f(0.119643, 1.75572);
    MeshUtil.Vec3f[] norm = {new MeshUtil.Vec3f(), new MeshUtil.Vec3f(), new MeshUtil.Vec3f()};
    MeshUtil.computeTBNNormalized(pa, pb, pc, ta, tb, tc, norm);
    MeshUtil.Vec3f[] expected = {
        Vec3f(-0.07274921, -0.36827022, 0.9268682),
        Vec3f(0.9969339, -0.0, 0.07824863),
        Vec3f(-0.028797228, 0.9297188, 0.36714247)};
    assertArrayEquals(expected, norm);
  }

  @Test
  void quat() {
    MeshUtil.Vec3f[] normals = {
        Vec3f(1.3914703E-17, -0.3691291, 0.9293782),
        Vec3f(0.9969339, -0.0, 0.07824863),
        Vec3f(-0.028797228, 0.9297188, 0.36714247)};
    MeshUtil.Quat4f quat = new MeshUtil.Quat4f();
    MeshUtil.fixTSpace(normals);
    MeshUtil.buildVSQuat(normals, quat);
    assertEquals(-0.38864684f, quat.x);
    assertEquals(-0.4055442f, quat.y);
    assertEquals(-0.57925284f, quat.z);
    assertEquals(0.590723f, quat.w);
  }
}
