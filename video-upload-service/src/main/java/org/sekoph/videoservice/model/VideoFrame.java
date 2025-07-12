package org.sekoph.videoservice.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class VideoFrame {
    @Field("frame_id")
    private String frameID;
    @Field("frame_number")
    private Long frameNumber;
    @Field("time_stump")
    private Long timeStamp;
    @Field("frame_s3_key")
    private String frameS3Key;
    @Field("is_key_frame")
    private Boolean isKeyFrame;
    @Field("frame_type")
    private String frameType; // I-frame, P-frame, B-frame
    private String quality;
    @Field("file_size")
    private Long fileSize;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    // Thumbnail information
    @Field("thumbnail_s3_key")
    private String thumbnailS3Key;
    private Integer width;
    private Integer height;

//    public String getFrameID() {
//        return frameID;
//    }
//
//    public void setFrameID(String frameID) {
//        this.frameID = frameID;
//    }
//
//    public Long getFrameNumber() {
//        return frameNumber;
//    }
//
//    public void setFrameNumber(Long frameNumber) {
//        this.frameNumber = frameNumber;
//    }
//
//    public Long getTimeStamp() {
//        return timeStamp;
//    }
//
//    public void setTimeStamp(Long timeStamp) {
//        this.timeStamp = timeStamp;
//    }
//
//    public String getFrameS3Key() {
//        return frameS3Key;
//    }
//
//    public void setFrameS3Key(String frameS3Key) {
//        this.frameS3Key = frameS3Key;
//    }
//
//    public Boolean getKeyFrame() {
//        return isKeyFrame;
//    }
//
//    public void setKeyFrame(Boolean keyFrame) {
//        isKeyFrame = keyFrame;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public String getFrameType() {
//        return frameType;
//    }
//
//    public void setFrameType(String frameType) {
//        this.frameType = frameType;
//    }
//
//    public String getQuality() {
//        return quality;
//    }
//
//    public void setQuality(String quality) {
//        this.quality = quality;
//    }
//
//    public Long getFileSize() {
//        return fileSize;
//    }
//
//    public void setFileSize(Long fileSize) {
//        this.fileSize = fileSize;
//    }
//
//    public String getThumbnailS3Key() {
//        return thumbnailS3Key;
//    }
//
//    public void setThumbnailS3Key(String thumbnailS3Key) {
//        this.thumbnailS3Key = thumbnailS3Key;
//    }
//
//    public Integer getWidth() {
//        return width;
//    }
//
//    public void setWidth(Integer width) {
//        this.width = width;
//    }
//
//    public Integer getHeight() {
//        return height;
//    }
//
//    public void setHeight(Integer height) {
//        this.height = height;
//    }
}
