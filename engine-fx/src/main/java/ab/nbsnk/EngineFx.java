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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Supplier;

/**
 * https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/paint/PhongMaterial.html
 * JavaFx right y down, limitations:
 * 3 light sources
 * No light attenuation with distance
 * No shadows
 * Missing setSelfIlluminationColor method that can change the brightness or color
 * of setSelfIlluminationMap the similar way as setDiffuseColor can alter setDiffuseMap
 * No ambient light per shape, only global AmbientLight
 * No reflection mapping
 * flat shading can be achieved by downgrading normals and disabling specular reflection
 */
public class EngineFx implements Engine3d {

  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;
  private NodeFx camera;
  private Supplier<String> textSupplier;
  private Map<BufferedImage, Image> imageCache = new HashMap<>();
  private BufferedImage backgroundImage;
  private Scene scene;
  private javafx.scene.Group root;
  private AmbientLight ambientLight;
  private PerspectiveCamera perspectiveCamera;

  public static TriangleMesh loadObj(Obj obj) {
    TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
    mesh.getPoints().addAll(Obj.copy(obj.vertex));
    mesh.getTexCoords().addAll(Obj.copy(obj.texture));
    mesh.getFaces().addAll(Obj.copy(obj.face));
    mesh.getNormals().addAll(Obj.copy(obj.normal));
    return mesh;
  }

