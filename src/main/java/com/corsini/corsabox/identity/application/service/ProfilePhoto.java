package com.corsini.corsabox.identity.application.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

final class ProfilePhoto {
    private ProfilePhoto() {}
    static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() > 400_000 || !value.matches("data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+"))
            throw new IllegalArgumentException("Use uma foto JPG ou PNG de até 300 KB.");
        try {
            var bytes = Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException();
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    int w = reader.getWidth(0), h = reader.getHeight(0);
                    if (w < 1 || h < 1 || w > 2048 || h > 2048) throw new IllegalArgumentException();
                    var image = reader.read(0);
                    int size = Math.min(512, Math.min(w, h));
                    var output = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                    var g = output.createGraphics();
                    try {
                        g.setColor(Color.WHITE); g.fillRect(0, 0, size, size);
                        int edge = Math.min(w, h), x = (w - edge) / 2, y = (h - edge) / 2;
                        g.drawImage(image, 0, 0, size, size, x, y, x + edge, y + edge, null);
                    } finally { g.dispose(); }
                    var buffer = new ByteArrayOutputStream();
                    ImageIO.write(output, "jpeg", buffer);
                    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(buffer.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Foto inválida. Use JPG ou PNG, com até 2048 × 2048 pixels.");
        }
    }
}
