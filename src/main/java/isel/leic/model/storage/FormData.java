package isel.leic.model.storage;

import java.io.File;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

public class FormData {

    @RestForm("file")
    public File data;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public String filename;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public String mimetype;


    public File getData() {
        return data;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimetype() {
        return mimetype;
    }
}