  private static Image loadImg(BufferedImage image) {
    // If we have a texture image and an array of (x, y) coordinates of the image,
    // and we need to pass this image to JavaFX, knowing that it uses a y-down coordinate system.
    // We might think we can simply invert the y coordinate, right? Wrong.
    // Because the moment we switch from a texture to a normal map, its green color
    // corresponds to the increase of the y coordinate. And it will always point in the wrong direction
    // unless we mirror flip the image itself.
    int width = image.getWidth();
    int height = image.getHeight();
    WritableImage writableImage = new WritableImage(width, height);
    Object data = image.getRaster().getDataElements(0, 0, width, height, null);
    switch (image.getType()) {
      case BufferedImage.TYPE_INT_ARGB:
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(),
            (int[]) data, width * (height - 1), -width);
        break;
      case BufferedImage.TYPE_3BYTE_BGR:
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteRgbInstance(),
            (byte[]) data, 3 * width * (height - 1), -3 * width);
        break;
      case BufferedImage.TYPE_4BYTE_ABGR:
        byte[] dataBytes = (byte[]) data;
        for (int i = 0; i < dataBytes.length; i += 4) {
          byte red = dataBytes[i];
          dataBytes[i] = dataBytes[i + 2]; // blue
          dataBytes[i + 2] = red;
        }
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(),
            dataBytes, 4 * width * (height - 1), -4 * width);
        break;
      default: throw new IllegalStateException(image.toString());
    }
    return writableImage;
  }

  private static Color color(int color) {
    return Color.rgb(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (color >> 24 & 0xFF) / 255.0);
  }

  public EngineFx() {
    System.setProperty("prism.forceGPU", "true");
    //assert com.sun.prism.impl.PrismSettings.forceGPU : "prism.forceGPU=true";
    if (!com.sun.prism.impl.PrismSettings.forceGPU) throw new AssertionError("prism.forceGPU=true");
    this.ambientLight = new AmbientLight(Color.BLACK);
    this.root = new javafx.scene.Group(this.ambientLight);
    this.perspectiveCamera = new PerspectiveCamera(true);
    this.perspectiveCamera.setFieldOfView(Math.atan2(24.0 / 2, 50.0) * 2 / (Math.PI * 2) * 360); // 50mm full frame
    GroupFx camera = new GroupFx();
    new NodeFx(this.perspectiveCamera).rotation(0, 0.5, 0).connect(camera); // make it right y up
    this.camera = camera;
  }

  @Override
  public EngineFx open(BufferedImage image) {
    if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
    this.image = image;
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();

    this.scene = new Scene(root, imageWidth, imageHeight, true, SceneAntialiasing.DISABLED);
    this.scene.setCamera(perspectiveCamera);
    applyBackground();
    CompletableFuture.runAsync(() -> JavaFx.App.main(new String[0]));
    try {
      JavaFx.App.io.take();
    } catch (InterruptedException ignore) {}
    return this;
  }

  private void applyBackground() {
    if (scene == null) return;
    scene.setFill(null);
    if (backgroundImage == null) return;
    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    int width = backgroundImage.getWidth();
    int height = backgroundImage.getHeight();
    image.getGraphics().drawImage(backgroundImage, 0, imageHeight, width, -height, null);
    scene.setFill(new ImagePattern(loadImg(image)));
  }

  @Override
  public EngineFx background(BufferedImage image) {
    backgroundImage = image;
    applyBackground();
    return this;
  }

  @Override
  public ShapeFx shape(Obj obj) {
    return new ShapeFx(obj);
  }

  @Override
  public GroupFx group() {
    return new GroupFx();
  }

  @Override
  public LightFx light() {
    return new LightFx();
  }

  @Override
  public EngineFx setAmbient(int color) {
    ambientLight.setColor(color(color));
    return this;
  }

  @Override
  public NodeFx camera() {
    return this.camera;
  }

  @Override
  public EngineFx setFarClip(double value) {
    perspectiveCamera.setFarClip(value);
    return this;
  }

  @Override
  public EngineFx setFocalLength(double value) {
    this.perspectiveCamera.setFieldOfView(Math.atan2(24.0 / 2, value) * 2 / (Math.PI * 2) * 360);
    return this;
  }

  @Override
  public void update() {
    JavaFx.App.scene = this.scene;
    try {
      JavaFx.App.io.put(this);
      JavaFx.App.io.take();
    } catch (InterruptedException ignore) {}
    int[] data = new int[imageWidth * imageHeight];
    JavaFx.App.snapshot.getPixelReader()
        .getPixels(0, 0, imageWidth, imageHeight, PixelFormat.getIntArgbInstance(), data, 0, imageWidth);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, data);
    if (textSupplier != null) {
      Graphics graphics = image.createGraphics();
      graphics.setColor(java.awt.Color.DARK_GRAY);
      graphics.drawString(textSupplier.get(), 2, imageHeight - 4);
    }
  }

  @Override
  public void sysex(int i) {
  }

  @Override
  public EngineFx textSupplier(Supplier<String> supplier) {
    textSupplier = supplier;
    return this;
  }

  @Override
  public void close() {
    Platform.exit();
  }

  private static class JavaFx { // private modifier for Application which is required to be public
    public static class App extends Application {
      public static Scene scene;
      public static WritableImage snapshot;
      public static BlockingQueue<Object> io = new SynchronousQueue<>();

      @Override
      public void start(Stage stage) {
        //stage.setScene(scene);
        //stage.show(); // do not open a window
        while (true) {
          try {
            io.put(this); // ready
            io.take(); // wait for request
            snapshot = scene.snapshot(snapshot);
          } catch (InterruptedException ignore) {
            break;
          }
        }
      }

      public static void main(String[] args) {
        launch();
      }
    }
  }

  private class NodeFx implements Node {
    protected final javafx.scene.Node node;
    private javafx.scene.Group group;
    private Translate t  = new Translate();
    private Rotate rx = new Rotate(0.0, Rotate.X_AXIS);
    private Rotate ry = new Rotate(0.0, Rotate.Y_AXIS);
    private Rotate rz = new Rotate(0.0, Rotate.Z_AXIS);

    private NodeFx(javafx.scene.Node node) {
      node.getTransforms().addAll(t, ry, rx, rz);
      this.node = node;
      this.group = root;
      this.group.getChildren().add(this.node);
    }

    @Override
    public NodeFx translation(double x, double y, double z) {
      t.setX(x);
      t.setY(y);
      t.setZ(z);
      return this;
    }

    @Override
    public NodeFx rotation(double y, double p, double r) {
      ry.setAngle(-y * 360);
      rx.setAngle(p * 360);
      rz.setAngle(-r * 360);
      return this;
    }

    private void connect(javafx.scene.Group group) {
      this.group.getChildren().remove(this.node);
      this.group = group;
      this.group.getChildren().add(this.node);
    }

    @Override
    public NodeFx connect(Group node) {
      connect((javafx.scene.Group) ((GroupFx) node).node);
      return this;
    }

    @Override
    public NodeFx setVisible(boolean value) {
      node.setVisible(value);
      return this;
    }
  }

  private class ShapeFx extends NodeFx implements Shape {

    private PhongMaterial material;

    public ShapeFx(Obj obj) {
      super(new MeshView(loadObj(obj)));
      MeshView meshView = (MeshView) this.node;
      material = new PhongMaterial();
      meshView.setMaterial(material);
    }

    @Override
    public ShapeFx setColor(int color) {
      material.setDiffuseColor(color(color));
      return this;
    }

    @Override
    public ShapeFx setSpecular(int color, double power) {
      material.setSpecularColor(color(color));
      material.setSpecularPower(power);
      return this;
    }

    @Override
    public ShapeFx selfIllumination(int color) {
      Image image = material.getDiffuseMap();
      material.setDiffuseMap(null);
      if ((color & 0xFFFFFF) != 0xFFFFFF) {
        double rMul = (color >> 16 & 0xFF) / 255.0;
        double gMul = (color >> 8 & 0xFF) / 255.0;
        double bMul = (color & 0xFF) / 255.0;
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] data = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), data, 0, width);
        for (int i = 0; i < data.length; i++) {
          double r = rMul * (data[i] >> 16 & 0xFF);
          double g = gMul * (data[i] >> 8 & 0xFF);
          double b = bMul * (data[i] & 0xFF);
          data[i] = 0xFF000000 | (int) r << 16 | (int) g << 8 | (int) b;
        }
        WritableImage writableImage = new WritableImage(width, height);
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), data, 0, width);
        image = writableImage;
      }
      material.setSelfIlluminationMap(image);
      material.setDiffuseColor(Color.BLACK);
      return this;
    }

    @Override
    public ShapeFx setDiffuseMap(BufferedImage image) {
      material.setDiffuseMap(imageCache.computeIfAbsent(image, EngineFx::loadImg));
      return this;
    }

    @Override
    public ShapeFx setBumpMap(BufferedImage image) {
      material.setBumpMap(imageCache.computeIfAbsent(image, EngineFx::loadImg));
      return this;
    }

    @Override
    public ShapeFx setReflectionMap(BufferedImage image, double alpha, Node skybox) {
      return this;
    }
  }

  private class GroupFx extends NodeFx implements Group {

    public GroupFx() {
      super(new javafx.scene.Group());
    }

  }

  private class LightFx extends NodeFx implements Light {

    public LightFx() {
      super(new PointLight(Color.WHITE));
    }

    @Override
    public LightFx setColor(int color) {
      ((PointLight) this.node).setColor(color(color));
      return this;
    }

  }

}
