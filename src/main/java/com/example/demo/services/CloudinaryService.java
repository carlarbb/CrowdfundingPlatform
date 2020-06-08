package com.example.demo.services;

import com.cloudinary.utils.ObjectUtils;
import com.example.demo.classes.Campaign;
import com.example.demo.configuration.CloudinaryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {
    @Autowired
    CloudinaryConfig cloudc;

    public void addMultipleImagesToCloudinary(List<MultipartFile> photosMultipart, Campaign campaign, String currentUserId) throws IOException {
        for(MultipartFile photo : photosMultipart){
            String imgNameWithExtension = photo.getOriginalFilename();
            String imgName = imgNameWithExtension.substring(0, imgNameWithExtension.indexOf('.'));
            Map uploadResult = cloudc.upload(photo.getBytes(), ObjectUtils.asMap("public_id",
                    currentUserId + "/photos/" + imgName, "resource_type", "auto", "overwrite", true));

            campaign.getCampaignImages().add(uploadResult.get("url").toString());
        }
    }

    //adds a category image to Cloudinary
    public Map addCategoryImageToCloudinary(MultipartFile photoMultipart) throws IOException{
        String imgNameWithExtension = photoMultipart.getOriginalFilename();
        String imgName = imgNameWithExtension.substring(0, imgNameWithExtension.indexOf('.'));
        Map uploadResult = cloudc.upload(photoMultipart.getBytes(), ObjectUtils.asMap("public_id",
                "categories/" + imgName, "resource_type", "auto", "overwrite", true));
        return uploadResult;
    }

    public void addVideoToCloudinary(MultipartFile videoMultipart, Campaign campaign, String currentUserId) throws IOException {
        String videoNameWithExtension = videoMultipart.getOriginalFilename();
        String videoName = videoNameWithExtension.substring(0, videoNameWithExtension.indexOf('.'));

        Map uploadVideo = cloudc.upload(videoMultipart.getBytes(), ObjectUtils.asMap( "public_id",
                currentUserId + "/videos/" + videoName, "resource_type", "auto", "overwrite", true));
        campaign.setCampaignVideo(uploadVideo.get("url").toString());
    }

    //sterge un fisier de orice tip din Cloudinary (fisier al unei campanii), in functie de id-ul sau public
    public boolean deleteFileFromCloudinary(String fileDbUrl, String userId, String fileType){
        //public_id-ul unui fisier din Cloudinary nu contine extensia
        String publicId = fileDbUrl.substring(fileDbUrl.indexOf(userId));
        publicId = publicId.substring(0, publicId.indexOf('.'));
        Map deleteMap;
        if(fileType.equals("image")) {
            deleteMap = cloudc.delete(publicId, ObjectUtils.emptyMap());
        }else {
            deleteMap = cloudc.delete(publicId, ObjectUtils.asMap("resource_type", "video"));
        }
        if(deleteMap.get("result").equals("ok")) return true;
        return false;
    }

    public Map deleteImageByIdFromCloudinary(String publicId){
        return cloudc.delete(publicId, ObjectUtils.emptyMap());
    }
}
