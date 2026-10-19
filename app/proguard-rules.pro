# Preserve tag implementations and reflection-based ID3 frame construction.
-keep class org.jaudiotagger.** { *; }

# jaudiotagger bundles desktop-only artwork and command-line helpers.
# TagEditor explicitly selects Android mode and AndroidArtwork; these AWT,
# ImageIO, and Swing APIs are never called by the Android application.
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.awt.Graphics2D
-dontwarn java.awt.Image
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.ImageWriter
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream
-dontwarn javax.swing.filechooser.FileFilter
