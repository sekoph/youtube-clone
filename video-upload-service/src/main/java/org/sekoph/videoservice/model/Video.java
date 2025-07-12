package org.sekoph.videoservice.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.sekoph.videoservice.model.enums.VideoStatus;
import org.sekoph.videoservice.model.enums.VisibleStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Document(collection="videos")
public class Video {
    @Id
    private String id;

//    @Indexed
    private String title;

    private String description;

    // uploaderID
    @Field("user_id")
//    @Indexed
    private UUID userID;

    @Field("original_filename")
    private String originalFilename;

    @Field("video_status")
    @Enumerated(EnumType.STRING)
//    @Indexed
    private VideoStatus videoStatus;

    @Field("video_s3_key")
    private String videoS3Key;

    private VisibleStatus visibility;

    private Integer views;

    @NotNull
    @Min(0)
    private long duration;

    private List<VideoSegment> segments = new ArrayList<>();

    private List<VideoFrame> keyFrames = new ArrayList<>();

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_date")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    // soft delete items
    private boolean deleted;
}
