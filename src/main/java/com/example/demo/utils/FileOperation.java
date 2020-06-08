package com.example.demo.utils;

import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FileOperation {

    public static String encodeFile(File file){
        byte[] fileContent = new byte[0];
        try {
            fileContent = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        return encodedString;
    }

    public static File decodeString(String encodedFile, String fileName){
        byte[] decodedBytes = Base64.getDecoder().decode(encodedFile);
        File file = new File("target\\classes\\static\\images\\" + fileName);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer.print("");
        writer.close();

        try {
            FileUtils.writeByteArrayToFile(file, decodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File convertMultipartFileToFile(MultipartFile multipartFile){
        String path = "target\\classes\\static\\images\\auxphoto.jpg";
        File file = new File(path);
        try {
            file.createNewFile();
            Path filepath = Paths.get(path);

            OutputStream os = Files.newOutputStream(filepath);
            os.write(multipartFile.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
