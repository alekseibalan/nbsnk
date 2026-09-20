/*
 * Copyright (C) 2025 Aleksei Balan
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

package ab.nbsnk;

import ab.nbsnk.lw.Mesh;
import ab.nbsnk.lw.Program;
import ab.nbsnk.lw.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL33C.*;

/**
 * OpenGL right y up
 */
public class EngineLw implements Engine3d {

  public static final boolean FLIP_Y = true;
  private int screenWidth;
  private int screenHeight;
  private BufferedImage screenImage;
  private long windowHandle;
  private int[] pixelInts;
  Program program;
  private NodeLw camera;
  GroupLw group = new GroupLw(null);
  private Matrix4f projectionMatrix;
  private Map<BufferedImage, Texture> imageCache = new HashMap<>();

  @Override
  public EngineLw open(BufferedImage image) {
    if (windowHandle != 0) throw new IllegalStateException();
    if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
    screenWidth = image.getWidth();
    screenHeight = image.getHeight();
    screenImage = image;
    if (!GLFW.glfwInit()) throw new IllegalStateException();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    windowHandle = GLFW.glfwCreateWindow(screenWidth, screenHeight, "", 0, 0);
    GLFW.glfwMakeContextCurrent(windowHandle);
    GL.createCapabilities();
    glEnable(GL_DEPTH_TEST);
    pixelInts = new int[screenWidth * (FLIP_Y ? 1: screenHeight)];
    program = Program.newDefault();
    camera = new NodeLw(null);
    projectionMatrix = new Matrix4f().setPerspective(
        (float) Math.toRadians(Math.atan2(24.0 / 2, 50.0) * 2 / (Math.PI * 2) * 360),
        (float) screenWidth / screenHeight, 0.01f, 100000.f);
    return this;
  }

  @Override
  public void close() {
    if (windowHandle == 0) return;
    program.close();
    GLFW.glfwDestroyWindow(windowHandle);
    GLFW.glfwTerminate();
    windowHandle = 0;
  }

  @Override
  public EngineLw background(BufferedImage image) {
    return this;
  }

  @Override
  public ShapeLw shape(Obj obj) {
    return new ShapeLw(group, obj);
  }

  @Override
  public GroupLw group() {
    return new GroupLw(group);
  }

  @Override
  public LightLw light() {
    return new LightLw(group);
  }

  @Override
  public EngineLw setAmbient(int color) {
    return this;
  }

  @Override
  public NodeLw camera() {
    return camera;
  }

  @Override
  public EngineLw setFarClip(double value) {
    return this;
  }

  @Override
  public EngineLw setFocalLength(double value) {
    return this;
  }

  private static void dfs(Set<NodeLw> nodes, Matrix4f tm, Map<NodeLw, Matrix4f> map) {
    for (NodeLw node : nodes) {
      if (!node.visible) continue;
      Matrix4f t = new Matrix4f(tm).mul(node.matrix);
      if (node instanceof GroupLw) {
        dfs(((GroupLw) node).nodes, t, map);
        continue;
      }
      map.put(node, t);
    }
  }

  @Override
  public void update() {
    glClearColor(0.5f, 0.5f, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    Map<NodeLw, Matrix4f> map = new HashMap<>();
    dfs(group.nodes, new Matrix4f().identity(), map);
    Matrix4f cameraMatrix = new Matrix4f(camera.matrix).invert();
    for (Map.Entry<NodeLw, Matrix4f> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof ShapeLw)) continue;
      program.use(projectionMatrix, cameraMatrix, entry.getValue());
      ShapeLw shape = (ShapeLw) entry.getKey();
      Optional.ofNullable(shape.texture).ifPresentOrElse(Texture::bind, Texture::unbind);
      shape.mesh.draw();
    }

    if (FLIP_Y) {
      for (int i = 0, j = screenHeight - 1; i < screenHeight; i++, j--) {
        glReadPixels(0, i, screenWidth, 1, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixelInts);
        screenImage.getRaster().setDataElements(0, j, screenWidth, 1, pixelInts);
      }
    } else {
      glReadPixels(0, 0, screenWidth, screenHeight, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixelInts);
      screenImage.getRaster().setDataElements(0, 0, screenWidth, screenHeight, pixelInts);
    }
  }

  @Override
  public void sysex(int i) {
    switch (i) {
      case 0:
        GLFW.glfwMakeContextCurrent(0);
        return;
      case 1:
        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
    }
  }

  @Override
  public EngineLw textSupplier(Supplier<String> supplier) {
    return this;
  }

  public static class NodeLw implements Node {
    Matrix4f matrix = new Matrix4f();
    Vector3f translation = new Vector3f();
    Vector3f rotation = new Vector3f();
    boolean visible = true;
    GroupLw group;

    public NodeLw(GroupLw group) {
      this.group = group;
      if (group != null) group.nodes.add(this);
    }

    @Override
    public NodeLw translation(double x, double y, double z) {
      translation.set(x, y, z);
      update();
      return this;
    }

    void update() {
      matrix.translation(translation).rotateYXZ(rotation);
    }

    @Override
    public NodeLw rotation(double yaw, double pitch, double roll) {
      rotation.set((float) (2 * Math.PI * pitch), (float) (-2 * Math.PI * yaw), (float) (-2 * Math.PI * roll));
      update();
      return this;
    }

    @Override
    public NodeLw connect(Group node) {
      group.nodes.remove(this);
      group = (GroupLw) node;
      group.nodes.add(this);
      return this;
    }

    @Override
    public NodeLw setVisible(boolean value) {
      visible = value;
      return this;
    }
  }

  public class ShapeLw extends NodeLw implements Shape {
    private Mesh mesh;
    private Texture texture;

    public ShapeLw(GroupLw group, Obj obj) {
      super(group);
      mesh = new Mesh(obj);
    }

    @Override
    public ShapeLw setColor(int color) {
      return this;
    }

    @Override
    public ShapeLw setSpecular(int color, double power) {
      return this;
    }

    @Override
    public ShapeLw selfIllumination(int color) {
      return this;
    }

    @Override
    public ShapeLw setDiffuseMap(BufferedImage image) {
      this.texture = imageCache.computeIfAbsent(image, Texture::new);
      return this;
    }

    @Override
    public ShapeLw setBumpMap(BufferedImage image) {
      return this;
    }

    @Override
    public ShapeLw setReflectionMap(BufferedImage image, double alpha, Node skybox) {
      return this;
    }
  }

  public static class GroupLw extends NodeLw implements Group {
    public GroupLw(GroupLw group) {
      super(group);
    }

    private Set<NodeLw> nodes = new LinkedHashSet<>();
  }

  public static class LightLw extends NodeLw implements Light {
    public LightLw(GroupLw group) {
      super(group);
    }

    @Override
    public LightLw setColor(int color) {
      return this;
    }
  }

}
