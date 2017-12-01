package uk.co.dphin.albumview.util;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;
import uk.co.dphin.albumview.models.*;

/**
 * Created by peter on 30/11/17.
 */

public class Importer {
    private static final String ns = null;
    private static final int BUFFER = 1024;

    public static Album importAlbumFromZip(ZipFile zipFile, File storageRoot) throws IOException
    {
        // Read album.xml file and construct the album objects
        ZipEntry albumXml = zipFile.getEntry("album.xml");
        if (albumXml == null)
        {
            throw new IOException("Cannot file album.xml inside zip file"); // TODO better exception
        }
        try {
            InputStream albumXmlInputStream = zipFile.getInputStream(albumXml);
            Importer importer = new Importer();
            Album album = importer.parseAlbumXml(albumXmlInputStream);

            // Unzip all the images to an internal folder
            File albumDir = new File(storageRoot, album.getName());
            albumDir.mkdirs();
            Enumeration zipEntryEnumeration = zipFile.entries();
            byte data[] = new byte[BUFFER];

            while (zipEntryEnumeration.hasMoreElements())
            {
                ZipEntry zipEntry = (ZipEntry)zipEntryEnumeration.nextElement();
                if (zipEntry.getName().startsWith("images/") && !zipEntry.isDirectory()) {
                    // Extract this image file
                    String filename = zipEntry.getName().substring(zipEntry.getName().lastIndexOf(File.separator));
                    File destination = new File(albumDir, filename);

                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    BufferedInputStream bis = new BufferedInputStream(inputStream, BUFFER);

                    FileOutputStream fileOutput = new FileOutputStream(destination);
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutput, BUFFER);

                    while (bis.read(data, 0, BUFFER) != -1) {
                        bos.write(data);
                    }

                    bis.close();
                    inputStream.close();
                    fileOutput.close();
                    bos.close();

                    Log.v("Import", "Imported image "+filename);
                }
            }

            // Update the image source directories
            // TODO: The album should have a baseDir or something similar
            for (Slide slide : album.getSlides()) {
                if (slide instanceof ImageSlide) {
                    ImageSlide is = (ImageSlide)slide;
                    String filename = is.getImagePath();
                    File file = new File(albumDir, filename);
                    is.setImagePath(file.getAbsolutePath());
                }
            }

            return album;

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Album parseAlbumXml(InputStream in) throws XmlPullParserException, IOException
    {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readAlbum(parser);
        } finally {
            in.close();
        }

    }

    private Album readAlbum(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "album");
        Album album = new Album();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("name")) {
                album.setName(readName(parser));
            } else if (name.equals("slides")) {
                readSlides(parser, album);
            } else {
                skip(parser);
            }
        }
        return album;

    }

    private void readSlides(XmlPullParser parser, Album album) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "slides");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("slide")) {
                album.addSlide(readSlide(parser));
            } else {
                skip(parser);
            }
        }
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String src = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return src;
    }

    private Slide readSlide(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, "slide");
        String src = null;
        String type = parser.getAttributeValue(null, "type");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("src")) {
                src = readSrc(parser);
            } else {
                skip(parser);
            }
        }


        if (type.equals("image")) {
            ImageSlide is = new ImageSlide();
            is.setImagePath(src);
            return is;
        } else {
            return null;
        }


    }

    private String readSrc(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "src");
        String src = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "src");
        return src;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
