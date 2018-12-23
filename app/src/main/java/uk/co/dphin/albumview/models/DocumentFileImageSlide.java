package uk.co.dphin.albumview.models;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import java.io.InputStream;
import java.util.Date;

public class DocumentFileImageSlide extends ImageSlide {
    public DocumentFileImageSlide(Context c) {
        super(c);
    }

    private DocumentFile file;

    public DocumentFile getFile()
    {
        return file;
    }

    public void setFile(DocumentFile f)
    {
        file = f;
    }

    public  String getFileName()
    {
        return file.getName();
    }

    public InputStream getFileContent()
    {
        return getContext().getContentResolver().openInputStream(file.getUri());
    }

    public Date getFileModifiedDate()
    {
        return new Date(getFile().lastModified());
    }
}
