package club.mydlq.swagger.kubernetes.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 读取文件
     * read file
     *
     * @return 读取的文本内容
     */
    public static String readFile(String fileName) {
        Path path = Paths.get(fileName);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return reader.readLine();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 检查文件是否存在
     *
     * @return 检查结果
     */
    public static boolean checkFolderExist(String path){
        File file = new File(path);
        return file.exists();
    }

}
