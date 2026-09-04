package hdu.ljq.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
  public final Path directory;
  private final CatalogService catalog;
  private final TransactionTemplate tx;

  public MediaService(
      @Value("${app.upload-dir}") String dir,
      CatalogService c,
      org.springframework.transaction.PlatformTransactionManager manager)
      throws IOException {
    directory = Path.of(dir).toAbsolutePath().normalize();
    Files.createDirectories(directory);
    catalog = c;
    tx = new TransactionTemplate(manager);
  }

  public ObjectNode upload(MultipartFile file, String actor, String requestId) {
    Path target = null;
    try {
      if (file.isEmpty()) throw ApiException.invalid("file", "Choose an image.");
      if (file.getSize() > 5242880)
        throw new ApiException(413, "PAYLOAD_TOO_LARGE", "Maximum 5 MiB.");
      String original = Objects.toString(file.getOriginalFilename(), "image").replace('\\', '/');
      original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
      if (original.length() > 255) original = original.substring(original.length() - 255);
      String ext =
          original.contains(".")
              ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
              : "";
      if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext)
          || !Set.of("image/jpeg", "image/png", "image/webp")
              .contains(Objects.toString(file.getContentType(), "")))
        throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Upload a JPEG, PNG or WebP image.");
      BufferedImage input;
      try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
        var readers = ImageIO.getImageReaders(in);
        if (!readers.hasNext()) throw ApiException.invalid("file", "Unable to decode this image.");
        ImageReader reader = readers.next();
        try {
          reader.setInput(in);
          String format = reader.getFormatName().toLowerCase(Locale.ROOT);
          String expected =
              format.equals("jpeg")
                  ? "image/jpeg"
                  : format.equals("png") ? "image/png" : format.equals("webp") ? "image/webp" : "";
          if (!expected.equals(file.getContentType())
              || !(ext.equals(format) || (format.equals("jpeg") && ext.equals("jpg"))))
            throw ApiException.invalid(
                "file", "Image contents do not match the extension and media type.");
          if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000)
            throw ApiException.invalid("file", "Maximum 20 million pixels.");
          input = reader.read(0);
        } finally {
          reader.dispose();
        }
      }
      int width = input.getWidth(), height = input.getHeight();
      double scale = Math.min(1, 1600.0 / Math.max(width, height));
      width = Math.max(1, (int) (width * scale));
      height = Math.max(1, (int) (height * scale));
      BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = result.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, width, height);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.drawImage(input, 0, 0, width, height, null);
      g.dispose();
      String name = UUID.randomUUID() + ".jpg";
      target = directory.resolve(name);
      ImageIO.write(result, "jpg", target.toFile());
      ObjectNode d =
          catalog
              .json()
              .createObjectNode()
              .put("url", "/media/" + name)
              .put("mime_type", "image/jpeg")
              .put("byte_size", Files.size(target))
              .put("width", width)
              .put("height", height)
              .put("original_name", original);
      return tx.execute(
          status -> {
            ObjectNode saved = catalog.repository().create(EntityType.MEDIA, d);
            catalog.audit(EntityType.MEDIA, actor, "upload", null, saved, requestId);
            return (ObjectNode) catalog.contract().output("Media", saved);
          });
    } catch (ApiException e) {
      cleanup(target);
      throw e;
    } catch (Exception e) {
      cleanup(target);
      throw new ApiException(422, "VALIDATION_ERROR", "Unable to process the uploaded image.");
    }
  }

  private void cleanup(Path path) {
    if (path != null)
      try {
        Files.deleteIfExists(path);
      } catch (IOException ignored) {
      }
  }
}
