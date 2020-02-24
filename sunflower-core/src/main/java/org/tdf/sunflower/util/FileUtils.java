package org.tdf.sunflower.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.tdf.sunflower.exception.ApplicationException;

import java.io.File;
import java.net.URL;

public class FileUtils {
    public static Resource getResource(String pathOrUrl) {
        try{
            URL url = new URL(pathOrUrl);
            return new UrlResource(url);
        }catch (Exception ignored){

        }
        Resource resource = new FileSystemResource(pathOrUrl);
        if (!resource.exists()) {
            resource = new ClassPathResource(pathOrUrl);
        }
        if (!resource.exists()) {
            throw new ApplicationException("resource " + pathOrUrl + " not found");
        }
        return resource;
    }

    public static boolean recursiveDelete(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            //check if the file is a directory
            if (file.isDirectory()) {
                if ((file.list()).length > 0) {
                    for(String s:file.list()){
                        //call deletion of file individually
                        recursiveDelete(fileName + System.getProperty("file.separator") + s);
                    }
                }
            }

            file.setWritable(true);
            boolean result = file.delete();
            return result;
        } else {
            return false;
        }
    }
}
