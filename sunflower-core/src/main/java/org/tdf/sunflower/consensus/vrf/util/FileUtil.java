package org.tdf.sunflower.consensus.vrf.util;

import java.io.*;
import java.util.Properties;

/**
 * Created by mawenpeng on 2016/10/24.
 */
public class FileUtil {
    //    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    public static String readTxtFile(String filePath) throws IOException {
        return readTxtFile(filePath, "UTF-8");
    }

    public static String readTxtFile(String filePath, String encoding) throws IOException {
        InputStreamReader read = null;
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            fis = new FileInputStream(file);

            read = new InputStreamReader(fis, encoding);
            BufferedReader bufferedReader = new BufferedReader(read);
            String results = "";
            String lineTxt = null;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                results += lineTxt;
            }
            fis.close();
            read.close();
            return results;
        } catch (IOException e) {
            throw e;
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (read != null) {
                read.close();
            }
        }
    }

    public static void writeTxtFile(String str, String filePath, boolean append, String encoding) throws IOException {
        OutputStreamWriter osw = null;
        FileOutputStream fos = null;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(filePath);
            fos = new FileOutputStream(file, append);

            osw = new OutputStreamWriter(fos, encoding);
            bufferedWriter = new BufferedWriter(osw);
            bufferedWriter.write(str);
            fos.flush();
            bufferedWriter.flush();
            bufferedWriter.close();
            fos.close();
            osw.close();

        } catch (IOException e) {
            throw e;
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (osw != null) {
                osw.close();
            }
        }
    }

    public static Properties readProperty(String filePath) throws IOException {
        Properties info = new Properties();
        FileInputStream fis = new FileInputStream(filePath);
        info.load(fis);
        return info;
    }

    public static String getProperty(String filePath, String propertyName) throws IOException {
        Properties info = new Properties();
        FileInputStream fis = new FileInputStream(filePath);
        info.load(fis);
        return info.getProperty(propertyName);
    }

    public static String getProperty(String propertyName) throws IOException {
        return getProperty(
                System.getProperty("user.dir") + File.separator + "config" + File.separator + "EthService.properties",
                propertyName);
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
    }
}
